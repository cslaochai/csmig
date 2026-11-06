package com.mig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mig.entity.GatewayLog;
import com.mig.vo.DailyMsgTypeStatVO;
import com.mig.vo.HourCountVO;
import com.mig.vo.MsgTypeCountVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface GatewayLogMapper extends BaseMapper<GatewayLog> {

    /**
     * 指定时刻之后失败笔数按报文类型分布（告警面板）。
     */
    @Select("""
            SELECT msg_type AS msgType, COUNT(*) AS cnt
            FROM mip_gateway_log
            WHERE success = 0 AND created_at >= #{since}
            GROUP BY msg_type
            ORDER BY cnt DESC
            """)
    List<MsgTypeCountVO> selectFailCountByType(@Param("since") LocalDateTime since);

    /**
     * 指定时刻之后失败笔数按小时分布（告警面板趋势图）。
     */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%H') AS hour, COUNT(*) AS cnt
            FROM mip_gateway_log
            WHERE success = 0 AND created_at >= #{since}
            GROUP BY hour
            ORDER BY hour
            """)
    List<HourCountVO> selectFailCountByHour(@Param("since") LocalDateTime since);

    /**
     * 指定时刻之后按「日 × 报文类型」聚合的结算统计（结算汇总页）。
     */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS day,
                   msg_type AS msgType,
                   COUNT(*) AS total,
                   SUM(success = 1) AS success,
                   SUM(success = 0) AS fail
            FROM mip_gateway_log
            WHERE created_at >= #{since}
            GROUP BY day, msgType
            ORDER BY day, msgType
            """)
    List<DailyMsgTypeStatVO> selectDailyTypeStats(@Param("since") LocalDateTime since);
}
