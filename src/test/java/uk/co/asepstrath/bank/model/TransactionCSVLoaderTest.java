package uk.co.asepstrath.bank.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.mockito.Mockito.*;

class TransactionCSVLoaderTest {

    // Helper method to create temporary CSV files
    private File createTempCSV(String content) throws IOException {
        File file = File.createTempFile("testTransactions", ".csv");
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
        return file;
    }

    @Test
    void testValidBuyTransaction() throws IOException {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        String csv = """
timestamp,type,ticker,shares,price,investor_id
2025-02-03T08:00:00,BUY,AAPL,10,150.0,123
""";

        File file = createTempCSV(csv);

        TransactionCSVLoader.loadInvestors(account, file.getAbsolutePath());

        verify(account).buyStock("AAPL", 10, 150.0);
    }

    @Test
    void testValidSellTransaction() throws IOException {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        String csv = """
timestamp,type,ticker,shares,price,investor_id
2025-02-03T08:00:00,SELL,AAPL,5,200.0,123
""";

        File file = createTempCSV(csv);

        TransactionCSVLoader.loadInvestors(account, file.getAbsolutePath());

        verify(account).sellStock("AAPL", 5, 200.0);
    }

    @Test
    void testIgnoresWrongInvestorId() throws IOException {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        String csv = """
            timestamp,type,ticker,shares,price,investor_id
            2025-02-03T08:00:00,BUY,AAPL,10,150.0,999
        """;

        File file = createTempCSV(csv);

        TransactionCSVLoader.loadInvestors(account, file.getAbsolutePath());

        verify(account, never()).buyStock(any(), anyInt(), anyDouble());
    }

    @Test
    void testDepositIsIgnored() throws IOException {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        String csv = """
            timestamp,type,ticker,shares,price,investor_id
            2025-02-03T08:00:00,DEPOSIT,AAPL,10,150.0,123
        """;

        File file = createTempCSV(csv);

        TransactionCSVLoader.loadInvestors(account, file.getAbsolutePath());

        verify(account, never()).buyStock(any(), anyInt(), anyDouble());
    }

    @Test
    void testInvalidDateSkipped() throws IOException {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        String csv = """
            timestamp,type,ticker,shares,price,investor_id
            invalid-date,BUY,AAPL,10,150.0,123
        """;

        File file = createTempCSV(csv);

        TransactionCSVLoader.loadInvestors(account, file.getAbsolutePath());

        verify(account, never()).buyStock(any(), anyInt(), anyDouble());
    }

    @Test
    void testInvalidNumberSkipped() throws IOException {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        String csv = """
            timestamp,type,ticker,shares,price,investor_id
            2025-02-03T08:00:00,BUY,AAPL,abc,150.0,123
        """;

        File file = createTempCSV(csv);

        TransactionCSVLoader.loadInvestors(account, file.getAbsolutePath());

        verify(account, never()).buyStock(any(), anyInt(), anyDouble());
    }

    @Test
    void testUnknownTickerSkipped() throws IOException {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        String csv = """
            timestamp,type,ticker,shares,price,investor_id
            2025-02-03T08:00:00,BUY,UNKNOWN,10,150.0,123
        """;

        File file = createTempCSV(csv);

        TransactionCSVLoader.loadInvestors(account, file.getAbsolutePath());

        verify(account, never()).buyStock(any(), anyInt(), anyDouble());
    }

    @Test
    void testFileNotFoundDoesNotCrash() {
        InvestorAccount account = mock(InvestorAccount.class);
        when(account.getId()).thenReturn("123");

        TransactionCSVLoader.loadInvestors(account, "nonexistent.csv");

        verify(account, never()).buyStock(any(), anyInt(), anyDouble());
    }
}