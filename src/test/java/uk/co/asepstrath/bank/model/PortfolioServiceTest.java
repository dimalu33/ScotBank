package uk.co.asepstrath.bank.model;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.service.PortfolioService;
import uk.co.asepstrath.bank.service.StockService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioServiceTest {
    private PortfolioService portfolioService;
    private InvestorAccount account;
    private DataSource ds;

    // Fake service for
    private static class FakeStockService extends StockService {
        @Override
        public void prefetchMetadata(Set<String> tickers) {
            // Do nothing, so we can guarantee each tickers metadata
        }

        @Override
        public CompanyMetaData getMetadata(String ticker) {
            if (ticker.equals("AAPL")) return new CompanyMetaData("USA", "North America", "Technology");
            if (ticker.equals("BP")) return new CompanyMetaData("UK", "Europe", "Energy");
            return new CompanyMetaData("Unknown", "Unknown", "Unknown");
        }
    }

    // Create a new database for tests
    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource jds = new JdbcDataSource();
        jds.setURL("jdbc:h2:mem:portfolio_test;DB_CLOSE_DELAY=-1");
        this.ds = jds;

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS StockPrices (ticker VARCHAR(10), timestamp TIMESTAMP, price DECIMAL(32,18))");
            stmt.execute("DELETE FROM StockPrices");
            stmt.execute("INSERT INTO StockPrices VALUES ('AAPL', NOW(), 150.00)");
            stmt.execute("INSERT INTO StockPrices VALUES ('BP', NOW(), 150.00)");
        }

        portfolioService = new PortfolioService(new FakeStockService());
        account = new InvestorAccount("test-investor");
        account.investorDepositForTests(1000000.00);
    }

    @Test
    void testMarketValueIsUsedInsteadOfBookValue() {
        account.buyStock("AAPL", 10, 10.00);

        Map<String, BigDecimal> result = portfolioService.getCategorizedData(account, "sector", ds);

        BigDecimal actualValue = result.get("Technology");
        BigDecimal expectedValue = BigDecimal.valueOf(1500.0);

        assertNotNull(actualValue, "Technology sector should exist in the chart");
        assertEquals(0, actualValue.compareTo(expectedValue));
    }

    @Test
    void testFivePercentRuleGroupsSmallPositions() {
        account.buyStock("AAPL", 100, 150.00); // Value: 15,000
        account.buyStock("BP", 1, 150.00);     // Value: 150 (Total 15,150. BP is < 5%)

        Map<String, BigDecimal> result = portfolioService.getCategorizedData(account, "sector", ds);

        // AAPL (Tech) is > 5%, BP (Energy) is < 5%
        assertTrue(result.containsKey("Technology"));
        assertFalse(result.containsKey("Energy"));
        assertTrue(result.containsKey("Other"));
    }

    @Test
    void testCashIsExcludedFromCalculation() {
        account.investorDepositForTests(999999.00);
        account.buyStock("AAPL", 1, 150.00);

        Map<String, BigDecimal> result = portfolioService.getCategorizedData(account, "sector", ds);

        BigDecimal totalChartValue = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, totalChartValue.compareTo(BigDecimal.valueOf(150.00)), "Chart should ignore cash balance");
    }
}