package com.pet.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页入参基类。
 * <p>
 * size 上限取 500，与 {@code MybatisPlusConfig} 里 {@code PaginationInnerInterceptor#setMaxLimit(500)}
 * 对齐——超过该值分页插件会<b>静默截断</b>而不报错，前端拿到的条数与请求不一致却看不出原因。
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "页码从 1 开始")
    private long page = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 500, message = "每页条数不能超过 500")
    private long size = 10;

    public <T> Page<T> toPage() {
        return new Page<>(page, size);
    }
}
