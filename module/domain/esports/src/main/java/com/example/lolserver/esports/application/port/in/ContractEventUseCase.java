package com.example.lolserver.esports.application.port.in;

import com.example.lolserver.esports.application.command.ContractCommand;
import com.example.lolserver.esports.application.command.DatedCommand;
import com.example.lolserver.esports.application.command.HiatusCommand;
import com.example.lolserver.esports.application.command.LoanCommand;
import com.example.lolserver.esports.application.command.MoveCommand;
import com.example.lolserver.esports.application.model.resultmodel.ContractEventResultModel;

/**
 * 관리자 — 계약 사건. 사건 하나는 트랜잭션 하나다(이적이면 직전 계약 종료와 새 계약 생성이 함께).
 * 규칙은 오늘이 아니라 효력 발생일(date) 기준으로 인물의 전체 타임라인에 대해 검사한다.
 */
public interface ContractEventUseCase {

    ContractEventResultModel sign(String personId, ContractCommand command);

    ContractEventResultModel renew(String personId, ContractCommand command);

    ContractEventResultModel transfer(String personId, ContractCommand command);

    ContractEventResultModel move(String personId, MoveCommand command);

    ContractEventResultModel loan(String personId, LoanCommand command);

    ContractEventResultModel recall(String personId, Long contractId, DatedCommand command);

    ContractEventResultModel release(String personId, DatedCommand command);

    ContractEventResultModel retire(String personId, DatedCommand command);

    ContractEventResultModel unretire(String personId, DatedCommand command);

    ContractEventResultModel hiatus(String personId, HiatusCommand command);

    ContractEventResultModel comeBack(String personId, DatedCommand command);
}
