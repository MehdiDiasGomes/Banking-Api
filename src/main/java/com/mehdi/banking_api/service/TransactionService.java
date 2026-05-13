package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.TransferRequest;
import com.mehdi.banking_api.dto.response.TransactionResponse;
import com.mehdi.banking_api.exception.BusinessException;
import com.mehdi.banking_api.exception.ForbiddenException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.Transaction;
import com.mehdi.banking_api.model.TransactionStatus;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.AccountRepository;
import com.mehdi.banking_api.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public List<TransactionResponse> getHistory(String iban, User connectedUser) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));

        if (!account.getOwner().getId().equals(connectedUser.getId())) {
            throw new ForbiddenException("You are not allowed to view this account's history");
        }

        return transactionRepository.findBySenderOrReceiver(account, account)
                .stream()
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getAmount(),
                        t.getSender().getIban(),
                        t.getReceiver().getIban(),
                        t.getStatus(),
                        t.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public TransactionResponse transfer(User connectedUser, TransferRequest request) {
        Account sender = accountRepository.findByIban(request.getSenderIban())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found: " + request.getSenderIban()));
        Account receiver = accountRepository.findByIban(request.getReceiverIban())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found: " + request.getReceiverIban()));

        if (!sender.getOwner().getId().equals(connectedUser.getId())) {
            throw new ForbiddenException("You are not allowed to transfer from this account");
        }

        if (sender.getBalance() < request.getAmount()) {
            throw new BusinessException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance() - request.getAmount());

        receiver.setBalance(receiver.getBalance() + request.getAmount());

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .sender(sender)
                .receiver(receiver)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return new TransactionResponse(
                saved.getId(),
                saved.getAmount(),
                saved.getSender().getIban(),
                saved.getReceiver().getIban(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }
}
