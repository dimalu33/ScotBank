package uk.co.asepstrath.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.annotation.GET;
import io.jooby.jackson.JacksonModule;
import io.jooby.Cookie;
import io.jooby.ServerOptions;
import io.jooby.SessionStore;
import io.jooby.netty.NettyServer;
import io.jooby.Jooby;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.helper.UniRestExtension;
import io.jooby.hikari.HikariModule;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.api.ApiController_;
import uk.co.asepstrath.bank.api.InvestorList;
import uk.co.asepstrath.bank.api.PriceSync;
import uk.co.asepstrath.bank.api.TransactionAPIToCSV;
import uk.co.asepstrath.bank.cash.CashUiController_;
import uk.co.asepstrath.bank.model.Investor;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

public class App extends Jooby {

    public App(){
        /*
        This section is used for setting up the Jooby Framework modules
         */
        install(new UniRestExtension());
        install(new HandlebarsModule());
        install(new HikariModule("mem"));
        install(new JacksonModule());

        setSessionStore(SessionStore.memory(new Cookie("scotbank").setHttpOnly(true)));

        /*
        This will host any files in src/main/resources/assets on <host>/assets
        For example in the dice template (dice.hbs) it references "assets/dice.png" which is in resources/assets folder
         */
        assets("/assets/*", "/assets");
        assets("/service_worker.js","/service_worker.js");

        assets("/*", "/public");

        /*
        Now we set up our controllers and their dependencies
         */
        DataSource ds = require(DataSource.class);
        Logger log = getLog();

        mvc(new ApiController_(ds,log));

        Account account = new Account();
        mvc(new CashUiController_());
        mvc(new HomeController_());
        mvc(new PortfolioController_(ds,log));

        /*
        Finally we register our application lifecycle methods
         */
        onStarted(this::onStart);
        onStop(this::onStop);

    }

    public static void main(final String[] args) {
        runApp(args, new NettyServer(new ServerOptions()), App::new);
    }

    /*
    This function will be called when the application starts up,
    it should be used to ensure that the DB is properly setup
     */
    public void onStart() {
        Logger log = getLog();
        log.info("Starting Up...");
        DataSource ds = require(DataSource.class);
        try (Connection conn = ds.getConnection()) {
            createTables(conn);
            importInvestors(conn);

            try {
                // The flow here is:
                // -Find all tickers
                // -Get prices for those tickers
                // -Write transaction with correct prices to csv
                TransactionAPIToCSV toCSV = new TransactionAPIToCSV(ds);
                PriceSync pSync = new PriceSync(ds);

                log.info("Discovering tickers from transactions...");
                Set<String> tickers = toCSV.discoverTickersFromAPI();

                pSync.syncAllPrices(tickers);

                log.info("Writing final transaction CSV with updated prices");
                toCSV.writeTransactionsToCSV();

                log.info("CSV Transaction data successfully updated.");
            } catch (IOException e) {
                log.error("Could not update CSV data from API", e);
            } catch (Exception e) {
                log.error("Unexpected error during price sync", e);
            }

        } catch (SQLException e) {
            log.error("Database Creation Error", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE Investor (" +
                            "id VARCHAR(255) PRIMARY KEY, " +
                            "name VARCHAR(255), " +
                            "cashBalance DECIMAL(32,18)" +
                            ")"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS StockPrices (" +
                            "ticker VARCHAR(10) NOT NULL, " +
                            "timestamp TIMESTAMP NOT NULL, " +
                            "price DECIMAL(32,18), " +
                            "PRIMARY KEY (ticker, timestamp))"
            );
        }
    }

    private void importInvestors(Connection conn) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.asep-strath.co.uk/api/investors"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        ObjectMapper mapper = new ObjectMapper();

        try {
            InvestorList investorList =
                    mapper.readValue(response.body(), InvestorList.class);

            String sql = "MERGE INTO Investor (id, name, cashBalance) KEY(id) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (Investor inv : investorList.getInvestors()) {

                    stmt.setString(1, inv.getId());
                    stmt.setString(2, inv.getName());
                    stmt.setBigDecimal(3, inv.getCashBalance());

                    stmt.executeUpdate();
                }
            }
        } catch (JsonProcessingException e) {
            getLog().error("Error parsing Investor JSON", e);
            throw e;
        } catch (SQLException e) {
            getLog().error("Database error during investors import ", e);
        }

    }

    /*
    This function will be called when the application shuts down
     */
    public void onStop() {
        Logger log = getLog();
        log.info("Shutting Down...");
    }

}
