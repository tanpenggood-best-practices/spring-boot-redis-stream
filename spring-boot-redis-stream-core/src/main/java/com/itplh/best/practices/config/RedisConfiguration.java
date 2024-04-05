package com.itplh.best.practices.config;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@Configuration
public class RedisConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        // jackson2JsonRedisSerializer
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = objectMapper.copy();
        om.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        // 指定序列化输入的类型，类必须是非final修饰的。序列化时将对象全类名一起保存下来
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // stringRedisSerializer
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // Using string serialization to serialize key
        redisTemplate.setKeySerializer(stringRedisSerializer);
        // Using jackson to serialize redis value
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        // Using string serialization to serialize hash key
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        // Using jackson to serialize hash value
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
