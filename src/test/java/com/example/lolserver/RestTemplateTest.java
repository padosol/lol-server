package com.example.lolserver;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

public class RestTemplateTest {


    @Test
    void RESTTEMPLATE_TEST() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForEntity()
    }

}
