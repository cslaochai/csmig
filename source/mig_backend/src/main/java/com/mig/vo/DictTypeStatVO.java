package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 医保目录按类型的匹配统计（/admin/dict/stats 目录管理页统计卡）。
 */
@Data
public class DictTypeStatVO implements Serializable {

    /**
     * 目录类型（DRUG-药品 DIAG-诊疗项目 MAT-医用耗材 IVD-体外诊断试剂）
     */
    private String dictType;

    /**
     * 该类型目录总条数
     */
    private Long total;

    /**
     * 该类型已匹配条数
     */
    private Long matched;
}
