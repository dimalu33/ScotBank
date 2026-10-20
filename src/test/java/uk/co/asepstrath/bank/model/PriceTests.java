package uk.co.asepstrath.bank.model;

import java.time.LocalDate;
import java.time.Month;
import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

class PriceTests {
    @Test
    void testPrice() {
        Price p = new Price("AMZ", LocalDate.of(2025, Month.MARCH, 4), new BigDecimal(13.53));

        Assertions.assertEquals("AMZ", p.getSymbol());
        Assertions.assertEquals(LocalDate.of(2025, Month.MARCH, 4), p.getDate());
        Assertions.assertEquals(0, p.getPrice().compareTo(new BigDecimal(13.53)));
    }
}

