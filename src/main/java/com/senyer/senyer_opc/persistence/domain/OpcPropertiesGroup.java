package com.senyer.senyer_opc.persistence.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author senyer
 * @since 2020-04-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="OpcPropertiesGroup对象", description="")
public class OpcPropertiesGroup extends Model<OpcPropertiesGroup> {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String hostname;

    private String domain;

    private String username;

    private String password;

    @TableField("progId")
    private String progId;

    private String clsid;

    private String intervalTime;

    private String tableName;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}
