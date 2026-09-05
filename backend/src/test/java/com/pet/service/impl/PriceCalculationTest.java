package com.pet.service.impl;

import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.entity.ServiceCategory;
import com.pet.mapper.ServiceCategoryMapper;
import com.pet.vo.PricePreviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 锁死计价规则。
 * <p>
 * previewPrice 是全项目唯一的计价实现，下单与结算都复用它，所以这里的每一条断言
 * 实际上都是在保护资金正确性。全部用 Mockito，不启 Spring 上下文——不依赖本机
 * MySQL / Redis 是否可用，换台机器也能跑绿。
 */
@ExtendWith(MockitoExtension.class)
class PriceCalculationTest {

    /** 2026-09-05 周六、09-06 周日、09-09 周三 */
    private static final LocalDateTime SATURDAY = LocalDateTime.of(2026, 9, 5, 10, 0);
    private static final LocalDateTime SUNDAY = LocalDateTime.of(2026, 9, 6, 10, 0);
    private static final LocalDateTime WEDNESDAY = LocalDateTime.of(2026, 9, 9, 10, 0);

    @Mock
    private ServiceCategoryMapper categoryMapper;

    private ServiceCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ServiceCategoryServiceImpl();
        // ServiceImpl 的 baseMapper 平时由 Spring 注入，@InjectMocks 认不出这个父类 protected 字段，
        // 只能反射塞进去，否则 getById 会 NPE
        ReflectionTestUtils.setField(service, "baseMapper", categoryMapper);
    }

    private void stubCategory(String basePrice, String holidayRate, String commissionRate) {
        ServiceCategory category = new ServiceCategory();
        category.setId(1L);
        category.setName("上门喂养");
        category.setBasePrice(new BigDecimal(basePrice));
        category.setHolidayRate(new BigDecimal(holidayRate));
        category.setCommissionRate(new BigDecimal(commissionRate));
        category.setStatus(1);
        when(categoryMapper.selectById(anyLong())).thenReturn(category);
    }

    @Test
    @DisplayName("工作日不溢价：40 元、抽成 10% 得 4 元、接单员到手 36 元")
    void weekdayHasNoSurcharge() {
        stubCategory("40.00", "1.500", "0.100");

        PricePreviewVO price = service.previewPrice(1L, WEDNESDAY);

        assertThat(price.isHoliday()).isFalse();
        assertThat(price.getAmount()).isEqualByComparingTo("40.00");
        assertThat(price.getCommission()).isEqualByComparingTo("4.00");
        assertThat(price.getSitterIncome()).isEqualByComparingTo("36.00");
    }

    @Test
    @DisplayName("周六周日都按 holidayRate 溢价：40 × 1.5 = 60 元")
    void weekendAppliesHolidayRate() {
        stubCategory("40.00", "1.500", "0.100");

        assertThat(service.previewPrice(1L, SATURDAY).getAmount()).isEqualByComparingTo("60.00");
        assertThat(service.previewPrice(1L, SATURDAY).isHoliday()).isTrue();
        assertThat(service.previewPrice(1L, SUNDAY).getAmount()).isEqualByComparingTo("60.00");
        assertThat(service.previewPrice(1L, SUNDAY).isHoliday()).isTrue();
    }

    @Test
    @DisplayName("金额一律 2 位小数：33.33 × 1.5 = 49.995 四舍五入为 50.00")
    void amountIsRoundedToTwoDecimals() {
        stubCategory("33.33", "1.500", "0.100");

        PricePreviewVO price = service.previewPrice(1L, SATURDAY);

        assertThat(price.getAmount()).isEqualByComparingTo("50.00");
        assertThat(price.getAmount().scale()).isEqualTo(2);
    }

    /**
     * 这条用例是「减法而非各自四舍五入」的守门员。
     * <p>
     * 1.00 × 0.125 = 0.125，四舍五入得抽成 0.13。若到手收入也独立按 1.00 × 0.875 = 0.875
     * 四舍五入，会得到 0.88 —— 0.13 + 0.88 = 1.01，凭空多出一分钱，验收结算时的金额守恒
     * 校验必然失败，而失败点距离出错点隔了整个订单生命周期。用减法得 0.87 才守恒。
     */
    @Test
    @DisplayName("半分陷阱：sitterIncome 必须用减法，否则抽成与到手之和会多出 1 分")
    void sitterIncomeIsDerivedBySubtractionNotIndependentRounding() {
        stubCategory("1.00", "1.000", "0.125");

        PricePreviewVO price = service.previewPrice(1L, WEDNESDAY);

        assertThat(price.getCommission()).isEqualByComparingTo("0.13");
        assertThat(price.getSitterIncome()).isEqualByComparingTo("0.87");
        assertThat(price.getCommission().add(price.getSitterIncome()))
                .isEqualByComparingTo(price.getAmount());
    }

    @Test
    @DisplayName("各种费率组合下抽成与到手之和恒等于订单金额")
    void commissionPlusIncomeAlwaysEqualsAmount() {
        String[][] cases = {
                {"40.00", "1.500", "0.100"},
                {"80.00", "1.500", "0.120"},
                {"35.00", "1.300", "0.100"},
                {"55.00", "1.250", "0.135"},
                {"0.01", "1.500", "0.999"},
        };
        for (String[] c : cases) {
            stubCategory(c[0], c[1], c[2]);
            for (LocalDateTime time : new LocalDateTime[]{SATURDAY, WEDNESDAY}) {
                PricePreviewVO price = service.previewPrice(1L, time);
                assertThat(price.getCommission().add(price.getSitterIncome()))
                        .as("basePrice=%s holidayRate=%s commissionRate=%s time=%s", c[0], c[1], c[2], time)
                        .isEqualByComparingTo(price.getAmount());
            }
        }
    }

    @Test
    @DisplayName("已下架的服务不能计价，抛 CATEGORY_OFF_SHELF")
    void offShelfCategoryIsRejected() {
        ServiceCategory category = new ServiceCategory();
        category.setId(1L);
        category.setBasePrice(new BigDecimal("40.00"));
        category.setHolidayRate(new BigDecimal("1.500"));
        category.setCommissionRate(new BigDecimal("0.100"));
        category.setStatus(0);
        when(categoryMapper.selectById(anyLong())).thenReturn(category);

        assertThatThrownBy(() -> service.previewPrice(1L, SATURDAY))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CATEGORY_OFF_SHELF.getCode());
    }

    @Test
    @DisplayName("类别不存在抛 CATEGORY_NOT_FOUND，id 为空同样按不存在处理")
    void missingCategoryIsRejected() {
        when(categoryMapper.selectById(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> service.previewPrice(999L, SATURDAY))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CATEGORY_NOT_FOUND.getCode());

        assertThatThrownBy(() -> service.previewPrice(null, SATURDAY))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CATEGORY_NOT_FOUND.getCode());
    }
}
