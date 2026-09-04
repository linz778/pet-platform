package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.entity.ServiceCategory;
import com.pet.vo.PricePreviewVO;
import com.pet.vo.ServiceCategoryVO;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceCategoryService extends IService<ServiceCategory> {

    /** 上架中的服务目录，用户端选购页用。 */
    List<ServiceCategoryVO> listOnShelf();

    /** 按 id 查详情。不校验上下架状态——管理端要能查看已下架的类别。 */
    ServiceCategoryVO getDetail(Long categoryId);

    /**
     * 计价预览：下单页实时显示总价，管理端改完价立刻验证联动效果。
     * <p>
     * 这也是下单时唯一的计价入口（OrderService 直接复用），保证「预览多少就付多少」。
     * 类别已下架时抛 CATEGORY_OFF_SHELF，顺带挡掉了对下架服务下单。
     *
     * @param serviceStart 预约开始时间，落在周六/周日按 holidayRate 溢价
     */
    PricePreviewVO previewPrice(Long categoryId, LocalDateTime serviceStart);
}
