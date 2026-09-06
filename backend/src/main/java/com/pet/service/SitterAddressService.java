package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.dto.SitterAddressSaveDTO;
import com.pet.entity.SitterAddress;
import com.pet.vo.SitterAddressVO;

import java.util.List;

public interface SitterAddressService extends IService<SitterAddress> {

    List<SitterAddressVO> listMine();

    SitterAddressVO create(SitterAddressSaveDTO dto);

    SitterAddressVO update(Long id, SitterAddressSaveDTO dto);

    SitterAddressVO setDefault(Long id);

    void delete(Long id);
}
