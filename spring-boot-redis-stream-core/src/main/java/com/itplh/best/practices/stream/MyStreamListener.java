package com.itplh.best.practices.stream;

import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.stream.StreamListener;

public interface MyStreamListener<K, V extends Record<K, ?>> extends StreamListener<K, V> {

    String getStream();

    default String getGroup() {
        return "default_group";
    }

    default String getConsumerName() {
        return "default_consumer";
    }

    default String getFullName() {
        return getStream() + ":" + getGroup() + ":" + getConsumerName();
    }

    default String getFullGroup() {
        return getStream() + ":" + getGroup();
    }

}
