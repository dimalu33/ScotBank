package uk.co.asepstrath.bank.service;

import uk.co.asepstrath.bank.model.InvestorAccount;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;

public class PortfolioService {
    private final StockService stockService;

    public PortfolioService(StockService stockService) {
        this.stockService = stockService;
    }

    public Map<String, BigDecimal> getCategorizedData(InvestorAccount account, String type, DataSource dataSource) {
        Map<String, Long> sharesOwned = account.getPortfolio();

        stockService.prefetchMetadata(sharesOwned.keySet());

        Map<String, BigDecimal> groupedValues = new HashMap<>();
        BigDecimal totalPortfolioValue = BigDecimal.ZERO;

        // Calculate Market Value & Group (Excluding Cash automatically)
        for (Map.Entry<String, Long> entry : sharesOwned.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal shares = BigDecimal.valueOf(entry.getValue());

            BigDecimal currentPrice = stockService.getCurrentPrice(ticker, dataSource);
            BigDecimal marketValue = shares.multiply(currentPrice);

            totalPortfolioValue = totalPortfolioValue.add(marketValue);

            String category = getCategoryByType(ticker, type);
            groupedValues.put(category, groupedValues.getOrDefault(category, BigDecimal.ZERO).add(marketValue));
        }

        // Apply 5% Rule for "Other" grouping
        return applyFivePercentRule(groupedValues, totalPortfolioValue);
    }

    private String getCategoryByType(String ticker, String type) {
        var meta = stockService.getMetadata(ticker);
        return switch (type.toLowerCase()) {
            case "country" -> meta.getCountry();
            case "region" -> meta.getRegion();
            case "sector" -> meta.getSector();
            default -> "Unknown";
        };
    }

    private Map<String, BigDecimal> applyFivePercentRule(Map<String, BigDecimal> data, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) return data;

        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal otherTotal = BigDecimal.ZERO;
        BigDecimal threshold = total.multiply(BigDecimal.valueOf(0.05));

        for (var entry : data.entrySet()) {
            if (entry.getValue().compareTo(threshold) < 0) {
                otherTotal = otherTotal.add(entry.getValue());
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        if (otherTotal.signum() > 0) {
            result.put("Other", result.getOrDefault("Other", BigDecimal.ZERO).add(otherTotal));
        }
        return result;
    }
}