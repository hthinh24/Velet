package com.velet.wallet.app;

import com.velet.wallet.models.Wallet;
import com.velet.wallet.models.enums.AccountType;
import com.velet.wallet.repository.WalletRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Getter
@Slf4j
public class SystemAccountCache implements CommandLineRunner {

    private final WalletRepository walletRepository;

    private Map<AccountType, Long> accountIdMap = Map.of();

    @Override
    public void run(String... args) throws Exception {
        List<Wallet> systemWallets = walletRepository.findByAccountTypeIn(
                Set.of(AccountType.SUSPENSE_ACCOUNT,
                       AccountType.EQUITY_CAPITAL, AccountType.REVENUE_MDR,
                       AccountType.MARKETING_PROMO_BUDGET, AccountType.RESERVE_ACCOUNT));

        accountIdMap = systemWallets.stream()
                                    .collect(Collectors.toMap(Wallet::getType, Wallet::getId));

        log.info("System account cache initialized, data: {}", accountIdMap);
    }

    public Long resolve(AccountType type) {
        return accountIdMap.get(type);
    }
}