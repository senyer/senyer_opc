package com.senyer.senyer_opc.datacenter;

import com.senyer.senyer_opc.dto.Tags;
import com.senyer.senyer_opc.persistence.domain.OpcPropertiesGroup;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * <><><><><>数据处理中心<><><><><>
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

  public static final ConcurrentHashMap<String, ConcurrentHashMap<Tags, ItemState>> memoryData = new ConcurrentHashMap<>();//存储全局变量至内存

  public static final CopyOnWriteArrayList<Server> opcServers = new CopyOnWriteArrayList<>();//存储所有创建连接的opc服务端。


  @Resource
  private OpcPropertiesGroupService opcPropertiesGroupService;


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

      Server server = new Server(connectionInformation, Executors.newSingleThreadScheduledExecutor());
      AutoReconnectController controller = new AutoReconnectController(server);//支持断线重连
      controller.connect();
      server.addStateListener((state) ->
        log.info("<><><><><><><><><><><><><><><<>><> connectionStateChanged state = {}", state)
      );
      log.info("********** Async OPC Init!!!! ********** ");
      log.info("********** hostname ********** {}",hostname);
      log.info("********** domain ********** {}",domain);
      log.info("********** progId ********** {}",progId);
      log.info("********** clsid ********** {}",clsid);
      log.info("********** Async OPC State ********** {}" , server.getServerState().getServerState());
      opcServers.add(server);

    });
  }


}
