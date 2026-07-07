package com.velet.wallet.repository;

import com.velet.wallet.models.Transaction;
import com.velet.wallet.models.enums.CancelReason;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Modifying
    @Query("UPDATE Transaction t SET t.status = :status WHERE t.id = :id")
    void updateTransactionStatus(Long id, String status);

    @Modifying
    @Query("UPDATE Transaction t SET t.status = 'FAILED', t.cancelReason = :reason WHERE t.id = :id")
    void cancelTransaction(Long id, @NotNull CancelReason reason);
}
