package com.example.lolserver.domain.member.application.port.out;

public interface OAuthStatePort {

    void saveState(String state, long ttlSeconds);

    boolean validateAndDelete(String state);
}
