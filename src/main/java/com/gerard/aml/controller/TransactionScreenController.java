package com.gerard.aml.controller;

import com.gerard.aml.domain.TransactionScreenRequest;
import com.gerard.aml.domain.TransactionScreenResponse;
import com.gerard.aml.service.TransactionScreenService;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TransactionScreenController {

    private final TransactionScreenService transactionScreenService;

    public TransactionScreenController(TransactionScreenService transactionScreenService) {
        this.transactionScreenService = transactionScreenService;
    }

    @PostMapping("/transactions/screen")
    public ResponseEntity<TransactionScreenResponse> screenTransaction(
            @Valid @RequestBody TransactionScreenRequest request) {
        return ResponseEntity.ok(transactionScreenService.screen(request));
    }
}
