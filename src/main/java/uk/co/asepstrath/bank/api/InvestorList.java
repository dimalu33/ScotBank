package uk.co.asepstrath.bank.api;
import uk.co.asepstrath.bank.model.Investor;

/*
This is the response type of /api/investors .
 */
public class InvestorList {
    Investor[] investors;

    public InvestorList() {}

    public InvestorList(Investor[] list) {
        this.investors = list;
    }

    public Investor[] getInvestors() {
        return investors;
    }
}

