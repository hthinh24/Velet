package com.velet.wallet.repository;

import com.velet.wallet.models.Transaction;
import com.velet.wallet.models.enums.CancelReason;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.idempotencyKey = :idempotencyKey")
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
