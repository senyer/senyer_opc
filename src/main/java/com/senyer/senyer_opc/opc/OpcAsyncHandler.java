package com.senyer.senyer_opc.opc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.webservice.SoapClient;
import com.senyer.senyer_opc.dto.*;
import com.senyer.senyer_opc.persistence.domain.DataGroups;
import com.senyer.senyer_opc.persistence.domain.OpcProperties;
import com.senyer.senyer_opc.service.DataGroupsService;
import com.senyer.senyer_opc.service.OpcPropertiesService;
import com.senyer.senyer_opc.utils.DateUtil;
import com.senyer.senyer_opc.utils.FastJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.*;
import org.openscada.opc.lib.da.browser.FlatBrowser;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import static com.senyer.senyer_opc.enums.ApiType.HTTP;
import static com.senyer.senyer_opc.enums.ApiType.WEBSERVICE;
import static com.senyer.senyer_opc.enums.DataStyle.*;
import static com.senyer.senyer_opc.enums.DataStyle.STYLE_D;

/**
 *     ¥¥¥¥¥¥¥¥封印¥¥¥¥¥¥¥¥
 * ¥¥¥¥¥¥¥¥¥用DataCenter¥¥¥¥¥¥¥¥¥
 */
@Component
@Slf4j
public class OpcAsyncHandler {

  public static final ConcurrentHashMap<String, ConcurrentHashMap<Tags, ItemState>> globalData = new ConcurrentHashMap<>();
  public static final ConcurrentHashMap<String ,String> requestInfoWS=new ConcurrentHashMap<>();
  private static final Integer INTERVAL_TIME=8000;//循环间隔时间。

  @Resource
  private OpcPropertiesService opcPropertiesService;
  @Resource
  private DataGroupsService dataGroupsService;
  @Resource
  private OpcHandler opcHandler;

  /**
   * 实现server的连接，以及将变量及其信息存放到全局map里面。
   * @throws Exception
   */
  public void asyncHandle() throws Exception {
    ConnectionInformation connectionInformation = opcHandler.ConnectionInformationConfig();
    Server server = new Server(connectionInformation, Executors.newSingleThreadScheduledExecutor());
    AutoReconnectController controller = new AutoReconnectController(server);//支持断线重连
    controller.connect();
    server.addStateListener((state) -> log.info("<><><><><><><><><><><><><><><<>><> connectionStateChanged state= {}", state));
    log.info("********** Async OPC Init!!!! ********** ");
    log.info("********** Async OPC State ********** {}" , server.getServerState().getServerState());


    //一条记录，一个数据组，n条记录，会发送N次。
    getDataGroups().forEach((o) -> {
      String tableName = o.getTableName();
      List<Tags> tags = getTags(tableName);
      try {
        final AccessBase access = new SyncAccess(server, INTERVAL_TIME);
        tagsPutToAccessToGlobal(tableName, tags, access);
        access.bind();
      } catch (final JIException | UnknownHostException | NotConnectedException | DuplicateGroupException e) {
        log.error(">>>>>>>>>>>>>>>>>> asyncHandle failed! {1}", e);
        controller.disconnect();
      }
    });
  }

