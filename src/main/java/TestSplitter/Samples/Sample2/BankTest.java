package TestSplitter.Samples.Sample2;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BankTest {

    @Test
    public void testS0() {
        Bank bank = new Bank();
        bank.addCustomer("Oguz",0, 500);
        assertTrue(bank.accounts.get("Oguz").getAccountBalance() == 500);

        bank.getFees(100);
        assertTrue(bank.accounts.get("Oguz").getAccountBalance() == 400);

        bank.addInterest(10);
        assertTrue(bank.accounts.get("Oguz").getAccountBalance() == 440);

    }

}
