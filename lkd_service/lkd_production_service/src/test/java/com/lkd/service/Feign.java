package com.lkd.service;

import com.lkd.ProductionServiceApplication;
import com.lkd.feignService.UserService;
import com.lkd.viewmodel.UserViewModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest( classes = ProductionServiceApplication.class)
@RunWith(SpringRunner.class)
public class Feign {


  @Autowired
  private UserService userService;

  @Test
  public void fegin(){
    UserViewModel user = userService.getUser(1);
    System.out.println("user = " + user);
  }

}
