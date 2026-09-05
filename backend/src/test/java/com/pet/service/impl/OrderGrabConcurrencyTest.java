package com.pet.service.impl;

import com.pet.common.api.ResultCode;
import com.pet.common.enums.OrderStatus;
import com.pet.common.exception.BusinessException;
import com.pet.common.geo.OrderGeoIndex;
import com.pet.common.lock.DistributedLock;
import com.pet.entity.Order;
import com.pet.mapper.OrderMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.SitterProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 锁死抢单的并发正确性：一单只能有一个 sitter_id。
 * <p>
 * 抢单是本项目唯一「多人同时争一条记录」的地方，超卖的后果不是数据难看而是真金白银——
 * 两个接单员都以为自己接了单，一个上门发现没单，验收结算时还会给两个人各入账一次。
 * 纯 Mockito，不启 Spring 上下文，不依赖本机 MySQL / Redis。
 */
@ExtendWith(MockitoExtension.class)
class OrderGrabConcurrencyTest {

    private static final long ORDER_ID = 77L;
    private static final int RACING_SITTERS = 20;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SitterProfileService sitterProfileService;

    @Mock
    private OrderGeoIndex geoIndex;

    @Mock
    private DistributedLock lock;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /**
     * 完全不加锁的 DistributedLock：直接执行动作，等于「锁整体失效」的最坏情况。
     * <p>
     * 用它而不是 mock，是为了让并发路径上完全没有 Mockito 的同步开销；
     * 同时也是在验证一件事——正确性不能只指望锁，{@code markTaken} 的
     * {@code WHERE status = 1} 条件更新才是最后一道防线（锁在事务提交前就释放了，本来就指望不住）。
     */
    private static class NoOpLock extends DistributedLock {

        NoOpLock() {
            super((RedissonClient) null);
        }

        @Override
        public <T> T tryLockAndRun(String key, long waitTime, long leaseTime, Supplier<T> action) {
            return action.get();
        }
    }

    /** 前五个依赖在抢单路径上用不到，全传 null；用得到的三个才给 mock。 */
    private OrderServiceImpl newService(DistributedLock grabLock) {
        OrderServiceImpl service = new OrderServiceImpl(
                null, null, null, null, null, sitterProfileService, geoIndex, grabLock);
        // baseMapper 是 ServiceImpl 的父类字段，@InjectMocks 注不进去，只能反射塞
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        return service;
    }

    private void loginAsSitter(long sitterId) {
        UserContext.set(new LoginUser(sitterId, "sitter" + sitterId, "SITTER"));
    }

    /**
     * 用 AtomicInteger 模拟 MySQL 的 {@code UPDATE ... WHERE status = 1}：
     * 只有第一个把状态从「待接单」推到「已接单」的调用返回 1，其余全部返回 0。
     *
     * @return 抢到单的接单员 id（0 表示没人抢到）
     */
    private AtomicLong stubConditionalMarkTaken(AtomicInteger status) {
        AtomicLong winner = new AtomicLong(0);
        when(orderMapper.markTaken(eq(ORDER_ID), anyLong())).thenAnswer(invocation -> {
            if (status.compareAndSet(OrderStatus.PENDING.getCode(), OrderStatus.TAKEN.getCode())) {
                winner.set(invocation.getArgument(1));
                return 1;
            }
            return 0;
        });
        return winner;
    }

    /** selectById 必须反映最新状态：抢单失败后要靠重读区分「被别人抢了」和「被主人取消了」。 */
    private void stubOrderSnapshot(AtomicInteger status, AtomicLong winner) {
        when(orderMapper.selectById(ORDER_ID)).thenAnswer(invocation -> {
            Order order = new Order();
            order.setId(ORDER_ID);
            order.setStatus(status.get());
            order.setSitterId(winner.get() == 0 ? null : winner.get());
            return order;
        });
    }

