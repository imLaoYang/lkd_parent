package com.lkd.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lkd.entity.TaskEntity;
import com.lkd.entity.TaskStatusTypeEntity;
import com.lkd.exception.LogicException;
import com.lkd.http.viewModel.TaskViewModel;
import com.lkd.viewmodel.CancelTaskModel;
import com.lkd.viewmodel.Pager;

import java.util.List;

public interface TaskService extends IService<TaskEntity> {


  /**
   * 创建工单
   *
   * @param taskViewModel
   * @return
   */
  boolean createTask(TaskViewModel taskViewModel) throws LogicException;


  /**
   * 完成工单
   *
   * @param id
   * @return
   */
  boolean completeTask(long id);


  boolean completeTask(long id,Double lat, Double lon,String addr);

  /**
   * 获取所有状态类型
   *
   * @return
   */
  List<TaskStatusTypeEntity> getAllStatus();

  /**
   * 通过条件搜索工单列表
   *
   * @param pageIndex
   * @param pageSize
   * @param innerCode
   * @param userId
   * @param taskCode
   * @param isRepair  是否是运维工单
   * @return
   */
  Pager<TaskEntity> search(Long pageIndex, Long pageSize, String innerCode, Integer userId, String taskCode, Integer status, Boolean isRepair, String start, String end);

  /**
   * 获取同一天内分配的工单最少的人
   *
   * @param innerCode
   * @param isRepair  是否是维修工单
   * @return
   */
  Integer getLeastUser(String innerCode, Boolean isRepair);

  /**
   * 接收工单
   *
   * @return
   */
  boolean accept(TaskEntity taskEntity);

  /**
   * 取消工单
   *
   * @param taskEntity
   * @param cancelTaskModel
   * @return
   */
  boolean cancel(TaskEntity taskEntity, CancelTaskModel cancelTaskModel);

  /**
   * 获取同一天内分配的工单最少的人
   * @param regionId
   * @param isRepair 是否是维修工单
   * @return
   */
  Integer getLeastTaskUser(Integer regionId,boolean isRepair);


}
