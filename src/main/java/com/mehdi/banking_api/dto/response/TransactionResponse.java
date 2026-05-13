package com.mehdi.banking_api.dto.response;

import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private Double amount;
    private String senderIban;
    private String receiverIban;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}
