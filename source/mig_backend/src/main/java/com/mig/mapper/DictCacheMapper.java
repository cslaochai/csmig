package com.mig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mig.entity.DictCache;
import com.mig.vo.DictTypeStatVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DictCacheMapper extends BaseMapper<DictCache> {

    /**
     * 目录匹配情况按目录类型统计（目录缓存页顶部统计卡）。
     */
    @Select("""
            SELECT dict_type AS dictType,
                   COUNT(*) AS total,
                   COALESCE(SUM(match_status = 1), 0) AS matched
            FROM mip_dict_cache
            GROUP BY dictType
            """)
    List<DictTypeStatVO> selectTypeStats();
}
