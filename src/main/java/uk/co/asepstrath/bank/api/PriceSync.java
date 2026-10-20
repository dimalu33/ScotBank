package uk.co.asepstrath.bank.api;

import kong.unirest.core.*;
import kong.unirest.core.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;

public class PriceSync {
    private static final Logger log = LoggerFactory.getLogger(PriceSync.class);
    private final DataSource ds;
    private static final String BASE_URL = "https://api.asep-strath.co.uk/api";

    public PriceSync(DataSource ds) {
        this.ds = ds;
    }

    public HttpResponse<JsonNode> getPriceResponse(String ticker, String nextToken) {
        GetRequest req = Unirest.get("https://api.asep-strath.co.uk/api/prices/" + ticker);
        if (nextToken != null) req.queryString("nextToken", nextToken);
        return req.asJson();
    }

    public void syncAllPrices(java.util.Set<String> tickers) {
        String sql = "MERGE INTO StockPrices (ticker, timestamp, price) KEY(ticker, timestamp) VALUES (?, ?, ?)";

        tickers.parallelStream().forEach(ticker -> {
            if (ticker == null || ticker.equalsIgnoreCase("CASH") || ticker.equalsIgnoreCase("null") || ticker.equalsIgnoreCase("UNKNOWN")) {
                return;
            }

            log.info("Fetching price history for: {}", ticker);

            // Each thread needs its own Connection and PreparedStatement
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                String nextToken = null;
                do {
                    HttpResponse<JsonNode> res = getPriceResponse(ticker, nextToken);
                    if (res.getStatus() != 200) {
                        log.warn("Failed to fetch {} - Status: {}", ticker, res.getStatus());
                        break;
                    }

                    JSONObject json = res.getBody().getObject();
                    JSONArray prices = json.optJSONArray("prices");

                    if (prices != null && prices.length() > 0) {
                        for (int i = 0; i < prices.length(); i++) {
                            JSONObject p = prices.getJSONObject(i);
                            stmt.setString(1, ticker);
                            stmt.setString(2, p.getString("timestamp"));
                            stmt.setDouble(3, p.getDouble("price"));
                            stmt.addBatch();
                        }
                        stmt.executeBatch(); // Saves the current page to the DB
                    }

                    JSONObject pag = json.optJSONObject("pagination");
                    nextToken = (pag != null && !pag.isNull("nextToken")) ? pag.getString("nextToken") : null;

                } while (nextToken != null && !nextToken.isEmpty());

            } catch (SQLException e) {
                log.error("Database error while syncing ticker: {}", ticker, e);
            } catch (Exception e) {
                log.error("General error while syncing ticker: {}", ticker, e);
            }
        });
    }
}