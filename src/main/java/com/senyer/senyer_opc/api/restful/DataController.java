package com.senyer.senyer_opc.api.restful;

import com.senyer.senyer_opc.opc.OpcAsyncHandler;
import com.senyer.senyer_opc.utils.FastJsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/realtime")
@Api(tags = "根据表名返回特定格式数据给")
public class DataController {

  @GetMapping("styleA")
  @ApiOperation("风格A")
  public String styleA(String tableName){
    return FastJsonUtil.toJSONString(OpcAsyncHandler.dealStyleA(OpcAsyncHandler.globalData.get(tableName)));
  }

  @GetMapping("styleB")
  @ApiOperation("风格B")
  public String styleB(String tableName){
    return FastJsonUtil.toJSONString(OpcAsyncHandler.dealStyleB(OpcAsyncHandler.globalData.get(tableName)));
  }

  @GetMapping("styleC")
  @ApiOperation("风格C")
  public String styleC(String tableName){
    return FastJsonUtil.toJSONString(OpcAsyncHandler.dealStyleC(OpcAsyncHandler.globalData.get(tableName)));
  }

  @GetMapping("styleD")
  @ApiOperation("风格D")
  public String styleD(String tableName){
    return FastJsonUtil.toJSONString(OpcAsyncHandler.dealStyleD(OpcAsyncHandler.globalData.get(tableName)));
  }

  @GetMapping("styleE")
  @ApiOperation("风格E")
  public String styleE(String tableName){
    return FastJsonUtil.toJSONString(OpcAsyncHandler.dealStyleE(OpcAsyncHandler.globalData.get(tableName)));
  }

  @GetMapping("styleF")
  @ApiOperation("风格F")
  public String styleF(String tableName){
    return FastJsonUtil.toJSONString(OpcAsyncHandler.dealStyleF(OpcAsyncHandler.globalData.get(tableName)));
  }
}
