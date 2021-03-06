package com.senyer.senyer_opc.datacenter;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.senyer.senyer_opc.dto.Tags;
import com.senyer.senyer_opc.persistence.domain.OpcPropertiesGroup;
import com.senyer.senyer_opc.persistence.domain.OpcRealtimedata;
import com.senyer.senyer_opc.service.DataGroupsService;
import com.senyer.senyer_opc.service.OpcPropertiesGroupService;
import com.senyer.senyer_opc.service.OpcRealtimedataService;
import com.senyer.senyer_opc.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDBFactory;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <><><><><> 数据处理中心 <><><><><>
 * <p>
 * 1. 与n个Opc服务端建立连接
 * 2. 获取所有服务端配置的变量数据
 * 3. 将实时数据存入到实时数据表
 * 4. 将实时数据存入一份内存
 * 5. 将实时数据写入到influxDB
 */

@Component
@Slf4j
public class DataCenter implements InitializingBean {

    private InfluxDB influxDB;
    //Check if a DB has been created in influxDB.
    private static AtomicBoolean HASDB = new AtomicBoolean(false);
    //存储全局变量至内存
    public static final ConcurrentHashMap<OpcPropertiesGroup, ConcurrentHashMap<Tags, ItemState>> memoryData = new ConcurrentHashMap<>();
    //存储所有创建连接的opc服务端。
    public static final ConcurrentHashMap<OpcPropertiesGroup, Server> opcServers = new ConcurrentHashMap<>();
    //循环读取的间隔时间,8s。
    private static final Integer INTERVAL_TIME = 8000;

    @Autowired
    private InfluxDBProperties influxDBProperties;
    @Resource
    private OpcPropertiesGroupService opcPropertiesGroupService;
    @Resource
    private DataGroupsService dataGroupsService;
    @Resource
    private OpcRealtimedataService opcRealtimedataService;


    // 解决无法在构造器里读取到@Value数据的问题。
    @Override
    public void afterPropertiesSet() throws Exception {
        influxDB = InfluxDBFactory.connect(influxDBProperties.getUrl(), influxDBProperties.getUser(), influxDBProperties.getPassword());
        influxDB.enableBatch(BatchOptions.DEFAULTS.actions(2000).flushDuration(100));
    }

    /**
     * 读取每个客户端的数据
     */
    public void exec() {
        creatConnection();//创建所有服务端的连接
        writeToInfluxDBOrMemoryOrDB();//将读取到的数据写入到内存和influx里面或者数据库里。
    }


