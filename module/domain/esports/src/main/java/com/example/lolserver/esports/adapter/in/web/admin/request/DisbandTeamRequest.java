package com.example.lolserver.esports.adapter.in.web.admin.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DisbandTeamRequest(@NotNull LocalDate disbandedAt) {
}
