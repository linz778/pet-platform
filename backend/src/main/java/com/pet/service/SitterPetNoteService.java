package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.dto.SitterPetNoteSaveDTO;
import com.pet.entity.SitterPetNote;
import com.pet.vo.SitterPetNoteVO;

import java.util.List;

public interface SitterPetNoteService extends IService<SitterPetNote> {

    List<SitterPetNoteVO> listMine();

    SitterPetNoteVO saveMine(SitterPetNoteSaveDTO dto);

    void deleteMine(Long id);
}
