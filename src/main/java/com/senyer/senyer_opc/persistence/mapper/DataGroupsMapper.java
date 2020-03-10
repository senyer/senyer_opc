package com.senyer.senyer_opc.persistence.mapper;

import com.senyer.senyer_opc.dto.Tags;
import com.senyer.senyer_opc.persistence.domain.DataGroups;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author senyer
 * @since 2020-02-17
 */
public interface DataGroupsMapper extends BaseMapper<DataGroups> {

  @Select("select id,seq_id,item_id,alies,accuracy ,unit from ${tableName}")
  @Results({
          @Result(property = "id", column = "id", id=true),
          @Result(property = "seqId", column = "seq_id"),
          @Result(property = "itemId", column = "item_id"),
          @Result(property = "alies", column = "alies"),
          @Result(property = "accuracy", column = "accuracy"),
          @Result(property = "unit", column = "unit")
  })
  List<Tags> getAllTagsByTableName(@Param("tableName") String tableName);
}
