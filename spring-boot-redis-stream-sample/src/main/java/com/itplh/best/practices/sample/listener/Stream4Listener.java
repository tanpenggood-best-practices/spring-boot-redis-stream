package com.itplh.best.practices.sample.listener;

import com.itplh.best.practices.stream.AbstractAutoRetryStreamListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class Stream4Listener extends AbstractAutoRetryStreamListener<String, MapRecord<String, String, String>> {

    @Override
    public void doOnMessage(MapRecord<String, String, String> message) {
        Map<String, String> msgMap = message.getValue();

        // do something

        double random = Math.random();
        if (random > 0.5) {
            log.warn("consume failure, consumer={} message={} random={}", getFullName(), message.getValue(), random);
            throw new RuntimeException("Message processing failed");
        }
        log.info("consume success, consumer={} message={}", getFullName(), message.getValue());
    }

    @Override
    public String getStream() {
        return "stream_test4";
    }

}
