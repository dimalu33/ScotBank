package uk.co.asepstrath.bank.cash;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CashTransactionCSVLoaderTest {

    @Test
    void testSuccessfulProcessing() throws Exception {
        String csvData = "timestamp,type,ticker,shares,price,investor_id\n" +
                "2026-03-18T10:00:00,DEPOSIT,CASH,100.00,1.0,user1\n" +
                "2026-03-18T11:00:00,WITHDRAWAL,CASH,40.00,1.0,user1";

        BufferedReader reader = new BufferedReader(new StringReader(csvData));
        CashSummary summary = CashTransactionCSVLoader.process("user1", reader);

        assertEquals(0, new BigDecimal("60.00").compareTo(summary.getBalance()));
        assertEquals(2, summary.getTransactions().size());
    }

    @Test
    void testFilteringLogic() throws Exception {
        String csvData = "timestamp,type,ticker,shares,price,investor_id\n" +
                "2026-03-18T10:00:00,DEPOSIT,STOCK,100.00,1.0,user1\n" + // Wrong type
                "2026-03-18T11:00:00,DEPOSIT,CASH,50.00,1.0,user2";     // Wrong user

        BufferedReader reader = new BufferedReader(new StringReader(csvData));
        CashSummary summary = CashTransactionCSVLoader.process("user1", reader);

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getBalance()));
        assertTrue(summary.getTransactions().isEmpty());
    }

    @Test
    void testInsufficientFundsThrowsException() {
        String csvData = "timestamp,type,ticker,shares,price,investor_id\n" +
                "2026-03-18T10:00:00,DEPOSIT,CASH,10.00,1.0,user1\n" +
                "2026-03-18T11:00:00,WITHDRAWAL,CASH,100.00,1.0,user1";

        BufferedReader reader = new BufferedReader(new StringReader(csvData));

        assertThrows(IllegalArgumentException.class, () -> {
            CashTransactionCSVLoader.process("user1", reader);
        });
    }
}