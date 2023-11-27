package com.lkd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.lkd.business.MsgHandlerContext;
import com.lkd.common.VMSystem;
import com.lkd.config.TopicConfig;
import com.lkd.contract.SupplyCfg;
import com.lkd.contract.SupplyChannel;
import com.lkd.contract.TaskCompleteContract;
import com.lkd.dao.TaskDao;
import com.lkd.emq.MqttProducer;
import com.lkd.entity.TaskDetailsEntity;
import com.lkd.entity.TaskEntity;
import com.lkd.entity.TaskStatusTypeEntity;
import com.lkd.exception.LogicException;
import com.lkd.feignService.UserService;
import com.lkd.feignService.VMService;
import com.lkd.http.viewModel.TaskViewModel;
import com.lkd.service.TaskDetailsService;
import com.lkd.service.TaskService;
import com.lkd.service.TaskStatusTypeService;
import com.lkd.viewmodel.CancelTaskModel;
import com.lkd.viewmodel.Pager;
import com.lkd.viewmodel.UserViewModel;
import com.lkd.viewmodel.VendingMachineViewModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工单任务状态
 */
@Service
@Slf4j
public class TaskServiceImpl extends ServiceImpl<TaskDao, TaskEntity> implements TaskService {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private TaskDetailsService taskDetailsService;

  @Autowired
  private VMService vmService;

  @Autowired
  private TaskStatusTypeService statusTypeService;

  @Autowired
  private UserService userService;

  @Autowired
  private MqttProducer mqttProducer;

  @Autowired
  private MsgHandlerContext msgHandlerContext;


  @Override
  @Transactional(rollbackFor = {Exception.class}, noRollbackFor = {LogicException.class})
  public boolean createTask(TaskViewModel taskViewModel) throws LogicException {

    // 校验设备状态
    checkCreateTask(taskViewModel.getInnerCode(), taskViewModel.getProductType());
    // 同一台设备下是否存在未完成的工单
    if (hasTask(taskViewModel.getInnerCode(), taskViewModel.getProductType())) {
      throw new LogicException("该机器有未完成的同类型工单");
    }

    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setTaskCode(this.generateTaskCode());
    taskEntity.setTaskStatus(VMSystem.TASK_STATUS_CREATE);
    taskEntity.setCreateType(taskViewModel.getCreateType());
    taskEntity.setDesc(taskViewModel.getDesc());
    taskEntity.setProductTypeId(taskViewModel.getProductType());
    UserViewModel user = userService.getUser(taskViewModel.getAssignorId());
    String userName = user.getUserName();
    taskEntity.setUserName(userName);
    taskEntity.setInnerCode(taskViewModel.getInnerCode());
    taskEntity.setAssignorId(taskViewModel.getAssignorId());
    taskEntity.setUserId(taskViewModel.getUserId());
    taskEntity.setAddr(vmService.getVMInfo(taskViewModel.getInnerCode()).getNodeAddr());
    taskEntity.setRegionId(vmService.getVMInfo(taskViewModel.getInnerCode()).getRegionId());

    // 存入数据库
    this.save(taskEntity);

    // 是否补货工单
    if (taskEntity.getProductTypeId() == VMSystem.TASK_TYPE_SUPPLY) {
      taskViewModel.getDetails().forEach(d -> {
        TaskDetailsEntity detailsEntity = new TaskDetailsEntity();
        detailsEntity.setChannelCode(d.getChannelCode());
        detailsEntity.setExpectCapacity(d.getExpectCapacity());
        detailsEntity.setTaskId(taskEntity.getTaskId());
        detailsEntity.setSkuId(d.getSkuId());
        detailsEntity.setSkuName(d.getSkuName());
        detailsEntity.setSkuImage(d.getSkuImage());
        taskDetailsService.save(detailsEntity);
      });


    }
    // 更新工单量缓存
    updateTaskZSet(taskEntity,1);

    return true;
  }


  @Override
  @Transactional
  public boolean completeTask(long id) {
    return this.completeTask(id, 0d, 0d, null);
  }

  @Override
  public boolean completeTask(long id, Double lat, Double lon, String addr) {
    TaskEntity taskEntity = this.getById(id);

    if (taskEntity.getTaskStatus() == VMSystem.TASK_STATUS_FINISH || taskEntity.getTaskStatus() == VMSystem.TASK_STATUS_CANCEL) {
      throw new LogicException("工单已经结束");
    }

    //   补货工单
    if (taskEntity.getProductTypeId() == VMSystem.TASK_TYPE_SUPPLY) {
      supplyTask(taskEntity);
    }


    taskEntity.setTaskStatus(VMSystem.TASK_STATUS_FINISH);
    taskEntity.setAddr(addr);
    this.updateById(taskEntity);

    // 向消息队列发送消息，通知售货机更改状态
    // 封装协议
    TaskCompleteContract taskCompleteContract = new TaskCompleteContract();
    taskCompleteContract.setInnerCode(taskEntity.getInnerCode()); // 售货机编号
    taskCompleteContract.setProductType(taskEntity.getProductTypeId()); // 工单类型
    // 经纬度信息
    taskCompleteContract.setLat(lat);
    taskCompleteContract.setLon(lon);
    // 发送
    try {
      mqttProducer.send(TopicConfig.COMPLETED_TASK_TOPIC, 2, taskCompleteContract);
    } catch (Exception e) {
      log.error("发送工单完成协议出错", e);
      throw new LogicException("发送工单完成协议出错");
    }



    return true;
  }

