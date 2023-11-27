// package com.lkd.rabbitmq;
//
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.amqp.core.Message;
// import org.springframework.amqp.core.MessageDeliveryMode;
// import org.springframework.amqp.core.MessageProperties;
// import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
//
// import java.io.UnsupportedEncodingException;
//
// @Component
// @Slf4j
// public class Sender{
//     @Autowired
//     private RabbitTemplate rabbitTemplate;
//
//     /**
//      * 正常发送
//      * @param routingKey
//      * @param msg
//      */
//     public void send(String routingKey,String msg){
//         try {
//             MessageProperties msgProp = new MessageProperties();
//             msgProp.setContentType(MessageProperties.CONTENT_TYPE_JSON);
//             msgProp.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
//             Message message = new Message(msg.getBytes("UTF-8"),msgProp);
//             rabbitTemplate.send(routingKey,message);
//         } catch (UnsupportedEncodingException e) {
//             log.error("send msg to mq error,msg is:"+msg);
//         }
//     }
//
//     /**
//      * 广播式发送
//      * @param exchange
//      * @param msg
//      */
//     public void broadcast(String exchange,String msg){
//         try {
//             MessageProperties msgProp = new MessageProperties();
//             msgProp.setContentType(MessageProperties.CONTENT_TYPE_JSON);
//             msgProp.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
//             Message message = new Message(msg.getBytes("UTF-8"),msgProp);
//             rabbitTemplate.send(exchange,null,message);
//         } catch (UnsupportedEncodingException e) {
//             log.error("send msg to mq error,msg is:"+msg);
//         }
//     }
// }
