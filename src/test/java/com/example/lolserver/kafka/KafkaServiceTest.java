package com.example.lolserver.kafka;

import com.example.lolserver.kafka.messageDto.SummonerMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaServiceTest {

    @Test
    void 직렬화_테스트() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        SummonerMessage summonerMessage = new SummonerMessage();
        summonerMessage.setId("setestes");
        summonerMessage.setPuuid("SETSEtestsetsetsets");

        String s = objectMapper.writeValueAsString(summonerMessage);

        System.out.println(s);

    }

}