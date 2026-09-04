package com.pet.common.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redisson 的分布式锁封装。
 * 典型场景：抢单防超卖——对同一订单加锁，保证只有一名接单员成功。
 */
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final RedissonClient redissonClient;

    /**
     * 尝试加锁执行；未抢到锁返回 null（由调用方转成业务提示，如“订单已被抢”）。
     *
     * @param key       锁 key，例如 order:grab:{orderId}
     * @param waitTime  等待获取锁的最长时间
     * @param leaseTime 持锁自动释放时间（防止死锁）
     */
    public <T> T tryLockAndRun(String key, long waitTime, long leaseTime, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!locked) {
                return null;
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
