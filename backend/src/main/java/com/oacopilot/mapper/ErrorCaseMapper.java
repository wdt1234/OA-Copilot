package com.oacopilot.mapper;

import com.oacopilot.model.ErrorCase;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ErrorCaseMapper {

    @Select("SELECT * FROM error_case ORDER BY is_pinned DESC, create_time DESC")
    List<ErrorCase> findAll();

    @Select("SELECT * FROM error_case WHERE category = #{category} ORDER BY is_pinned DESC, create_time DESC")
    List<ErrorCase> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM error_case WHERE id = #{id}")
    ErrorCase findById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT * FROM error_case WHERE " +
            "<if test='category != null and category != \"\"'>category = #{category} AND </if>" +
            "(title LIKE #{keyword} OR symptom LIKE #{keyword} OR cause LIKE #{keyword} OR solution LIKE #{keyword} OR tags LIKE #{keyword}) " +
            "ORDER BY is_pinned DESC, create_time DESC" +
            "</script>")
    List<ErrorCase> search(@Param("category") String category, @Param("keyword") String keyword);

    @Insert("INSERT INTO error_case (category, title, symptom, cause, solution, example_wrong, example_correct, tags, is_pinned, create_time, update_time) " +
            "VALUES (#{category}, #{title}, #{symptom}, #{cause}, #{solution}, #{exampleWrong}, #{exampleCorrect}, #{tags}, #{isPinned}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ErrorCase errorCase);

    @Update("UPDATE error_case SET category=#{category}, title=#{title}, symptom=#{symptom}, cause=#{cause}, " +
            "solution=#{solution}, example_wrong=#{exampleWrong}, example_correct=#{exampleCorrect}, tags=#{tags}, " +
            "is_pinned=#{isPinned}, update_time=#{updateTime} WHERE id=#{id}")
    int update(ErrorCase errorCase);

    @Update("UPDATE error_case SET is_pinned = #{isPinned}, update_time = #{updateTime} WHERE id = #{id}")
    int updatePinned(@Param("id") Long id, @Param("isPinned") Integer isPinned, @Param("updateTime") String updateTime);

    @Delete("DELETE FROM error_case WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("<script>DELETE FROM error_case WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);
}
