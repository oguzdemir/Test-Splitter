package Samples.Sample2;

import java.util.HashMap;
import java.util.Map;

public class Bank {
    HashMap<String,BankAccount> accounts;

    public Bank() {
        accounts = new HashMap<>();
    }

    public void addCustomer(String owner, int id, int startMoney) {
        BankAccount account = new BankAccount();
        account.accountId = id;
        account.owner = owner;
        account.accountBalance = startMoney;
        accounts.put(owner, account);
    }

    public void getFees(int fee) {
        for(Map.Entry<String,BankAccount> entry : accounts.entrySet()) {
            entry.getValue().withdrawMoney(fee);
        }
    }

    public void addInterest(int ratio) {
        for(Map.Entry<String,BankAccount> entry : accounts.entrySet()) {
            BankAccount account = entry.getValue();
            int interest = (account.accountBalance / 100 ) * ratio;
            account.addMoney(interest);
        }
    }


}
