package com.senyer.senyer_opc.datacenter;

import com.senyer.senyer_opc.dto.Tags;
import com.senyer.senyer_opc.persistence.domain.DataGroups;
import com.senyer.senyer_opc.persistence.domain.OpcPropertiesGroup;
import com.senyer.senyer_opc.service.DataGroupsService;
import com.senyer.senyer_opc.service.OpcPropertiesGroupService;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * <><><><><> 数据处理中心 <><><><><>
 * <p>
 * 1. 与n个Opc服务端建立连接
 * 2. 获取所有服务端配置的变量数据
 * 3. 将实时数据存入到实时数据表
 * 4. 将实时数据存入一份内存
 * 5. 将实时数据写入到influxDB
 */

@Component
@Slf4j
public class DataCenter {

  public static final ConcurrentHashMap<OpcPropertiesGroup, ConcurrentHashMap<Tags, ItemState>> memoryData = new ConcurrentHashMap<>();//存储全局变量至内存

  public static final ConcurrentHashMap<OpcPropertiesGroup, Server> opcServers = new ConcurrentHashMap<>();//存储所有创建连接的opc服务端。

  private static final Integer INTERVAL_TIME = 8000;//循环读取的间隔时间。

  @Resource
  private OpcPropertiesGroupService opcPropertiesGroupService;
  @Resource
  private DataGroupsService dataGroupsService;

  /**
   * 读取每个客户端的数据
   */
  public void read() {
    creatConnection();//创建所有服务端的连接
    writeToInfluxDBAndMemory();//将读取到的数据写入到内存和influx里面。
  }


  /**
   * 创建所有服务端的连接
   */
  public void creatConnection() {
    List<OpcPropertiesGroup> listProp = opcPropertiesGroupService.list();

    listProp.forEach((prop) -> {
      String hostname = prop.getHostname();
      String domain = prop.getDomain();
      String username = prop.getUsername();
      String password = prop.getPassword();
      String clsid = prop.getClsid();
      String progId = prop.getProgId();

      final ConnectionInformation connectionInformation = new ConnectionInformation();
      connectionInformation.setHost(hostname);
      connectionInformation.setDomain(domain);
      connectionInformation.setUser(username);
      connectionInformation.setPassword(password);
      connectionInformation.setClsid(clsid);
      connectionInformation.setProgId(progId);

      //建立连接：
      Server server = new Server(connectionInformation, Executors.newSingleThreadScheduledExecutor());
      AutoReconnectController controller = new AutoReconnectController(server);//支持断线重连
      controller.connect();

      log.info("********** Async OPC Init!!!! ********** ");
      log.info("********** hostname ******************** {}", hostname);
      log.info("********** domain ********************** {}", domain);
      log.info("********** progId ********************** {}", progId);
      log.info("********** clsid *********************** {}", clsid);
      log.info("********** Async OPC State ************* {}", server.getServerState());

      opcServers.put(prop, server);
    });
    log.info("<><><><><><><><><><><><> 共建立连接 {} 个<><><><><><><><><><><><> ", listProp.size());
  }

  /*
   * 根据服务数据库获取所有的groups
   */
  private void writeToInfluxDBAndMemory() {
    opcServers.forEach((opcProp, server) -> {
      try {
        List<Tags> tags = dataGroupsService.getAllTagsByTableName(opcProp.getTableName());
        final AccessBase access = new SyncAccess(server, INTERVAL_TIME);
        accessReadOneserver(opcProp, tags, access);
        access.bind();
      } catch (final JIException | UnknownHostException | NotConnectedException | DuplicateGroupException e) {
        log.error(">>>>>>>>>>>>>>>>>> asyncHandle failed! {1}", e);
        //controller.disconnect(); //TODO (senyer) fix it.
        server.disconnect();
      }
    });
  }

  /*
    将每组tags变量集 PUT到OPC的access里面
    accessReadOneserver只会触发一次，
    但是access.addItem(ItemId,(item,state)->{})这个方法会根据INTERVAL_TIME，定时触发！
   */
  private void accessReadOneserver(OpcPropertiesGroup opcProp, List<Tags> tags, AccessBase access) {
    tags.forEach((tag -> {
      try {
        access.addItem(tag.getItemId(), (item, state) -> {
            if (item != null) {
              //TODO 这些操作后续可以考虑异步化处理
              writeToMemory(tag, state, opcProp);     //To Memory
              writeToInfluxDB(tag, state);            //To InfluxDB
              writeToDB(tag, state);                  //To DB
            }
          }
        );
      } catch (JIException | AddFailedException e) {
        log.error(">>>>>>>>>>>>>>>>>> accessReadOneserver failed! {1}", e);
      }
    }));
  }


  /**
   *  TODO （senyer） 这里需要考虑一下，是否会造成内存溢出！！！！！
   * @param tag 变量名
   * @param state 读取的数据对象。
   */
  private void writeToMemory(Tags tag, ItemState state, OpcPropertiesGroup opcProp) {
    final ConcurrentHashMap<Tags, ItemState> itemMap;
    if (memoryData.get(opcProp) == null) {
      itemMap = new ConcurrentHashMap<>();
    } else {
      itemMap = memoryData.get(opcProp);
    }
    itemMap.put(tag, state);
    //TODO (senyer) improve this.  想一个更高效合理的方案。
    //memoryData.remove(opcProp);//校验一下，这一步是不是可以删掉、
    memoryData.put(opcProp, itemMap);
    /*
    log.error(">>>>>>>>>>>>>>>>>> 遍历：：：");
    log.error(">>>>>>>>>>>>>>>>>> 遍历XXXXXXX：：："+memoryData.size());
    memoryData.forEach((k,v)->{
      log.error("kkkkkkkkkkkkkkkkk:      "+k);
      log.error("vvvvvvvvvvvvv::::::"+v);
    });*/
  }

  private void writeToInfluxDB(Tags tag, ItemState state) {
    //TODO (senyer) : 写入到influxDB
  }

  /**
   * Write To DB
   * @param tag
   * @param state
   */
  private void writeToDB(Tags tag, ItemState state) {
    //TODO (senyer) : 写入到关系型数据库 saveOrUpdate
  }
}
