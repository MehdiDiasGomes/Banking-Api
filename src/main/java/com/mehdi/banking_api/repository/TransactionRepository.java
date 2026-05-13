package com.mehdi.banking_api.repository;

import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findBySenderOrReceiver(Account sender, Account receiver);
}
