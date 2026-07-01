package com.velet.wallet.worker;

import com.velet.wallet.models.BalanceComponents;
import com.velet.wallet.models.BalanceSnapshot;
import com.velet.wallet.models.BalanceSnapshot.BalanceSnapshotId;
import com.velet.wallet.repository.BalanceSnapshotRepository;
import com.velet.wallet.repository.LedgerRepository;
import com.velet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceSnapshotWorker {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final BalanceSnapshotRepository snapshotRepository;

    // Auto start at 3:00 am every day
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void createSnapshots() {
        List<Long> walletIds = ledgerRepository.findAllActiveWalletIds();
        log.info("snapshot.start walletCount={}", walletIds.size());

        for (Long walletId : walletIds) {
            try {
                snapshotWallet(walletId);
            } catch (Exception e) {
                log.error("snapshot.failed walletId={}", walletId, e);
            }
        }

        log.info("snapshot.complete");
    }

    private void snapshotWallet(Long walletId) {
        Long latestEntryId = ledgerRepository.findMaxIdByWalletId(walletId);
        if (latestEntryId == null) return;

        WalletRepository.BalanceRow row = walletRepository.computeBalanceRaw(walletId);
        BalanceComponents balance = new BalanceComponents(
                row.getPostedDebits(), row.getPostedCredits(),
                row.getPendingDebits(), row.getPendingCredits()
        );

        snapshotRepository.save(BalanceSnapshot.builder()
                .id(new BalanceSnapshotId(walletId, Instant.now()))
                .postedDebits(balance.postedDebits())
                .postedCredits(balance.postedCredits())
                .pendingDebits(balance.pendingDebits())
                .pendingCredits(balance.pendingCredits())
                .lastLedgerEntryId(latestEntryId)
                .build());
    }
}
