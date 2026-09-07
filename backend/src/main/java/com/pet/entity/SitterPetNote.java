package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 接单员私人宠物照护手记。宠物基础信息保存快照，避免宠物档案删除后笔记失去对象。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sitter_pet_note")
public class SitterPetNote extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sitterId;
    private Long petId;
    private String petName;
    private String petSpecies;
    private String petAvatar;
    private String habits;
    private String feedingNotes;
    private String careNotes;
}
