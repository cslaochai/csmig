package com.mig.dto;

import com.mig.common.validation.AllowedValues;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 医保目录缓存新增/修改（/admin/dict、/admin/dict/{id}）入参。
 */
@Data
public class DictUpsertDTO {

    /**
     * 目录类型（DRUG-药品 DIAG-诊疗项目 MAT-医用耗材 IVD-体外诊断试剂）
     */
    @NotBlank(message = "目录类型不能为空")
    @AllowedValues(values = {"DRUG", "DIAG", "MAT", "IVD"}, message = "目录类型不合法（DRUG-药品 DIAG-诊疗项目 MAT-医用耗材 IVD-体外诊断试剂）")
    private String dictType;

    /**
     * 院内编码
     */
    @NotBlank(message = "院内编码不能为空")
    @Size(max = 64, message = "院内编码不能超过64字")
    private String hisCode;

    /**
     * 院内名称
     */
    @NotBlank(message = "院内名称不能为空")
    @Size(max = 200, message = "院内名称不能超过200字")
    private String hisName;

    /**
     * 医保编码（未匹配时为空）
     */
    @Size(max = 64, message = "医保编码不能超过64字")
    private String mediCode;

    /**
     * 医保名称（未匹配时为空）
     */
    @Size(max = 200, message = "医保名称不能超过200字")
    private String mediName;

    /**
     * 匹配状态（0-未匹配 1-已匹配）
     */
    @AllowedValues(values = {"0", "1"}, message = "匹配状态取值不合法（0-未匹配 1-已匹配）")
    private Integer matchStatus;
}
