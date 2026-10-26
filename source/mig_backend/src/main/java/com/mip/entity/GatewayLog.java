package com.mip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mip_gateway_log")
public class GatewayLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgType;
    private String tradeNo;
    private Integer success;
    private Integer costMs;
    private String requestBody;
    private String responseBody;
    private String errMsg;
    private LocalDateTime createdAt;
}
