package com.itplh.best.practices.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public abstract class AbstractAutoRetryStreamListener<K, V extends Record<K, ?>> implements MyStreamListener<K, V> {

    /**
     * key = fullGroup + RecordId
     * value = retries
     */
    private static Map<String, LongAdder> retryMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate redisTemplate;

    public abstract void doOnMessage(V v);

    @Override
    public void onMessage(V message) {
        try {
            doOnMessage(message);
            try {
                onSuccess(message);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        } catch (Throwable e) {
            // XADD cannot send messages to the specified group,
            // don't enable retry mechanism when the stream has multiple consume groups
            if (redisTemplate.opsForStream().groups(getStream()).size() > 1) {
                onFailure(message, e);
                return;
            }
            // Consume failure, reaching maximum retry count
            LongAdder retries = retryMap.getOrDefault(retryKey(message.getId()), new LongAdder());
            if (retries.longValue() >= maxRetries()) {
                onFailure(message, e);
                return;
            }
            // Add the message to the Stream queue, once again
            RecordId newId = redisTemplate.opsForStream().add(message.withId(RecordId.autoGenerate()));
            // update retries
            retries.increment();
            retryMap.put(retryKey(newId), retries);
        } finally {
            // clear retry
            retryMap.remove(retryKey(message.getId()));
            // Submit ACK
            redisTemplate.opsForStream().acknowledge(getGroup(), message);
            // delete message, release memory
            redisTemplate.opsForStream().delete(getStream(), message.getId());
            // hooks
            onFinally(message);
        }
    }

    /**
     * max retries
     * default：10
     * The implementation class can customize the maximum number of retries by overriding this method
     */
    protected long maxRetries() {
        return 10L;
    }

    /**
     * hooks, triggered when consume success
     *
     * @param message
     */
    protected void onSuccess(V message) {
    }

    /**
     * hooks, triggered when consume failure
     *
     * @param message
     * @param e
     */
    protected void onFailure(V message, Throwable e) {
        saveToDeadLetterQueue(message);

        LongAdder retriesCount = retryMap.getOrDefault(retryKey(message.getId()), new LongAdder());
        log.error("message is put to dead letter queue, consumer={} message={} retries={} error={}", getFullName(), message.getValue(), retriesCount, e.getMessage(), e);
    }

    /**
     * hooks, triggered when finally end
     *
     * @param message
     */
    protected void onFinally(V message) {
    }

    private String retryKey(RecordId recordId) {
        return getFullGroup() + "-" + recordId.toString();
    }

    /**
     * save message to dead letter queue
     *
     * @param message
     */
    private void saveToDeadLetterQueue(V message) {
        String key = "my:hash:dead-letter-queue";
        String hk = getFullName() + ":" + message.getId();
        Object hv = message.getValue();
        redisTemplate.opsForHash().put(key, hk, hv);
    }

}
