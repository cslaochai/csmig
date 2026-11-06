package com.mig.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医保目录匹配行（/admin/dict/page 目录管理页列表）。
 */
@Data
public class DictCacheVO implements Serializable {

    /**
     * 目录ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 目录类型（DRUG-药品 DIAG-诊疗项目 MAT-医用耗材 IVD-体外诊断试剂）
     */
    private String dictType;

    /**
     * 院内编码
     */
    private String hisCode;

    /**
     * 院内名称
     */
    private String hisName;

    /**
     * 医保编码
     */
    private String mediCode;

    /**
     * 医保名称
     */
    private String mediName;

    /**
     * 匹配状态（0-未匹配 1-已匹配）
     */
    private Integer matchStatus;

    /**
     * 目录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
