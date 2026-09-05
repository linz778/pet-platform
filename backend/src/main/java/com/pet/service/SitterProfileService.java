package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.dto.SitterProfileSaveDTO;
import com.pet.entity.SitterProfile;
import com.pet.vo.SitterProfileVO;

public interface SitterProfileService extends IService<SitterProfile> {

    /**
     * SITTER 角色注册时自动建立待审资质档案（audit_status=0）。
     * <p>
     * 没有这一行，接单员登录后无处提交实名与证件信息，也就永远无法通过审核去抢单。
     */
    void initProfile(Long userId);

    /** 当前登录接单员的资质档案；缺行时懒初始化一条待审档案（见实现类注释）。 */
    SitterProfileVO getMine();

    /**
     * 提交 / 重新提交实名与证件资料，提交后一律回到待审状态。
     * <p>
     * 已通过审核的档案不允许自助修改，抛 SITTER_ALREADY_AUDITED。
     */
    SitterProfileVO submit(SitterProfileSaveDTO dto);

    /**
     * 抢单前置校验：档案存在、审核已通过、且处于可接单状态。
     * <p>
     * 放在抢单入口而不是只靠前端隐藏按钮——前端拦得住误点，拦不住直接打接口的人。
     *
     * @throws com.pet.common.exception.BusinessException 1004 档案不存在 / 1005 未通过审核 / 1006 暂停接单
     */
    void requireGrabable(Long sitterId);
}
