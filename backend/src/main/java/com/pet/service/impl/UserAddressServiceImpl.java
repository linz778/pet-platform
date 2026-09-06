package com.pet.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.UserAddressSaveDTO;
import com.pet.entity.UserAddress;
import com.pet.mapper.UserAddressMapper;
import com.pet.security.UserContext;
import com.pet.service.UserAddressService;
import com.pet.vo.UserAddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    @Override
    public List<UserAddressVO> listMine() {
        return list(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, UserContext.userId())
                .orderByDesc(UserAddress::getDefaultAddress)
                .orderByDesc(UserAddress::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public UserAddressVO create(UserAddressSaveDTO dto) {
        Long userId = UserContext.userId();
        boolean firstAddress = count(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)) == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(dto.getDefaultAddress());
        if (makeDefault) clearDefaults(userId);

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        copyEditableFields(address, dto);
        address.setDefaultAddress(makeDefault ? 1 : 0);
        save(address);
        return toVO(address);
    }

    @Override
    @Transactional
    public UserAddressVO update(Long id, UserAddressSaveDTO dto) {
        UserAddress existing = requireMine(id);
        boolean makeDefault = Integer.valueOf(1).equals(existing.getDefaultAddress())
                || Boolean.TRUE.equals(dto.getDefaultAddress());
        if (makeDefault) clearDefaults(existing.getUserId());

        update(Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getId, existing.getId())
                .set(UserAddress::getLabel, dto.getLabel())
                .set(UserAddress::getProvince, dto.getProvince())
                .set(UserAddress::getCity, dto.getCity())
                .set(UserAddress::getDistrict, dto.getDistrict())
                .set(UserAddress::getDetailAddress, dto.getDetailAddress())
                .set(UserAddress::getLng, dto.getLng())
                .set(UserAddress::getLat, dto.getLat())
                .set(UserAddress::getDefaultAddress, makeDefault ? 1 : 0)
                .set(UserAddress::getUpdateTime, LocalDateTime.now()));

        return toVO(getById(existing.getId()));
    }

    @Override
    @Transactional
    public UserAddressVO setDefault(Long id) {
        UserAddress address = requireMine(id);
        clearDefaults(address.getUserId());
        update(Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getId, address.getId())
                .set(UserAddress::getDefaultAddress, 1)
                .set(UserAddress::getUpdateTime, LocalDateTime.now()));
        address.setDefaultAddress(1);
        return toVO(address);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserAddress address = requireMine(id);
        boolean wasDefault = Integer.valueOf(1).equals(address.getDefaultAddress());
        removeById(address.getId());
        if (!wasDefault) return;

        UserAddress replacement = getOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, address.getUserId())
                .orderByDesc(UserAddress::getId)
                .last("LIMIT 1"));
        if (replacement != null) setDefault(replacement.getId());
    }

    private UserAddress requireMine(Long id) {
        UserAddress address = id == null ? null : getById(id);
        if (address == null || !address.getUserId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.USER_ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private void clearDefaults(Long userId) {
        update(Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDefaultAddress, 1)
                .set(UserAddress::getDefaultAddress, 0)
                .set(UserAddress::getUpdateTime, LocalDateTime.now()));
    }

    private void copyEditableFields(UserAddress address, UserAddressSaveDTO dto) {
        address.setLabel(dto.getLabel());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());
        address.setLng(dto.getLng());
        address.setLat(dto.getLat());
    }

    private UserAddressVO toVO(UserAddress address) {
        UserAddressVO vo = new UserAddressVO();
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
