package uk.co.asepstrath.bank.model;

import uk.co.scotbank.InvestmentAccount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class InvestorAccount implements InvestmentAccount {

    private String id;
    private BigDecimal cashBalance;
    private Map<String, Long> portfolio = new HashMap<>();
    private List<Transaction> history = new ArrayList<>();
    private Map<String, BigDecimal> bookPrices = new HashMap<>();
    private static final Logger logger = Logger.getLogger(InvestorAccount.class.getName());
    private LocalDateTime manualTransactionTime;
    private Map<String, Deque<TaxLot>> taxLots = new HashMap<>();
    private record TaxLot(int shares, BigDecimal priceAtBuy) {}


    public InvestorAccount() {
        this.id = UUID.randomUUID().toString();
        this.cashBalance = BigDecimal.ZERO;
    }

    public InvestorAccount(String id) {
        this.id = id;
        this.cashBalance = BigDecimal.ZERO;
    }

    /*
    •	When a BUY transaction is imported, the system must record:
        o	Timestamp
        o	Equity ticker symbol (e.g., "AAPL", "MSFT")
        o	Number of shares (must be positive integer)
        o	Price per share at time of purchase
        o	Total cost (shares × price per share)
        o	Running position after transaction
     */
    @Override
    public void buyStock(String ticker, int shares, double sharePrice) {

        if (shares <= 0) {
            throw new IllegalArgumentException("You must buy at least 1 share");
        }

        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank");
        }

        if (!ticker.matches("[A-Z0-9.]{1,10}")) {
            throw new IllegalArgumentException("Invalid ticker format: " + ticker);
        }

        BigDecimal price = validatePrice(sharePrice);
        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(shares));

        // Log insufficient funds in data, but still process transaction
        if (cashBalance.compareTo(totalCost) < 0) {
            logger.warning("Insufficient funds at time of BUY for ticker: " + ticker);
        }

        cashBalance = cashBalance.subtract(totalCost);

        // Add to end of queue
        taxLots.computeIfAbsent(ticker, k -> new ArrayDeque<>()).addLast(new TaxLot(shares, price));

        // System must maintain current holdings for each equity ticker
        long currentShares = portfolio.getOrDefault(ticker, 0L);
        long newTotalShares = currentShares + shares;
        portfolio.put(ticker, newTotalShares);

        // Track average book price (average purchase price) per ticker
        // (existing total value + new purchase cost) / total shares after purchase
        BigDecimal oldAvg = bookPrices.getOrDefault(ticker, BigDecimal.ZERO);
        BigDecimal oldTotalValue = oldAvg.multiply(BigDecimal.valueOf(currentShares));
        BigDecimal newTotalValue = oldTotalValue.add(totalCost);
        BigDecimal newAvg = newTotalValue.divide(BigDecimal.valueOf(newTotalShares), 2, RoundingMode.HALF_UP);

        bookPrices.put(ticker, newAvg);

        Timestamp timestamp;
        if (this.manualTransactionTime != null) {
            timestamp = Timestamp.valueOf(this.manualTransactionTime);
        } else {
            timestamp = Timestamp.valueOf(LocalDateTime.now());
        }

        Transaction t = new Transaction(
                UUID.randomUUID().toString(),
                Transaction.TransactionType.BUY,
                timestamp,
                id,
                totalCost,
                ticker,
                shares
        );

        history.add(t);
    }


    /*
    •	When a SELL transaction is imported, the system must record:
        o	Timestamp
        o	Equity ticker symbol
        o	Number of shares (must be positive integer)
        o	Price per share at time of sale
        o	Total proceeds (shares × price per share)
        o	Running position after transaction
    •	Must validate sufficient shares existed to complete the sale
     */
    @Override
    public void sellStock(String ticker, int sharesToSell, double sharePrice) {
        if (sharesToSell <= 0) {
            throw new IllegalArgumentException("Must sell at least 1 share");
        }

        // A Deque (Double ended queue) is needed as we need to find the tax lots for a ticker in order (FIFO)
        Deque<TaxLot> lots = taxLots.get(ticker);
        int totalOwned = getShares(ticker);

        if (sharesToSell > totalOwned) {
            throw new IllegalArgumentException("Insufficient shares to complete sale");
        }

        BigDecimal sellPrice = validatePrice(sharePrice);
        BigDecimal totalProceeds = sellPrice.multiply(BigDecimal.valueOf(sharesToSell));
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        int remainingToProcess = sharesToSell;

        // FIFO LOGIC: Peeling off the oldest shares from the Deque
        while (remainingToProcess > 0 && lots != null && !lots.isEmpty()) {
            TaxLot oldestLot = lots.pollFirst();

            if (oldestLot.shares() <= remainingToProcess) {
                BigDecimal lotCost = oldestLot.priceAtBuy().multiply(BigDecimal.valueOf(oldestLot.shares()));
                totalCostBasis = totalCostBasis.add(lotCost);
                remainingToProcess -= oldestLot.shares();
            } else {
                BigDecimal partialCost = oldestLot.priceAtBuy().multiply(BigDecimal.valueOf(remainingToProcess));
                totalCostBasis = totalCostBasis.add(partialCost);

                int leftoverShares = oldestLot.shares() - remainingToProcess;
                lots.addFirst(new TaxLot(leftoverShares, oldestLot.priceAtBuy()));

                remainingToProcess = 0;
            }
        }

        BigDecimal gain = totalProceeds.subtract(totalCostBasis);

        this.cashBalance = cashBalance.add(totalProceeds);

        Timestamp timestamp = (this.manualTransactionTime != null) ?
                Timestamp.valueOf(this.manualTransactionTime) :
                Timestamp.valueOf(LocalDateTime.now());

        Transaction t = new Transaction(
                UUID.randomUUID().toString(),
                Transaction.TransactionType.SELL,
                timestamp,
                id,
                totalProceeds,
                ticker,
                sharesToSell
        );

        long currentShares = portfolio.getOrDefault(ticker, 0L);
        long newTotalShares = currentShares - sharesToSell;

        if (newTotalShares == 0) {
            portfolio.remove(ticker);
            bookPrices.remove(ticker);
        } else {
            portfolio.put(ticker, newTotalShares);
        }

        t.setCapitalGain(gain);
        history.add(t);

        if (lots == null || lots.isEmpty()) {
            taxLots.remove(ticker);
        }
    }

    // Track total shares owned per ticker
    @Override
    public int getShares(String ticker) {
        return Math.toIntExact(portfolio.getOrDefault(ticker, 0L));
    }

    @Override
    public double getBookPrice(String ticker) {
        BigDecimal price = bookPrices.getOrDefault(ticker, BigDecimal.ZERO);
        return price.doubleValue();
    }

    public Map<String, BigDecimal> getBookPrices() {
        return bookPrices;
    }

    public Map<String, BigDecimal> getTotalInvestedMap() {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (String ticker : portfolio.keySet()) {
            // Use getOrDefault to prevent crashes if a key was just removed
            totals.put(ticker, getTotalInvested(ticker));
        }
        return totals;
    }

    // **Not in interface
    private BigDecimal validatePrice(double sharePrice) {
        BigDecimal price = BigDecimal.valueOf(sharePrice);

        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        if (price.scale() > 2) {
            throw new IllegalArgumentException("Price cannot have more than 2 decimal places");
        }

        return price;
    }

    //**Not in interface
    // Calculate total invested amount per ticker
    public BigDecimal getTotalInvested(String ticker) {
        BigDecimal bookPrice = bookPrices.getOrDefault(ticker, BigDecimal.ZERO);
        long shares = portfolio.getOrDefault(ticker, 0L);
        return bookPrice.multiply(BigDecimal.valueOf(shares));
    }

    // System must calculate capital gains from SELL transactions
    // capital gain = total sale proceeds - cost basis
    public BigDecimal getCapitalGainForYear(int year) {
        BigDecimal total = BigDecimal.ZERO;

        for (Transaction t : history) {

            if (t.getType() == Transaction.TransactionType.SELL &&
                    t.getTime().toLocalDateTime().getYear() == year &&
                    t.getCapitalGain() != null) {

                total = total.add(t.getCapitalGain());
            }
        }
        return total;
    }

    public BigDecimal getTotalLosses(int year) {
        BigDecimal total = BigDecimal.ZERO;

        for (Transaction t : history) {
            if (t.getType() == Transaction.TransactionType.SELL &&
                    t.getTime().toLocalDateTime().getYear() == year &&
                    t.getCapitalGain() != null &&
                    t.getCapitalGain().compareTo(BigDecimal.ZERO) < 0) {

                total = total.add(t.getCapitalGain());
            }
        }
        return total;
    }

    // Requirement Capital Gains Tax Calculation
    public BigDecimal calculateCapitalGainsTax(int year) {
        BigDecimal netGains = getCapitalGainForYear(year);
        BigDecimal allowance = BigDecimal.valueOf(3000);
        BigDecimal taxable = netGains.subtract(allowance);

        if (taxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return taxable.multiply(BigDecimal.valueOf(0.24));
    }

    // GETTERS FOR UI REQUIREMENTS THAT DON'T ALREADY EXIST:
    /*
    1. Portfolio Overview
    •	List of all current positions (after processing all transactions)
    •	For each position show:
        o	Ticker symbol
        o	Number of shares owned
        o	Average book price per share
        o	Total invested amount
        o	Current value (calculate later in Investment Evaluation)
    2. Transaction History
    •	Sortable/filterable table showing all processed transactions
    •	Columns: Date, Type (BUY/SELL), Ticker, Shares, Price, Total
    •	Visual distinction between buys and sells
    •	Ability to filter by ticker symbol
    •	Chronological display
    3. Cash Balance Display
    •	Always visible on portfolio pages
    •	Updated to reflect all processed transactions
    */

    public double getCashBalance() {
        return cashBalance.doubleValue();
    }

    public List<Transaction> getHistory() {
        return new ArrayList<>(history);
    }

    public Map<String, Long> getPortfolio() {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : portfolio.entrySet()) {
            if (e.getValue() > 0) snapshot.put(e.getKey(), e.getValue());
        }
        return snapshot;
    }

    // helper method when reading time from csv
    public void setManualTransactionTime(LocalDateTime time) {
        this.manualTransactionTime = time;
    }

    // FOR TEST PURPOSES:
    public void investorDepositForTests(double amount) {
        cashBalance = cashBalance.add(BigDecimal.valueOf(amount));
    }

    public String getId() {
        return id;
    }
}
