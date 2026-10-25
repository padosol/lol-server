package com.example.lolserver.member.application.port.out;

import com.example.lolserver.member.domain.vo.RiotAccount;

public interface RiotAccountPort {

    RiotAccount fetchAccount(String accessToken);
}
