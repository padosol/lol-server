package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.LinkSummonerCommand;
import jakarta.validation.constraints.Size;

/** {@code visible} 을 빼면 공개(true). */
public record LinkSummonerRequest(
        boolean main,
        Boolean visible,
        @Size(max = 300) String note
) {
    public LinkSummonerCommand toCommand() {
        return new LinkSummonerCommand(main, visible == null || visible, note);
    }
}
