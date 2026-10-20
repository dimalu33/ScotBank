package uk.co.asepstrath.bank;

import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.model.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/portfolio")
public class PortfolioController {

    private final DataSource dataSource;
    private final Logger logger;

    public PortfolioController(DataSource ds, Logger log) {
        this.dataSource = ds;
        this.logger = log;
    }

    @GET("/{id}")
    public ModelAndView<Map<String, Object>> portfolio(@PathParam String id) throws SQLException {

        // Load investor cash balance from DB
        InvestorAccount account = new InvestorAccount(id);
        String cashSql = "SELECT cashBalance FROM Investor WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(cashSql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Investor with id " + id + " not found");
                }
                account.investorDepositForTests(rs.getDouble("cashBalance"));
            }
        }

        // Load transactions from CSV (already synced from API at startup)
        TransactionCSVLoader.loadInvestors(account, "src/main/resources/transactions.csv");

        // Load EoD prices from StockPrices DB table (already synced from API at startup)
        Map<String, BigDecimal> eodPrices = new HashMap<>();
        List<String> tickers = new ArrayList<>(account.getPortfolio().keySet());

        String priceSql = "SELECT price FROM StockPrices WHERE ticker = ? ORDER BY timestamp DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(priceSql)) {
            for (String ticker : tickers) {
                stmt.setString(1, ticker);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        eodPrices.put(ticker, rs.getBigDecimal("price").setScale(2, java.math.RoundingMode.HALF_UP));
                    } else {
                        logger.warn("No price found in DB for ticker: {}", ticker);
                    }
                }
            }
        }

        PortfolioSummary summary = new PortfolioEvaluator(account, eodPrices).evaluate();

        Map<String, Object> model = new HashMap<>();
        model.put("summary", summary);
        model.put("positions", summary.getPositions());
        model.put("cash", summary.getCash().setScale(2, java.math.RoundingMode.HALF_UP));
        model.put("totalBookValue", summary.getTotalBookValue());
        model.put("totalMarketValue", summary.getTotalMarketValue());
        model.put("pnl", summary.getpnl());
        model.put("pnlPct", summary.getpnlPct());
        model.put("totalPortfolioValue", summary.getTotalPortfolioValue().setScale(2, java.math.RoundingMode.HALF_UP));
        model.put("missingPrices", summary.isHasMissingPrices());
        model.put("pnlPositive", summary.getpnl().signum() >= 0);

        return new ModelAndView<>("portfolioEvaluation.hbs", model);
    }
}