  /**
   * 补货协议封装与发送
   */
  private void supplyTask(TaskEntity taskEntity) {

    // 协议内容封装
    LambdaQueryWrapper<TaskDetailsEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(TaskDetailsEntity::getTaskId, taskEntity.getTaskId());
    List<TaskDetailsEntity> taskDetailsEntity = taskDetailsService.list(queryWrapper);
    // 补货数据
    List<SupplyChannel> supplyData = new ArrayList<>();
    taskDetailsEntity.forEach(detailEntity -> {
      SupplyChannel supplyChannel = new SupplyChannel();
      supplyChannel.setCapacity(detailEntity.getExpectCapacity());
      supplyChannel.setChannelId(detailEntity.getChannelCode());
      supplyData.add(supplyChannel);
    });
    // 构建协议内容
    SupplyCfg supplyCfg = new SupplyCfg();
    supplyCfg.setInnerCode(taskEntity.getInnerCode());
    supplyCfg.setSupplyData(supplyData);

    // 下发补货协议
    try {
      mqttProducer.send(TopicConfig.SUPPLY_TOPIC, 2, supplyCfg);
    } catch (Exception e) {
      log.error("发送工单完成协议出错", e);
      throw new LogicException("发送工单完成协议出错");
    }
  }


  @Override
  public List<TaskStatusTypeEntity> getAllStatus() {
    QueryWrapper<TaskStatusTypeEntity> qw = new QueryWrapper<>();
    qw.lambda()
            .ge(TaskStatusTypeEntity::getStatusId, VMSystem.TASK_STATUS_CREATE);

    return statusTypeService.list(qw);
  }

  @Override
  public Pager<TaskEntity> search(Long pageIndex, Long pageSize, String innerCode, Integer userId, String taskCode, Integer status, Boolean isRepair, String start, String end) {
    Page<TaskEntity> page = new Page<>(pageIndex, pageSize);
    LambdaQueryWrapper<TaskEntity> qw = new LambdaQueryWrapper<>();
    if (!Strings.isNullOrEmpty(innerCode)) {
      qw.eq(TaskEntity::getInnerCode, innerCode);
    }
    if (userId != null && userId > 0) {
      qw.eq(TaskEntity::getAssignorId, userId);
    }
    if (!Strings.isNullOrEmpty(taskCode)) {
      qw.like(TaskEntity::getTaskCode, taskCode);
    }
    if (status != null && status > 0) {
      qw.eq(TaskEntity::getTaskStatus, status);
    }
    if (isRepair != null) {
      if (isRepair) {
        qw.ne(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
      } else {
        qw.eq(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
      }
    }
    if (!Strings.isNullOrEmpty(start) && !Strings.isNullOrEmpty(end)) {
      qw
              .ge(TaskEntity::getCreateTime, LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE))
              .le(TaskEntity::getCreateTime, LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE));
    }
    // 根据最后更新时间倒序排序
    qw.orderByDesc(TaskEntity::getUpdateTime);

    return Pager.build(this.page(page, qw));
  }


