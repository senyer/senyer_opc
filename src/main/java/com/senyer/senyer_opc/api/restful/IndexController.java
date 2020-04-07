package com.senyer.senyer_opc.api.restful;

import com.senyer.senyer_opc.datacenter.InfluxDBProperties;
import com.senyer.senyer_opc.opc.OpcAsyncHandler;
import com.senyer.senyer_opc.utils.DateUtil;
import com.senyer.senyer_opc.utils.FastJsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
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
    @ApiOperation("从influxDB获取历史数据")
    @ApiImplicitParam(value = "需要查询的表名（变量清单）" ,required = true,name = "measurements" )
    public Map<String , Object> fromMemory(String[] measurements, String beginTime, String endTime){
        Map<String , Object> result= new HashMap<>();


        List<Map<Object,Object>> datas = new ArrayList<>();

        String beginTimeToUTC = localToUTC(beginTime);
        String endTimeToUTC = localToUTC(endTime);

        try (InfluxDB influxDB = InfluxDBFactory.connect(influxDBProperties.getUrl(), influxDBProperties.getUser(), influxDBProperties.getPassword())){
            for (int i = 0; i < measurements.length; i++) {
                Map<Object,Object> data = new TreeMap<>();
                Query query = select().column("value").from(influxDBProperties.getDatabase(),measurements[i])
                        .where(gte("time",beginTimeToUTC))
                        .and(lte("time",endTimeToUTC));
                QueryResult queryResult = influxDB.query(query);
                System.out.println(queryResult);
                List<List<Object>> querys = queryResult.getResults().get(0).getSeries().get(0).getValues();
                querys.forEach((list)->{
                    data.put(list.get(0),list.get(1)); // time + value
                });
                datas.add(data);
            }
            result.put("code",200);
            result.put("status","OK");
            result.put("data",datas);
        } catch (Exception e) {
            result.put("code",500);
            result.put("status","inner error");
            result.put("data",e);
        }

        return result;
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



}
