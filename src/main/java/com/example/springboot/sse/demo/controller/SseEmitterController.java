package com.example.springboot.sse.demo.controller;

import com.example.springboot.sse.demo.service.SseEmitterServer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @Description: SseEmitter 推送消息
 * @Author:
 * @Date: 2020/12/22 15:51
 **/
@RestController
@RequestMapping("/sse")
public class SseEmitterController {

    /**
     * 用于创建连接
     */
    @GetMapping(value = "/connect/{userId}", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter connect(@PathVariable String userId) {
        return SseEmitterServer.connect(userId);
    }

    @GetMapping("/close/{userId}")
    public void close(@PathVariable String userId) {
        SseEmitterServer.removeUser(userId);
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> list() {
        return ResponseEntity.ok(SseEmitterServer.getIds());
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getUserCount() {
        return ResponseEntity.ok(SseEmitterServer.getUserCount());
    }

    @GetMapping("/push/{message}")
    public ResponseEntity<String> push(@PathVariable(name = "message") String message) {
        SseEmitterServer.batchSendMessage(message);
        return ResponseEntity.ok("SseEmitter 推送消息给所有人");
    }

    @GetMapping("/push/{userId}/{message}")
    public ResponseEntity<String> pushTag(@PathVariable(name = "userId") String userId,
                                          @PathVariable(name = "message") String message) {
        SseEmitterServer.sendMessage(userId, message);
        return ResponseEntity.ok("SseEmitter 推送消息给：" + userId);
    }
}
