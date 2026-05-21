package com.oacopilot.mapper;

import com.oacopilot.model.SqlCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SqlCacheMapper {

    SqlCache findByKey(@Param("cacheKey") String cacheKey);

    int insert(SqlCache record);

    int deleteExpired(@Param("days") int days);

    int deleteAll();

    int deleteByPrompt(@Param("prompt") String prompt);
}
