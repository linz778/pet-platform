package com.pet.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.EvidenceType;
import com.pet.common.enums.OrderStatus;
import com.pet.common.exception.BusinessException;
import com.pet.config.GeoProperties;
import com.pet.config.JacksonConfig;
import com.pet.dto.CheckInDTO;
import com.pet.dto.EvidenceSaveDTO;
import com.pet.dto.TrackPointDTO;
import com.pet.dto.TrackSaveDTO;
import com.pet.entity.Order;
import com.pet.entity.OrderEvidence;
import com.pet.mapper.OrderEvidenceMapper;
import com.pet.mapper.OrderMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.OrderEvidenceVO;
import com.pet.vo.ServiceCategoryVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 锁死履约存证的三道门控。
 * <p>
 * 存证是用户验收时唯一的凭据，这里的每一条都是「凭据可信度」防线：打卡距离放宽就等于允许远程打卡，
 * 清单项不做服务端比对就等于允许接单员自造一项「已完成」糊弄验收，清单没拍全就能 finish
 * 则让用户在验收页看到一份残缺的证据链。前端禁用按钮拦得住误点，拦不住直接调接口的人。
 * 纯 Mockito，不启 Spring 上下文，不依赖本机 MySQL / Redis。
 */
@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {

    private static final long ORDER_ID = 77L;
    private static final long SITTER_ID = 3L;
    private static final long OWNER_ID = 2L;
    private static final long ADMIN_ID = 1L;
    private static final long CATEGORY_ID = 1L;

    /** 种子数据里「上门喂养」的作业清单，顺序即模板顺序，缺项提示按它排 */
    private static final List<String> FEEDING_CHECKLIST = List.of("换粮", "添水", "铲砂", "梳毛", "陪玩");

    /** 订单的服务地址，与种子数据里的人民广场坐标一致 */
    private static final BigDecimal ADDRESS_LAT = new BigDecimal("31.2304");
    private static final BigDecimal ADDRESS_LNG = new BigDecimal("121.4737");

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderEvidenceMapper evidenceMapper;

    @Mock
    private ServiceCategoryService serviceCategoryService;

    /** 用默认值 200 米：打卡门限是可配置项，测试要锁的是「超限就拒绝」这个行为而不是某个具体数字 */
    private final GeoProperties geoProperties = new GeoProperties();

    private FulfillmentServiceImpl service;

    /**
     * lambdaUpdate 的 {@code .set(列, 值)} 会立刻解析列名（{@code .eq()} 是延迟到生成 SQL 时才解析，
     * 所以纯 mock 下不用管），解析要走 MP 的 lambda 缓存，而缓存只在 mapper 注册时才建。
     * 单测里没有 MyBatis 环境，不手工初始化就会抛「can not find lambda cache for this entity」。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(OrderEvidenceMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, OrderEvidence.class);
    }

    @BeforeEach
    void setUp() {
        service = new FulfillmentServiceImpl(orderMapper, serviceCategoryService, geoProperties, objectMapper());
        // baseMapper 是 ServiceImpl 的父类字段，@InjectMocks 注不进去，只能反射塞
        ReflectionTestUtils.setField(service, "baseMapper", evidenceMapper);
        loginAs(SITTER_ID, "SITTER");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /** 裸 new ObjectMapper() 不认识 LocalDateTime，writeTrack 会直接抛异常变成 400，轨迹相关的断言就全是假的。 */
    private static ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().localDateTimeCustomizer().customize(builder);
        return builder.build();
    }

    private void loginAs(long userId, String role) {
        UserContext.set(new LoginUser(userId, role.toLowerCase() + userId, role));
    }

    /** 造一条已被当前登录接单员抢到、停在指定状态的订单 */
    private Order stubOrder(int status, Long sitterId) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(OWNER_ID);
        order.setCategoryId(CATEGORY_ID);
        order.setStatus(status);
        order.setSitterId(sitterId);
        order.setAddressLat(ADDRESS_LAT);
        order.setAddressLng(ADDRESS_LNG);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        return order;
    }

    private void stubInServiceOrder() {
        stubOrder(OrderStatus.IN_SERVICE.getCode(), SITTER_ID);
    }

    private void stubChecklist() {
        ServiceCategoryVO category = new ServiceCategoryVO();
        category.setChecklist(FEEDING_CHECKLIST);
        when(serviceCategoryService.getDetail(CATEGORY_ID)).thenReturn(category);
    }

    /** 库里已有的 type=2 存证项，用于模拟「清单拍到第几项」 */
    private void stubDoneItems(String... items) {
        when(evidenceMapper.selectList(any())).thenReturn(List.of(items).stream().map(item -> {
            OrderEvidence e = new OrderEvidence();
            e.setOrderId(ORDER_ID);
            e.setType(EvidenceType.CHECKLIST.getCode());
            e.setCheckItem(item);
            return e;
        }).toList());
    }

    private OrderEvidence capturedInsert() {
        ArgumentCaptor<OrderEvidence> captor = ArgumentCaptor.forClass(OrderEvidence.class);
        verify(evidenceMapper).insert(captor.capture());
        return captor.getValue();
    }

    private void assertRejected(int expectedCode, ThrowingCall call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(expectedCode));
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }

    private CheckInDTO checkInAt(String lat, String lng) {
        CheckInDTO dto = new CheckInDTO();
        dto.setLat(new BigDecimal(lat));
        dto.setLng(new BigDecimal(lng));
        return dto;
    }

    private EvidenceSaveDTO evidenceOf(String checkItem) {
        EvidenceSaveDTO dto = new EvidenceSaveDTO();
        dto.setCheckItem(checkItem);
        dto.setImageUrl("/uploads/evidence/" + checkItem + ".jpg");
        return dto;
    }

    // ───────────────────────── 定位打卡 ─────────────────────────

    @Test
    @DisplayName("范围内打卡：推进到服务中，并存一条带距离的 type=1 存证")
    void checkInWithinRadiusAdvancesStatusAndStoresEvidence() {
        stubOrder(OrderStatus.TAKEN.getCode(), SITTER_ID);
        when(orderMapper.markCheckedIn(ORDER_ID)).thenReturn(1);

        // 距服务地址约 73 米，落在默认 200 米内
        service.checkIn(ORDER_ID, checkInAt("31.2310", "121.4740"));

        verify(orderMapper).markCheckedIn(ORDER_ID);
        OrderEvidence evidence = capturedInsert();
        assertThat(evidence.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(evidence.getSitterId()).isEqualTo(SITTER_ID);
        assertThat(evidence.getType()).isEqualTo(EvidenceType.CHECK_IN.getCode());
        assertThat(evidence.getLat()).isEqualByComparingTo("31.2310");
        assertThat(evidence.getLng()).isEqualByComparingTo("121.4740");
        // 距离写进备注，用户验收时能看出这次打卡到底准不准
        assertThat(evidence.getRemark()).matches("距服务地址约 \\d+ 米");
        assertThat(Integer.parseInt(evidence.getRemark().replaceAll("\\D", "")))
                .isBetween(1, geoProperties.getCheckInRadius());
    }

    /**
     * 距离校验必须在 markCheckedIn 之前：反过来的话，一个在城市另一头的接单员也能把订单推到
     * 「服务中」，然后靠报错重试慢慢挪近，状态机已经脏了。
     */
    @Test
    @DisplayName("超出打卡半径拒绝，且状态与存证都不动")
    void checkInTooFarIsRejectedBeforeTouchingStatus() {
        stubOrder(OrderStatus.TAKEN.getCode(), SITTER_ID);

        assertRejected(ResultCode.GEO_CHECK_IN_FAILED.getCode(),
                () -> service.checkIn(ORDER_ID, checkInAt("31.3000", "121.6000")));

        verify(orderMapper, never()).markCheckedIn(anyLong());
        verify(evidenceMapper, never()).insert(any(OrderEvidence.class));
    }

    @Test
    @DisplayName("重复打卡被条件更新挡住：影响行数 0 就不留第二条存证")
    void checkInIsIdempotentViaConditionalUpdate() {
        stubOrder(OrderStatus.TAKEN.getCode(), SITTER_ID);
        when(orderMapper.markCheckedIn(ORDER_ID)).thenReturn(0);

        assertRejected(ResultCode.ORDER_STATUS_ILLEGAL.getCode(),
                () -> service.checkIn(ORDER_ID, checkInAt("31.2310", "121.4740")));

        verify(evidenceMapper, never()).insert(any(OrderEvidence.class));
    }

    @Test
    @DisplayName("别人的单不能打卡，回 2005 而不是 2003")
    void checkInOnAnotherSittersOrderIsDenied() {
        stubOrder(OrderStatus.TAKEN.getCode(), 999L);

        assertRejected(ResultCode.ORDER_ACCESS_DENIED.getCode(),
                () -> service.checkIn(ORDER_ID, checkInAt("31.2310", "121.4740")));

        verify(orderMapper, never()).markCheckedIn(anyLong());
    }

    /** 没人抢的单 sitterId 为 null，equals 自然是 false —— 不能因此变成 NPE 或 500 */
    @Test
    @DisplayName("订单还没被抢（sitterId 为空）时打卡同样回 2005")
    void checkInOnUntakenOrderIsDenied() {
        stubOrder(OrderStatus.PENDING.getCode(), null);

        assertRejected(ResultCode.ORDER_ACCESS_DENIED.getCode(),
                () -> service.checkIn(ORDER_ID, checkInAt("31.2310", "121.4740")));

        verify(orderMapper, never()).markCheckedIn(anyLong());
    }

    @Test
    @DisplayName("订单不存在返回 2001")
    void missingOrderIsNotFound() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

        assertRejected(ResultCode.ORDER_NOT_FOUND.getCode(),
                () -> service.checkIn(ORDER_ID, checkInAt("31.2310", "121.4740")));

        verifyNoInteractions(evidenceMapper);
    }

    // ───────────────────────── 作业清单存证 ─────────────────────────

    @Test
    @DisplayName("未到「服务中」不能传清单照片")
    void checklistEvidenceRequiresInServiceStatus() {
        stubOrder(OrderStatus.TAKEN.getCode(), SITTER_ID);

        assertRejected(ResultCode.ORDER_STATUS_ILLEGAL.getCode(),
                () -> service.saveChecklistEvidence(ORDER_ID, evidenceOf("换粮")));

        verifyNoInteractions(serviceCategoryService, evidenceMapper);
    }

    /**
     * 清单项由服务端比对。让接单员自由填一个「已完成」，用户验收时看到的清单就不再是
     * 平台承诺的那份标准作业流程，存证也就失去了对照意义。
     */
    @Test
    @DisplayName("清单项不在该服务的模板里就拒绝，并在提示里点出是哪一项")
    void checkItemOutsideCategoryChecklistIsRejected() {
        stubInServiceOrder();
        stubChecklist();

        assertThatThrownBy(() -> service.saveChecklistEvidence(ORDER_ID, evidenceOf("已喂过")))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
                    assertThat(e.getMessage()).contains("已喂过").contains("不在该服务的作业清单里");
                });

        verify(evidenceMapper, never()).insert(any(OrderEvidence.class));
    }

    @Test
    @DisplayName("首次拍照插入一条 type=2 存证")
    void firstPhotoOfAnItemInsertsChecklistEvidence() {
        stubInServiceOrder();
        stubChecklist();
        when(evidenceMapper.selectOne(any())).thenReturn(null);
        EvidenceSaveDTO dto = evidenceOf("换粮");
        dto.setLat(new BigDecimal("31.2304"));
        dto.setRemark("狗粮已按 40g 补满");

        OrderEvidenceVO vo = service.saveChecklistEvidence(ORDER_ID, dto);

        OrderEvidence evidence = capturedInsert();
        assertThat(evidence.getType()).isEqualTo(EvidenceType.CHECKLIST.getCode());
        assertThat(evidence.getCheckItem()).isEqualTo("换粮");
        assertThat(evidence.getImageUrl()).isEqualTo(dto.getImageUrl());
        assertThat(evidence.getLat()).isEqualByComparingTo("31.2304");
        assertThat(evidence.getRemark()).isEqualTo("狗粮已按 40g 补满");

        assertThat(vo.getTypeText()).isEqualTo(EvidenceType.CHECKLIST.getDesc());
        assertThat(vo.getCheckItem()).isEqualTo("换粮");
    }

    /**
     * 重拍同一项走覆盖而不是再插一条：插重复行会让验收页出现两张同名照片，
     * 用户分不清哪张是最终的。覆盖必须显式 set 每一列——默认字段策略会跳过 null，
     * 这次不带定位就会把上一次的坐标留在库里。
     */
    @Test
    @DisplayName("同一项重拍覆盖旧存证，不产生重复行")
    void retakingTheSameItemOverwritesInsteadOfDuplicating() {
        stubInServiceOrder();
        stubChecklist();
        OrderEvidence existing = new OrderEvidence();
        existing.setId(5L);
        existing.setOrderId(ORDER_ID);
        existing.setType(EvidenceType.CHECKLIST.getCode());
        existing.setCheckItem("换粮");
        existing.setImageUrl("/uploads/evidence/old.jpg");
        when(evidenceMapper.selectOne(any())).thenReturn(existing);
        when(evidenceMapper.selectById(5L)).thenReturn(existing);

        OrderEvidenceVO vo = service.saveChecklistEvidence(ORDER_ID, evidenceOf("换粮"));

        verify(evidenceMapper).update(isNull(), any());
        verify(evidenceMapper, never()).insert(any(OrderEvidence.class));
        assertThat(vo.getId()).isEqualTo(5L);
    }

    // ───────────────────────── 散步轨迹 ─────────────────────────

    /**
     * track_json 是入库再读出的往返，序列化用的是 Spring 配好的 ObjectMapper：
     * 时间必须落成 {@code yyyy-MM-dd HH:mm:ss}，否则前端画轨迹时解析不出时间轴。
     * 缺时间的点由服务端补，否则那个点在图上是 null，整条线断掉。
     */
    @Test
    @DisplayName("轨迹按约定格式入库，读回来点数、坐标、时间都对得上")
    void trackRoundTripsThroughTrackJson() {
        stubInServiceOrder();
        TrackSaveDTO dto = new TrackSaveDTO();
        TrackPointDTO timed = new TrackPointDTO();
        timed.setLat(new BigDecimal("31.2304"));
        timed.setLng(new BigDecimal("121.4737"));
        timed.setTime(LocalDateTime.of(2026, 9, 5, 9, 30, 0));
        TrackPointDTO untimed = new TrackPointDTO();
        untimed.setLat(new BigDecimal("31.2350"));
        untimed.setLng(new BigDecimal("121.4800"));
        dto.setPoints(List.of(timed, untimed));

        OrderEvidenceVO vo = service.saveTrack(ORDER_ID, dto);

        OrderEvidence evidence = capturedInsert();
        assertThat(evidence.getType()).isEqualTo(EvidenceType.TRACK.getCode());
        assertThat(evidence.getTrackJson())
                .contains("\"time\":\"2026-09-05 09:30:00\"")
                .doesNotContain("T09:30:00");

        assertThat(vo.getTrackPoints()).hasSize(2);
        assertThat(vo.getTrackPoints().get(0).getLat()).isEqualByComparingTo("31.2304");
        assertThat(vo.getTrackPoints().get(0).getTime()).isEqualTo(LocalDateTime.of(2026, 9, 5, 9, 30, 0));
        assertThat(vo.getTrackPoints().get(1).getTime()).isNotNull();
    }

    /** track_json 是库里的自由文本列，读到脏数据只丢这一条轨迹，不能让整个存证列表 500 */
    @Test
    @DisplayName("轨迹 JSON 损坏时降级为空点集，存证列表照常返回")
    void corruptTrackJsonDegradesToEmptyPoints() {
        stubOrder(OrderStatus.PENDING_ACCEPT.getCode(), SITTER_ID);
        OrderEvidence broken = new OrderEvidence();
        broken.setId(9L);
        broken.setOrderId(ORDER_ID);
        broken.setType(EvidenceType.TRACK.getCode());
        broken.setTrackJson("{oops");
        when(evidenceMapper.selectList(any())).thenReturn(List.of(broken));

        List<OrderEvidenceVO> list = service.listEvidence(ORDER_ID);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTrackPoints()).isEmpty();
    }

    // ───────────────────────── 完成服务 ─────────────────────────

    @Test
    @DisplayName("清单没拍全不能完成服务，提示里点名缺哪几项")
    void finishWithoutFullChecklistNamesTheMissingItems() {
        stubInServiceOrder();
        stubChecklist();
        stubDoneItems("换粮", "添水");

        assertThatThrownBy(() -> service.finish(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ResultCode.EVIDENCE_REQUIRED.getCode());
                    // 只说「请先提交存证」接单员不知道该补哪几项
                    assertThat(e.getMessage()).contains("还有 3 项").contains("铲砂").contains("梳毛").contains("陪玩");
                });

        verify(orderMapper, never()).markFinished(anyLong());
    }

    @Test
    @DisplayName("清单齐全后完成服务，状态推进到待验收")
    void finishWithFullChecklistAdvancesStatus() {
        stubInServiceOrder();
        stubChecklist();
        stubDoneItems(FEEDING_CHECKLIST.toArray(new String[0]));
        when(orderMapper.markFinished(ORDER_ID)).thenReturn(1);

        service.finish(ORDER_ID);

        verify(orderMapper).markFinished(ORDER_ID);
    }

    @Test
    @DisplayName("重复点击完成被条件更新挡住，返回 2003")
    void finishIsIdempotentViaConditionalUpdate() {
        stubInServiceOrder();
        stubChecklist();
        stubDoneItems(FEEDING_CHECKLIST.toArray(new String[0]));
        when(orderMapper.markFinished(ORDER_ID)).thenReturn(0);

        assertRejected(ResultCode.ORDER_STATUS_ILLEGAL.getCode(), () -> service.finish(ORDER_ID));
    }

    // ───────────────────────── 存证可见性 ─────────────────────────

    /** 与 OrderServiceImpl#getDetail 同一套可见性：当事人双方 + 管理员 */
    @Test
    @DisplayName("下单用户与管理员都能看存证，与接单员视角一致")
    void evidenceIsVisibleToOwnerAndAdmin() {
        stubOrder(OrderStatus.PENDING_ACCEPT.getCode(), SITTER_ID);
        OrderEvidence evidence = new OrderEvidence();
        evidence.setId(9L);
        evidence.setOrderId(ORDER_ID);
        evidence.setType(EvidenceType.CHECK_IN.getCode());
        when(evidenceMapper.selectList(any())).thenReturn(List.of(evidence));

        loginAs(OWNER_ID, "USER");
        assertThat(service.listEvidence(ORDER_ID)).hasSize(1);

        loginAs(ADMIN_ID, "ADMIN");
        assertThat(service.listEvidence(ORDER_ID)).hasSize(1);
    }

    @Test
    @DisplayName("无关用户看存证回 2005，且不会去查存证表")
    void strangerCannotListEvidence() {
        stubOrder(OrderStatus.PENDING_ACCEPT.getCode(), SITTER_ID);
        loginAs(888L, "USER");

        assertRejected(ResultCode.ORDER_ACCESS_DENIED.getCode(), () -> service.listEvidence(ORDER_ID));

        verify(evidenceMapper, never()).selectList(any());
    }
}
