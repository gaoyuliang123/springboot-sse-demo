package com.example.springboot.sse.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: SseEmitter
 *
 * SseEmitter是SpringMVC(4.2+)提供的一种技术,它是基于Http协议的，相比WebSocket，它更轻量，但是它只能从服务端向客户端单向发送信息。
 * 在SpringBoot中我们无需引用其他jar就可以使用，默认支持断线重连.
 * 
 * sse 规范
 *
 * 请求头: 开启长连接 + 流方式传递
 * Content-Type: text/event-stream;charset=UTF-8
 * Cache-Control: no-cache
 * Connection: keep-alive
 *
 * 数据格式:
 * 服务端发送的消息，由 message 组成，其格式如下:
 * field:value\n\n
 * 其中 field 有五种可能
 * 空: 即以:开头，表示注释，可以理解为服务端向客户端发送的心跳，确保连接不中断
 * data：数据
 * event: 事件，默认值
 * id: 数据标识符用 id 字段表示，相当于每一条数据的编号
 * retry: 重连时间
 * @Author:
 * @Date: 2020/12/22 15:17
 **/
public class SseEmitterServer {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterServer.class);

    /**
     * 当前连接数
     */
    private static AtomicInteger count = new AtomicInteger(0);

    /**
     * 使用map对象，便于根据userId来获取对应的SseEmitter，或者放redis里面
     */
    private static Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();

    public static SseEmitter connect(final String userId) {
        // 设置超时时间，0表示不过期。默认30秒，超过时间未完成会抛出异常：AsyncRequestTimeoutException
        SseEmitter sseEmitter = new SseEmitter(0L);
        // 注册回调
        sseEmitter.onCompletion(() -> {
            logger.info("结束连接：{}", userId);
            removeUser(userId);
        });
        sseEmitter.onTimeout(() -> {
            logger.info("连接超时：{}", userId);
            removeUser(userId);
        });
        sseEmitter.onError((throwable) -> {
            logger.info("连接异常：{}", userId);
            removeUser(userId);
        });
        try {
            // 设置前端的重试时间为3s
            sseEmitter.send(SseEmitter.event().reconnectTime(5000).data("连接成功"));
        } catch (IOException e) {
            logger.error("用户[{}]连接推送异常:{}", userId, e.getMessage());
        }
        sseEmitterMap.put(userId, sseEmitter);
        count.getAndIncrement();
        return sseEmitter;
    }

    /**
     * 给指定单个用户发送信息
     */
    public static void sendMessage(String userId, String message) {
        if (sseEmitterMap.containsKey(userId)) {
            try {
                sseEmitterMap.get(userId).send(message);
            } catch (IOException e) {
                logger.error("用户[{}]推送异常:{}", userId, e.getMessage());
                removeUser(userId);
            }
        }
    }

    /**
     * 群发指定人
     * @param userIds
     * @param message
     */
    public static void batchSendMessage(List<String> userIds, String message) {
        userIds.forEach((userId) -> {sendMessage(userId, message);});
    }

    /**
     * 群发所有人
     * @param message
     */
    public static void batchSendMessage(String message) {
        sseEmitterMap.forEach((userId, sseEmitterServer) -> {
            try {
                sseEmitterServer.send(message, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                logger.error("用户[{}]推送异常:{}", userId, e.getMessage());
                removeUser(userId);
            }
        });
    }
    /**     
     * 移除用户连接     
     */
    public static void removeUser(String userId) {
        sseEmitterMap.remove(userId);
        count.getAndDecrement();
        logger.info("移除用户：{}", userId);
    }


    /**
     * 获取当前连接信息
     */
    public static List<String> getIds() {
        return new ArrayList<>(sseEmitterMap.keySet());
    }

    /**
     * 获取当前连接数量
     */
    public static int getUserCount() {
        return count.intValue();
    }





}
