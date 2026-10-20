package uk.co.asepstrath.bank.model;

import java.sql.Timestamp;
import java.math.BigDecimal;

public class Transaction {
  private String id;
  private TransactionType type;
  private Timestamp time; 
  private String investorId;
  private BigDecimal cashAmount;
  private BigDecimal capitalGain;

  // These fields are for BUY and SELL transactions.
  private String ticker;
  private long shareAmount;

  public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    BUY,
    SELL,
  }

  public Transaction(
    String tid,
    TransactionType tt,
    Timestamp ts,
    String iid,
    BigDecimal cash,
    String tick,
    long shares
  ) {
    id = tid;
    type = tt;
    time = ts;
    investorId = iid;
    ticker = tick;
    shareAmount = shares;
    cashAmount = cash;
  }

  public Transaction(
    String tid,
    TransactionType tt,
    Timestamp ts,
    String iid,
    BigDecimal cash
  ) {
    id = tid;
    type = tt;
    time = ts;
    investorId = iid;
    cashAmount = cash;
    ticker = null;
    shareAmount = 0;
  }

  public String getId() { return id; }

  public TransactionType getType() { return type; }

  public Timestamp getTime() { return time; }

  public String getInvestorId() { return investorId; }

  public BigDecimal getCashAmount() { return cashAmount; }

  public String getTicker() { return ticker; }

  public long getShareAmount() { return shareAmount; }

  public BigDecimal getCapitalGain() { return capitalGain; }

  public void setCapitalGain(BigDecimal capitalGain) {
    this.capitalGain = capitalGain;
  }
}
