package com.oacopilot.mapper;

import com.oacopilot.model.DataDictionary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataDictionaryMapper {

    List<DataDictionary> findAll();

    DataDictionary findByFormCode(@Param("formCode") String formCode);

    int insert(DataDictionary record);

    int update(DataDictionary record);

    int deleteByFormCode(@Param("formCode") String formCode);

    int deleteAll();

    int setPinned(@Param("formCode") String formCode, @Param("pinned") int pinned);

    List<DataDictionary> search(@Param("keyword") String keyword);
}