  /**
   * 获取同一天内分配的工单最少的人
   *
   * @param innerCode
   * @param isRepair  是否是维修工单
   * @return
   */
  @Override
  public Integer getLeastUser(String innerCode, Boolean isRepair) {
    List<UserViewModel> userList = null;
    if (true) {
      userList = userService.getRepairerListByInnerCode(innerCode);
    } else {
      userList = userService.getOperatorListByInnerCode(innerCode);
    }
    if (userList == null) return null;
    QueryWrapper<TaskEntity> qw = new QueryWrapper<>();
    // 按人分组，取工作量。将工单数暂存到了user_id列里
    qw.select("assignor_id,count(1) as user_id");
    if (isRepair) {
      qw.lambda().ne(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
    } else {
      qw.lambda().eq(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
    }
    qw
            .lambda()
            //.le(TaskEntity::getTaskStatus,VMSystem.TASK_STATUS_PROGRESS) //根据未完成的工单
            .ne(TaskEntity::getTaskStatus, VMSystem.TASK_STATUS_CANCEL) // 根据所有未被取消的工单做统计
            .ge(TaskEntity::getCreateTime, LocalDate.now())
            .in(TaskEntity::getAssignorId, userList.stream().map(UserViewModel::getUserId).collect(Collectors.toList()))
            .groupBy(TaskEntity::getAssignorId)
            .orderByAsc(TaskEntity::getUserId);
    List<TaskEntity> result = this.list(qw);

    List<TaskEntity> taskList = Lists.newArrayList();
    Integer userId = 0;
    for (UserViewModel user : userList) {
      Optional<TaskEntity> taskEntity = result.stream().filter(r -> r.getAssignorId() == user.getUserId()).findFirst();

      // 当前人员今日没有分配工单
      if (taskEntity.isEmpty()) {
        return user.getUserId();
      }
      TaskEntity item = new TaskEntity();
      item.setAssignorId(user.getUserId());
      item.setUserId(taskEntity.get().getUserId());
      taskList.add(item);
    }
    // 取最少工单的人
    taskList.stream().sorted(Comparator.comparing(TaskEntity::getUserId));

    return taskList.get(0).getAssignorId();
  }

  @Override
  public boolean accept(TaskEntity taskEntity) {
    // 更改工单状态
    if (!taskEntity.getTaskStatus().equals(VMSystem.TASK_STATUS_CREATE)) {
      throw new LogicException("工单状态不是待处理");
    }
    taskEntity.setTaskStatus(VMSystem.TASK_STATUS_PROGRESS);
    return updateById(taskEntity);
  }

  @Override
  public boolean cancel(TaskEntity taskEntity, CancelTaskModel cancelTaskModel) {
    // 判断工单是否已经取消或结束
    if (taskEntity.getTaskStatus().equals(VMSystem.TASK_STATUS_CANCEL) || taskEntity.getTaskStatus().equals(VMSystem.TASK_STATUS_FINISH)) {
      throw new LogicException("该工单已经结束");
    }
    // 理由
    taskEntity.setDesc(cancelTaskModel.getDesc());
    taskEntity.setTaskStatus(VMSystem.TASK_STATUS_CANCEL);

    // 更新工单量缓存
    updateTaskZSet(taskEntity,-1);

    return this.updateById(taskEntity);
  }


  @Override
  public Integer getLeastTaskUser(Integer regionId, boolean isRepair) {
    String roleCode = "1002";
    if (isRepair){
      //如果是维修工单
      roleCode = "1003";
    }
    String key = VMSystem.REGION_TASK_KEY_PREF + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "." + regionId + "." + roleCode;
    Set<Object> userId = redisTemplate.opsForZSet().range(key, 0, 1);

    if (userId == null || userId.isEmpty()){
      throw  new LogicException("该区域暂时没有相关人员");
    }

    return (Integer) userId.toArray()[0];

  }


  /**
   * 同一台设备下是否存在未完成的工单
   *
   * @param innerCode
   * @param productionType
   * @return
   */
  private boolean hasTask(String innerCode, int productionType) {
    QueryWrapper<TaskEntity> qw = new QueryWrapper<>();
    qw.lambda()
            .select(TaskEntity::getTaskId)
            .eq(TaskEntity::getInnerCode, innerCode)
            .eq(TaskEntity::getProductTypeId, productionType)
            .le(TaskEntity::getTaskStatus, VMSystem.TASK_STATUS_PROGRESS);

    return this.count(qw) > 0;
  }

  /**
   * 校验设备状态
   */
  private void checkCreateTask(String innerCode, int productType) throws LogicException {
    VendingMachineViewModel vmInfo = vmService.getVMInfo(innerCode);
    if (vmInfo == null) throw new LogicException("设备校验失败");
    if (productType == VMSystem.TASK_TYPE_DEPLOY && vmInfo.getVmStatus() == VMSystem.VM_STATUS_RUNNING) {
      throw new LogicException("该设备已在运营");
    }

    if (productType == VMSystem.TASK_TYPE_SUPPLY && vmInfo.getVmStatus() != VMSystem.VM_STATUS_RUNNING) {
      throw new LogicException("该设备不在运营状态");
    }

    if (productType == VMSystem.TASK_TYPE_REVOKE && vmInfo.getVmStatus() != VMSystem.VM_STATUS_RUNNING) {
      throw new LogicException("该设备不在运营状态");
    }
  }

  /**
   * 生成工单编码
   * 工单编号为12位，由8位日期+4位当日工单序号组成
   *
   * @return 8位日期+4位当日工单序号组成( 20230102 + 0001 )
   */
  private String generateTaskCode() {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    // redis key
    String key = "lkd.task.code" + time;
    Object code = redisTemplate.opsForValue().get(key);
    if (code == null) {
      // 1天过期
      redisTemplate.opsForValue().set(key, 1L, Duration.ofDays(1));
      return time + "0001";
    }

    Long increment = redisTemplate.opsForValue().increment(key, 1);
    assert increment != null;
    // Strings.padStart()占位符 补四个0
    return time + Strings.padStart(increment.toString(), 4, '0');

  }


  /**
   * 更新redis工单量列表
   *
   * @param taskEntity
   * @param score
   */
  private void updateTaskZSet(TaskEntity taskEntity, int score) {

    String roleCode = "1003"; // 维修员
    // 补货工单
    if (taskEntity.getProductTypeId() == 2) {
      roleCode = "1003"; // 运营员
    }

    String key = VMSystem.REGION_TASK_KEY_PREF + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "." + taskEntity.getRegionId() + "." + roleCode;
    redisTemplate.opsForZSet().incrementScore(key,taskEntity.getAssignorId(),score);
  }




}
