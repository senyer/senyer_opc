package com.senyer.senyer_opc.persistence.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author senyer
 * @since 2020-02-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="DataGroups对象", description="")
public class DataGroups extends Model<DataGroups> {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String tableName;

    private String apiType;//无参数的http接口、webservice接口

    private String dataStyle;//A：变量+数据、B：变量+数据+单位、C：变量+数据+时间+单位、D：变量+数据+时间。。。。。。

    /*
        <>Format-WebService<> : 4	default_tags	webservice	C	http://127.0.0.1:8082/services/realtime?wsdl,web:realtime,http://webservice.zy.com/,data	推送默认变量集的webservice
        <>Format-Http<> ： 5	default_tags	http	C	http://127.0.0.1:8080/demo/temp,post,data	推送默认变量集的http接口
     */
    private String url;//请求的URL

    private String descriptions;//接口的描述信息


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}
