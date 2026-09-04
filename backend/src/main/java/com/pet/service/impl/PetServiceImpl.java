package com.pet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.common.util.CommaListUtil;
import com.pet.dto.PetSaveDTO;
import com.pet.entity.Pet;
import com.pet.mapper.PetMapper;
import com.pet.security.UserContext;
import com.pet.service.PetService;
import com.pet.vo.PetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements PetService {

    @Override
    public List<PetVO> listMine() {
        return list(Wrappers.<Pet>lambdaQuery()
                .eq(Pet::getUserId, UserContext.userId())
                .orderByDesc(Pet::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public Pet requireMine(Long petId) {
        Pet pet = petId == null ? null : getById(petId);
        // 别人的宠物一律按「不存在」处理：回一个「无权访问」等于确认了这个 id 真实存在，
        // 给遍历 id 探测他人档案留了口子。
        if (pet == null || !pet.getUserId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.PET_NOT_FOUND);
        }
        return pet;
    }

    @Override
    public PetVO getMine(Long petId) {
        return toVO(requireMine(petId));
    }

    @Override
    public PetVO create(PetSaveDTO dto) {
        Pet pet = new Pet();
        BeanUtil.copyProperties(dto, pet, "vaccineCerts");
        pet.setVaccineCert(CommaListUtil.join(dto.getVaccineCerts()));
        // userId 只认登录态，DTO 里没有这个字段
        pet.setUserId(UserContext.userId());
        save(pet);
        return toVO(pet);
    }

    @Override
    public PetVO update(Long petId, PetSaveDTO dto) {
        requireMine(petId);
        // 逐列显式 set，而不是 updateById：默认字段策略会跳过 null，
        // 用户把体重 / 喂养禁忌清空后保存就会「点了没反应」，旧值原样留在库里。
        // 同理 update(null, wrapper) 不触发 MetaObjectHandler，update_time 必须自己写。
        update(Wrappers.<Pet>lambdaUpdate()
                .eq(Pet::getId, petId)
                .set(Pet::getName, dto.getName())
                .set(Pet::getSpecies, dto.getSpecies())
                .set(Pet::getBreed, dto.getBreed())
                .set(Pet::getGender, dto.getGender())
                .set(Pet::getAgeMonths, dto.getAgeMonths())
                .set(Pet::getWeightKg, dto.getWeightKg())
                .set(Pet::getAvatar, dto.getAvatar())
                .set(Pet::getVaccineCert, CommaListUtil.join(dto.getVaccineCerts()))
                .set(Pet::getPersonality, dto.getPersonality())
                .set(Pet::getFeedingTaboo, dto.getFeedingTaboo())
                .set(Pet::getUpdateTime, LocalDateTime.now()));
        return getMine(petId);
    }

    @Override
    public void delete(Long petId) {
        requireMine(petId);
        removeById(petId);
    }

    private PetVO toVO(Pet pet) {
        PetVO vo = BeanUtil.copyProperties(pet, PetVO.class);
        vo.setVaccineCerts(CommaListUtil.split(pet.getVaccineCert()));
        return vo;
    }
}
