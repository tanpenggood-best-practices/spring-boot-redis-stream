package com.itplh.best.practices.config;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itplh.best.practices.stream.MyStreamListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@Slf4j
@Configuration
@AllArgsConstructor
public class RedisStreamConfiguration {

    private static Set<String> listenerContainerNames = new ConcurrentSkipListSet<>();

    private RedisTemplate<String, Object> redisTemplate;
    private List<MyStreamListener> streamListeners;

    @Bean
    public StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> streamMessageListenerContainerOptions() {
        // jackson2JsonRedisSerializer
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // stringRedisSerializer
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        return StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                // Block read timeout
                .pollTimeout(Duration.ofSeconds(3))
                // batch size(only obtaining one message at a time)
                .batchSize(3)
                // Serialization rules
                .keySerializer(stringRedisSerializer)
                .hashKeySerializer(stringRedisSerializer)
                .hashValueSerializer(jackson2JsonRedisSerializer)
                .executor(redisStreamThreadPoolTaskExecutor())
                .build();
    }

    @Bean("redisStreamThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor redisStreamThreadPoolTaskExecutor() {
        int coreSize = Runtime.getRuntime().availableProcessors() + 1;
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(coreSize);
        taskExecutor.setMaxPoolSize(coreSize);
        // https://github.com/spring-projects/spring-data-redis/issues/2753
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("my-redis-stream-");
        taskExecutor.setRejectedExecutionHandler((r, executor) -> log.error("RejectedExecutionHandler: runnable={} executor={}", r, executor));
        // If not initialized, the actuator cannot be found
        taskExecutor.initialize();
        return taskExecutor;
    }

    /**
     * 开启监听器接收消息
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(RedisConnectionFactory factory,
                                                                                                                    StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> streamMessageListenerContainerOptions) {
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(factory, streamMessageListenerContainerOptions);

        for (MyStreamListener streamListener : streamListeners) {
            registerListener(listenerContainer, streamListener);
        }

        return listenerContainer;
    }

    private void registerListener(StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer,
                                  MyStreamListener listener) {
        String stream = listener.getStream();
        String group = listener.getGroup();
        String consumerName = listener.getConsumerName();
        // assert
        Assert.hasText(stream, "Stream is required.");
        Assert.hasText(group, "Group is required.");
        Assert.hasText(consumerName, "Consumer name is required.");
        String fullName = listener.getFullName();
        Assert.isTrue(!listenerContainerNames.contains(fullName), String.format("listener [%s] already registered", fullName));
        // create stream, if not exists
        if (!redisTemplate.hasKey(stream)) {
            redisTemplate.opsForStream().add(stream, Collections.singletonMap("", ""));
            log.info("init stream [{}] success", stream);
        }
        // create group
        try {
            redisTemplate.opsForStream().createGroup(stream, group);
        } catch (Exception e) {
            log.warn("consumer group [{}:{}] already exists", stream, group);
        }
        // register stream listener
        // set consumer name, starting consume from which message
        // > messages that have not been consumed
        // $ the latest messages
        listenerContainer.register(StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create(stream, ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(group, consumerName))
                        // Manually submit ACK
                        .autoAcknowledge(false)
                        .build(),
                listener);
        listenerContainerNames.add(fullName);
    }

}
