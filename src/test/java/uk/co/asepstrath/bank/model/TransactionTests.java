package uk.co.asepstrath.bank.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

class TransactionTests {

  @Test
  void testDepositTransaction() {
    Transaction t = new Transaction(
      "000-abc-123",
      Transaction.TransactionType.DEPOSIT,
      Timestamp.from(Instant.parse("2007-12-03T10:15:30.00Z")),
      "000-xyz-555",
      new BigDecimal(17.75)
    );

    Assertions.assertEquals("000-abc-123", t.getId());
    Assertions.assertEquals(Transaction.TransactionType.DEPOSIT, t.getType());
    Assertions.assertEquals(
      Timestamp.from(Instant.parse("2007-12-03T10:15:30.00Z")),
      t.getTime()
    );
    Assertions.assertEquals(0, t.getCashAmount().compareTo(new BigDecimal(17.75)));
  }

  @Test
  void testBuyTransaction() {
    Transaction t = new Transaction(
      "000-fah-892",
      Transaction.TransactionType.BUY,
      Timestamp.from(Instant.parse("2008-07-05T11:00:30.00Z")),
      "000-xyz-555",
      new BigDecimal(80.03),
      "AAPL",
      2
    );

    Assertions.assertEquals("000-fah-892", t.getId());
    Assertions.assertEquals(Transaction.TransactionType.BUY, t.getType());
    Assertions.assertEquals(
      Timestamp.from(Instant.parse("2008-07-05T11:00:30.00Z")),
      t.getTime()
    );
    Assertions.assertEquals(0, t.getCashAmount().compareTo(new BigDecimal(80.03)));
    Assertions.assertEquals("AAPL", t.getTicker());
    Assertions.assertEquals(2, t.getShareAmount());
  }
  
}

