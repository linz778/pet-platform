package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface PetMapper extends BaseMapper<Pet> {

    /**
     * 批量取宠物快照，<b>故意不带 {@code deleted = 0}</b>。
     * <p>
     * t_order 只存 pet_id、没有宠物名快照，而宠物档案允许被主人逻辑删除。
     * 若走 BaseMapper 的查询，删掉宠物后历史订单详情就再也拼不出「服务宠物」是谁了。
     * 这里连同 deleted 一起取出，由调用方决定是否给用户看「档案已删除」标记。
     * <p>
     * 传入的 ids 为空集合时必须由调用方短路，否则 foreach 会生成 {@code IN ()} 语法错误。
     */
    @Select("<script>"
            + "SELECT id, user_id, name, species, breed, avatar, deleted "
            + "FROM t_pet WHERE id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<Pet> selectSnapshots(@Param("ids") Collection<Long> ids);
}
