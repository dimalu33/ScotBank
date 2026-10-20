package uk.co.asepstrath.bank.model;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("java:S1700")
public class Price {

    private String symbol;
    private LocalDate date;
    private BigDecimal price;

    public Price(String symbol, LocalDate date, BigDecimal price) {
        this.symbol = symbol;
        this.date = date;
        this.price = price;
    }

    public String getSymbol() { return symbol; }

    public LocalDate getDate() { return date; }

    public BigDecimal getPrice() { return price; }

}
