package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 履约存证。下单用户验收时看的就是这份数据，所以三种类型共用一个 VO：
 * type=1 只有 lat/lng，type=2 有 checkItem/imageUrl，type=3 有 trackPoints，
 * 用不到的字段保持 null，Jackson non_null 下键会消失，前端按 type 分支渲染即可。
 * <p>
 * 刻意不含 sitterId：谁能看这条存证由 {@code listEvidence} 的可见性校验决定，
 * 而上传人是谁对看的人没有意义（用户看的是「这一单」，管理员看的是订单详情里的接单员）。
 */
@Data
public class OrderEvidenceVO {

    private Long id;

    private Long orderId;

    /** 1=进门定位打卡 2=作业清单存证 3=散步轨迹 */
    private Integer type;

    private String typeText;

    /** 清单项名称，仅 type=2 */
    private String checkItem;

    /** 存证照片地址，仅 type=2 */
    private String imageUrl;

    private BigDecimal lat;

    private BigDecimal lng;

    /** 轨迹点数组，仅 type=3 */
    private List<TrackPointVO> trackPoints;

    private String remark;

    private LocalDateTime createTime;
}
