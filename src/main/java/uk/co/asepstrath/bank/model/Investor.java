package uk.co.asepstrath.bank.model;

// We need an Investor class to store investor data, including ID, Name, Cash Balance

import java.math.BigDecimal;

public class Investor {

    private String id;
    private String name;
    private BigDecimal cashBalance;

    public Investor() {}

    public Investor(String id, String name, BigDecimal cashBalance) {
        this.id = id;
        this.name = name;
        this.cashBalance = cashBalance;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public BigDecimal getCashBalance() { return cashBalance; }

    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public void deposit(BigDecimal amount) {
        cashBalance = cashBalance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(cashBalance) > 0) {
            throw new IllegalArgumentException("Withdrawal amount exceeds balance.");
        }
        cashBalance = cashBalance.subtract(amount);
    }

}
