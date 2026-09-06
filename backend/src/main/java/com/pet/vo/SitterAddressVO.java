package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SitterAddressVO {

    private Long id;
    private String label;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private BigDecimal lng;
    private BigDecimal lat;
    private boolean defaultAddress;
}
