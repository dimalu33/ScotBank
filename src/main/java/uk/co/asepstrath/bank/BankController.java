package uk.co.asepstrath.bank;

import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

@Path("/")
public class BankController {

    private final DataSource dataSource;
    private final Logger logger;

    public BankController(DataSource ds, Logger log) {
        dataSource = ds;
        logger = log;
    }

    @GET
    public ModelAndView<Map<String, Object>> home() {
        List<Map<String, Object>> investors = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM Investor");
            while (rs.next()) {
                Map<String, Object> inv = new HashMap<>();
                inv.put("id", rs.getString("id"));
                inv.put("name", rs.getString("name"));
                inv.put("cashBalance", rs.getBigDecimal("cashBalance"));
                investors.add(inv);
            }
        } catch (SQLException e) {
            logger.error("DB Error", e);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("investors", investors);
        return new ModelAndView<>("home.hbs", model);
    }

    @POST("/deposit")
    public io.jooby.Context deposit(io.jooby.Context ctx) {
        String id = ctx.form("id").value();
        BigDecimal amount = new BigDecimal(ctx.form("amount").value());
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Investor SET cashBalance = cashBalance + ? WHERE id = ?");
            ps.setBigDecimal(1, amount);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("DB Error", e);
        }
        ctx.sendRedirect("/");
        return ctx;
    }

    @POST("/withdraw")
    public io.jooby.Context withdraw(io.jooby.Context ctx) {
        String id = ctx.form("id").value();
        BigDecimal amount = new BigDecimal(ctx.form("amount").value());
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Investor SET cashBalance = cashBalance - ? WHERE id = ?");
            ps.setBigDecimal(1, amount);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("DB Error", e);
        }
        ctx.sendRedirect("/");
        return ctx;
    }
}
