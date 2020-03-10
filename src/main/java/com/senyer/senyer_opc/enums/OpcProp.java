package com.senyer.senyer_opc.enums;

public interface OpcProp {
  String HOSTNAME="hostname";//主机ip
  String DOMAIN="domain";//域名，一般为空即可
  String USERNAME="username";//计算机用户名
  String PASSWORD="password";//计算机密码
  String PROGID="progId";//服务名称
  String CLSID="clsid";//注册列表里对应的服务id
  String INTERVAL_TIME="interval_time";//数据读取间隔时间
}
