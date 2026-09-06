package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.dto.UserAddressSaveDTO;
import com.pet.entity.UserAddress;
import com.pet.vo.UserAddressVO;

import java.util.List;

public interface UserAddressService extends IService<UserAddress> {

    List<UserAddressVO> listMine();

    UserAddressVO create(UserAddressSaveDTO dto);

    UserAddressVO update(Long id, UserAddressSaveDTO dto);

    UserAddressVO setDefault(Long id);

    void delete(Long id);
}
