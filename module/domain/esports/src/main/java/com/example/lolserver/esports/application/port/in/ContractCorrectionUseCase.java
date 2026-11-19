package com.example.lolserver.esports.application.port.in;

import com.example.lolserver.esports.application.command.CorrectContractCommand;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterIssueReadModel;

import java.util.List;

/**
 * 관리자 — 오입력 정정·삭제와 검증 결과. 사건 규칙을 거치지 않으므로 앞뒤가 어긋날 수 있고,
 * 어긋나면 {@link #getRosterIssues()} 가 보여준다. 기간 겹침은 DB 제약이 최종 방어한다(409).
 */
public interface ContractCorrectionUseCase {

    List<ContractReadModel> getContracts(String personId);

    ContractReadModel correctContract(Long contractId, CorrectContractCommand command);

    void deleteContract(Long contractId);

    void deleteCareerEvent(Long eventId);

    List<RosterIssueReadModel> getRosterIssues();
}
