package com.pet.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.SitterAddressSaveDTO;
import com.pet.entity.SitterAddress;
import com.pet.mapper.SitterAddressMapper;
import com.pet.security.UserContext;
import com.pet.service.SitterAddressService;
import com.pet.service.SitterProfileService;
import com.pet.vo.SitterAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SitterAddressServiceImpl extends ServiceImpl<SitterAddressMapper, SitterAddress> implements SitterAddressService {

    private final SitterProfileService sitterProfileService;

    @Override
    public List<SitterAddressVO> listMine() {
        return list(Wrappers.<SitterAddress>lambdaQuery()
                .eq(SitterAddress::getSitterId, UserContext.userId())
                .orderByDesc(SitterAddress::getDefaultAddress)
                .orderByDesc(SitterAddress::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public SitterAddressVO create(SitterAddressSaveDTO dto) {
        Long sitterId = UserContext.userId();
        boolean firstAddress = count(Wrappers.<SitterAddress>lambdaQuery()
                .eq(SitterAddress::getSitterId, sitterId)) == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(dto.getDefaultAddress());
        if (makeDefault) clearDefaults(sitterId);

        SitterAddress address = new SitterAddress();
        address.setSitterId(sitterId);
        copyEditableFields(address, dto);
        address.setDefaultAddress(makeDefault ? 1 : 0);
        save(address);
        if (makeDefault) syncProfileLocation(address);
        return toVO(address);
    }

    @Override
    @Transactional
    public SitterAddressVO update(Long id, SitterAddressSaveDTO dto) {
        SitterAddress existing = requireMine(id);
        boolean makeDefault = Integer.valueOf(1).equals(existing.getDefaultAddress())
                || Boolean.TRUE.equals(dto.getDefaultAddress());
        if (makeDefault) clearDefaults(existing.getSitterId());

        update(Wrappers.<SitterAddress>lambdaUpdate()
                .eq(SitterAddress::getId, existing.getId())
                .set(SitterAddress::getLabel, dto.getLabel())
                .set(SitterAddress::getProvince, dto.getProvince())
                .set(SitterAddress::getCity, dto.getCity())
                .set(SitterAddress::getDistrict, dto.getDistrict())
                .set(SitterAddress::getDetailAddress, dto.getDetailAddress())
                .set(SitterAddress::getLng, dto.getLng())
                .set(SitterAddress::getLat, dto.getLat())
                .set(SitterAddress::getDefaultAddress, makeDefault ? 1 : 0)
                .set(SitterAddress::getUpdateTime, LocalDateTime.now()));

        SitterAddress updated = getById(existing.getId());
        if (makeDefault) syncProfileLocation(updated);
        return toVO(updated);
    }

    @Override
    @Transactional
    public SitterAddressVO setDefault(Long id) {
        SitterAddress address = requireMine(id);
        clearDefaults(address.getSitterId());
        update(Wrappers.<SitterAddress>lambdaUpdate()
                .eq(SitterAddress::getId, address.getId())
                .set(SitterAddress::getDefaultAddress, 1)
                .set(SitterAddress::getUpdateTime, LocalDateTime.now()));
        address.setDefaultAddress(1);
        syncProfileLocation(address);
        return toVO(address);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SitterAddress address = requireMine(id);
        boolean wasDefault = Integer.valueOf(1).equals(address.getDefaultAddress());
        removeById(address.getId());
        if (!wasDefault) return;

        SitterAddress replacement = getOne(Wrappers.<SitterAddress>lambdaQuery()
                .eq(SitterAddress::getSitterId, address.getSitterId())
                .orderByDesc(SitterAddress::getId)
                .last("LIMIT 1"));
        if (replacement == null) {
            sitterProfileService.syncSearchLocation(null, null);
            return;
        }
        setDefault(replacement.getId());
    }

    private SitterAddress requireMine(Long id) {
        SitterAddress address = id == null ? null : getById(id);
        if (address == null || !address.getSitterId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.SITTER_ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private void clearDefaults(Long sitterId) {
        update(Wrappers.<SitterAddress>lambdaUpdate()
                .eq(SitterAddress::getSitterId, sitterId)
                .eq(SitterAddress::getDefaultAddress, 1)
                .set(SitterAddress::getDefaultAddress, 0)
                .set(SitterAddress::getUpdateTime, LocalDateTime.now()));
    }

    private void copyEditableFields(SitterAddress address, SitterAddressSaveDTO dto) {
        address.setLabel(dto.getLabel());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());
        address.setLng(dto.getLng());
        address.setLat(dto.getLat());
    }

    private void syncProfileLocation(SitterAddress address) {
        sitterProfileService.syncSearchLocation(address.getLng(), address.getLat());
    }

    private SitterAddressVO toVO(SitterAddress address) {
        SitterAddressVO vo = new SitterAddressVO();
        vo.setId(address.getId());
        vo.setLabel(address.getLabel());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setLng(address.getLng());
        vo.setLat(address.getLat());
        vo.setDefaultAddress(Integer.valueOf(1).equals(address.getDefaultAddress()));
        return vo;
    }
}
