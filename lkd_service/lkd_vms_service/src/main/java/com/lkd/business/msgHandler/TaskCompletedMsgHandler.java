package com.lkd.business.msgHandler;

import com.lkd.annotations.ProcessType;
import com.lkd.business.MsgHandler;
import com.lkd.common.VMSystem;
import com.lkd.contract.TaskCompleteContract;
import com.lkd.service.VendingMachineService;
import com.lkd.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 订单完成
 */
@Component
@ProcessType(value = "taskCompleted")
public class TaskCompletedMsgHandler implements MsgHandler {


  @Autowired
  private VendingMachineService vendingMachineService;

  @Override
  public void process(String jsonMsg) throws IOException {
    TaskCompleteContract contract = JsonUtil.getByJson(jsonMsg, TaskCompleteContract.class);
    String innerCode = contract.getInnerCode();
    if (StringUtils.isEmpty(innerCode)) return;
    // 如果是投放工单，将售货机修改为运营状态
    if (contract.getProductType() == VMSystem.TASK_TYPE_DEPLOY) {
      vendingMachineService.updateStatus(innerCode, VMSystem.VM_STATUS_RUNNING, null, null);
      // todo 保存设备的坐标（数据库+es）
    }

    // 如果是撤机工单，将售货机修改为撤机状态
    if (contract.getProductType() == VMSystem.TASK_TYPE_REVOKE) {
      vendingMachineService.updateStatus(innerCode, VMSystem.VM_STATUS_REVOKE, null, null);
    }

  }
}
