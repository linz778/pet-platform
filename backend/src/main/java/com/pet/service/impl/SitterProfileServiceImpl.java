package com.pet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.entity.SitterProfile;
import com.pet.mapper.SitterProfileMapper;
import com.pet.service.SitterProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SitterProfileServiceImpl extends ServiceImpl<SitterProfileMapper, SitterProfile> implements SitterProfileService {

    @Override
    public void initProfile(Long userId) {
        SitterProfile profile = new SitterProfile();
        profile.setUserId(userId);
        profile.setExperienceYears(0);
        profile.setAuditStatus(0);
        profile.setCreditLevel(3);
        profile.setAvailable(1);
        save(profile);
    }
}
