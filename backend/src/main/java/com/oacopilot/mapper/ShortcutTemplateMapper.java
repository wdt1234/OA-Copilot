package com.oacopilot.mapper;

import com.oacopilot.model.ShortcutTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShortcutTemplateMapper {

    @Select("SELECT * FROM shortcut_template WHERE category = #{category} ORDER BY sort_order ASC, id ASC")
    List<ShortcutTemplate> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM shortcut_template ORDER BY category, sort_order ASC, id ASC")
    List<ShortcutTemplate> findAll();

    @Insert("INSERT INTO shortcut_template (category, content, sort_order, create_time, update_time) " +
            "VALUES (#{category}, #{content}, #{sortOrder}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ShortcutTemplate template);

    @Update("UPDATE shortcut_template SET content = #{content}, update_time = #{updateTime} WHERE id = #{id}")
    void update(@Param("id") Long id, @Param("content") String content, @Param("updateTime") java.time.LocalDateTime updateTime);

    @Update("UPDATE shortcut_template SET sort_order = #{sortOrder} WHERE id = #{id}")
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    @Delete("DELETE FROM shortcut_template WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Delete("DELETE FROM shortcut_template WHERE id IN (${ids})")
    void deleteByIds(@Param("ids") String ids);

    @Delete("DELETE FROM shortcut_template WHERE category = #{category}")
    void deleteByCategory(@Param("category") String category);

    @Select("SELECT COUNT(*) FROM shortcut_template WHERE category = #{category}")
    int countByCategory(@Param("category") String category);
}