  /**
   * 定时任务，定时推送信息给接口
   */
  public void scheduledHandle() {
    //一条记录，一个数据组，n条记录，会发送N次。
    getDataGroups().forEach((o) -> {
      String tableName = o.getTableName();
      String apiType = o.getApiType();
      String dataStyle = o.getDataStyle();
      String url = o.getUrl();
      ConcurrentHashMap<Tags, ItemState> tagsItemStateConcurrentHashMap = globalData.get(tableName);
      switch (apiType) {
        case WEBSERVICE://dataStyle暂时默认支持一种。
          switch (dataStyle) {
            case STYLE_A:
              List<Data_A> dataAs = dealStyleA(tagsItemStateConcurrentHashMap);
              sendSoap(FastJsonUtil.toJSONString(dataAs), url,dataStyle);
              break;
            case STYLE_B:
              List<Data_B> dataBs = dealStyleB(tagsItemStateConcurrentHashMap);
              sendSoap(FastJsonUtil.toJSONString(dataBs), url,dataStyle);
              break;
            case STYLE_C:
              List<Data_C> dataCs = dealStyleC(tagsItemStateConcurrentHashMap);
              sendSoap(FastJsonUtil.toJSONString(dataCs), url,dataStyle);
              break;
            case STYLE_D:
              List<Data_D> dataDs = dealStyleD(tagsItemStateConcurrentHashMap);
              sendSoap(FastJsonUtil.toJSONString(dataDs), url,dataStyle);
              break;
            case STYLE_E:
              List<Data_E> dataEs = dealStyleE(tagsItemStateConcurrentHashMap);
              sendSoap(FastJsonUtil.toJSONString(dataEs), url,dataStyle);
              break;
            case STYLE_F:
              List<Data_F> dataFs = dealStyleF(tagsItemStateConcurrentHashMap);
              sendSoap(FastJsonUtil.toJSONString(dataFs), url,dataStyle);
              break;
              default:
                log.error(">>>>>>>>>>>>>>>>>> 不支持的数据格式，可选：A、B、C、D、E、F! <<<<<<<<<<<<<<<<<<<");
                break;
          }
          break;
        case HTTP:
          switch (dataStyle) {
            case STYLE_A:
              List<Data_A> dataAs = dealStyleA(tagsItemStateConcurrentHashMap);
              sendHttp(FastJsonUtil.toJSONString(dataAs), url);
              break;
            case STYLE_B:
              List<Data_B> dataBs = dealStyleB(tagsItemStateConcurrentHashMap);
              sendHttp(FastJsonUtil.toJSONString(dataBs), url);
              break;
            case STYLE_C:
              List<Data_C> dataCs = dealStyleC(tagsItemStateConcurrentHashMap);
              sendHttp(FastJsonUtil.toJSONString(dataCs), url);
              break;
            case STYLE_D:
              List<Data_D> dataDs = dealStyleD(tagsItemStateConcurrentHashMap);
              sendHttp(FastJsonUtil.toJSONString(dataDs), url);
              break;
            case STYLE_E:
              List<Data_E> dataEs = dealStyleE(tagsItemStateConcurrentHashMap);
              sendHttp(FastJsonUtil.toJSONString(dataEs), url);
              break;
            case STYLE_F:
              List<Data_F> dataFs = dealStyleF(tagsItemStateConcurrentHashMap);
              sendHttp(FastJsonUtil.toJSONString(dataFs), url);
              break;
          }
          break;
        default:
          log.error(">>>>>>>>>>>>>>>>>> 不支持的API格式，可选：http、webservice! <<<<<<<<<<<<<<<<<<<");
          break;
      }
    });
  }
  /*
  处理A格式下的数据，做好对象集封装
 */
  public static List<Data_A> dealStyleA(ConcurrentHashMap<Tags, ItemState> itemMap) {
    List<Data_A> datas = new ArrayList<>();
    if (itemMap==null||itemMap.isEmpty()) {
      return datas;
    }
    for (Map.Entry<Tags, ItemState> entry : itemMap.entrySet()) {
      try {
        Data_A dataA = new Data_A();
        dataA.setValue(entry.getValue().getValue().getObject());
        dataA.setName(entry.getKey().getAlies());
        datas.add(dataA);
      } catch (JIException e) {
        log.error(">>>>>>>>>>>>>>>>>> Async DealStyleA failed! {1}", e);
      }
    }
    return datas;
  }
  /*
    处理A格式下的数据，做好对象集封装
   */
  public static List<Data_B> dealStyleB(ConcurrentHashMap<Tags, ItemState> itemMap) {
    List<Data_B> datas = new ArrayList<>();
    if (itemMap==null||itemMap.isEmpty()) {
      return datas;
    }
    for (Map.Entry<Tags, ItemState> entry : itemMap.entrySet()) {
      try {
        Data_B dataB = new Data_B();
        dataB.setValue(entry.getValue().getValue().getObject());
        dataB.setName(entry.getKey().getAlies());
        dataB.setUnit(entry.getKey().getUnit());
        datas.add(dataB);
      } catch (JIException e) {
        log.error(">>>>>>>>>>>>>>>>>> Async DealStyleB failed! {1}", e);
      }
    }
    return datas;
  }
  /*
    处理A格式下的数据，做好对象集封装
   */
  public static List<Data_C> dealStyleC(ConcurrentHashMap<Tags, ItemState> itemMap) {
    List<Data_C> datas = new ArrayList<>();
    if (itemMap==null||itemMap.isEmpty()) {
      return datas;
    }
    for (Map.Entry<Tags, ItemState> entry : itemMap.entrySet()) {
      try {
      log.info(">>>>>>>>RawData>>entry.toString()>>>>>>>> {}", entry.toString());
      log.info(">>>>>>>>RawData>>entry.getKey()>>>>>>>> {}", entry.getKey());
      log.info(">>>>>>>>RawData>>entry.getValue()>>>>>>>> {}", entry.getValue());
      log.info(">>>>>>>>RawData>>entry.getValue().getValue().getObject()>>>>>>>> {}", entry.getValue().getValue().getObject());

        Data_C dataC = new Data_C();
        dataC.setValue(entry.getValue().getValue().getObject());
        dataC.setName(entry.getKey().getAlies());
        dataC.setUnit(entry.getKey().getUnit());
        dataC.setTime(DateUtil.calenderToString(entry.getValue().getTimestamp()));
        datas.add(dataC);
      } catch (Exception e) {
        log.error(">>>>>>>>>>>>>>>>>> Async DealStyleC failed! {1}", e);
      }
    }
    return datas;
  }
  /*
    处理A格式下的数据，做好对象集封装
   */
  public static List<Data_D> dealStyleD(ConcurrentHashMap<Tags, ItemState> itemMap) {
    List<Data_D> datas = new ArrayList<>();
    if (itemMap==null||itemMap.isEmpty()) {
      return datas;
    }
    for (Map.Entry<Tags, ItemState> entry : itemMap.entrySet()) {
      try {
        Data_D dataD = new Data_D();
        dataD.setValue(entry.getValue().getValue().getObject());
        dataD.setName(entry.getKey().getAlies());
        dataD.setTime(DateUtil.calenderToString(entry.getValue().getTimestamp()));
        datas.add(dataD);
      } catch (JIException e) {
        log.error(">>>>>>>>>>>>>>>>>> Async DealStyleD failed! {1}", e);
      }
    }
    return datas;
  }
  /*
    处理A格式下的数据，做好对象集封装
   */
  public static List<Data_E> dealStyleE(ConcurrentHashMap<Tags, ItemState> itemMap) {
    List<Data_E> datas = new ArrayList<>();
    if (itemMap==null||itemMap.isEmpty()) {
      return datas;
    }
    for (Map.Entry<Tags, ItemState> entry : itemMap.entrySet()) {
      try {
        Data_E dataE = new Data_E();
        dataE.setValue(entry.getValue().getValue().getObject());
        dataE.setId(entry.getKey().getSeqId());
        dataE.setTime(DateUtil.calenderToString(entry.getValue().getTimestamp()));
        datas.add(dataE);
      } catch (JIException e) {
        log.error(">>>>>>>>>>>>>>>>>> Async DealStyleE failed! {1}", e);
      }
    }
    return datas;
  }
  /*
    处理A格式下的数据，做好对象集封装
   */
  public static List<Data_F> dealStyleF(ConcurrentHashMap<Tags, ItemState> itemMap) {
    List<Data_F> datas = new ArrayList<>();
    if (itemMap==null||itemMap.isEmpty()) {
      return datas;
    }
    for (Map.Entry<Tags, ItemState> entry : itemMap.entrySet()) {
      try {
        Data_F dataF = new Data_F();
        dataF.setValue(entry.getValue().getValue().getObject());
        dataF.setId(entry.getKey().getSeqId());
        datas.add(dataF);
      } catch (JIException e) {
        log.error(">>>>>>>>>>>>>>>>>> Async DealStyleF failed! {1}", e);
      }
    }
    return datas;
  }


