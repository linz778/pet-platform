package com.pet.service;

import com.pet.common.api.PageResult;
import com.pet.dto.ArbitrationCreateDTO;
import com.pet.dto.ArbitrationDecisionDTO;
import com.pet.dto.ArbitrationQuery;
import com.pet.vo.ArbitrationVO;

public interface ArbitrationService {

    void submit(Long orderId, ArbitrationCreateDTO dto);

    /** 当事人双方与管理员可查看；订单尚未申诉时返回 null。 */
    ArbitrationVO getByOrder(Long orderId);

    PageResult<ArbitrationVO> pageAdmin(ArbitrationQuery query);

    void decide(Long arbitrationId, ArbitrationDecisionDTO dto);
}
