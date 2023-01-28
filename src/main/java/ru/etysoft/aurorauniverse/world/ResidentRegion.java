package ru.etysoft.aurorauniverse.world;

import org.bukkit.Bukkit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.etysoft.aurorauniverse.Logger;
import ru.etysoft.aurorauniverse.data.Residents;
import ru.etysoft.aurorauniverse.data.Towns;
import ru.etysoft.aurorauniverse.exceptions.TownNotFoundedException;
import ru.etysoft.aurorauniverse.utils.Permissions;

import java.util.ArrayList;

public class ResidentRegion extends Region {

    private Resident owner;
    private ArrayList<String> members = new ArrayList<>();
    private boolean isPvp;

    public ResidentRegion(Town town, Resident owner) {
        super(town);
        this.owner = owner;
        members.add(owner.getName());
        isPvp = false;
    }

    public void setPvp(boolean pvp) {
        isPvp = pvp;
    }

    public boolean isPvp() {
        return isPvp;
    }

    public boolean addMember(Resident resident)
    {
        String nickname = resident.getName();
        if(members.contains(nickname))
        {
            return  false;
        }
        else
        {
            members.add(nickname);
            return true;
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject regionObj = super.toJson();
        regionObj.put(JsonKeys.OWNER, owner.getName());
        JSONArray jsonArray = new JSONArray();
        for(String member : members)
        {
            jsonArray.add(member);
        }
        regionObj.put(JsonKeys.MEMBERS, jsonArray);
        regionObj.put(JsonKeys.PVP, isPvp);

        return regionObj;
    }

    public static ResidentRegion fromJSON(JSONObject regionObj) throws TownNotFoundedException {
        String ownerName = (String) regionObj.get(JsonKeys.OWNER);
        Resident ownerResident = Residents.getResident(ownerName);

        if(ownerResident != null) {
            ResidentRegion residentRegion = new ResidentRegion(Towns.getTown((String) regionObj.get(Region.JsonKeys.TOWN_NAME)), ownerResident);

            JSONArray membersJsonArray = (JSONArray) regionObj.get(JsonKeys.MEMBERS);

            for (int i = 0; i < membersJsonArray.size(); i++) {
                String memberName = (String) membersJsonArray.get(i);


                residentRegion.members.add(memberName);

            }
            try
            {
                boolean isPvp = (boolean) regionObj.get(JsonKeys.PVP);
                residentRegion.setPvp(isPvp);
            }
            catch (Exception e)
            {
                Logger.warning("Cannot read property PvP of region of " + residentRegion.getOwner().getName());
            }

            return residentRegion;

        }
        else
        {
            Logger.warning("ResidentRegion: Owner with name " + ownerName + " not found");
            return null;
        }

    }

    public static class JsonKeys
    {
        public static final String OWNER = "OWNER";
        public static final String MEMBERS = "MEMBERS";
        public static final String PVP = "PVP";

    }

    public boolean removeMember(Resident resident)
    {
        String nickname = resident.getName();
        if(members.contains(nickname))
        {
            members.remove(nickname);
            return true;
        }
        else
        {
            return false;
        }
    }

    public ArrayList<String> getMembers() {
        for(String memberName : new ArrayList<>(members))
        {
            if(Residents.getResident(memberName) == null)
            {
                members.remove(memberName);
            }
        }
        return members;
    }

    public boolean canEdit(Resident resident)
    {
        if(getTown().isResident(resident))
        {
            try {
                if (Permissions.canBypassRegion(Bukkit.getPlayer(resident.getName()))) {
                    return true;
                }
            }
            catch (Exception e)
            {
                Logger.error("Error bypass permission check!");
                return false;
            }
           return members.contains(resident.getName());
        }
        else
        {
            members.remove(resident.getName());
            return false;
        }
    }

    public Resident getOwner() {
        return owner;
    }
}
