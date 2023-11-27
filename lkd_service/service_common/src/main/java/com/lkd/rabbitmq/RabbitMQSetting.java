package com.lkd.rabbitmq;

//import com.lkd.rabbitmq.MqConfig;
//import org.springframework.amqp.core.*;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.listener.Topic;
//
//@Configuration
//public class RabbitMQSetting{

//    /**
//     * 售货机端队列
//     * @return
//     */
//    @Bean(name = "toVmQueue")
//    public Queue toVmQueue() {
//        return new Queue(MqConfig.TO_VM_ROUTING_KEY,true);
//    }
//
//
//    /**
//     * 售货机消息交换机
//     * @return
//     */
//    @Bean(name = "toVmExchange")
//    public TopicExchange toVMExchange() {
//        return new TopicExchange(MqConfig.TO_VM_ROUTING_KEY,true,false);
//    }
//
//
//
//    @Bean
//    public Binding bindingToVmExchangeMessage(
//            @Qualifier("toVmQueue") Queue toVmQueue,
//            @Qualifier("toVmExchange") TopicExchange exchange) {
//        return BindingBuilder.bind(toVmQueue).to(exchange).with(MqConfig.TO_VM_ROUTING_KEY);
//    }
//
//    /**
//     * 工单队列
//     * @return
//     */
//    @Bean
//    public Queue TaskQueue(){
//        return new Queue(MqConfig.VM_PRODUCTION_ROUTING_KEY,true);
//    }
//
//    /**
//     * 处理工单的交换机
//     * @return
//     */
//    @Bean
//    public TopicExchange taskExchange(){
//        return new TopicExchange(MqConfig.VM_PRODUCTION_ROUTING_KEY,true,false);
//    }
//
//    /**
//     * 将队列和交换机绑定
//     * @return
//     */
//    @Bean
//    public Binding bindingTaskExchange(){
//        return BindingBuilder.bind(TaskQueue()).to(taskExchange()).with(MqConfig.VM_PRODUCTION_ROUTING_KEY);
//    }
//
//    /**
//     * 售货机服务队列
//     * @return
//     */
//    @Bean
//    public Queue vmServiceQueue(){
//        return new Queue(MqConfig.VM_SERVICE_ROUTING_KEY,true);
//    }
//
//    @Bean
//    public TopicExchange vmServiceExchange(){
//        return new TopicExchange(MqConfig.VM_SERVICE_ROUTING_KEY,true,false);
//    }
//
//    @Bean
//    public Binding bindingVMService()
//    {
//        return BindingBuilder.bind(vmServiceQueue()).to(vmServiceExchange()).with(MqConfig.VM_SERVICE_ROUTING_KEY);
//    }
//
//    /**
//     * 售货机服务处理出货队列
//     * @return
//     */
//    @Bean
//    public Queue vendoutResultQueue(){
//        return new Queue("vmService",true);
//    }
//
//    @Bean
//    public FanoutExchange vendoutExchange(){
//        return new FanoutExchange(MqConfig.VM_VENDOUT_RESULT,true,false);
//    }
//
//    @Bean
//    Binding bindingToVendoutResultExchangeMessage() {
//        return BindingBuilder.bind(vendoutResultQueue()).to(vendoutExchange());
//    }
//
//
//    //订单处理出货队列
//    @Bean
//    public Queue orderVendoutResultQueue(){
//        return new Queue("orderService",true);
//    }
//
//
//    @Bean
//    Binding bindingToOrderVendoutExchangeMessage() {
//        return BindingBuilder.bind(orderVendoutResultQueue()).to(vendoutExchange());
//    }
//
//    //状态服务处理队列
//    @Bean
//    public Queue statusServiceQueue(){
//        return new Queue("statusService",true);
//    }
//
//    @Bean
//    public FanoutExchange statusExchange(){
//        return new FanoutExchange(MqConfig.VM_STATUS_ROUTING_KEY,true,false);
//    }
//
//    @Bean
//    public Binding bindToStatusExchange(){
//        return BindingBuilder.bind(statusServiceQueue()).to(statusExchange());
//    }
//
//    /**
//     * 定义队列
//     * @return
//     */
//    @Bean
//    public Queue productServiceQueue(){
//        return new Queue("productService",true);
//    }
//
//
//
//    /**
//     * 将队列绑定到交换机
//     * @return
//     */
//    @Bean
//    public Binding bindTaskToStatusExchange(){
//        return BindingBuilder.bind(productServiceQueue()).to(statusExchange());
//    }
//}
