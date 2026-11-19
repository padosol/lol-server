package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.esports.adapter.in.web.admin.request.ContractEventRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.DatedEventRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.HiatusRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.LoanRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.MoveRequest;
import com.example.lolserver.esports.adapter.in.web.admin.response.ContractEventResponse;
import com.example.lolserver.esports.application.model.resultmodel.ContractEventResultModel;
import com.example.lolserver.esports.application.port.in.ContractEventUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 — 계약 사건. 사건 하나가 한 번에 처리된다(이적이면 직전 계약 종료 + 새 계약). 모든 date 는 효력 발생일.
 * {@code /api/v1/admin/**} 는 ADMIN 만 (SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/esports/persons/{personId}")
@RequiredArgsConstructor
public class AdminContractEventController {

    private final ContractEventUseCase contractEventUseCase;

    @PostMapping("/signings")
    public ResponseEntity<ApiResponse<ContractEventResponse>> sign(
            @PathVariable String personId, @Valid @RequestBody ContractEventRequest request) {
        return created(contractEventUseCase.sign(personId, request.toCommand()));
    }

    @PostMapping("/renewals")
    public ResponseEntity<ApiResponse<ContractEventResponse>> renew(
            @PathVariable String personId, @Valid @RequestBody ContractEventRequest request) {
        return created(contractEventUseCase.renew(personId, request.toCommand()));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<ContractEventResponse>> transfer(
            @PathVariable String personId, @Valid @RequestBody ContractEventRequest request) {
        return created(contractEventUseCase.transfer(personId, request.toCommand()));
    }

    @PostMapping("/moves")
    public ResponseEntity<ApiResponse<ContractEventResponse>> move(
            @PathVariable String personId, @Valid @RequestBody MoveRequest request) {
        return created(contractEventUseCase.move(personId, request.toCommand()));
    }

    @PostMapping("/loans")
    public ResponseEntity<ApiResponse<ContractEventResponse>> loan(
            @PathVariable String personId, @Valid @RequestBody LoanRequest request) {
        return created(contractEventUseCase.loan(personId, request.toCommand()));
    }

    @PostMapping("/loans/{contractId}/recall")
    public ResponseEntity<ApiResponse<ContractEventResponse>> recall(
            @PathVariable String personId, @PathVariable Long contractId,
            @Valid @RequestBody DatedEventRequest request) {
        return created(contractEventUseCase.recall(personId, contractId, request.toCommand()));
    }

    @PostMapping("/releases")
    public ResponseEntity<ApiResponse<ContractEventResponse>> release(
            @PathVariable String personId, @Valid @RequestBody DatedEventRequest request) {
        return created(contractEventUseCase.release(personId, request.toCommand()));
    }

    @PostMapping("/retirements")
    public ResponseEntity<ApiResponse<ContractEventResponse>> retire(
            @PathVariable String personId, @Valid @RequestBody DatedEventRequest request) {
        return created(contractEventUseCase.retire(personId, request.toCommand()));
    }

    @PostMapping("/unretirements")
    public ResponseEntity<ApiResponse<ContractEventResponse>> unretire(
            @PathVariable String personId, @Valid @RequestBody DatedEventRequest request) {
        return created(contractEventUseCase.unretire(personId, request.toCommand()));
    }

    @PostMapping("/hiatuses")
    public ResponseEntity<ApiResponse<ContractEventResponse>> hiatus(
            @PathVariable String personId, @Valid @RequestBody HiatusRequest request) {
        return created(contractEventUseCase.hiatus(personId, request.toCommand()));
    }

    @PostMapping("/returns")
    public ResponseEntity<ApiResponse<ContractEventResponse>> comeBack(
            @PathVariable String personId, @Valid @RequestBody DatedEventRequest request) {
        return created(contractEventUseCase.comeBack(personId, request.toCommand()));
    }

    private static ResponseEntity<ApiResponse<ContractEventResponse>> created(ContractEventResultModel result) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ContractEventResponse.from(result)));
    }
}
