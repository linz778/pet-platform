package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.entity.SitterProfile;

public interface SitterProfileService extends IService<SitterProfile> {

    /**
     * SITTER 角色注册时自动建立待审资质档案（audit_status=0）。
     * <p>
     * 没有这一行，接单员登录后无处提交实名与证件信息，也就永远无法通过审核去抢单。
     */
    void initProfile(Long userId);
}
