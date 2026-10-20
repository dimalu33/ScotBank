package uk.co.asepstrath.bank.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TransactionCSVLoader {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCSVLoader.class);

    public static void loadInvestors(InvestorAccount account, String path) {
        String targetId = account.getId();
        List<String[]> allRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(path))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 6 && tokens[5].equals(targetId)) {
                    allRows.add(tokens);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("Transaction file not found");
            return;
        } catch (IOException e) {
            logger.error("Error reading transaction file");
            return;
        }

            allRows.sort(Comparator.comparing(row -> row[0]));

            for (String[] tokens : allRows) {
                try {
                    String type = tokens[1];

                    if (type.equalsIgnoreCase("DEPOSIT") ||
                        type.equalsIgnoreCase("WITHDRAWAL")) {
                        continue;
                    }

                    String timestampStr = tokens[0];
                    String ticker = tokens[2];
                    double rawShares = Double.parseDouble(tokens[3]);
                    double price = Double.parseDouble(tokens[4]);

                    account.setManualTransactionTime(LocalDateTime.parse(timestampStr));
                    int shares = (int) Math.round(rawShares); // Required by interface to have int shares

                    if (shares <= 0 || ticker.equals("UNKNOWN")) continue;

                    if (type.equals("BUY")) {
                        account.buyStock(ticker, shares, price);
                    } else if (type.equals("SELL")) {
                        account.sellStock(ticker, shares, price);
                    }

                } catch (DateTimeParseException e) {
                    logger.warn("Skipping invalid date in token: '{}'", tokens[0]);
                } catch (NumberFormatException e) {
                    logger.warn("Skipping numeric data token: '{}' or '{}'", tokens[3], tokens[4]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    logger.warn("Missing columns in CSV");
                } catch (Exception e) {
                    logger.warn("Unexpected error occurred");
                }
            }
    }


}
