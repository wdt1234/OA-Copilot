package com.oacopilot.mapper;

import com.oacopilot.model.DeeHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeeHistoryMapper {

    List<DeeHistory> findRecent(@Param("limit") int limit);

    int insert(DeeHistory record);

    int updatePinned(@Param("id") Long id, @Param("isPinned") boolean isPinned);

    int deleteById(@Param("id") Long id);

    int deleteByIds(@Param("ids") List<Long> ids);
}
