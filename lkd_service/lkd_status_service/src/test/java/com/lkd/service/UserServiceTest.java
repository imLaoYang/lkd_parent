package com.lkd.service;

import com.google.common.base.Strings;
import com.lkd.feignService.StatusService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest{
//    @Autowired
//    private UserService userService;
//    @Test
//    public void getRepairer() throws IOException {
//        String result = userService.getRepairer("037900004");
//        ArrayNode dataNode = (ArrayNode)JsonUtil.getNodeByName("data",result);
//
//        String userName = dataNode.get(0).findPath("userName").asText();
//        Assert.assertTrue(true);
//    }

  @Autowired
  private StatusService service;

  @Autowired
  private RedisTemplate redisTemplate;

  @Test
  public void generateTaskCode(){
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    // redis key
    String key = "lkd.task.code" + time;
    Object code = redisTemplate.opsForValue().get(key);
    if (code == null) {
      // 1天过期
      redisTemplate.opsForValue().set(key,1L, Duration.ofDays(1));
      System.out.println(time + "0001");
    }

    Long increment = redisTemplate.opsForValue().increment(key,1);
    assert increment != null;
    // Strings.padStart()占位符 补四个0
    System.out.println(time + Strings.padStart(increment.toString(), 4, '0'));

  }

}