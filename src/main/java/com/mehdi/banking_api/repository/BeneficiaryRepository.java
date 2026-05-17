package com.mehdi.banking_api.repository;

import com.mehdi.banking_api.model.Beneficiary;
import com.mehdi.banking_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {
    List<Beneficiary> findByOwner(User owner);
}
