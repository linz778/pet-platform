package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SitterPetNoteVO {
    private Long id;
    private Long petId;
    private String petName;
    private String petSpecies;
    private String petAvatar;
    private String habits;
    private String feedingNotes;
    private String careNotes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
