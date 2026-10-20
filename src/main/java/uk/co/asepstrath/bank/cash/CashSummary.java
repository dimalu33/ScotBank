package uk.co.asepstrath.bank.cash;

import uk.co.asepstrath.bank.model.Transaction;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

public class CashSummary {
    private final BigDecimal balance;
    private final BigDecimal totalDeposits;
    private final BigDecimal totalWithdrawals;
    private final List<Transaction> transactions;

    public CashSummary(BigDecimal balance, BigDecimal totalDeposits, BigDecimal totalWithdrawals, List<Transaction> transactions) {
        this.balance = balance;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.transactions = transactions;
    }
    public BigDecimal getBalance() {

        return balance;
    }
    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }
    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }
    public List<Transaction> getTransactions() {
        return transactions;
    }

}
