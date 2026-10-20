package uk.co.asepstrath.bank.cash;

import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import uk.co.asepstrath.bank.model.Transaction;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Path("/cash/{id}")
public class CashUiController {

   @GET
   public ModelAndView<Map<String, Object>> page(@PathParam String id) {
       CashSummary summary = CashTransactionCSVLoader.loadCashSummary(id);

       Map<String, Object> model = new HashMap<>();
       model.put("balance", String.format("%.2f", summary.getBalance()));
       model.put("totalDeposits", String.format("%.2f", summary.getTotalDeposits()));
       model.put("totalWithdrawals", String.format("%.2f", summary.getTotalWithdrawals()));

       List<Map<String, Object>> recentTransactions = new ArrayList<>();
       BigDecimal runningBalance = BigDecimal.ZERO;

       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

       for (Transaction transaction : summary.getTransactions()) {
           if (transaction.getType() == Transaction.TransactionType.DEPOSIT) {
               runningBalance = runningBalance.add(transaction.getCashAmount());
           } else {
               runningBalance = runningBalance.subtract(transaction.getCashAmount());
           }

       Map<String, Object> row = new HashMap<>();

       String formattedDate = transaction.getTime()
                       .toInstant()
                       .atZone(ZoneId.systemDefault())
                       .toLocalDateTime()
                       .format(formatter);

        boolean isDeposit = transaction.getType() == Transaction.TransactionType.DEPOSIT;

               row.put("date", formattedDate);
               row.put("type", isDeposit ? "DEPOSIT" : "WITHDRAWAL");
               row.put("amount", (isDeposit ? "+£" : "-£") + String.format("%.2f", transaction.getCashAmount()));
               row.put("balanceAfter", "£" + String.format("%.2f", runningBalance));
               row.put("rowClass", isDeposit ? "bank-row-deposit" : "bank-row-withdrawal");
               row.put("badgeClass", isDeposit ? "text-bg-success" : "text-bg-danger");

      recentTransactions.add(row);
   }

       model.put("recentTransactions", recentTransactions);

       return new ModelAndView<>("cash.hbs", model);

   }
}