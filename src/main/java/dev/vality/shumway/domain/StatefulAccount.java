package dev.vality.shumway.domain;

import java.time.Instant;
import java.util.Objects;

public class StatefulAccount extends Account {
    private final AccountState accountState;

    public StatefulAccount(
            long id,
            Instant creationTime,
            String currSymCode,
            String description,
            AccountState accountState
    ) {
        super(id, creationTime, currSymCode, description);
        this.accountState = accountState;
    }

    public StatefulAccount(Account prototype, AccountState accountState) {
        super(prototype);
        this.accountState = accountState;
    }

    public AccountState getAccountState() {
        return accountState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatefulAccount)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        StatefulAccount that = (StatefulAccount) o;
        return Objects.equals(accountState, that.accountState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accountState);
    }

    @Override
    public String toString() {
        return "StatefulAccount{" +
                "accountState=" + accountState +
                "} " + super.toString();
    }
}