    @Test
    @DisplayName("20 人同时抢一单：即使锁完全失效，也只有 1 人成功，其余全部收到「已被抢」")
    void onlyOneOfTwentyRacingSittersWins() throws InterruptedException {
        AtomicInteger status = new AtomicInteger(OrderStatus.PENDING.getCode());
        AtomicLong winner = stubConditionalMarkTaken(status);
        stubOrderSnapshot(status, winner);
        OrderServiceImpl service = newService(new NoOpLock());

        List<Integer> codes = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(RACING_SITTERS);
        CountDownLatch ready = new CountDownLatch(RACING_SITTERS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(RACING_SITTERS);
        try {
            for (int i = 0; i < RACING_SITTERS; i++) {
                long sitterId = 100L + i;
                pool.execute(() -> {
                    // UserContext 是 ThreadLocal，每个线程必须自己塞身份，否则会读到 null 直接 401
                    loginAsSitter(sitterId);
                    ready.countDown();
                    try {
                        start.await();
                        service.grab(ORDER_ID);
                        codes.add(ResultCode.SUCCESS.getCode());
                    } catch (BusinessException e) {
                        codes.add(e.getCode());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        UserContext.clear();
                        finished.countDown();
                    }
                });
            }
            // 等所有线程就位再放行，确保它们是真正同时撞上 markTaken，而不是排队依次执行
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(codes).hasSize(RACING_SITTERS);
        assertThat(codes).filteredOn(c -> c == ResultCode.SUCCESS.getCode()).hasSize(1);
        assertThat(codes)
                .filteredOn(c -> c == ResultCode.ORDER_ALREADY_TAKEN.getCode())
                .hasSize(RACING_SITTERS - 1);
        assertThat(winner.get()).isBetween(100L, 100L + RACING_SITTERS);

        verify(orderMapper, times(RACING_SITTERS)).markTaken(eq(ORDER_ID), anyLong());
        // 索引只能被移除一次：每个失败者都去 remove 一次虽然无害，但说明失败路径多做了一次 Redis 往返
        verify(geoIndex, times(1)).remove(ORDER_ID);
    }

    @Test
    @DisplayName("抢单锁按订单 id 分开，两单不会互相排队")
    void grabLocksPerOrder() {
        AtomicInteger status = new AtomicInteger(OrderStatus.PENDING.getCode());
        AtomicLong winner = stubConditionalMarkTaken(status);
        stubOrderSnapshot(status, winner);
        loginAsSitter(100L);
        // 让 mock 真的去执行动作，否则 grab 会因为拿到 null 而误判成「没抢到锁」
        when(lock.tryLockAndRun(anyString(), anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());

        newService(lock).grab(ORDER_ID);

        verify(lock).tryLockAndRun(eq("order:grab:" + ORDER_ID), anyLong(), anyLong(), any());
        verify(geoIndex).remove(ORDER_ID);
        assertThat(winner.get()).isEqualTo(100L);
    }

    @Test
    @DisplayName("没抢到锁按「订单已被抢」返回，且不动索引")
    void lockContentionIsReportedAsAlreadyTaken() {
        loginAsSitter(100L);
        when(lock.tryLockAndRun(anyString(), anyLong(), anyLong(), any())).thenReturn(null);

        assertThatThrownBy(() -> newService(lock).grab(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_ALREADY_TAKEN.getCode()));

        verify(geoIndex, never()).remove(any());
    }

    /**
     * 资质校验放在锁外只是性能考虑，不能因此变成可选步骤：
     * 前端隐藏按钮拦得住误点，拦不住直接调接口的人。
     */
    @Test
    @DisplayName("资质未过审的接单员连锁都不该碰到")
    void unauditedSitterCannotGrab() {
        loginAsSitter(100L);
        doThrow(new BusinessException(ResultCode.SITTER_NOT_AUDITED))
                .when(sitterProfileService).requireGrabable(100L);

        assertThatThrownBy(() -> newService(lock).grab(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.SITTER_NOT_AUDITED.getCode()));

        verifyNoInteractions(lock, geoIndex);
        verify(orderMapper, never()).markTaken(any(), any());
    }

    @Test
    @DisplayName("订单不存在返回 2001，而不是含混的「状态不允许」")
    void missingOrderIsNotFound() {
        loginAsSitter(100L);
        when(lock.tryLockAndRun(anyString(), anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> newService(lock).grab(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_NOT_FOUND.getCode()));

        verify(orderMapper, never()).markTaken(any(), any());
        verify(geoIndex, never()).remove(any());
    }

    /**
     * 主人在接单员点下抢单的同一瞬间取消了订单：markTaken 同样返回 0，
     * 但报「已被抢」会让接单员以为是自己手慢，反复刷新重试；报「状态不允许」才是事实。
     */
    @Test
    @DisplayName("订单已被取消时报「状态不允许」，与「被人抢走」区分开")
    void cancelledOrderIsStatusIllegal() {
        loginAsSitter(100L);
        when(lock.tryLockAndRun(anyString(), anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(orderMapper.markTaken(ORDER_ID, 100L)).thenReturn(0);
        Order stillPending = new Order();
        stillPending.setId(ORDER_ID);
        stillPending.setStatus(OrderStatus.PENDING.getCode());
        Order cancelled = new Order();
        cancelled.setId(ORDER_ID);
        cancelled.setStatus(OrderStatus.CANCELLED.getCode());
        cancelled.setSitterId(null);
        // 第一次读是抢单前的存在性校验，第二次是 markTaken 失败后的归因重读
        when(orderMapper.selectById(ORDER_ID)).thenReturn(stillPending, cancelled);

        assertThatThrownBy(() -> newService(lock).grab(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_STATUS_ILLEGAL.getCode()));

        verify(geoIndex, never()).remove(any());
    }
}
