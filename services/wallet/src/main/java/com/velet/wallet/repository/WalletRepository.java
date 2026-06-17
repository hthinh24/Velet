package com.velet.wallet.repository;

import com.velet.wallet.models.Wallet;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    @Query("SELECT a FROM Wallet a WHERE a.ownerId = :ownerId")
    List<Wallet> findAllByOwnerId(String ownerId);

    @Modifying
    @Query("UPDATE Wallet w SET w.availableBalance = w.availableBalance - :amount WHERE w.id = :walletId")
    void deductBalance(@Param("walletId") Long walletId, @Param("amount") Long amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.availableBalance = w.availableBalance + :amount WHERE w.id = :walletId")
    void addBalance(@Param("walletId") Long walletId, @Param("amount") Long amount);
}
