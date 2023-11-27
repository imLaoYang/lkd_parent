package com.lkd.business.msgHandler;

import com.google.common.collect.Maps;
import com.lkd.annotations.ProcessType;
import com.lkd.business.MsgHandler;
import com.lkd.business.MsgHandlerContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MsgHandlerContextImp实现了ApplicationContextAware接口，在微服务启动的时候扫描所有MsgHandler接口的实现类，
 * 并提取其ProcessType注解中定义的值（协议名称），
 * 将这些类加载到map集合中，以协议名称作为key，以类的对象作为值。
 */
@Component
public class MsgHandlerContextImp implements ApplicationContextAware, MsgHandlerContext {
  private ApplicationContext ctx;
  private Map<String, MsgHandler> handlerMap = Maps.newHashMap();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    ctx = applicationContext;
    Map<String, MsgHandler> map = ctx.getBeansOfType(MsgHandler.class);
    map.values().stream().forEach(v -> {
      String msgType = v.getClass().getAnnotation(ProcessType.class).value();
      handlerMap.put(msgType, v);
    });
  }

    /**
     * getMsgHandler方法，根据协议找到MsgHandler的实现类
     */
  public MsgHandler getMsgHandler(String msgType) {
    return handlerMap.get(msgType);
  }
}
