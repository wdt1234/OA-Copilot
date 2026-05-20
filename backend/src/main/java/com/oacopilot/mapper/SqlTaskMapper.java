package com.oacopilot.mapper;

import com.oacopilot.model.SqlTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SqlTaskMapper {

    SqlTask findByTaskId(@Param("taskId") String taskId);

    int insert(SqlTask record);

    int updateStatus(@Param("taskId") String taskId, @Param("status") String status,
                     @Param("sqlResult") String sqlResult, @Param("errorMessage") String errorMessage);

    int deleteExpired(@Param("days") int days);
}
