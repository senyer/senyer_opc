package com.senyer.senyer_opc.api.restful;

import com.senyer.senyer_opc.datacenter.InfluxDBProperties;
import com.senyer.senyer_opc.opc.OpcAsyncHandler;
import com.senyer.senyer_opc.utils.DateUtil;
import com.senyer.senyer_opc.utils.FastJsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.influxdb.querybuilder.BuiltQuery.QueryBuilder.*;

@RestController
@RequestMapping("/")
@Api(tags = "返回实时数据和历史数据")
public class IndexController {

    @Autowired
    private InfluxDBProperties influxDBProperties;

    @GetMapping("/realtime/fromDB")
    @ApiOperation("从数据库获取实时数据")
    @ApiImplicitParam(value = "需要查询的表名（变量清单）" ,required = true,name = "tableName" )
    public String fromDB(String tableName){
        return FastJsonUtil.toJSONString(OpcAsyncHandler.dealStyleA(OpcAsyncHandler.globalData.get(tableName)));
    }

    @GetMapping("/realtime/fromMemory")
    @ApiOperation("从内存获取实时数据")
    @ApiIgnore
    @ApiImplicitParam(value = "需要查询的表名（变量清单）" ,required = true,name = "tableName" )
    public String fromMemory(String tableName){
        return "未实现";
    }


    @GetMapping("/history")
    @ApiOperation("从influxDB获取历史数据（全部的）")
    @ApiImplicitParams ({
        @ApiImplicitParam(value = "需要查询的表名（变量清单）支持多个字段同时查询" ,required = true,name = "measurements" ),
        @ApiImplicitParam(value = "起始时间" ,required = true,name = "beginTime" ),
        @ApiImplicitParam(value = "截止时间" ,required = true,name = "endTime" )
    })
    public Map<String , Object> fromMemory(String[] measurements, String beginTime, String endTime){
        Map<String , Object> results= new HashMap<>();
        Map<String , Object> result= new HashMap<>();
        String beginTimeToUTC = localToUTC(beginTime);
        String endTimeToUTC = localToUTC(endTime);

        try (InfluxDB influxDB = InfluxDBFactory.connect(influxDBProperties.getUrl(), influxDBProperties.getUser(), influxDBProperties.getPassword())){
            for (int i = 0; i < measurements.length; i++) {

                List<Map<Object,Object>> datas = new ArrayList<>();
                Map<Object,Object> data = new TreeMap<>();
                Query query = select().column("value").from(influxDBProperties.getDatabase(),measurements[i])
                        .where(gte("time",beginTimeToUTC))
                        .and(lte("time",endTimeToUTC));
                QueryResult queryResult = influxDB.query(query);
                System.out.println(queryResult);
                List<List<Object>> querys = queryResult.getResults().get(0).getSeries().get(0).getValues();
                querys.forEach((list)->{
                    data.put(UTCToCST(String.valueOf(list.get(0))),list.get(1)); // time + value
                });
                datas.add(data);
                result.put(measurements[i],datas);
            }
            results.put("code",200);
            results.put("status","OK");
            results.put("data",result);
        } catch (Exception e) {
            results.put("code",500);
            results.put("status","inner error");
            results.put("data",e);
        }

        return results;
    }







    /**
     * local时间转换成UTC时间（仅仅转换格式，不转换时间差：因为fluxdb存的时间加了8小时。)
     * @param localTime localTime
     * @return localToUTC
     */
    public static String localToUTC(String localTime) {
        Date date = DateUtil.stringToDate(localTime, DateUtil.DATETIME_NORMAL_FORMAT);
        return DateUtil.dateToString(date, "yyyy-MM-dd'T'HH:mm:ss'Z'");
    }


    /**
     * 这里只是转换格式，时区不做调整，因为录入的时候加好了间隔时间（似乎不太合理，哈哈）。
     * @param UTCStr UTC時間
     * @return
     */
    public static String UTCToCST(String UTCStr) {
        Date date = null;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            date = sdf.parse(UTCStr);
        } catch (ParseException e) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            try {
                date = sdf.parse(UTCStr);
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
        //System.out.println("UTC时间: " + date);
        //Calendar calendar = Calendar.getInstance();
        //calendar.setTime(date);
        //calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 8);
        //calendar.getTime() 返回的是Date类型，也可以使用calendar.getTimeInMillis()获取时间戳

        return DateUtil.dateToString(date, DateUtil.DATETIME_NORMAL_FORMAT);
    }


}
