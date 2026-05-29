package com.oacopilot.mapper;

import com.oacopilot.model.AiConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiConfigMapper {

    @Select("SELECT * FROM ai_config ORDER BY is_active DESC, create_time DESC")
    List<AiConfig> findAll();

    @Select("SELECT * FROM ai_config WHERE id = #{id}")
    AiConfig findById(@Param("id") Long id);

    @Select("SELECT * FROM ai_config WHERE is_active = 1 LIMIT 1")
    AiConfig findActive();

    @Insert("INSERT INTO ai_config (name, endpoint, api_key, model, timeout, is_active, create_time, update_time) " +
            "VALUES (#{name}, #{endpoint}, #{apiKey}, #{model}, #{timeout}, #{isActive}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiConfig aiConfig);

    @Update("UPDATE ai_config SET name=#{name}, endpoint=#{endpoint}, api_key=#{apiKey}, " +
            "model=#{model}, timeout=#{timeout}, update_time=#{updateTime} WHERE id=#{id}")
    int update(AiConfig aiConfig);

    @Update("UPDATE ai_config SET is_active = 0")
    int deactivateAll();

    @Update("UPDATE ai_config SET is_active = 1, update_time = #{updateTime} WHERE id = #{id}")
    int activate(@Param("id") Long id, @Param("updateTime") String updateTime);

    @Delete("DELETE FROM ai_config WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
