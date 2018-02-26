package Sample2;

import Sample1.SingleLinkedList;
import Transformator.ObjectRecorder;
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
        assertTrue(account.owner.equals("SampleOwner") && account.accountId == 1001
                && account.accountBalance == 599);

        // test boundary -- default: each test assertion defines test boundary
        // another option: each public method invocation defines a test boundary
        // pre-state for next test -- save to file/read from file
        assertTrue(account.owner.equals("SampleOwner") && account.accountId == 1001
                && account.accountBalance == 499);
    }

    @Test
    public void testU0() { // unit test to create as output
        BankAccount account = new BankAccount();
        account.owner = "SampleOwner";
        account.accountBalance = 0;
        account.accountId = 1001;

        account.addMoney(599);
        assertTrue(account.owner.equals("SampleOwner") && account.accountId == 1001
                && account.accountBalance == 599);

    }

    @Test
    public void testU1() { // unit test to create as output
        BankAccount account = (BankAccount) ObjectRecorder.readObject(1, "BankAccount");
        account.withdrawMoney(100);
        assertTrue(account.owner.equals("SampleOwner") && account.accountId == 1001
                && account.accountBalance == 499);
    }
}
