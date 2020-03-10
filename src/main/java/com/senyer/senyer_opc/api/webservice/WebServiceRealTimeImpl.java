package com.senyer.senyer_opc.api.webservice;

import com.senyer.senyer_opc.dto.*;
import com.senyer.senyer_opc.opc.OpcAsyncHandler;
import com.senyer.senyer_opc.utils.FastJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.da.ItemState;
import org.springframework.stereotype.Service;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import static com.senyer.senyer_opc.enums.DataStyle.*;
import static com.senyer.senyer_opc.enums.DataStyle.STYLE_F;

@Service
@WebService(serviceName = "RealTimeData", // 与接口中指定的name一致
        targetNamespace = "http://webservice.senyer.com/", // 与接口中的命名空间一致,一般是接口的包名倒
        endpointInterface = "com.senyer.senyer_opc.api.webservice.RealTimeData" // 接口地址
)
@Slf4j
@BindingType(value= SOAPBinding.SOAP12HTTP_BINDING)
public class WebServiceRealTimeImpl implements RealTimeData {

  @Override
  public String realtime(String tableName,String type)  {
    try {
      log.info("【【receive】】tableName :{} ，type：  {}", tableName,type);
      ConcurrentHashMap<Tags, ItemState> map = OpcAsyncHandler.globalData.get(tableName);
      switch (type) {
        case STYLE_A:
          List<Data_A> dataAs = OpcAsyncHandler.dealStyleA(map);
          return FastJsonUtil.toJSONString(dataAs);
        case STYLE_B:
          List<Data_B> dataBs = OpcAsyncHandler.dealStyleB(map);
          return FastJsonUtil.toJSONString(dataBs);
        case STYLE_C:
          List<Data_C> dataCs = OpcAsyncHandler.dealStyleC(map);
          return FastJsonUtil.toJSONString(dataCs);
        case STYLE_D:
          List<Data_D> dataDs = OpcAsyncHandler.dealStyleD(map);
          return FastJsonUtil.toJSONString(dataDs);
        case STYLE_E:
          List<Data_E> dataEs = OpcAsyncHandler.dealStyleE(map);
          return FastJsonUtil.toJSONString(dataEs);
        case STYLE_F:
          List<Data_F> dataFs = OpcAsyncHandler.dealStyleF(map);
          return FastJsonUtil.toJSONString(dataFs);
        default:
          log.error(">>>>>>>>>>>>>>>>>> 不支持的数据格式，可选：A、B、C、D、E、F! <<<<<<<<<<<<<<<<<<<");
          break;
      }
    }catch (Exception e) {
      log.error("Inner Error : {}",e.toString());
      return "inner error";
    }
    return "Not found data";
  }

}
