package com.pet.service;

import com.pet.common.api.PageResult;
import com.pet.dto.SitterAuditDecisionDTO;
import com.pet.dto.SitterAuditQuery;
import com.pet.vo.SitterAuditVO;

public interface SitterAuditService {
    PageResult<SitterAuditVO> page(SitterAuditQuery query);
    SitterAuditVO decide(Long profileId, SitterAuditDecisionDTO dto);
}
