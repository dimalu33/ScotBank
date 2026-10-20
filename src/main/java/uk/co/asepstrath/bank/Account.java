package uk.co.asepstrath.bank;

import java.math.BigDecimal;

public class Account {

    private BigDecimal balance = BigDecimal.ZERO;

    public BigDecimal getBalance() {
        return balance;
    }
    public void deposit(BigDecimal amount) {
        positiveAmount(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        positiveAmount(amount);

        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance = balance.subtract(amount);
    }

    private void positiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Insufficient balance: Amount is negative");
        }
    }
}