    /**
     * 创建所有服务端的连接
     */
    public void creatConnection() {
        try{
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

                //建立连接：
                Server server = new Server(connectionInformation, Executors.newSingleThreadScheduledExecutor());
                AutoReconnectController controller = new AutoReconnectController(server);//支持断线重连
                controller.connect();

                log.info("********** Async OPC Init!!!! ********** ");
                log.info("********** hostname ******************** {}", hostname);
                log.info("********** domain ********************** {}", domain);
                log.info("********** progId ********************** {}", progId);
                log.info("********** clsid *********************** {}", clsid);
                log.info("********** Async OPC State ************* {}", server.getServerState());

                opcServers.put(prop, server);
            });
            log.info("<><><><><><><><><><><><> 共建立连接 {} 个<><><><><><><><><><><><> ", listProp.size());
        }catch (Exception e) {
            log.error("<><><><><><><><><><><><> 创建服务端连接失败 <><><><><><><><><><><><> {1}", e);
        }
    }

    /*
     * 遍历所有的服务，实现数据的读取并写入到内存、influxDB和数据库
     */
    private void writeToInfluxDBOrMemoryOrDB() {

        opcServers.forEach((opcProp, server) -> {
            try {
                List<Tags> tags = dataGroupsService.getAllTagsByTableName(opcProp.getTableName());
                final AccessBase access = new SyncAccess(server, INTERVAL_TIME);
                execOneserver(opcProp, tags, access);
                access.bind();
            } catch (final JIException | UnknownHostException | NotConnectedException | DuplicateGroupException e) {
                log.error(">>>>>>>>>>>>>>>>>> asyncHandle failed! {1}", e);
                //controller.disconnect(); //TODO (senyer) fix it.
                server.disconnect();
            }
        });
    }

    /*
      将每组tags变量集 PUT到OPC的access里面
      accessReadOneserver只会触发一次，
      但是access.addItem(ItemId,(item,state)->{})这个方法会根据INTERVAL_TIME，定时触发！
     */
    private void execOneserver(OpcPropertiesGroup opcProp, List<Tags> tags, AccessBase access) {
        tags.forEach((tag -> {
            try {
                access.addItem(tag.getItemId(), (item, state) -> {
                    log.info(">>>>>>>>>>>>>>>>>> opcProp : {}", opcProp);
                    log.info(">>>>>>>>>>>>>>>>>> tag : {}", tag);
                    log.info(">>>>>>>>>>>>>>>>>> state : {}", state);
                    log.info(">>>>>>>>>>>>>>>>>> item : {}", item);
                            if (item != null) {
                                writeToMemory(tag, state, opcProp);     //To Memory
                                writeToInfluxDB(tag, state);            //To InfluxDB
                                writeToDB(tag, state);                  //To DB
                            }
                        }
                );
            } catch (JIException | AddFailedException e) {
                log.error(">>>>>>>>>>>>>>>>>> accessReadOneserver failed! {1}", e);
            }
        }));
    }


    /**
     * TODO （senyer） 这里需要考虑一下，是否会造成内存溢出！！！！！
     *
     * Write To Memory
     *
     * @param tag   变量名
     * @param state 读取的数据对象。
     */
    private void writeToMemory(Tags tag, ItemState state, OpcPropertiesGroup opcProp) {
        try {
            final ConcurrentHashMap<Tags, ItemState> itemMap;
            if (memoryData.get(opcProp) == null) {
                itemMap = new ConcurrentHashMap<>();
            } else {
                itemMap = memoryData.get(opcProp);
            }
            itemMap.put(tag, state);
            memoryData.put(opcProp, itemMap);
        } catch(Exception e) {
            log.error("<><><><><><><><><><><><> Write To Memory Failed <><><><><><><><><><><><> {1}", e);
        }

    }

    /**
     * Write To InfluxDB
     *
     * @param tag   tag
     * @param state state
     */
    private void writeToInfluxDB(Tags tag, ItemState state) {
        try {
            hasDBOrCreate();
            influxDB.write(influxDBProperties.getDatabase(), "autogen", buildPoint(tag, state));
        } catch(Exception e) {
            log.error("<><><><><><><><><><><><> Write To InfluxDB Failed <><><><><><><><><><><><> {1}", e);
        }

    }


    /**
     * Write To DB
     * //TODO 这些操作后续可以考虑异步化处理
     * @param tag tag
     * @param state state
     */
    private void writeToDB(Tags tag, ItemState state) {
        try {
            Object value = readValue(state);
            String timeStamp = readTimeStamp(state);

            OpcRealtimedata opcRealtimedata = new OpcRealtimedata();
            opcRealtimedata.setAlias(tag.getAlies());
            opcRealtimedata.setValue(String.valueOf(value));
            opcRealtimedata.setTime(DateUtil.stringToDate(timeStamp,DateUtil.DATETIME_NORMAL_FORMAT));
            opcRealtimedataService.saveOrUpdate(opcRealtimedata,new UpdateWrapper<OpcRealtimedata>().eq("alias",tag.getAlies()));
        } catch(Exception e) {
            log.error("<><><><><><><><><><><><> Write To DB Failed <><><><><><><><><><><><> {1}", e);
        }

    }



    /**
     * 判断influxdb数据库是否已经创建，没有创建则create
     */
    private void hasDBOrCreate() {
        if (!HASDB.get()) {
            QueryResult showDatabases = influxDB.query(new Query("show databases"));
            List<List<Object>> values = showDatabases.getResults().get(0).getSeries().get(0).getValues();
            values.forEach((objectList) -> {
                String dbName = String.valueOf(values.get(0).get(0));
                if (influxDBProperties.getDatabase().equals(dbName)) {
                    HASDB.set(true);
                }
            });
            if (!HASDB.get()) {
                influxDB.query(new Query("CREATE DATABASE " + influxDBProperties.getDatabase()));
                HASDB.set(true);
            }
        }
    }

    /**
     * 构建Point对象，
     * 根据value的类型，生成特定的point对象。（对应数据库的一行表记录概念，value字段的类型，influx只支持那么几个）
     *
     * @param tag   tag对象
     * @param state state对象
     * @return Point
     */
    private Point buildPoint(Tags tag, ItemState state) {
        String timeStamp = readTimeStamp(state);
        Object value = readValue(state);

        Point point = null;
        if (value instanceof Boolean) {
            point = Point
                    .measurement(tag.getAlies())
                    .time(System.currentTimeMillis() + 28800000, TimeUnit.MILLISECONDS)//UTC+8个小时等于北京时间
                    .addField("value", (boolean) value)
                    .addField("sourceTime", timeStamp)
                    .build();
        } else if (value instanceof Double) {
            point = Point
                    .measurement(tag.getAlies())
                    .time(System.currentTimeMillis() + 28800000, TimeUnit.MILLISECONDS)//UTC+8个小时等于北京时间
                    .addField("value", (double) value)
                    .addField("sourceTime", timeStamp)
                    .build();
        } else if (value instanceof Integer) {
            point = Point
                    .measurement(tag.getAlies())
                    .time(System.currentTimeMillis() + 28800000, TimeUnit.MILLISECONDS)//UTC+8个小时等于北京时间
                    .addField("value", (int) value)
                    .addField("sourceTime", timeStamp)
                    .build();
        } else if (value instanceof BigDecimal) {
            point = Point
                    .measurement(tag.getAlies())
                    .time(System.currentTimeMillis() + 28800000, TimeUnit.MILLISECONDS)//UTC+8个小时等于北京时间
                    .addField("value", String.valueOf(value))
                    .addField("sourceTime", timeStamp)
                    .build();
        } else {
            point = Point
                    .measurement(tag.getAlies())
                    .time(System.currentTimeMillis() + 28800000, TimeUnit.MILLISECONDS)//UTC+8个小时等于北京时间
                    .addField("value", String.valueOf(value))
                    .addField("sourceTime", timeStamp)
                    .build();
        }
        return point;
    }


    /**
     * Read Value From ItemState
     * @param state data object
     * @return
     */
    private Object readValue(ItemState state) {
        Object value = null;
        try {
            if (state.getQuality() == 0) {
                value = "未读取到数据";
            } else {
                value = state.getValue().getObject();
            }
        } catch (JIException e) {
            log.error(">>>>>>>>>>>>>>>>>> state.getValue().getObject() failed! {1}", e);
        }
        return value;
    }

    /**
     * Read TimeStamp From ItemState
     * @param state data object
     * @return
     */
    private String readTimeStamp(ItemState state) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(state.getTimestamp().getTime());
    }


}
