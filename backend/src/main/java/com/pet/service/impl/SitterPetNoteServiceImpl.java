package com.pet.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.SitterPetNoteSaveDTO;
import com.pet.entity.Order;
import com.pet.entity.Pet;
import com.pet.entity.SitterPetNote;
import com.pet.mapper.OrderMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.SitterPetNoteMapper;
import com.pet.security.UserContext;
import com.pet.service.SitterPetNoteService;
import com.pet.vo.SitterPetNoteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SitterPetNoteServiceImpl extends ServiceImpl<SitterPetNoteMapper, SitterPetNote>
        implements SitterPetNoteService {

    private final OrderMapper orderMapper;
    private final PetMapper petMapper;

    @Override
    public List<SitterPetNoteVO> listMine() {
        return list(Wrappers.<SitterPetNote>lambdaQuery()
                .eq(SitterPetNote::getSitterId, UserContext.userId())
                .orderByDesc(SitterPetNote::getUpdateTime)
                .orderByDesc(SitterPetNote::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public SitterPetNoteVO saveMine(SitterPetNoteSaveDTO dto) {
        Long sitterId = UserContext.userId();
        Long servedCount = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getSitterId, sitterId)
                .eq(Order::getPetId, dto.getPetId()));
        if (servedCount == null || servedCount == 0) {
            throw new BusinessException(ResultCode.SITTER_PET_NOT_SERVED);
        }

        List<Pet> snapshots = petMapper.selectSnapshots(List.of(dto.getPetId()));
        if (snapshots.isEmpty()) {
            throw new BusinessException(ResultCode.PET_NOT_FOUND);
        }
        Pet pet = snapshots.getFirst();

        SitterPetNote note = getOne(Wrappers.<SitterPetNote>lambdaQuery()
                .eq(SitterPetNote::getSitterId, sitterId)
                .eq(SitterPetNote::getPetId, dto.getPetId())
                .last("LIMIT 1"));
        if (note == null) {
            note = new SitterPetNote();
            note.setSitterId(sitterId);
            note.setPetId(dto.getPetId());
        }
        note.setPetName(pet.getName());
        note.setPetSpecies(pet.getSpecies());
        note.setPetAvatar(pet.getAvatar());
        note.setHabits(trimToNull(dto.getHabits()));
        note.setFeedingNotes(trimToNull(dto.getFeedingNotes()));
        note.setCareNotes(trimToNull(dto.getCareNotes()));

        if (note.getId() == null) {
            save(note);
        } else {
            updateById(note);
        }
        return toVO(note);
    }

    @Override
    @Transactional
    public void deleteMine(Long id) {
        SitterPetNote note = id == null ? null : getById(id);
        if (note == null || !note.getSitterId().equals(UserContext.userId())) {
            throw new BusinessException(ResultCode.SITTER_PET_NOTE_NOT_FOUND);
        }
        removeById(note.getId());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private SitterPetNoteVO toVO(SitterPetNote note) {
        SitterPetNoteVO vo = new SitterPetNoteVO();
        vo.setId(note.getId());
        vo.setPetId(note.getPetId());
        vo.setPetName(note.getPetName());
        vo.setPetSpecies(note.getPetSpecies());
        vo.setPetAvatar(note.getPetAvatar());
        vo.setHabits(note.getHabits());
        vo.setFeedingNotes(note.getFeedingNotes());
        vo.setCareNotes(note.getCareNotes());
        vo.setCreateTime(note.getCreateTime());
        vo.setUpdateTime(note.getUpdateTime());
        return vo;
    }
}
