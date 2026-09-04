package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 宠物档案出参。不直接暴露 Pet 实体，剔除 userId / deleted / updateTime。
 */
@Data
public class PetVO {

    private Long id;
    private String name;
    private String species;
    private String breed;

    /** 0=未知 1=公 2=母 */
    private Integer gender;

    private Integer ageMonths;
    private BigDecimal weightKg;
    private String avatar;

    /**
     * 疫苗免疫证明图片 URL。库里是逗号分隔的一列，出入参统一用列表，
     * 免得前端再去猜存储格式。
     * <p>
     * 初始化为空列表：Jackson 配了 non_null，null 会让这个 key 直接消失，
     * 前端 `v-for` 拿到 undefined 会报错，而 [] 至少能正常渲染成空。
     */
    private List<String> vaccineCerts = new ArrayList<>();

    private String personality;
    private String feedingTaboo;
    private LocalDateTime createTime;
}
