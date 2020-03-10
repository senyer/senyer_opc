package com.senyer.senyer_opc.api.websocket;

import com.senyer.senyer_opc.opc.OpcAsyncHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 模拟的生产数据推送：假数据！
 * 正式数据由张馨予的ws提供。
 */
@Component
@ServerEndpoint(value = "/realtime/{style}/{tickets}")
@Slf4j
public class RealTimeDataWebSocket {
  private static int onlineCount = 0;
  private static CopyOnWriteArraySet<RealTimeDataWebSocket> webSocketSet = new CopyOnWriteArraySet<>();
  private Session session;
  private String tickets;
  private String style;

  @OnOpen
  public void onOpen(@PathParam("tickets") String tickets, @PathParam("style") String style, Session session) throws Exception {
    addOnlineCount();
    this.session = session;
    log.info("【生产数据】已加入session-id： {}",session.getId());
    log.info("tickets： {}",tickets);
    log.info("style： {}",style);
    this.tickets=tickets;
    this.style=style;
    OpcAsyncHandler.requestInfoWS.put(tickets,style);
    webSocketSet.add(this); // 加入set中
    System.out.println("【生产数据】有连接接入，当前连接数：" + getOnlineCount());
    //这里对用户传递的信息做处理，
    // handlerResponse(null);
  }

  @OnClose
  public void onClose() throws Exception {
    subOnlineCount();
    OpcAsyncHandler.requestInfoWS.remove(tickets);
    webSocketSet.remove(this); // 从set中删除
    System.out.println("【生产数据】有连接断开，当前连接数：" + getOnlineCount());
  }

  @OnMessage
  public void onMessage(String message, Session session) throws IOException {
  }

  @OnError
  public void onError(Session session, Throwable error) {
    OpcAsyncHandler.requestInfoWS.remove(tickets);
    System.out.println("【生产数据】onError:::id=" + session.getId() + "的用户,连接出错,原因:" + error);
  }

  public void sendMessage(String message) throws IOException {
    this.session.getBasicRemote().sendText(message);
    // this.session.getAsyncRemote().sendText(message);
  }


  public static void sendMessageTo(String message, String tickets, String style) throws IOException {
    for (RealTimeDataWebSocket item : webSocketSet) {
      if(item.tickets.equals(tickets)&&item.style.equals(style)){
        item.session.getBasicRemote().sendText(message);
      }
    }
  }
  /**
   * 群发自定义消息
   *
   **/
  public static void sendInfo(String message) throws IOException {
    // 群发消息
    for (RealTimeDataWebSocket item : webSocketSet) {
      item.sendMessage(message);
    }
  }



  // 返回连接总数
  private static synchronized int getOnlineCount() {
    return onlineCount;
  }

  // 用户量加一
  private static synchronized void addOnlineCount() {
    RealTimeDataWebSocket.onlineCount++;
  }

  // 用户量减一
  private static synchronized void subOnlineCount() {
    RealTimeDataWebSocket.onlineCount--;
  }

  private void handlerResponse(String message) throws IOException {
    sendInfo(message);
  }

}
