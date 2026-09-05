package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.dto.CheckInDTO;
import com.pet.dto.EvidenceSaveDTO;
import com.pet.dto.TrackSaveDTO;
import com.pet.entity.OrderEvidence;
import com.pet.vo.OrderEvidenceVO;

import java.util.List;

/**
 * 履约：到达打卡 → 逐项拍照存证 / 记录轨迹 → 标记服务完成。
 * <p>
 * 独立于 {@link OrderService}：订单服务管「单子本身的状态与钱」，这里管「服务过程留下的凭证」。
 * 状态推进复用 OrderMapper 里带前置状态条件的 UPDATE（markCheckedIn / markFinished），
 * 与支付、抢单同一套幂等防线。
 */
public interface FulfillmentService extends IService<OrderEvidence> {

    /**
     * 到达定位打卡：已接单 → 服务中，并留一条 type=1 的存证。
     * <p>
     * 坐标与服务地址的距离超过 {@code pet-platform.geo.check-in-radius} 抛 2004；
     * 距离校验放在状态更新之前，打卡失败不能把订单推进到「服务中」。
     */
    void checkIn(Long orderId, CheckInDTO dto);

    /**
     * 作业清单单项存证（type=2），仅服务中可上传。
     * <p>
     * 同一清单项重复提交按「重拍」处理，覆盖原记录而不是再插一条，
     * 否则清单会被同一项的多张照片撑乱，完成校验也说不清到底做没做。
     */
    OrderEvidenceVO saveChecklistEvidence(Long orderId, EvidenceSaveDTO dto);

    /** 散步轨迹（type=3），仅服务中可上传；同一单允许多条（分段遛）。 */
    OrderEvidenceVO saveTrack(Long orderId, TrackSaveDTO dto);

    /**
     * 标记服务完成：服务中 → 待验收。
     * <p>
     * 该服务类别 checklist_template 里的每一项都必须已有存证，否则抛 2008 并在提示里列出缺哪几项。
     */
    void finish(Long orderId);

    /** 某单的全部存证，按上传先后。下单用户、该单接单员与管理员可见，其余人 2005。 */
    List<OrderEvidenceVO> listEvidence(Long orderId);
}
