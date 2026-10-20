package uk.co.asepstrath.bank.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvestorAccountTests {

    private InvestorAccount investorAccount;

    @BeforeEach
    void setUp() {
        investorAccount = new InvestorAccount();
        // Without deposit, the logger always logs since there is no cash balance. The lots of red text annoyed me.
        // The tests still pass without this, so feel free to comment out/remove
        investorAccount.investorDepositForTests(10000.00);
    }

    @Test
    void testBuyStockAddsShares() {
        investorAccount.buyStock("AAPL", 10, 100.00);
        assertEquals(10, investorAccount.getShares("AAPL"));
    }

    @Test
    void testBuyStocksUpdateBookPrice() {
        investorAccount.buyStock("AAPL", 10, 100.00);
        assertEquals(100, investorAccount.getBookPrice("AAPL"));
    }

    @Test
    void testBuyStockCalculatesAverageBookPrice() {
        investorAccount.buyStock("AAPL", 10, 100.00);
        investorAccount.buyStock("AAPL", 10, 200.00);

        BigDecimal price = BigDecimal.valueOf(investorAccount.getBookPrice("AAPL"));

        assertEquals(0, price.compareTo(new BigDecimal("150.00")));
    }

    @Test
    void testBuyStockAddsTransactionsToHistory() {
        investorAccount.buyStock("AAPL", 10, 100.00);
        investorAccount.buyStock("AAPL", 10, 200.00);

        List<Transaction> history = investorAccount.getHistory();

        assertEquals(2, history.size());
        assertEquals(Transaction.TransactionType.BUY, history.get(1).getType());
    }

    @Test
    void testSellStockAddsTransactionToHistory() {
        investorAccount.buyStock("AAPL", 10, 100);
        investorAccount.sellStock("AAPL", 5, 120);

        List<Transaction> history = investorAccount.getHistory();

        assertEquals(2, history.size());
        assertEquals(Transaction.TransactionType.SELL, history.get(1).getType());
    }

    @Test
    void testBuyStockInvalidShares() {
        assertThrows(IllegalArgumentException.class, () ->
                investorAccount.buyStock("AAPL", 0, 100.00));
    }

    @Test
    void testBuyStockInvalidTicker() {
        assertThrows(IllegalArgumentException.class, () ->
            investorAccount.buyStock("aapl", 10, 100));
    }

    @Test
    void testBuyStockInvalidPrice() {
        assertThrows(IllegalArgumentException.class, () ->
            investorAccount.buyStock("AAPL", 10, -5));
    }

    @Test
    void testSellStockReducesShares() {
        investorAccount.buyStock("AAPL", 10, 100);
        investorAccount.sellStock("AAPL", 5, 120);

        assertEquals(5, investorAccount.getShares("AAPL"));
    }

    @Test
    void testSellStockAddsCashBalance() {
        investorAccount.buyStock("AAPL", 10, 100);
        investorAccount.sellStock("AAPL", 5, 200);
        // 10000 as that is deposited at start. Need to remove the 10000 here if you removed the deposit in BeforeEach
        assertEquals(10000, investorAccount.getCashBalance());
    }

    @Test
    void testSellStockRemovesTickerIfZeroShares() {
        investorAccount.buyStock("AAPL", 10, 100);
        investorAccount.sellStock("AAPL", 10, 100);

        assertEquals(0, investorAccount.getShares("AAPL"));
        assertEquals(0.0, investorAccount.getBookPrice("AAPL"));
    }

    @Test
    void testSellStockMoreThanOwned() {
        investorAccount.buyStock("AAPL", 5, 100);

        assertThrows(IllegalArgumentException.class, () ->
            investorAccount.sellStock("AAPL", 10, 100));
    }

    @Test
    void testSellStockInvalidShares() {
        assertThrows(IllegalArgumentException.class, () ->
            investorAccount.sellStock("AAPL", 0, 100));
    }

    @Test
    void testGetTotalInvested() {
        investorAccount.buyStock("AAPL", 10, 100);

        BigDecimal invested = investorAccount.getTotalInvested("AAPL");

        assertEquals(new BigDecimal("1000.00"), invested);
    }

    @Test
    void testSellStockTickerNotOwned() {
        assertThrows(IllegalArgumentException.class, () ->
                investorAccount.sellStock("AAPL", 5, 100));
    }

}
