package com.oacopilot.mapper;

import com.oacopilot.model.SqlHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SqlHistoryMapper {

    List<SqlHistory> findRecent(@Param("limit") int limit);

    int insert(SqlHistory record);
}
