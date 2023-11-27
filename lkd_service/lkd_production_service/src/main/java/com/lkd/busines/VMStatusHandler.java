package com.lkd.busines;

import com.lkd.annotations.ProcessType;
import com.lkd.business.MsgHandler;
import com.lkd.common.VMSystem;
import com.lkd.contract.VmStatusContract;
import com.lkd.exception.LogicException;
import com.lkd.feignService.VMService;
import com.lkd.http.viewModel.TaskViewModel;
import com.lkd.service.TaskService;
import com.lkd.utils.JsonUtil;
import com.lkd.viewmodel.VendingMachineViewModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ProcessType("vmStatus")
@Slf4j
public class VMStatusHandler implements MsgHandler {

  @Autowired
  private TaskService taskService;
  // feign
  @Autowired
  private VMService vmService;

  @Override
  public void process(String jsonMsg) throws IOException {
    VmStatusContract vmStatusContract = JsonUtil.getByJson(jsonMsg, VmStatusContract.class);
    // 判空
    if (vmStatusContract == null || vmStatusContract.getStatusInfo() == null || vmStatusContract.getStatusInfo().size() == 0) return;

    // 如果为非正常状态，则创建维修工单

      if (vmStatusContract.getStatusInfo().stream().anyMatch(statusInfo -> statusInfo.isStatus() == false )) {
        try {
        VendingMachineViewModel vmInfo = vmService.getVMInfo(vmStatusContract.getInnerCode());
        TaskViewModel taskViewModel = new TaskViewModel();
        // 获取最少订单量运维人员Id
        Integer assignorId = taskService.getLeastTaskUser( vmInfo.getRegionId().intValue(), true);

        taskViewModel.setCreateType(0); // 自动工单
        taskViewModel.setProductType(VMSystem.TASK_TYPE_REPAIR); // 维修工单
        taskViewModel.setInnerCode(vmStatusContract.getInnerCode());
        taskViewModel.setAssignorId(assignorId); // 执行人
        taskViewModel.setDesc("自动维修工单");

        // 创建
        taskService.createTask(taskViewModel);
        log.info("创建自动维修工单成功 设备id:{}",vmStatusContract.getInnerCode());

        } catch (Exception e) {
          log.error("创建自动维修工单失败，msg is:"+jsonMsg);
          throw new LogicException("创建自动维修工单失败",e);
        }
      }
  }
}
