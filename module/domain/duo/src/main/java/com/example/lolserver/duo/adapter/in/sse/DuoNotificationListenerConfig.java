package com.example.lolserver.duo.adapter.in.sse;

import com.example.lolserver.duo.adapter.notification.DuoNotificationChannels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 듀오 알림 채널 구독 컨테이너 등록.
 *
 * <p>현재 Redis pub/sub 구독은 duo 컨텍스트뿐이라 컨테이너를 duo 어댑터에 둔다.
 * 다른 컨텍스트가 구독을 추가하면 common/app 으로 승격을 검토한다.
 */
@Configuration
public class DuoNotificationListenerConfig {

    @Bean
    public RedisMessageListenerContainer duoNotificationListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            DuoNotificationRedisSubscriber duoNotificationRedisSubscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                duoNotificationRedisSubscriber,
                new ChannelTopic(DuoNotificationChannels.DUO_NOTIFICATION));
        return container;
    }
}
