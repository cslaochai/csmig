package com.mig.dto;

import com.mig.common.PageParam;
import com.mig.common.validation.AllowedValues;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 医保目录缓存分页查询（/admin/dict/page）入参。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictPageQueryDTO extends PageParam {

    /**
     * 目录类型（DRUG-药品 DIAG-诊疗项目 MAT-医用耗材 IVD-体外诊断试剂；不传不筛选）
     */
    private String dictType;

    /**
     * 综合关键字（模糊匹配院内编码/名称、医保编码/名称）
     */
    private String keyword;

    /**
     * 匹配状态（0-未匹配 1-已匹配）
     */
    @AllowedValues(values = {"0", "1"}, message = "匹配状态取值不合法（0-未匹配 1-已匹配）")
    private Integer matchStatus;
}
