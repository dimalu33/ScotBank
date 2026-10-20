package uk.co.asepstrath.bank.model;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

class InvestorTests {

  Investor i;

  @BeforeEach
  void setUp() {
    i = new Investor("123", "Steve", new BigDecimal(4003.26));
  }

  @Test
  void createInvestor() {
    Assertions.assertNotNull(i);

    Assertions.assertEquals("123", i.getId());
    Assertions.assertEquals("Steve", i.getName());
    Assertions.assertEquals(new BigDecimal(4003.26), i.getCashBalance());
  }

  @Test
  void depositCash() {
    Assertions.assertEquals(new BigDecimal(4003.26), i.getCashBalance());

    i.deposit(new BigDecimal(150.00));

    Assertions.assertEquals(new BigDecimal(4153.26), i.getCashBalance());
  }

  @Test
  void withdrawCash() {
    Assertions.assertEquals(new BigDecimal(4003.26), i.getCashBalance());

    i.withdraw(new BigDecimal(25.00));

    Assertions.assertEquals(new BigDecimal(3978.26), i.getCashBalance());
  }

  @Test
  void withdrawAllCash() {
    i.withdraw(i.getCashBalance());

    Assertions.assertEquals(0, i.getCashBalance().compareTo(new BigDecimal(0)));
  }

  @Test
  void withdrawTooMuchThrowsError() {
    BigDecimal amount = new BigDecimal("5000.00");
    Assertions.assertThrows(IllegalArgumentException.class, () -> i.withdraw(amount));
  }
}

