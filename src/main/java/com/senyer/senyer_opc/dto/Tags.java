package com.senyer.senyer_opc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

@Data
public class Tags {

  @JsonIgnore
  private Integer id;
  @JsonIgnore
  private Integer seqId;
  @JsonIgnore
  private String itemId;

  private String alies;

  @JsonIgnore
  private String accuracy;

  private String unit;

  private Date date;

  private Object value;

}
