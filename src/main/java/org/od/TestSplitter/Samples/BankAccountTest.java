package org.od.TestSplitter.Samples;

import org.od.TestSplitter.Transformator.ObjectRecorder;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by od on 25.02.2018.
 */
public class BankAccountTest {

    @Test
    public void testS0() { // system test given as input
        BankAccount account = new BankAccount();
        account.owner = "SampleOwner";
        account.accountBalance = 0;
        account.accountId = 1001;

        account.addMoney(599);
        account.addMoney(100);
        account.withdrawMoney(100);
        assertTrue(account.owner.equals("SampleOwner") && account.accountId == 1001
                && account.accountBalance == 599);

        account.withdrawMoney(100);
        account.withdrawMoney(100);
        assertTrue(account.owner.equals("SampleOwner") && account.accountId == 1001
                && account.accountBalance == 399);
    }

}
