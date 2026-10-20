package uk.co.scotbank;

import java.math.BigDecimal;

public class ScotBankCashAccount implements CashAccount {

    private long balancePennies = 0;                // stores the account balance (money in pennies instead of pounds)

    // takes amount and turns into pennies(long)
    private long toPennies(double amount) {
        BigDecimal bd = BigDecimal.valueOf(amount); // converts doubles into BigDecimal object

        // rejects negative amounts or zero amounts
        if (bd.signum() <= 0) {
            throw new IllegalArgumentException();
        }

        // reject more than 2 decimal places
        if (bd.scale() > 2) {
            throw new IllegalArgumentException();
        }

        return bd.movePointRight(2).longValue(); // shifts the decimal point two spaces to the right
    }

    // coverts pennies back into pounds (so user sees balance in pounds)
    @Override
    public double getBalance() {
        return balancePennies / 100.0;
    }

    // converts pounds to pennies + adds pennies to balance
    @Override
    public void deposit(double amount) {
        long pennies = toPennies(amount);
        balancePennies += pennies;
    }

    // converts withdraw amount to pennies
    @Override
    public void withdraw(double amount) {
        long pennies = toPennies(amount);

        if (pennies > balancePennies) {
            throw new IllegalArgumentException();
        }

        balancePennies -= pennies;   // subtract withdrawal from balance
    }
}