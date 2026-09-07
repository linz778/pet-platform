package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 三端共用的申诉详情；图片证据以数组返回，便于直接渲染图片墙。 */
@Data
public class ArbitrationVO {

    private Long id;
    private Long orderId;
    private String orderNo;
    private String categoryName;
    private BigDecimal orderAmount;
    private String complainantName;
    private String sitterName;
    private String reason;
    private List<String> evidenceUrls;
    private Integer status;
    private String statusText;
    /** 未裁定时为 null；裁定后 true=申诉通过并退款，false=申诉驳回。 */
    private Boolean approved;
    private String result;
    private BigDecimal refundAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