  /**
   * 检查opc server中是否包含指定的监测点列表
   */
  public boolean checkItemList(List<String> list, Server server) {
    // 获取opc server上的所有检测点
    FlatBrowser flatBrowser = server.getFlatBrowser();
    if (flatBrowser == null) {
      return false;
    }

    try {
      Collection<String> collection = flatBrowser.browse();
      return collection.containsAll(list);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /*
    将每组tags变量集 PUT到OPC的access里面
   */
  private void tagsPutToAccessToGlobal(String tableName, List<Tags> tags, AccessBase access) {
    final ConcurrentHashMap<Tags, ItemState> itemMap;
    if (globalData.get(tableName) == null) {
      itemMap = new ConcurrentHashMap<>();
    } else {
      itemMap = globalData.get(tableName);
    }
    tags.forEach((tag -> {
      try {
        access.addItem(tag.getItemId(), (item, state) -> {
                  if (item != null) {
                    itemMap.put(tag, state);
                  }//bb263c82-0bb6-93d2-81c1-00c04f790f3b//bb352c72-0bb6-93d2-81c1-00c04f790f3b
                }
        );
      } catch (JIException | AddFailedException e) {
        log.error(">>>>>>>>>>>>>>>>>> tagsPutToAccessToGlobal failed! {1}", e);
      }
    }));
    globalData.remove(tableName);
    globalData.put(tableName, itemMap);
  }
  /*
      调用接口，发送Soap数据
     */
  protected static void sendSoap(String data, String url,String dataStyle) {

    if (StrUtil.isNullOrUndefined(url)) return;
    try {
      String[] strs = url.split(",");
      String wsdl = strs[0];
      String method = strs[1];
      String namespace = strs[2];
      String param = strs[3];
      SoapClient.create(wsdl)
              .setMethod(method, namespace)
              .setParam(param, data)
              .setParam("type", dataStyle)
              .setConnectionTimeout(5000)
              .setReadTimeout(5000)
              .send(true);
      log.info(">>>>>>>>>>>>>>>>>> SoapClient send Success! {}", url);
      log.info("==========data======== {}", data);
    } catch (Exception e) {
      log.error(">>>>>>>>>>>>>>>>>> SoapClient send failed! url :{},exception:{2}",url, e);
    }
  }

  /*
    调用接口，发送Http数据
   */
  protected static void sendHttp(String data, String url) {
    if (StrUtil.isNullOrUndefined(url)) return;
    try {
      String[] strs = url.split(",");
      String address = strs[0];
      String requestType = strs[1];
      String param = strs[2];
      if ("post".equals(requestType) || "POST".equals(requestType)) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put(param, data);
        HttpUtil.get(address, paramMap, 5000);
      }
      if ("get".equals(requestType) || "GET".equals(requestType)) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put(param, data);
        HttpUtil.post(address, paramMap, 5000);
      }
      log.info(">>>>>>>>>>>>>>>>>> HttpClient send Success! {}", url);
      log.info("==========data======== {}", data);
    } catch (Exception e) {
      log.error(">>>>>>>>>>>>>>>>>> HttpClient send failed! url :{},exception:{2}", url,e);
    }
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
  private List<DataGroups> getGroups() {
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
