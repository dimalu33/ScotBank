package uk.co.asepstrath.bank.api;

import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.co.asepstrath.bank.model.*;

import io.jooby.annotation.*;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.service.PortfolioService;
import uk.co.asepstrath.bank.service.StockService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.sql.*;
import java.util.*;

@Path("/api")
public class ApiController {
    private final DataSource dataSource;
    private final Logger logger;
    private final PortfolioService portfolioService;
    private final StockService stockService;

    public ApiController(DataSource ds, Logger log) {
        dataSource = ds;
        logger = log;
        this.stockService = new StockService();
        this.portfolioService = new PortfolioService(stockService);
    }
    @GET("/investors")
    public InvestorList investors() {
        logger.info("Fetching investors from API");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet result = stmt.executeQuery("SELECT id, name, cashBalance FROM Investor")) {

            ArrayList<Investor> investors = new ArrayList<>();

            while (result.next()) {
                investors.add(new Investor(
                        result.getString("id"),
                        result.getString("name"),
                        result.getBigDecimal("cashBalance")
                ));
            }

            return new InvestorList(investors.toArray(new Investor[0]));

        } catch (SQLException e) {
        throw new IllegalStateException("Database Error Occurred while fetching investors", e);
        }
    }


    @GET("/investors/{id}/portfolio")
    public InvestorAccount getPortfolio(@PathParam String id) throws SQLException {

        Connection conn = dataSource.getConnection();

        String sql = "SELECT cashBalance FROM Investor WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);

            try (ResultSet result = stmt.executeQuery()) {

                if (!result.next()) {
                    throw new RuntimeException("Investor with id " + id + " not found");
                }

                double cash = result.getDouble("cashBalance");
                InvestorAccount account = new InvestorAccount(id);
                account.investorDepositForTests(cash);
                TransactionCSVLoader.loadInvestors(account, "src/main/resources/transactions.csv");
                return account;
            }
        }
    }

    @GET("/investors/{id}/positions")
    public Map<String, Long> getPositions(@PathParam String id) throws SQLException {
        InvestorAccount account = getPortfolio(id);
        return account.getPortfolio();
    }

    @GET("/investors/{id}/transactions")
    public List<Transaction> getTransactions(@PathParam String id) throws SQLException {
        InvestorAccount account = getPortfolio(id);
        return account.getHistory();
    }

    private static String placeholderChallengeToken = "kyc_challenge_abc123xyz";

    @GET("/kyc/questions")
    public Questionnaire questions() {
        logger.info("Responding to /api/kyc/questions");
        return new Questionnaire(placeholderChallengeToken);
    }

    @GET("/investors/{id}/charts/{category}")
    public Map<String, BigDecimal> getChartData(@PathParam String id, @PathParam String category) throws SQLException {
        logger.info("Generating chart data for investor: {} category: {}", id, category);

        InvestorAccount account = getPortfolio(id);

        return portfolioService.getCategorizedData(account, category, this.dataSource);
    }
    @GET("/stocks/metadata")
    public Map<String, CompanyMetaData> getAllMetadata() {
        Set<String> tickers = new HashSet<>();

        // Get all unique tickers we actually have prices for
        String sql = "SELECT DISTINCT ticker FROM StockPrices";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tickers.add(rs.getString("ticker"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        stockService.prefetchMetadata(tickers);

        Map<String, CompanyMetaData> metadataMap = new HashMap<>();
        for (String t : tickers) {
            metadataMap.put(t, stockService.getMetadata(t));
        }
        return metadataMap;
    }

    @GET("/prices/{ticker}")
    public Map<String, List<Map<String, Object>>> getPrices(@PathParam String ticker) throws SQLException {

        List<Map<String, Object>> pricesList = new java.util.ArrayList<>();

        String sql = "SELECT timestamp, price FROM StockPrices WHERE ticker = ? ORDER BY timestamp ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ticker);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new java.util.HashMap<>();
                    row.put("timestamp", rs.getString("timestamp"));
                    row.put("price", rs.getDouble("price"));
                    pricesList.add(row);
                }
            }
        }

        return Map.of("prices", pricesList);
    }

}
