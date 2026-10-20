package uk.co.asepstrath.bank.model;

import java.math.BigDecimal;
import java.util.List;
import java.math.RoundingMode;

public class PortfolioSummary {
    private final BigDecimal cash;
    private final List<PortfolioPosition> positions;
    private final BigDecimal totalBookValue;
    private final BigDecimal totalMarketValue;
    private final BigDecimal pnl;
    private final BigDecimal pnlPct;
    private final BigDecimal totalPortfolioValue;
    private final boolean missingPrices;

    public PortfolioSummary(BigDecimal cash, List<PortfolioPosition> positions) {
        this.cash      = cash;
        this.positions = positions;

        BigDecimal sumBook         = BigDecimal.ZERO;
        BigDecimal sumMarket       = BigDecimal.ZERO;
        BigDecimal sumBookOfPriced = BigDecimal.ZERO;
        boolean missing            = false;

        for (PortfolioPosition p : positions) {
            sumBook = sumBook.add(p.getBookValue());
            if (p.hasPriceData()) {
                sumMarket       = sumMarket.add(p.getMarketValue());
                sumBookOfPriced = sumBookOfPriced.add(p.getBookValue());
            } else {
                missing = true;
            }
        }

        this.totalBookValue   = sumBook;
        this.totalMarketValue = sumMarket;
        this.missingPrices = missing;

        BigDecimal pnl     = sumMarket.subtract(sumBookOfPriced);
        this.pnl = pnl;
        if (sumBookOfPriced.signum() > 0) {
            this.pnlPct = pnl.divide(sumBookOfPriced, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        } else {
            this.pnlPct = BigDecimal.ZERO;
        }

        this.totalPortfolioValue = this.cash.add(sumMarket).add(sumBook.subtract(sumBookOfPriced));
    }

    public BigDecimal getCash()                          { return cash; }
    public List<PortfolioPosition> getPositions()        { return positions; }
    public BigDecimal getTotalBookValue()      { return totalBookValue; }
    public BigDecimal getTotalMarketValue()    { return totalMarketValue; }
    public BigDecimal getpnl()                 { return pnl; }
    public BigDecimal getpnlPct()              { return pnlPct; }
    public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
    public boolean isHasMissingPrices()        { return missingPrices; }
}