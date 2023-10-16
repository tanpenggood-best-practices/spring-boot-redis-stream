package com.itplh.best.practices.sample.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/redis/stream")
public class RedisStreamController {

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/push-data")
    public String pushData(String stream) {
        Map<String, String> map = new HashMap<>();
        map.put("k", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS").format(LocalDateTime.now()));
        RecordId recordId = redisTemplate.opsForStream().add(stream, map);
        return "Success: " + recordId;
    }

}
