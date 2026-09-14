package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.SiteNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SiteNotificationMapper extends BaseMapper<SiteNotification> {

    @Update("UPDATE t_site_notification SET read_status = 1, update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND read_status = 0 AND deleted = 0")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE t_site_notification SET read_status = 1, update_time = NOW() " +
            "WHERE user_id = #{userId} AND read_status = 0 AND deleted = 0")
    int markAllRead(@Param("userId") Long userId);
}
