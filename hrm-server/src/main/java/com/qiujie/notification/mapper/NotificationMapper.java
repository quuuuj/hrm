package com.qiujie.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.notification.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 站内通知 Mapper。
 *
 * @author qiujie
 */
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 物理删除过期通知。清理任务需要真正释放空间，绕开 @TableLogic。
     */
    @Delete("DELETE FROM sys_notification WHERE create_time < #{before} LIMIT #{limit}")
    int deleteExpired(@Param("before") LocalDateTime before, @Param("limit") int limit);

    /**
     * 按业务类型和业务主键物理删除关联通知。
     */
    @Delete("<script>"
            + "DELETE FROM sys_notification WHERE biz_type = #{bizType} AND biz_id IN "
            + "<foreach collection='bizIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    int deleteByBizTypeAndIds(@Param("bizType") String bizType, @Param("bizIds") Collection<Long> bizIds);
}
