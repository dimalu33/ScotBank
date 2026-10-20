package uk.co.asepstrath.bank;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AccountTests {

    @Test
    void createAccount(){
        Account a = new Account();
        Assertions.assertNotNull(a);
    }


}
