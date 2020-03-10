package com.senyer.senyer_opc.service;

import com.senyer.senyer_opc.dto.Tags;
import com.senyer.senyer_opc.persistence.domain.DataGroups;
import com.senyer.senyer_opc.persistence.mapper.DataGroupsMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author senyer
 * @since 2020-02-17
 */
@Service
public class DataGroupsServiceImpl extends ServiceImpl<DataGroupsMapper, DataGroups> implements DataGroupsService {

  @Resource
  private DataGroupsMapper dataGroupsMapper;

  @Override
  public List<Tags> getAllTagsByTableName(String tableName) {
    return dataGroupsMapper.getAllTagsByTableName(tableName);
  }
}
