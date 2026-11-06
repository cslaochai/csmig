package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 医保局目录下载同步结果（/admin/dict/download 目录下载按钮返回）。
 */
@Data
public class DictDownloadResultVO implements Serializable {

    /**
     * 下载的目录类型（DRUG-药品 DIAG-诊疗项目 MAT-医用耗材）
     */
    private String type;

    /**
     * 医保局返回的目录条目总数
     */
    private Integer total;

    /**
     * 新增入库条数
     */
    private Integer imported;

    /**
     * 名称变更更新条数
     */
    private Integer updated;
}
