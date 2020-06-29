package ru.etysoft.aurorauniverse.world;

import ru.etysoft.aurorauniverse.AuroraUniverse;
import ru.etysoft.aurorauniverse.data.Towns;
import ru.etysoft.aurorauniverse.economy.Bank;

public class Resident {

    private String nickname;
    private boolean lastwild = true;
    private String lasttownname = null;

    private String townname = null;
    private Bank bank;


    public Resident(String name)
    {
        nickname = name;
        bank = new Bank(nickname,  AuroraUniverse.getPlugin(AuroraUniverse.class).getConfig().getDouble("start-balance"), nickname);
        AuroraUniverse.getInstance().getEconomy().addBank(bank);
    }

    public double getBalance()
    {
        return bank.getBalance();
    }

    public void setBalance(double d)
    {
        bank.setBalance(d);
    }

    public void giveBalance(double d) { bank.deposit(d); }


    public boolean takeBalance(double d)
    {
       return bank.withdraw(d);
    }

    public void setTown(String town)
    {
        townname = town;
    }

    public boolean hasTown()
    {
        if(townname != null)
        {
            return  true;
        }
        else
        {
            return  false;
        }
    }

    public String getName()
    {
        return nickname;
    }

    public boolean isLastWild() {
        return lastwild;
    }

    public void setLastwild(boolean lastwild) {
        this.lastwild = lastwild;
    }

    public Town getTown()
    {
      return Towns.getTown(townname);
    }

    public String getLastTown()
    {
        return lasttownname;
    }

    public void setLastTown(String name)
    {
      lasttownname = name;
    }
}
