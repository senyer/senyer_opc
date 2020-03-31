package com.senyer.senyer_opc.datacenter;

import com.senyer.senyer_opc.dto.Tags;
import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.da.ItemState;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <><><><><>数据处理中心<><><><><>
 *
 * 1. 与n个Opc服务端建立连接
 * 2. 获取所有服务端配置的变量数据
 * 3. 将实时数据存入到实时数据表
 * 4. 将实时数据存入一份内存
 * 5. 将实时数据写入到influxDB
 */

@Component
@Slf4j
public class DataCenter {
  public static final ConcurrentHashMap<String, ConcurrentHashMap<Tags, ItemState>> memoryData = new ConcurrentHashMap<>();




}
