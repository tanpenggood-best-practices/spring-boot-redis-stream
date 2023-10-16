## Spring Boot Redis Stream

I use the best practices of Redis stream in spring boot.

### Feature

- [x] Verify FullName(stream + group + consumer_name) **not blank** and **duplicate**, when register Listener
- [x] `com.itplh.best.practices.stream.MyStreamListener` add the following extensions:
    1. getStream()
    1. getGroup()
    1. getConsumerName()
    1. getFullName()
    1. getFullGroup()
- [x] Abstract class `com.itplh.best.practices.stream.AbstractAutoRetryStreamListener`
    1. Automatically enable the failed retry mechanism, when the stream is single consume group
        1. default maximum retry count: 10
        2. customize the override of the 'maxRetries' method
    1. Support successful callback ` onSuccess`
    1. Support failure callback (reaching the max retries) `onFailure`
    1. Support finally callback `onFinally`

### Development Environment

- Java 1.8
- Redis 5.0.10
- Spring Boot 2.3.4.RELEASE
- Jackson 2.11.2
- Lombok 1.18.12

### Sample

1. Run `com.itplh.best.practices.Application`
2. push data to stream
    ```bash
    curl --location --request GET 'http://localhost:8080/redis/stream/push-data?stream=stream_test1'
    ```

### Docs

[Redis Stream - Best Practices](http://showdoc.itplh.com/web/#/4?page_id=382)

[Should XDEL be executed after XACK ?](http://showdoc.itplh.com/web/#/4?page_id=403)