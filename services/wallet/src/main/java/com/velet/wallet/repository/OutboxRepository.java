package com.velet.wallet.repository;

import com.velet.wallet.models.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {}
