package com.velet.wallet.repository;

import com.velet.wallet.models.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {}
