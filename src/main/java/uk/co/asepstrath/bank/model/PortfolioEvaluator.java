package uk.co.asepstrath.bank.model;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

public class PortfolioEvaluator {

    private final InvestorAccount account;
    private final Map<String, BigDecimal> eodPrices;
    private static final Logger logger = Logger.getLogger(PortfolioEvaluator.class.getName());

    public PortfolioEvaluator(InvestorAccount account, Map<String, BigDecimal> eodPrices) {
        this.account   = account;
        this.eodPrices = eodPrices;
    }

    public PortfolioSummary evaluate() {
        List<PortfolioPosition> positions = new ArrayList<>();

        for (Map.Entry<String, Long> entry : account.getPortfolio().entrySet()) {
            String ticker          = entry.getKey();
            int shares             = entry.getValue().intValue();
            BigDecimal bookPrice   = BigDecimal.valueOf(account.getBookPrice(ticker));
            BigDecimal marketPrice = eodPrices.get(ticker);

            if (marketPrice == null) {
                logger.warning("No EoD price for '" + ticker + "' — showing book value only");
            }

            positions.add(new PortfolioPosition(ticker, shares, bookPrice, marketPrice));
        }

        positions.sort(Comparator.comparing(PortfolioPosition::getTicker));
        return new PortfolioSummary(BigDecimal.valueOf(account.getCashBalance()), positions);
    }
}