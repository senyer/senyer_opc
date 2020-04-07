package com.senyer.senyer_opc.opc;

import com.senyer.senyer_opc.dto.Data_A;
import com.senyer.senyer_opc.dto.Tags;
import com.senyer.senyer_opc.enums.OpcProp;
import com.senyer.senyer_opc.persistence.domain.DataGroups;
import com.senyer.senyer_opc.persistence.domain.OpcProperties;
import com.senyer.senyer_opc.service.DataGroupsService;
import com.senyer.senyer_opc.service.OpcPropertiesService;
import com.senyer.senyer_opc.utils.FastJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.*;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import static com.senyer.senyer_opc.enums.ApiType.*;
import static com.senyer.senyer_opc.enums.DataStyle.*;


/**
 *     ¥¥¥¥¥¥¥¥封印¥¥¥¥¥¥¥¥
 * ¥¥¥¥¥¥¥¥¥用DataCenter¥¥¥¥¥¥¥¥¥
 */
@Component
@Slf4j
public class OpcHandler {
  @Resource
  private OpcPropertiesService opcPropertiesService;
  @Resource
  private DataGroupsService dataGroupsService;

  public void handle() throws Exception {
    // 启动服务
    Server server = new Server(ConnectionInformationConfig(), Executors.newSingleThreadScheduledExecutor());
    server.connect();
    System.out.println("********** OPC State **********"+server.getServerState().getServerState());
    Group group = server.addGroup();

    //一条记录，一个数据组，n条记录，会发送N次。
    getDataGroups().forEach((o) -> {

      String tableName = o.getTableName();
      String apiType = o.getApiType();
      String dataStyle = o.getDataStyle();
      String url = o.getUrl();
      List<Tags> tags = getTags(tableName);
      Map<Tags, Item> itemMap = tagsPutToGroup(tags, group);

      switch (apiType) {
        case WEBSERVICE://dataStyle暂时默认支持一种。
          switch (dataStyle) {
            case STYLE_A:
              List<Data_A> datas = dealStyleA(itemMap);
              OpcAsyncHandler.sendSoap(FastJsonUtil.toJSONString(datas), url,dataStyle);
              break;
            case STYLE_B://TODO
            case STYLE_C://TODO
            case STYLE_D://TODO
          }
          break;
        case HTTP:
          switch (dataStyle) {
            case STYLE_A:
              List<Data_A> datas = dealStyleA(itemMap);
              OpcAsyncHandler.sendHttp(FastJsonUtil.toJSONString(datas), url);
              break;
            case STYLE_B://TODO
            case STYLE_C://TODO
            case STYLE_D://TODO
          }
          break;
        default:
          log.error(">>>>>>>>>>>>>>>>>> Nothing Happend! <<<<<<<<<<<<<<<<<<<");
          break;
      }
    });
    server.disconnect();
  }

  /*
    配置连接信息
   */
  public ConnectionInformation ConnectionInformationConfig(){
    //获取属性
    Map<String, String> properties = getProperties();
    String hostname = properties.get(OpcProp.HOSTNAME);// 电脑IP
    String domain = properties.get(OpcProp.DOMAIN);// 域，为空就行
    String username = properties.get(OpcProp.USERNAME);// 电脑上自己建好的用户名
    String password = properties.get(OpcProp.PASSWORD);// 密码
    String progId = properties.get(OpcProp.PROGID);//服务名称
    String clsid = properties.get(OpcProp.CLSID);// 注册表ID，可以在“组件服务”里看到

    final ConnectionInformation ci = new ConnectionInformation();
    ci.setHost(hostname);
    ci.setDomain(domain);
    ci.setUser(username);
    ci.setPassword(password);
    ci.setClsid(clsid);
    ci.setProgId(progId);
    return ci;
  }

  /*
    将每组tags变量集 PUT到OPC的group里面
   */
  private Map<Tags, Item> tagsPutToGroup(List<Tags> tags, Group group) {

    Map<Tags, Item> itemMap = new HashMap<>();
    tags.forEach((tag) -> {
      try {

        itemMap.put(tag, group.addItem(tag.getItemId()));
      } catch (JIException | AddFailedException e) {
        log.error(">>>>>>>>>>>>>>>>>> Tags Put To OPC Group failed! {1}", e);
      }
    });
    return itemMap;
  }

  /*
    处理A格式下的数据，做好对象集封装
   */
  private List<Data_A> dealStyleA(Map<Tags, Item> itemMap) {
    List<Data_A> datas = new ArrayList<>();
    for (Map.Entry<Tags, Item> entry : itemMap.entrySet()) {
      try {
        Data_A dataA = new Data_A();
        dataA.setValue(entry.getValue().read(false).getValue().getObjectAsDouble());
        dataA.setName(entry.getKey().getAlies());
        datas.add(dataA);
      } catch (JIException e) {
        log.error(">>>>>>>>>>>>>>>>>> DealStyleA failed! {1}", e);
      }
    }
    return datas;
  }



  /*
   * 数据库获取所有的groups
   */
  private List<DataGroups> getDataGroups() {
    return dataGroupsService.list();
  }

  /*
   * 数据库获取所有的groups
   */
  private List<Tags> getTags(String tableName) {
    return dataGroupsService.getAllTagsByTableName(tableName);
  }

  /*
   * 从数据库属性表获取属性集合
   */
  private Map<String, String> getProperties() {
    Map<String, String> result = new HashMap<>();
    List<OpcProperties> opcProperties = opcPropertiesService.list();
    opcProperties.forEach((o) -> result.put(o.getName(), o.getValue()));
    return result;
  }


}
