package ru.etysoft.aurorauniverse.economy;

import ru.etysoft.aurorauniverse.Logger;
import ru.etysoft.aurorauniverse.utils.Numbers;

public class Bank {

    private final String name;
    private double amount;
    private final String owner;
    private boolean isPlayerAccount;

    public Bank(String accountName, double startBalance, String player)
    {
        name = accountName;
        owner = player;
        amount = startBalance;
        isPlayerAccount = false;
        if(player != null) {
            if (player.equals(name)) {
                isPlayerAccount = true;
            }
        }

    }

    public String getOwner()
    {
        return owner;
    }

    public boolean isPlayerAccount()
    {
        return isPlayerAccount;
    }

    public boolean hasAmount(double money)
    {
            return amount >= money;
    }
    public double getBalance()
    {
        return Numbers.round(amount);
    }

    public void setBalance(double d)
    {
        amount = d;
        Logger.log("Balance of  " + getName() + " set to " + getBalance());
    }

    public String getName()
    {
        return name;
    }

    public void deposit(double money)
    {
        if(Double.isNaN(money)) return;
        if(money <= 0) return;
        Logger.log("Deposited  " + money + " to " + getName());
        amount += money;
    }

    public boolean withdraw(double money)
    {

        if(Double.isNaN(money)) return false;
        if(money == 0)
        {
            return true;
        }
        if(money > amount | money <= 0)
        {
            return false;
        }
        else
        {
            amount -= money;
            Logger.log("Withdrawn  " + money + " from " + getName());
            return true;
        }

    }

    public boolean withdraw(double money, Bank toBank)
    {
        if(money == 0)
        {
            return true;
        }
        if(money > amount | money <= 0)
        {
            return false;
        }
        else
        {
            amount -= money;
            toBank.deposit(money);
            Logger.log("Withdrawn  " + money + " from " + getName() + " to "+ toBank.getName());
            return true;
        }

    }

}
