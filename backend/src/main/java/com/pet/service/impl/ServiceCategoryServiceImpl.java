package com.pet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.common.util.CommaListUtil;
import com.pet.entity.ServiceCategory;
import com.pet.mapper.ServiceCategoryMapper;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.PricePreviewVO;
import com.pet.vo.ServiceCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceCategoryServiceImpl extends ServiceImpl<ServiceCategoryMapper, ServiceCategory>
        implements ServiceCategoryService {

    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final int MONEY_SCALE = 2;

    @Override
    public List<ServiceCategoryVO> listOnShelf() {
        return list(Wrappers.<ServiceCategory>lambdaQuery()
                .eq(ServiceCategory::getStatus, 1)
                .orderByAsc(ServiceCategory::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ServiceCategoryVO getDetail(Long categoryId) {
        return toVO(requireCategory(categoryId));
    }

    @Override
    public PricePreviewVO previewPrice(Long categoryId, LocalDateTime serviceStart) {
        ServiceCategory category = requireCategory(categoryId);
        if (category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(ResultCode.CATEGORY_OFF_SHELF);
        }

        boolean holiday = WEEKEND.contains(serviceStart.getDayOfWeek());
        BigDecimal rate = holiday ? category.getHolidayRate() : BigDecimal.ONE;
        BigDecimal amount = category.getBasePrice().multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal commission = amount.multiply(category.getCommissionRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        PricePreviewVO vo = new PricePreviewVO();
        vo.setCategoryId(category.getId());
        vo.setCategoryName(category.getName());
        vo.setServiceStart(serviceStart);
        vo.setHoliday(holiday);
        vo.setAmount(amount);
        vo.setCommission(commission);
        // 到手收入用减法得出，而不是 amount × (1 − commissionRate) 再各自四舍五入：
        // 后者会让 commission + sitterIncome 偶发性地不等于 amount，结算时的守恒校验就会失败。
        vo.setSitterIncome(amount.subtract(commission));
        return vo;
    }

    private ServiceCategory requireCategory(Long categoryId) {
        ServiceCategory category = categoryId == null ? null : getById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private ServiceCategoryVO toVO(ServiceCategory category) {
        ServiceCategoryVO vo = BeanUtil.copyProperties(category, ServiceCategoryVO.class);
        vo.setChecklist(CommaListUtil.split(category.getChecklistTemplate()));
        return vo;
    }
}
