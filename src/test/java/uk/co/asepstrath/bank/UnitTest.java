package uk.co.asepstrath.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.model.Investor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UnitTest {
    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
    }

    @Test
    void newAccountShouldHaveZeroBalance() {
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void depositShouldIncreaseBalance() {
        account.deposit(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    void withdrawShouldDecreaseBalance() {
        account.deposit(new BigDecimal("50.00"));
        account.withdraw(new BigDecimal("20.00"));
        assertEquals(new BigDecimal("30.00"), account.getBalance());
    }

    @Test
    void shouldThrowExceptionOnInsufficientFunds() {
        account.deposit(new BigDecimal("10.00"));
        BigDecimal amount = new BigDecimal("20.00");
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(amount));
    }

    @Test
    void shouldNotAllowNegativeDeposit() {
        BigDecimal negativeAmount = new BigDecimal("-1.00");
        assertThrows(IllegalArgumentException.class, () -> account.deposit(negativeAmount));
    }

    @Test
    void investorShouldStoreCorrectData() {
        Investor investor = new Investor("ID-123", "John Doe", new BigDecimal("500.50"));
        assertEquals("John Doe", investor.getName());
        assertEquals(new BigDecimal("500.50"), investor.getCashBalance());
    }

    @Test
    void investorDepositShouldWork() {
        Investor i = new Investor("1", "Test", BigDecimal.TEN);
        i.deposit(BigDecimal.ONE);
        assertEquals(new BigDecimal("11"), i.getCashBalance());
    }
}