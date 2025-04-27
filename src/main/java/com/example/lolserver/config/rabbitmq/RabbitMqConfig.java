package com.example.lolserver.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    /* Queue: Consumer 가 메시지를 소비하기 전 메시지 보관소 */
    @Bean
    public Queue queue1() {
        return new Queue("mmrtr.summoner");
    }

    @Bean
    public Queue queue2() {
        return new Queue("mmrtr.match");
    }

    @Bean
    public TopicExchange directExchange() {
        return new TopicExchange(exchangeName);
    }

    /* Binding: Queue 와 Exchange 관계 정의 */
    @Bean
    public Binding binding1() {
        return BindingBuilder
                .bind(queue1())
                .to(directExchange())
                .with(routingKey);
    }

    @Bean
    public Binding binding2() {
        return BindingBuilder
                .bind(queue2())
                .to(directExchange())
                .with("mmrtr.routing.match");
    }

    /* RabbitMQ 연결 설정 */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitmqHost);
        connectionFactory.setPort(rabbitmqPort);
        connectionFactory.setUsername(rabbitmqUsername);
        connectionFactory.setPassword(rabbitmqPassword);
        return connectionFactory;
    }


    /* 연결 설정으로 연결 후 실제 작업을 위한 RabbitTemplate */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // JSON 형식의 메시지를 직렬화하고 역직렬할 수 있도록 설정
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /* 메시지를 JSON 기반으로 변환하는 메시지 컨버터 */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
