package ru.etysoft.aurorauniverse.world;


import com.mysql.fabric.xmlrpc.base.Array;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Warning;

import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.etysoft.aurorauniverse.AuroraUniverse;
import ru.etysoft.aurorauniverse.Logger;
import ru.etysoft.aurorauniverse.data.Messages;
import ru.etysoft.aurorauniverse.data.Nations;
import ru.etysoft.aurorauniverse.data.Residents;
import ru.etysoft.aurorauniverse.data.Towns;
import ru.etysoft.aurorauniverse.economy.Bank;
import ru.etysoft.aurorauniverse.events.PreTownDeleteEvent;
import ru.etysoft.aurorauniverse.events.TownDeleteEvent;
import ru.etysoft.aurorauniverse.exceptions.RegionException;
import ru.etysoft.aurorauniverse.exceptions.TownException;
import ru.etysoft.aurorauniverse.permissions.AuroraPermissions;
import ru.etysoft.aurorauniverse.permissions.Group;
import ru.etysoft.aurorauniverse.placeholders.PlaceholderFormatter;
import ru.etysoft.aurorauniverse.utils.Messaging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Town {

    public String name;
    public Chunk homeblock;
    public Location townSpawnPoint;
    public float bank;
    public boolean pvp = false;

    private boolean fire = false;
    private double resTax = 0;
    private Resident mayor;
    private Map<Chunk, Region> townChunks = new ConcurrentHashMap<>();
    private ArrayList<Resident> residents = new ArrayList<>();
    private Chunk mainChunk = null;
    private Bank townBank;
    private String nationName;
    private ArrayList<Resident> invitedResidents = new ArrayList<>();

    // Permission type -> list of groups with permission
    private Set<String> buildGroups = new HashSet<String>();
    private Set<String> destroyGroups = new HashSet<String>();
    private Set<String> useGroups = new HashSet<String>();
    private Set<String> switchGroups = new HashSet<String>();

    public static final class JsonKeys {
        public static final String NAME = "NAME";
        public static final String MAYOR = "MAYOR";
        public static final String PVP = "PVP";
        public static final String FIRE = "FIRE";
        public static final String BANK = "BANK";
        public static final String RESIDENTS = "RESIDENTS";
        public static final String RESIDENT_TAX = "RES_TAX";
        public static final String NATION_NAME = "NATION_NAME";

        public static final String REGIONS = "REGIONS";
        public static final String X = "X";
        public static final String Z = "Z";
        public static final String WORLD = "WORLD";
        public static final String REGION = "REGION";

        public static final String SPAWN_X = "SPAWN_X";
        public static final String SPAWN_Y = "SPAWN_Y";
        public static final String SPAWN_YAW = "SPAWN_YAW";
        public static final String SPAWN_PITCH = "SPAWN_PITCH";
        public static final String SPAWN_Z = "SPAWN_Z";
        public static final String SPAWN_DIR_X = "SPAWN_DX";
        public static final String SPAWN_DIR_Y = "SPAWN_DY";
        public static final String SPAWN_DIR_Z = "SPAWN_DX";
        public static final String SPAWN_WORLD = "SPAWN_WORLD";


        public static final String MAIN_CHUNK = "MAIN_CHUNK";


        public static final String BUILD_GROUPS = "BUILD_GROUPS";
        public static final String DESTROY_GROUPS = "DESTROY_GROUPS";
        public static final String USE_GROUPS = "USE_GROUPS";
        public static final String SWITCH_GROUPS = "SWITCH_GROUPS";
    }

    @Warning
    public Town() {
    }

    public ArrayList<Resident> getInvitedResidents() {
        return invitedResidents;
    }

    public double getResTax() {
        return resTax;
    }

    public void setResTax(double resTax) {
        this.resTax = resTax;
    }

    public double getNewChunkPrice()
    {
        float defPrice = AuroraUniverse.getInstance().getConfig().getLong("town-chunk-price");
        double modifier = AuroraUniverse.getInstance().getConfig().getDouble("town-new-chunk-tax-mod");
        modifier =  Math.pow(modifier, getChunksCount() - 1);
        Logger.debug("Chunk mod: " + modifier);
        double finalPrice;
        finalPrice = Math.round(modifier * defPrice * 100);
        finalPrice = finalPrice/100;
        return finalPrice;
    }

    public void setMayor(Resident mayor) {
        this.mayor = mayor;
    }

    public void setNationName(String nationName) {
        this.nationName = nationName;
    }

    public static Town loadTownFromJSON(JSONObject jsonObject) throws TownException {


        /**
         * Firstly, initialize the town
         */

        String townName = (String) jsonObject.get(JsonKeys.NAME);
        double resTax = (double) jsonObject.get(JsonKeys.RESIDENT_TAX);
        String nation = (String) jsonObject.get(JsonKeys.NATION_NAME);
        Resident mayorResident = Resident.fromJSON((JSONObject) jsonObject.get(JsonKeys.MAYOR));
        Chunk mainChunk = getChunkFromInfo((JSONObject) jsonObject.get(JsonKeys.MAIN_CHUNK));

        Town town = Towns.loadTown(townName, mayorResident, mainChunk);
        Logger.debug("Loaded town " + townName);

        town.setResTax(resTax);
        town.setNationName(nation);

        try {

            /**
             * Secondary, initialize the town residents
             */

            JSONArray residentsArray = (JSONArray) jsonObject.get(JsonKeys.RESIDENTS);

            for (int i = 0; i < residentsArray.size(); i++) {
                JSONObject residentObj = (JSONObject) residentsArray.get(i);
                Resident resident = Resident.fromJSON(residentObj);
                if (resident != null) {
                    town.addResident(resident);
                    Residents.loadResident(resident);
                    Logger.debug(townName + " > Loaded resident " + resident.getName());
                }

            }

            /**
             * Now, load regions and chunks
             */

            JSONArray chunkInfos = (JSONArray) jsonObject.get(JsonKeys.REGIONS);

            for (int i = 0; i < chunkInfos.size(); i++) {
                JSONObject chunkInfo = (JSONObject) chunkInfos.get(i);

                Chunk chunk = getChunkFromInfo(chunkInfo);

                JSONObject regionObj = (JSONObject) chunkInfo.get(JsonKeys.REGION);

                Region region;

                if (regionObj.containsKey(ResidentRegion.JsonKeys.MEMBERS)) {
                    region = ResidentRegion.fromJSON(regionObj);
                } else {
                    region = Region.fromJSON(regionObj);
                }
                Logger.debug(townName + " > Loaded chunk " + chunk.getX() + ", " + chunk.getZ());
                town.townChunks.put(chunk, region);
            }
            AuroraUniverse.getTownBlocks().putAll(town.getTownChunks());

            String spawnWorld = (String) jsonObject.get(JsonKeys.SPAWN_WORLD);
            double spawnX = (double) jsonObject.get(JsonKeys.SPAWN_X);
            double spawnY = (double) jsonObject.get(JsonKeys.SPAWN_Y);
            double spawnZ = (double) jsonObject.get(JsonKeys.SPAWN_Z);

            double spawnDirX = (double) jsonObject.get(JsonKeys.SPAWN_DIR_X);
            double spawnYaw = (double) jsonObject.get(JsonKeys.SPAWN_YAW);
            double spawnPitch = (double) jsonObject.get(JsonKeys.SPAWN_PITCH);
            double spawnDirY = (double) jsonObject.get(JsonKeys.SPAWN_DIR_Y);
            double spawnDirZ = (double) jsonObject.get(JsonKeys.SPAWN_DIR_Z);

            Location spawnLocation = new Location(Bukkit.getWorld(spawnWorld), spawnX, spawnY, spawnZ);

            spawnLocation.setYaw(-Float.parseFloat(String.valueOf(spawnYaw)));



           // spawnLocation.setDirection(new Vector(spawnDirX,spawnDirY,spawnDirZ));

            spawnLocation.setPitch(Float.parseFloat(String.valueOf(spawnPitch)));

            town.setBuildGroups(getList((JSONArray) jsonObject.get(JsonKeys.BUILD_GROUPS)));
            town.setDestroyGroups(getList((JSONArray) jsonObject.get(JsonKeys.DESTROY_GROUPS)));
            town.setSwitchGroups(getList((JSONArray) jsonObject.get(JsonKeys.SWITCH_GROUPS)));
            town.setUseGroups(getList((JSONArray) jsonObject.get(JsonKeys.USE_GROUPS)));

            town.setSpawn(spawnLocation);
        }
        catch (Exception e)
        {
            Logger.error("Error loading town " + townName);
            e.printStackTrace();
        }

        return town;
    }

    private static JSONArray getJsonArrayFromList(Set<String> list)
    {
        JSONArray jsonArrayToApply = new JSONArray();
        for(String string : list)
        {
            jsonArrayToApply.add(string);
        }
        return jsonArrayToApply;
    }

    private static Set<String> getList(JSONArray jsonArray)
    {
        Set<String> arrayList = new HashSet<>();

        for(int i = 0; i < jsonArray.size(); i++)
        {
            String string = (String) jsonArray.get(i);
            arrayList.add(string);
        }
        return arrayList;
    }

    public String getNationName() {
        return nationName;
    }

    public Nation getNation()
    {
        return Nations.getNation(nationName);
    }

    private static Chunk getChunkFromInfo(JSONObject chunkInfo)
    {
        long x = (long) chunkInfo.get(JsonKeys.X);
        long z = (long) chunkInfo.get(JsonKeys.Z);
        String world = (String) chunkInfo.get(JsonKeys.WORLD);

        Location location = new Location(Bukkit.getWorld(world), x * 16, 0, z * 16);
        return Bukkit.getWorld(world).getChunkAt(location);
    }


    public void setBuildGroups(Set<String> buildGroups) {
        this.buildGroups = buildGroups;
    }

    public void setDestroyGroups(Set<String> destroyGroups) {
        this.destroyGroups = destroyGroups;
    }

    public void setUseGroups(Set<String> useGroups) {
        this.useGroups = useGroups;
    }

    public void setSwitchGroups(Set<String> switchGroups) {
        this.switchGroups = switchGroups;
    }


    public void sendMessage(String message)
    {
        for(Resident player : getResidents())
        {
            if(Bukkit.getPlayer(player.getName()) != null)
            {
                Bukkit.getPlayer(player.getName()).sendMessage(PlaceholderFormatter.process(message, Bukkit.getPlayer(player.getName())));
            }
        }
    }

    public JSONObject toJSON() {

        JSONObject townJsonObject = new JSONObject();
        townJsonObject.put(JsonKeys.NAME, name);
        townJsonObject.put(JsonKeys.MAYOR, mayor.toJson());
        townJsonObject.put(JsonKeys.PVP, pvp);
        townJsonObject.put(JsonKeys.FIRE, fire);

        townJsonObject.put(JsonKeys.SPAWN_X, townSpawnPoint.getX());
        townJsonObject.put(JsonKeys.SPAWN_Y, townSpawnPoint.getY());
        townJsonObject.put(JsonKeys.SPAWN_Z, townSpawnPoint.getZ());

        townJsonObject.put(JsonKeys.SPAWN_DIR_X, townSpawnPoint.getDirection().getX());
        townJsonObject.put(JsonKeys.SPAWN_YAW, townSpawnPoint.getYaw());
        townJsonObject.put(JsonKeys.SPAWN_PITCH, townSpawnPoint.getPitch());
        townJsonObject.put(JsonKeys.SPAWN_DIR_Y, townSpawnPoint.getDirection().getY());
        townJsonObject.put(JsonKeys.SPAWN_DIR_Z, townSpawnPoint.getDirection().getZ());
        townJsonObject.put(JsonKeys.SPAWN_WORLD, townSpawnPoint.getWorld().getName());
        townJsonObject.put(JsonKeys.RESIDENT_TAX, getResTax());
        townJsonObject.put(JsonKeys.NATION_NAME, nationName);

        townJsonObject.put(JsonKeys.BANK, townBank.getBalance());

        JSONArray jsonArrayResidents = new JSONArray();

        for (Resident resident : residents) {
            jsonArrayResidents.add(resident.toJson());
        }

        townJsonObject.put(JsonKeys.RESIDENTS, jsonArrayResidents);

        JSONArray regions = new JSONArray();

        for (Chunk chunk : townChunks.keySet()) {
            JSONObject chunkInfo = new JSONObject();

            Region region = townChunks.get(chunk);

            chunkInfo.put(JsonKeys.REGION, region.toJson());

            chunkInfo.put(JsonKeys.X, chunk.getX());
            chunkInfo.put(JsonKeys.Z, chunk.getZ());
            chunkInfo.put(JsonKeys.WORLD, chunk.getWorld().getName());

            regions.add(chunkInfo);

            if(mainChunk == chunk)
            {
                townJsonObject.put(JsonKeys.MAIN_CHUNK, chunkInfo);
            }
        }

        townJsonObject.put(JsonKeys.REGIONS, regions);

        townJsonObject.put(JsonKeys.USE_GROUPS, getJsonArrayFromList(useGroups));
        townJsonObject.put(JsonKeys.BUILD_GROUPS, getJsonArrayFromList(buildGroups));
        townJsonObject.put(JsonKeys.SWITCH_GROUPS, getJsonArrayFromList(switchGroups));
        townJsonObject.put(JsonKeys.DESTROY_GROUPS, getJsonArrayFromList(destroyGroups));

        return townJsonObject;
    }

    public Set<String> getDestroyGroups() {
        return destroyGroups;
    }

    public Set<String> getBuildGroups() {
        return buildGroups;
    }

    public Set<String> getUseGroups() {
        return useGroups;
    }

    public Set<String> getSwitchGroups() {
        return switchGroups;
    }

    public ResidentRegion getResidentRegion(Chunk chunk) {
        Region region = townChunks.get(chunk);
        if (region instanceof ResidentRegion) {
            return (ResidentRegion) region;
        } else {
            return null;
        }
    }

    public void createPlayerRegion(Chunk originalLocation, Resident resident) throws RegionException {
        if (residents.contains(resident)) {
            if (townChunks.containsKey(originalLocation)) {
                if (originalLocation != mainChunk) {
                    townChunks.remove(originalLocation);
                    townChunks.put(originalLocation, new ResidentRegion(this, resident));
                } else {
                    throw new RegionException("Unable to remove town's main chunk");
                }
            } else {
                throw new RegionException("Chunk don't belong to town");
            }

        } else {
            throw new RegionException("Resident don't belong to town");
        }
    }

    public void resetRegion(Chunk originalLocation) throws RegionException {

        if (hasChunk(originalLocation)) {
            if (originalLocation != mainChunk) {
                townChunks.remove(originalLocation);
                townChunks.put(originalLocation, new Region(this));
            } else {
                throw new RegionException("Unable to reset town's main chunk");
            }
        } else {
            throw new RegionException("Chunk don't belong to town");
        }


    }

    public ArrayList<Resident> getResidents() {
        return residents;
    }

    public void isFireAllowed(Boolean enableFireSpreading) {
        fire = enableFireSpreading;
    }

    public boolean isFireAllowed(Chunk chunk) {
        return fire;
    }

    public Bank getBank() {
        return townBank;
    }

    public void depositBank(double d) {
        townBank.deposit(d);
    }

    public boolean withdrawBank(double d) {
        return townBank.withdraw(d);
    }


    public Town(String name2, Resident mayor, Chunk homeblock) throws TownException {
        if (!mayor.hasTown()) {

            if (!AuroraUniverse.townList.containsKey(name2)) {
                if (!name2.equals("") && !name2.contains("\\")) {
                    if (!Towns.hasTown(homeblock)) {
                        name = name2;

                        this.mayor = mayor;
                        addResident(this.mayor);
                        this.mayor.setTown(name);
                        Group mayorPermissions = AuroraPermissions.getGroup("mayor");
                        toggleBuild(mayorPermissions);
                        toggleDestroy(mayorPermissions);
                        toggleUse(mayorPermissions);
                        toggleSwitch(mayorPermissions);
                        townChunks.put(homeblock, new Region(this));
                        mainChunk = homeblock;
                        townBank = new Bank("aun.town." + name, 0, mayor.getName());
                        AuroraUniverse.getInstance().getEconomy().addBank(townBank);
                    } else {
                        throw new TownException(AuroraUniverse.getLanguage().getString("e1"));
                    }

                } else {
                    throw new TownException(AuroraUniverse.getLanguage().getString("e2"));
                }
            } else {
                throw new TownException(AuroraUniverse.getLanguage().getString("e3").replace("%s", name2));
            }
        } else {
            throw new TownException(AuroraUniverse.getLanguage().getString("e4"));
        }
    }

    public Region getRegion(Location location) {
        if (hasChunk(location.getChunk())) {
            return townChunks.get(location.getChunk());
        } else {
            return null;
        }
    }

    public int getChunksCount() {
        return townChunks.size();
    }

    public String getMembersList() {
        String res = "";
        for (Resident resident : residents) {
            if (res.equals("")) {
                res = resident.getName();
            } else {
                res = res + ", " + resident.getName();
            }
        }
        return res;
    }

    public int getMembersCount() {
        return residents.size();
    }

    public Chunk getMainChunk() {
        return mainChunk;
    }

    public boolean getPvP(Resident resident, Chunk ch) {
        return pvp;
    }

    public boolean isMayor(Resident r) {
        if (mayor.getName().equals(r.getName())) {
            return true;
        } else {
            return false;
        }
    }

    public void setPvP(boolean pvp2) {
        pvp = pvp2;
    }

    public void addResident(Resident resident) {
        if (!resident.hasTown()) {
            residents.add(resident);
            resident.setTown(name);
        }

    }

    public String getName() {
        return name;
    }


    public void removeResident(Resident resident) {
        if (residents.contains(resident)) {
            if (!isMayor(resident)) {
                residents.remove(resident);
                resident.setPermissonGroup("newbies");
                resident.setTown(null);
            }

        }

    }

    public boolean toggleBuild(Group group) {
        if (buildGroups.contains(group.getName())) {
            buildGroups.remove(group.getName());
            Logger.debug("Group " + group.getName() + " now can't build in " + getName());
            return false;
        } else {
            buildGroups.add(group.getName());
            Logger.debug("Group " + group.getName() + " now can build in " + getName());
            return true;
        }
    }

    public boolean toggleDestroy(Group group) {
        if (destroyGroups.contains(group.getName())) {
            destroyGroups.remove(group.getName());
            Logger.debug("Group " + group.getName() + " now can't destroy in " + getName());
            return false;
        } else {
            destroyGroups.add(group.getName());
            Logger.debug("Group " + group.getName() + " now can destroy in " + getName());
            return true;
        }
    }

    public boolean toggleUse(Group group) {
        if (useGroups.contains(group.getName())) {
            useGroups.remove(group.getName());
            Logger.debug("Group " + group.getName() + " now can't use in " + getName());
            return false;
        } else {
            useGroups.add(group.getName());
            Logger.debug("Group " + group.getName() + " now can use in " + getName());
            return true;
        }
    }

    public boolean toggleSwitch(Group group) {
        if (switchGroups.contains(group.getName())) {
            switchGroups.remove(group.getName());
            Logger.debug("Group " + group.getName() + " now can't switch in " + getName());
            return false;
        } else {
            switchGroups.add(group.getName());
            Logger.debug("Group " + group.getName() + " now can switch in " + getName());
            return true;
        }
    }

    public boolean canSwitch(Resident resident, Chunk chunk) {
        if (!isResident(resident)) return false;
        ResidentRegion residentRegion = getResidentRegion(chunk);
        if (residentRegion != null && !residentRegion.canEdit(resident)) return false;
        return switchGroups.contains(resident.getPermissonGroupName());
    }

    public boolean canUse(Resident resident, Chunk chunk) {
        if (!isResident(resident)) return false;
        ResidentRegion residentRegion = getResidentRegion(chunk);
        if (residentRegion != null && !residentRegion.canEdit(resident)) return false;
        return useGroups.contains(resident.getPermissonGroupName());
    }

    public boolean canDestroy(Resident resident, Chunk chunk) {
        if (!isResident(resident)) return false;
        ResidentRegion residentRegion = getResidentRegion(chunk);
        if (residentRegion != null && !residentRegion.canEdit(resident)) return false;
        return destroyGroups.contains(resident.getPermissonGroupName());
    }

    public boolean canBuild(Resident resident, Chunk chunk) {
        if (!isResident(resident)) return false;
        ResidentRegion residentRegion = getResidentRegion(chunk);
        if (residentRegion != null && !residentRegion.canEdit(resident)) return false;
        return buildGroups.contains(resident.getPermissonGroupName());
    }

    public boolean isResident(Resident resident) {
        return residents.contains(resident);
    }

    public boolean isConnected(Chunk chunk, Player player) {
        AtomicBoolean connected = new AtomicBoolean(false);
        AuroraUniverse.alltownblocks.forEach((chunk1, region) -> {
            int m = AuroraUniverse.minTownBlockDistance;
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    if (chunk1.getX() + x == chunk.getX() && chunk1.getZ() + z == chunk.getZ()) {
                        if (region.getTown().getName().equals(name)) //если это наш чанк(приват рядом со своим городом
                        {
                            //Проверяем есть ли диагональ
                            if (chunk1.getX() == chunk.getX() + m && chunk1.getZ() == chunk.getZ() + m) //++
                            {
                            } else if (chunk1.getX() == chunk.getX() - m && chunk1.getZ() == chunk.getZ() + m) // -+
                            {
                            } else if (chunk1.getX() == chunk.getX() + m && chunk1.getZ() == chunk.getZ() - m) //+-
                            {
                            } else if (chunk1.getX() == chunk.getX() - m && chunk1.getZ() == chunk.getZ() - m) //--
                            {
                            } else {
                                connected.set(true);
                            }
                        } else {
                            Messaging.sendPrefixedMessage(Messages.claimTooClose(), player);
                        }
                    } else {
                        if (!region.getTown().getName().equals(name)) {
                            Messaging.sendPrefixedMessage(Messages.claimTooFar(), player);
                        }

                    }
                }
            }


            // chunk 2 is either adjacent to or has the same coordinates as chunk1


        });

        return connected.get();
    }

    public boolean delete() {
        PreTownDeleteEvent preTownDeleteEvent = new PreTownDeleteEvent(this);
        AuroraUniverse.callEvent(preTownDeleteEvent);

        if (!preTownDeleteEvent.isCancelled()) {
            for (Resident r :
                    residents) {
                r.setTown(null);
                r.setLastwild(true);
                r.setPermissonGroup("newbies");
            }

            if(hasNation())
            {
                getNation().delete();
            }

            AuroraUniverse.townList.remove(name);
            townChunks.forEach((chunk, region) -> {
                AuroraUniverse.alltownblocks.remove(chunk);
            });
            TownDeleteEvent townDeleteEvent = new TownDeleteEvent(this);
            AuroraUniverse.callEvent(townDeleteEvent);
            return true;
        } else {
            return false;
        }


    }

    public boolean hasNation()
    {
        return (nationName != null);
    }


    public boolean claimChunk(Chunk chunk) throws TownException {
        AtomicBoolean claimed = new AtomicBoolean(false);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicBoolean far = new AtomicBoolean(false);
        AuroraUniverse.alltownblocks.forEach((chunk1, region) -> {
            int m = AuroraUniverse.minTownBlockDistance;
            if (!hasChunk(chunk)) // есть ли чанк, который мы хотим заприватить в городе
            {
                //такого чанка нет


                //другой чанк города лежит не более чем в 1 блоке рядом
                for (int x = -1; x < 2; x++) {
                    for (int z = -1; z < 2; z++) {
                        if (chunk1.getX() + x == chunk.getX() && chunk1.getZ() + z == chunk.getZ()) {
                            if (region.getTown().getName().equals(name)) //если это наш чанк(приват рядом со своим городом
                            {
                                //Проверяем есть ли диагональ
                                if (chunk1.getX() == chunk.getX() + m && chunk1.getZ() == chunk.getZ() + m) //++
                                {

                                    //Diagonal
                                } else if (chunk1.getX() == chunk.getX() - m && chunk1.getZ() == chunk.getZ() + m) // -+
                                {

                                    //Diagonal
                                } else if (chunk1.getX() == chunk.getX() + m && chunk1.getZ() == chunk.getZ() - m) //+-
                                {

                                    //Diagonal
                                } else if (chunk1.getX() == chunk.getX() - m && chunk1.getZ() == chunk.getZ() - m) //--
                                {

                                    //Diagonal
                                } else {
                                    if (!hasChunk(chunk)) {
                                        success.set(true);

                                    } else {

                                    }
                                }


                            } else {

                                claimed.set(true);
                                // TODO: too close to another town
                            }
                        } else {
                            if (!region.getTown().getName().equals(name)) {
                                // far.set(true);
                            }
                            // TODO: too far from town
                        }
                    }
                }
            }


            // chunk 2 is either adjacent to or has the same coordinates as chunk1


        });
        if (success.get() && !claimed.get() && !far.get()) {
            townChunks.put(chunk, new Region(this));
            Logger.info("Claimed chunk: " + chunk + " for town " + name);
            AuroraUniverse.alltownblocks.put(chunk, new Region(this));
        } else {
            success.set(false);
        }
        return success.get();
    }

    public boolean unclaimChunk(Chunk chunk) {
        if (townChunks.containsKey(chunk) && mainChunk != chunk) {
            townChunks.remove(chunk);
            AuroraUniverse.alltownblocks.remove(chunk);
            return true;
        } else {
            return false;
        }
    }

    public void teleportToTownSpawn(Player pl) {
        pl.teleport(townSpawnPoint);
        Towns.ChangeChunk(pl, pl.getLocation().getChunk());
        //TODO: teleport message
    }

    public boolean hasChunk(Location lc) {
        if (townChunks.containsKey(lc.getChunk())) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasChunk(Chunk chunk) {
        return townChunks.containsKey(chunk);
    }

    public void setSpawn(Location location) throws TownException {
        if (hasChunk(location)) {
            townSpawnPoint = location;
            mainChunk = location.getChunk();
            // До лучших времён
//            Bukkit.getServer().getScheduler().runTaskTimer(AuroraUniverse.getInstance(), new Runnable() {
//                @Override
//                public void run() {
//                    Hohol hohol = new Hohol(location);
//                    WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();
//                    worldServer.addEntity(hohol);
//                    hohol.setPosition(location.getX(), location.getY(), location.getZ());
//                }
//            }, 20L, 1L);

        } else {
            throw new TownException(AuroraUniverse.getLanguage().getString("e5"));
        }


    }

    public double getTownTax()
    {
        long townChunkTax = AuroraUniverse.getInstance().getConfig().getLong("town-chunk-tax");


        return townChunkTax * getChunksCount();
    }

    public void rename(String newName)
    {
        AuroraUniverse.getTownList().remove(name);
        AuroraUniverse.getTownList().put(newName, this);
        name = newName;



        for(Resident resident : residents)
        {
            resident.setTown(name);
        }
    }


    public Map<Chunk, Region> getTownChunks() {
        return townChunks;
    }

    public Resident getMayor() {
        return mayor;
    }
}
