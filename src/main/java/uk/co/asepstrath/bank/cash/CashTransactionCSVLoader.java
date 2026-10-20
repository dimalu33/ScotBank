package uk.co.asepstrath.bank.cash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.asepstrath.bank.model.Transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CashTransactionCSVLoader {

    private static final Logger logger = LoggerFactory.getLogger(CashTransactionCSVLoader.class);

    public static CashSummary loadCashSummary(String investorID) {
        try (InputStream input = CashTransactionCSVLoader.class.getResourceAsStream("/transactions.csv")) {
            if (input == null) {
                throw new RuntimeException("Could not find transactions.csv");
            }
            return process(investorID, new BufferedReader(new InputStreamReader(input)));
        } catch (Exception e) {
            logger.error("Failed to load cash transaction csv", e);
            throw new RuntimeException("Failed to load cash transaction csv", e);
        }
    }

    public static CashSummary process(String investorID, BufferedReader reader) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;

        reader.readLine(); // skip header
        String line;

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            if (tokens.length < 6) continue;

            // Filter by investor
            if (!tokens[5].equals(investorID)) continue;

            // Filter by cash transaction
            if (!tokens[2].equals("CASH")) continue;

            String timestampStr = tokens[0];
            String typeStr = tokens[1];
            BigDecimal amount = new BigDecimal(tokens[3]);

            LocalDateTime dateTime = LocalDateTime.parse(timestampStr);
            Timestamp timestamp = Timestamp.valueOf(dateTime);

            Transaction.TransactionType type;

            if (typeStr.equals("DEPOSIT")) {
                type = Transaction.TransactionType.DEPOSIT;
                balance = balance.add(amount);
                totalDeposits = totalDeposits.add(amount);
            } else if (typeStr.equals("WITHDRAWAL")) {
                type = Transaction.TransactionType.WITHDRAW;
                if (balance.compareTo(amount) < 0) {
                    throw new IllegalArgumentException("Withdrawal exceeds current balance");
                }
                balance = balance.subtract(amount);
                totalWithdrawals = totalWithdrawals.add(amount);
            } else {
                continue; // ignore buy/sell instead of crashing
            }

            transactions.add(new Transaction(UUID.randomUUID().toString(), type, timestamp, "cash-ui", amount));
        }
        return new CashSummary(balance, totalDeposits, totalWithdrawals, transactions);
    }

}
