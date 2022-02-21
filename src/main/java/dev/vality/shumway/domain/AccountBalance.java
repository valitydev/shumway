package dev.vality.shumway.domain;

public class AccountBalance {
    private final Long id;
    private final Long startAmount;
    private final Long finalAmount;

    public AccountBalance(Long id, Long startAmount, Long finalAmount) {
        this.id = id;
        this.startAmount = startAmount;
        this.finalAmount = finalAmount;
    }

    public Long getId() {
        return id;
    }

    public Long getStartAmount() {
        return startAmount;
    }

    public Long getFinalAmount() {
        return finalAmount;
    }
}
