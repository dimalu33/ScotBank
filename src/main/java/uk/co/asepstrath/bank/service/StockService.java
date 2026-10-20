package uk.co.asepstrath.bank.service;

import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.asepstrath.bank.model.CompanyMetaData;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class StockService {

    private static final String BASE_URL = "https://api.asep-strath.co.uk/api";
    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private final Map<String, CompanyMetaData> registry = new HashMap<>();

    public StockService() {}

    public void prefetchMetadata(Set<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return;

        String tickerParams = String.join(",", tickers);

        try {
            var response = Unirest.get(BASE_URL + "/equities/metadata")
                    .queryString("tickers", tickerParams)
                    .asJson();

            if (response.getStatus() == 200) {

                JSONObject body = response.getBody().getObject();

                if (body.has("assets")) {
                    JSONArray assets = body.getJSONArray("assets");

                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String ticker = asset.getString("ticker");

                        registry.put(ticker, new CompanyMetaData(
                                asset.optString("country", "Unknown"),
                                asset.optString("region", "Unknown"),
                                asset.optString("sector", "Unknown")
                        ));
                    }
                    log.info("Successfully cached metadata for {} tickers", assets.length());
                } else {
                    log.warn("API response did not contain 'assets' key: {}", body);
                }
            } else {
                log.error("Metadata API error: {} - {}", response.getStatus(), response.getStatusText());
            }
        } catch (Exception e) {
            log.error("Failed to prefetch metadata for: {}", tickerParams, e);
        }
    }

    public CompanyMetaData getMetadata(String ticker) {
        return registry.getOrDefault(ticker, new CompanyMetaData(null, null, null));
    }

    public BigDecimal getCurrentPrice(String ticker, DataSource ds) {
        String sql = "SELECT price FROM StockPrices WHERE ticker = ? ORDER BY timestamp DESC LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticker);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("price");
            }
        } catch (SQLException e) {
            log.error("DB error for ticker {}", ticker, e);
        }
        return BigDecimal.ONE; // Fallback
    }


}