package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.dto.PetSaveDTO;
import com.pet.entity.Pet;
import com.pet.vo.PetVO;

import java.util.List;

public interface PetService extends IService<Pet> {

    /** 当前登录用户名下的全部宠物，按 id 倒序（新加的排在前面）。 */
    List<PetVO> listMine();

    /**
     * 按 id 取宠物并校验归属，返回实体。
     * <p>
     * 下单时也要用它——订单必须挂在调用者自己的宠物上，否则可以拿别人的 petId 下单，
     * 让陌生接单员上门到别人家。
     */
    Pet requireMine(Long petId);

    PetVO getMine(Long petId);

    PetVO create(PetSaveDTO dto);

    PetVO update(Long petId, PetSaveDTO dto);

    void delete(Long petId);
}
