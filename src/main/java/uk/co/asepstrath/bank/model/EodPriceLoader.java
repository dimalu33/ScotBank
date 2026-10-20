package uk.co.asepstrath.bank.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class EodPriceLoader {

    private static final Logger logger = LoggerFactory.getLogger(EodPriceLoader.class);

    public static Map<String, BigDecimal> load() {
        Map<String, BigDecimal> prices = new HashMap<>();

        try {
            InputStream input = EodPriceLoader.class.getResourceAsStream("/eod_prices.csv");

            if (input == null) {
                throw new RuntimeException("Could not find eod_prices.csv");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            reader.readLine(); // skip header row

            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");

                String ticker = tokens[0];
                BigDecimal price = new BigDecimal(tokens[1].trim());

                prices.put(ticker, price);
            }

        } catch (Exception e) {
            logger.error("Failed to load EoD prices csv", e);
        }

        return prices;
    }
}