package uk.co.asepstrath.bank.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioEvaluatorTest {

    @Test
    void testEvaluate() {
        InvestorAccount account = new InvestorAccount();
        account.investorDepositForTests(10000.00);
        account.buyStock("AAPL", 10, 150.00);  // cost £1500
        account.buyStock("MSFT", 5, 300.00);   // cost £1500

        PortfolioSummary summary = new PortfolioEvaluator(account, Map.of(
                "AAPL", new BigDecimal("185.50"),
                "MSFT", new BigDecimal("420.00")
        )).evaluate();

        assertEquals(0, summary.getTotalBookValue().compareTo(new BigDecimal("3000.00")));
        assertEquals(0, summary.getTotalMarketValue().compareTo(new BigDecimal("3955.00")));
        assertEquals(0, summary.getpnl().compareTo(new BigDecimal("955.00")));
        assertEquals(0, summary.getCash().compareTo(new BigDecimal("7000.0")));
        assertFalse(summary.isHasMissingPrices());
    }

    @Test
    void testMissingPrice() {
        InvestorAccount account = new InvestorAccount();
        account.investorDepositForTests(5000.00);
        account.buyStock("AAPL", 10, 150.00);

        PortfolioSummary summary = new PortfolioEvaluator(account, Map.of()).evaluate();

        assertTrue(summary.isHasMissingPrices());
        assertNull(summary.getPositions().get(0).getMarketValue());
        assertNull(summary.getPositions().get(0).getPnl());
    }
}