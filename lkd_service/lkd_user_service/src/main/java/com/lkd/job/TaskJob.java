package com.lkd.job;

import com.lkd.common.VMSystem;
import com.lkd.entity.UserEntity;
import com.lkd.service.UserService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 工单定时任务
 */
@Slf4j
@Component
public class TaskJob {

  @Autowired
  private UserService userService;

  @Autowired
  private RedisTemplate redisTemplate;

  /**
   * 每日工单量列表初始化
   * redis ZSet
   * {
   * key: key的规则，以固定字符串（前缀）+时间+区域+角色（运营/运维）为大key，
   * value:以人员id做value，过期时间为1天。
   * score:0
   * }
   * @return
   * @throws Exception
   */
  @XxlJob("workCountInitJobHandler")
  public ReturnT<String> workCountInitJobHandler(String param) throws Exception{
    List<UserEntity> userList = userService.list();
    userList.forEach(userEntity -> {
      // 非管理员
      if (userEntity.getRegionId().intValue() != 1){
        String key = VMSystem.REGION_TASK_KEY_PREF + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "." + userEntity.getRegionId() + "." + userEntity.getRoleCode();
        // 初始化为0
        redisTemplate.opsForZSet().add(key,userEntity.getId(),0);
        redisTemplate.expire(key, Duration.ofDays(1));
      }
    });
    return ReturnT.SUCCESS;

  }


}
