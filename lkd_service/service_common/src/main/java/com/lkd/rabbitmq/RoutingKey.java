package com.lkd.rabbitmq;

/**
 * 路由key
 */
public class RoutingKey {

    //下发售货机消息的routing key
    public final static String TO_VM_ROUTING_KEY = "smart.vm.toVm";
    //售货机服务的routing key
    public final static String VM_SERVICE_ROUTING_KEY = "smart.vm.vmService";
    //出货结果的routing key
    public final static String VM_VENDOUT_RESULT = "smart.vm.vendoutResult";
    //工单服务的routing key
    public final static String VM_PRODUCTION_ROUTING_KEY = "smart.vm.production";
    //状态服务的routing key
    public final static String VM_STATUS_ROUTING_KEY = "smart.vm.status";
}
