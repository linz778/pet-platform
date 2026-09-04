package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 宠物档案。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pet")
public class Pet extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属主人 */
    private Long userId;

    private String name;

    /** 物种：狗 / 猫 / 其他 */
    private String species;

    private String breed;

    /** 0=未知 1=公 2=母 */
    private Integer gender;

    private Integer ageMonths;

    private BigDecimal weightKg;

    private String avatar;

    /** 疫苗免疫证明图片 URL，多个以逗号分隔 */
    private String vaccineCert;

    /** 性格习性 */
    private String personality;

    /** 喂养禁忌 */
    private String feedingTaboo;
}
