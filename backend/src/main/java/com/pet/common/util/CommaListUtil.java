package com.pet.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 「库里逗号分隔的一列」与「出入参 List」之间的转换。
 * <p>
 * t_pet.vaccine_cert、t_service_category.checklist_template 都是这种形态：一列存多个值，
 * 但前端要的是数组（图片墙、逐项存证的清单）。转换集中在这里，避免各 Service 各写一份、
 * 有的 trim 有的不 trim。
 * <p>
 * 前提是单个值本身不含逗号——这里存的是 UUID 文件名的图片 URL 和「换粮/添水」这类短词条，成立。
 */
public final class CommaListUtil {

    private CommaListUtil() {
    }

    /** 列表转逗号串；空列表返回 null，让列保持 NULL 而不是空串。 */
    public static String join(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        List<String> cleaned = values.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .toList();
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

    /** 逗号串转列表；null / 空串返回可变空列表，方便直接塞进 VO（non_null 下空列表仍会输出 []）。 */
    public static List<String> split(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new ArrayList<>();
        }
        return StrUtil.splitTrim(raw, ',');
    }
}
