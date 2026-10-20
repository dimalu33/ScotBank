package uk.co.asepstrath.bank.api;

import kong.unirest.core.*;
import kong.unirest.core.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class TransactionAPIToCSV {
    private static final Logger log = LoggerFactory.getLogger(TransactionAPIToCSV.class);
    private String CSV_PATH = "src/main/resources/transactions.csv";
    private static final String TEMP_CSV_PATH = "src/main/resources/transactions.csv" + ".tmp";
    private static final String BASE_URL = "https://api.asep-strath.co.uk/api";
    private final DataSource ds;
    private final Set<String> discoveredTickers = new HashSet<>();

    public TransactionAPIToCSV(DataSource ds) {
        this.ds = ds;
    }

    public void setCSVPath(String path) {
        this.CSV_PATH = path;
    }

    public HttpResponse<JsonNode> getAPIResponse(String url, String nextToken) {
        GetRequest request = Unirest.get(url);
        if (nextToken != null && !nextToken.isBlank()) {
            request.queryString("nextToken", nextToken);
        }
        return request.asJson();
    }

    public Set<String> discoverTickersFromAPI() throws IOException {
        log.info("Scanning API for unique tickers...");
        String nextToken = null;
        try {
            do {
                HttpResponse<JsonNode> response = getAPIResponse(BASE_URL + "/transactions", nextToken);

                if (response.getStatus() != 200) {
                    throw new IOException("Ticker discovery failed. API returned status: " + response.getStatus());
                }

                JSONObject json = response.getBody().getObject();
                JSONArray transactions = json.getJSONArray("transactions");

                for (int i = 0; i < transactions.length(); i++) {
                    String ticker = transactions.getJSONObject(i).optString("ticker", "UNKNOWN");
                    if (!ticker.equals("UNKNOWN") && !ticker.equalsIgnoreCase("CASH")) {
                        discoveredTickers.add(ticker);
                    }
                }

                JSONObject pag = json.optJSONObject("pagination");
                nextToken = (pag != null && pag.has("nextToken") && !pag.isNull("nextToken"))
                        ? pag.getString("nextToken") : null;

            } while (nextToken != null && !nextToken.isEmpty() && !nextToken.equals("null"));

        } catch (UnirestException e) {
            throw new IOException("Network error during ticker discovery", e);
        }

        return discoveredTickers;
    }

    public void writeTransactionsToCSV() throws IOException {
        File tempFile = new File(TEMP_CSV_PATH);
        log.info("Starting API sync to temporary file: {}", TEMP_CSV_PATH);

        // Write to a TEMP file first
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("timestamp,type,ticker,shares,price,investor_id");
            writer.newLine();

            String nextToken = null;
            do {
                HttpResponse<JsonNode> response = getAPIResponse(BASE_URL + "/transactions", nextToken);

                if (response.getStatus() != 200) {
                    throw new RemoteException("API Error " + response.getStatus());
                }

                JSONObject json = response.getBody().getObject();
                JSONArray transactions = json.getJSONArray("transactions");

                for (int i = 0; i < transactions.length(); i++) {
                    JSONObject txn = transactions.getJSONObject(i);
                    String type = txn.optString("type", "UNKNOWN").toUpperCase();
                    String ticker = txn.optString("ticker", "UNKNOWN");

                    String timestamp = txn.optString("timestamp", "2025-01-01T00:00:00");
                    double totalCash = txn.optDouble("totalCashAmount", 0.0);
                    String investorId = txn.optString("investorId", "UNKNOWN");

                    double price = fetchPriceAtTimestamp(ticker, timestamp);
                    double shares = (price > 0) ? (totalCash / price) : 0.0;

                    if (type.equals("DEPOSIT") || type.equals("WITHDRAW")) {
                        writer.write(String.format("%s,%s,%s,%.4f,%.2f,%s",
                                timestamp, type, "CASH", totalCash, 1.0, investorId));
                        writer.newLine();
                    } else { // buy and sell
                        writer.write(String.format("%s,%s,%s,%.4f,%.2f,%s",
                                timestamp, type, ticker, shares, price, investorId));
                        writer.newLine();
                    }

                }

                JSONObject pag = json.optJSONObject("pagination");
                nextToken = (pag != null && pag.has("nextToken") && !pag.isNull("nextToken"))
                        ? pag.getString("nextToken") : null;

            } while (nextToken != null && !nextToken.isEmpty() && !nextToken.equals("null"));

            // Close the writer manually to flush buffer before renaming
            writer.flush();
        } catch (UnirestException e) {
            throw new IOException("Network error during API sync to temporary file", e);
        }

        //Move temp file to final destination
        File finalFile = new File(CSV_PATH);
        try {
            Files.move(tempFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully updated transactions.csv. Atomic swap complete.");
        } catch (IOException e) {
            log.error("Failed to swap temp file to final CSV path!", e);
            throw e;
        }
    }

    private double fetchPriceAtTimestamp(String ticker, String timestamp) {
        if (ticker == null || ticker.equals("CASH")) return 1.0;

        String sql = "SELECT price " +
                     "FROM StockPrices " +
                     "WHERE ticker = ? AND timestamp <= ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ticker);
            stmt.setString(2, timestamp);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
            }
        } catch (SQLException e) {
            return 0.0;
        }
        return 0.0;
    }


}