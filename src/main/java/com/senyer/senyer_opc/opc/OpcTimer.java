package com.senyer.senyer_opc.opc;

import com.senyer.senyer_opc.api.websocket.RealTimeDataWebSocket;
import com.senyer.senyer_opc.dto.*;
import com.senyer.senyer_opc.utils.FastJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.da.ItemState;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.senyer.senyer_opc.enums.DataStyle.*;
import static com.senyer.senyer_opc.enums.DataStyle.STYLE_F;

/**
 *     ¥¥¥¥¥¥¥¥封印¥¥¥¥¥¥¥¥
 * ¥¥¥¥¥¥¥¥¥用DataCenter¥¥¥¥¥¥¥¥¥
 */
@Component
@Slf4j
@Lazy(false)
public class OpcTimer implements InitializingBean {

  @Resource
  private OpcHandler opcHandler;

  @Resource
  private OpcAsyncHandler opcAsyncHandler;

  /*
    这个任务会开辟一个全局内存。暂时不清楚会不会造成频繁的GC。
   */
  //@Scheduled(cron = "0/10 * * * * *")
  public void scheduledAsyncOpc(){
    try {
      log.info(">>>>>>>>>>>>>>>>>>>>>>>> Running Async Scheduled OPC ");
      opcAsyncHandler.scheduledHandle();
    } catch (Exception e) {
      log.error(">>>>>>>>>>>>>>>>>>>>>>>> Scheduled Async OPC Error : {1}",e);
    }
  }

  /*
    这个任务会定时推送数据给websocket
   */
  //@Scheduled(cron = "0/8 * * * * *")
  public void websocketPush(){
    try {
      for(Map.Entry<String, String> entry : OpcAsyncHandler.requestInfoWS.entrySet()){
        ConcurrentHashMap<Tags, ItemState> tagsItemStateConcurrentHashMap = OpcAsyncHandler.globalData.get(entry.getKey());
        switch (entry.getValue()) {
          case STYLE_A:
            List<Data_A> dataAs = OpcAsyncHandler.dealStyleA(tagsItemStateConcurrentHashMap);
            RealTimeDataWebSocket.sendMessageTo(FastJsonUtil.toJSONString(dataAs),entry.getKey(),entry.getValue());
            break;
          case STYLE_B:
            List<Data_B> dataBs = OpcAsyncHandler.dealStyleB(tagsItemStateConcurrentHashMap);
            RealTimeDataWebSocket.sendMessageTo(FastJsonUtil.toJSONString(dataBs),entry.getKey(),entry.getValue());
            break;
          case STYLE_C:
            List<Data_C> dataCs = OpcAsyncHandler.dealStyleC(tagsItemStateConcurrentHashMap);
            RealTimeDataWebSocket.sendMessageTo(FastJsonUtil.toJSONString(dataCs),entry.getKey(),entry.getValue());
            break;
          case STYLE_D:
            List<Data_D> dataDs = OpcAsyncHandler.dealStyleD(tagsItemStateConcurrentHashMap);
            RealTimeDataWebSocket.sendMessageTo(FastJsonUtil.toJSONString(dataDs),entry.getKey(),entry.getValue());
            break;
          case STYLE_E:
            List<Data_E> dataEs = OpcAsyncHandler.dealStyleE(tagsItemStateConcurrentHashMap);
            RealTimeDataWebSocket.sendMessageTo(FastJsonUtil.toJSONString(dataEs),entry.getKey(),entry.getValue());
            break;
          case STYLE_F:
            List<Data_F> dataFs = OpcAsyncHandler.dealStyleF(tagsItemStateConcurrentHashMap);
            RealTimeDataWebSocket.sendMessageTo(FastJsonUtil.toJSONString(dataFs),entry.getKey(),entry.getValue());
            break;
          default:
            log.error(">>>>>>>>>>>>>>>>>> 不支持的数据格式，可选：A、B、C、D、E、F! <<<<<<<<<<<<<<<<<<<");
            break;
        }
      }

    } catch (Exception e) {
      log.error(">>>>>>>>>>>>>>>>>>>>>>>> Scheduled Async OPC Error : {1}",e);
    }
  }

  /*
    这个任务，每次需要new新的server端，
   */
  //@Scheduled(cron = "0/10 * * * * *")
  public void scheduledOpc(){
    try {
      Thread.sleep(3000);
      log.info(">>>>>>>>>>>>>>>>>>>>>>>> Running Scheduled OPC ");
      opcHandler.handle();
    } catch (Exception e) {
      log.error(">>>>>>>>>>>>>>>>>>>>>>>> Scheduled OPC Error : {1}",e);
    }
  }

  // 这个保证项目启动时会执行一次
  @Override
  public void afterPropertiesSet() {
    try {
     // opcAsyncHandler.asyncHandle();
    } catch (Exception e) {
      log.error(">>>>>>>>>>>>>>>>>>>>>>>> Init OPC Error : {1}",e);
    }
    //this.scheduledOpc();
    //this.scheduledAsyncOpc();
  }
}
