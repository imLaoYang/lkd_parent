package com.lkd.contract;

import lombok.Data;

/**
 * 完成工单协议
 */
@Data
public class TaskCompleteContract extends BaseContract{
    public TaskCompleteContract() {
        this.setMsgType("taskCompleted");
    }

    // 工单类型
    private int productType;
    /**
     * 纬度
     */
    private Double lat;
    /**
     * 经度
     */
    private Double lon;
}
