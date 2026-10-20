package uk.co.asepstrath.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PortfolioPosition {

    private final String ticker;
    private final int shares;
    private final BigDecimal bookPrice;
    private final BigDecimal bookValue;
    private final BigDecimal marketPrice;
    private final BigDecimal marketValue;
    private final BigDecimal pnl;
    private final BigDecimal pnlPct;

    public PortfolioPosition(String ticker, int shares, BigDecimal bookPrice, BigDecimal marketPrice) {
        this.ticker    = ticker;
        this.shares    = shares;
        this.bookPrice = bookPrice;
        this.bookValue = bookPrice.multiply(BigDecimal.valueOf(shares));

        if (marketPrice != null) {
            this.marketPrice = marketPrice;
            this.marketValue = marketPrice.multiply(BigDecimal.valueOf(shares));
            this.pnl         = this.marketValue.subtract(this.bookValue);
            this.pnlPct      = this.pnl.divide(this.bookValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        } else {
            this.marketPrice = null;
            this.marketValue = null;
            this.pnl         = null;
            this.pnlPct      = null;
        }
    }

    public String getTicker()          { return ticker; }
    public int getShares()             { return shares; }
    public BigDecimal getBookPrice()   { return bookPrice; }
    public BigDecimal getBookValue()   { return bookValue; }
    public BigDecimal getMarketPrice() { return marketPrice; }
    public BigDecimal getMarketValue() { return marketValue; }
    public BigDecimal getPnl()         { return pnl; }
    public BigDecimal getPnlPct()      { return pnlPct; }
    public boolean hasPriceData()      { return marketPrice != null; }
}