package com.example.lolserver.rabbitmq;

import com.example.lolserver.rabbitmq.service.RabbitMqService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RabbitController {

    private final RabbitMqService rabbitMqService;

    @GetMapping("/rabbit/{count}")
    public String rabbitTest(
            @PathVariable("count") int count
    ) {
        for (int i = 0;i < count; i++) {
            rabbitMqService.sendMessageForMatch("test"+ i);
        }

        return "ok";
    }
}
