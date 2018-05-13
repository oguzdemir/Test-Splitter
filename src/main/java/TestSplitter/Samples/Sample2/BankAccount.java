package TestSplitter.Samples.Sample2;

/**
 * Created by od on 25.02.2018.
 */
public class BankAccount {
    public int accountId;
    public String owner;
    public int accountBalance;


    public void addMoney(int amount) {
        accountBalance += amount;
    }

    public void withdrawMoney(int amount) {
        accountBalance -= amount;
    }

    public int getAccountBalance() {
        return accountBalance;
    }

}
