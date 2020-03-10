package com.senyer.senyer_opc.service;

import com.senyer.senyer_opc.dto.Tags;
import com.senyer.senyer_opc.persistence.domain.DataGroups;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author senyer
 * @since 2020-02-17
 */
public interface DataGroupsService extends IService<DataGroups> {

  /**
   *  根据表名获取对应表名所有的变量集合及其详细信息。
   *
   * @param tableName 表名
   * @return tags集合
   */
  List<Tags> getAllTagsByTableName(String tableName);
}
