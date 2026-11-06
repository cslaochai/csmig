package com.mig.dto;

import com.mig.common.validation.AllowedValues;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 医保目录下载（/admin/dict/download）入参。
 */
@Data
public class DictDownloadQueryDTO {

    /**
     * 目录类型（DRUG-药品 DIAG-诊疗项目 MAT-医用耗材）
     */
    @NotBlank(message = "目录类型不能为空")
    @AllowedValues(values = {"DRUG", "DIAG", "MAT"}, message = "目录类型仅支持 DRUG/DIAG/MAT")
    private String type;
}
