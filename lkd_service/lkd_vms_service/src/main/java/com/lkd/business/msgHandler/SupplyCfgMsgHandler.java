package com.lkd.business.msgHandler;

import com.lkd.annotations.ProcessType;
import com.lkd.business.MsgHandler;
import com.lkd.business.VmCfgService;
import com.lkd.config.TopicConfig;
import com.lkd.contract.SupplyCfg;
import com.lkd.emq.MqttProducer;
import com.lkd.entity.VmCfgVersionEntity;
import com.lkd.service.VendingMachineService;
import com.lkd.service.VmCfgVersionService;
import com.lkd.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ProcessType("supplyResp")
public class SupplyCfgMsgHandler implements MsgHandler {

  @Autowired
  private VendingMachineService vendingMachineService;

  @Autowired
  private VmCfgVersionService vmCfgVersionService;

  @Autowired
  private VmCfgService vmCfgService;


  @Autowired
  private MqttProducer mqttProducer;

  /**
   * 处理补货工单
   */
  @Override
  public void process(String jsonMsg) throws IOException {
    SupplyCfg supplyCfg = JsonUtil.getByJson(jsonMsg, SupplyCfg.class);

    // 更新售货机库存
    vendingMachineService.supply(supplyCfg);

    String innerCode = supplyCfg.getInnerCode();
    VmCfgVersionEntity vmVersion = vmCfgVersionService.getVmVersion(innerCode);

    // 发送主题
    String topic = TopicConfig.TO_VM_TOPIC + innerCode;
    //
    // // 下发商品配置
    // SkuCfg skuCfg = vmCfgService.getSkuCfg(innerCode);
    // skuCfg.setSn(System.nanoTime());// 纳秒
    // skuCfg.setVersionId(vmVersion.getSkuCfgVersion());
    // // 商品配置下发售货机
    // mqttProducer.send(topic, 2, skuCfg);
    //
    // // 下发价格配置
    // ChannelCfg channelCfg = vmCfgService.getChannelCfg(innerCode);
    // channelCfg.setSn(System.nanoTime());
    // channelCfg.setVersionId(vmVersion.getChannelCfgVersion());
    // // 价格配置发到售货机
    // mqttProducer.send(topic, 2, channelCfg);
    //
    // // 下发货道配置
    // SkuPriceCfg skuPriceCfg = vmCfgService.getSkuPriceCfg(innerCode);
    // skuPriceCfg.setSn(System.nanoTime());
    // skuPriceCfg.setVersionId(vmVersion.getPriceCfgVersion());
    // // 将货道配置发送到售货机
    // mqttProducer.send(topic, 2, skuPriceCfg);
    //
    // // 下发补货信息
    // supplyCfg.setSn(System.nanoTime());
    // supplyCfg.setVersionId(vmVersion.getSupplyVersion());
    // supplyCfg.setNeedResp(true);
    // // 补货配置下发售货机
    // mqttProducer.send(topic, 2, supplyCfg);

  }


}
