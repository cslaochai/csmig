package com.mig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医保目录缓存：院内目录 ↔ 医保目录匹配关系。
 * 开发期本地维护；生产期由医保局目录下载接口（1xxx）增量同步。
 */
@Data
@TableName("mip_dict_cache")
public class DictCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 目录类型：DRUG-药品 DIAG-诊疗项目 MAT-医用耗材 IVD-体外诊断试剂 */
    private String dictType;
    /** 院内编码 */
    private String hisCode;
    /** 院内名称 */
    private String hisName;
    /** 医保编码 */
    private String mediCode;
    /** 医保名称 */
    private String mediName;
    /** 匹配状态：0-未匹配 1-已匹配 */
    private Integer matchStatus;
    private LocalDateTime updatedAt;
}
