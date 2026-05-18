package com.oacopilot.mapper;

import com.oacopilot.model.FieldMappingHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FieldMappingHistoryMapper {

    List<FieldMappingHistory> findRecent(@Param("limit") int limit);

    int insert(FieldMappingHistory record);
}
