package ru.etysoft.aurorauniverse.commands.town;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.etysoft.aurorauniverse.AuroraUniverse;
import ru.etysoft.aurorauniverse.data.Towns;
import ru.etysoft.aurorauniverse.exceptions.TownNotFoundedException;
import ru.etysoft.aurorauniverse.utils.AuroraLanguage;
import ru.etysoft.aurorauniverse.utils.ColorCodes;
import ru.etysoft.aurorauniverse.utils.Messaging;
import ru.etysoft.aurorauniverse.utils.Permissions;
import ru.etysoft.aurorauniverse.world.*;
import ru.etysoft.epcore.gui.GUITable;
import ru.etysoft.epcore.gui.Items;
import ru.etysoft.epcore.gui.Slot;
import ru.etysoft.epcore.gui.SlotRunnable;

import java.util.HashMap;

public class TownGuiCommand {

    private Player player;
    private Town town;

    public TownGuiCommand(Resident resident, Player pl, String[] args, CommandSender sender) {
        try {
            player = pl;
             town = resident.getTown();

            Chunk chunk = pl.getLocation().getChunk();

            HashMap<Integer, Slot> matrix = new HashMap<>();

            Slot townSlot = new Slot(new SlotRunnable() {
                @Override
                public void run() {

                }
            }, Items.createNamedItem(new ItemStack(Material.COBBLESTONE_WALL, 1), resident.getTown().getName(),
                    ColorCodes.toColor(AuroraUniverse.getLanguage().getString("gui.mayor").replace("%s", town.getMayor().getName())),
                    ColorCodes.toColor(AuroraUniverse.getLanguage().getString("gui.bank").replace("%s", String.valueOf(town.getBank().getBalance()))),
                    ColorCodes.toColor(AuroraUniverse.getLanguage().getString("gui.chunks").replace("%s", String.valueOf(town.getChunksCount()))
                            .replace("%m", String.valueOf(town.getMaxChunks())).replace("%b", String.valueOf(town.getBonusChunks()))).replace("%o", String.valueOf(town.getOutPosts().size()))
                            .replace("%p", String.valueOf(AuroraUniverse.getMaxOutposts()))
            ));



            if(town.getTownChunks().keySet().contains(ChunkPair.fromChunk(chunk)))
            {
                if(Permissions.canUnClaim(sender))
                {
                Slot unclaimSlot = new Slot(new SlotRunnable() {
                    @Override
                    public void run() {
                        new TownUnclaimCommand(resident, sender, pl);
                        pl.closeInventory();

                    }
                }, Items.createNamedItem(new ItemStack(Material.RED_STAINED_GLASS_PANE, 1), AuroraLanguage.getColorString("gui.unclaim")
                ));

                    matrix.put(46, unclaimSlot);
                }
            }
            else
            {

                if(Permissions.canClaim(sender))
                {
                    Slot claimSlot = new Slot(new SlotRunnable() {
                        @Override
                        public void run() {
                            new TownClaimCommand(resident, sender, pl);
                            pl.closeInventory();
                        }
                    }, Items.createNamedItem(new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1), AuroraLanguage.getColorString("gui.claim")
                            .replace("%s", String.valueOf(town.getNewChunkPrice()))
                    ));
                    matrix.put(46, claimSlot);
                }

            }


            Slot standingSlot;

            if (Permissions.canEditTowns(sender) | Permissions.canGetRegionInfo(sender)) {



                Town standingTown = Towns.getTown(((Player) sender).getLocation().getChunk());
                if (standingTown != null) {
                    if(standingTown == resident.getTown() | Permissions.canEditTowns(sender) ) {
                        Region region = standingTown.getRegion(((Player) sender).getLocation());

                        if (resident.getTown() == standingTown | Permissions.canEditTowns(sender)) {
                            if (region != null) {
                                if (region instanceof ResidentRegion) {
                                    ResidentRegion residentRegion = (ResidentRegion) region;

                                    String membersString = "";

                                    for (String nickname : residentRegion.getMembers()) {
                                        membersString += nickname + "; ";
                                    }
                                    standingSlot = new Slot(new SlotRunnable() {
                                        @Override
                                        public void run() {

                                        }
                                    }, Items.createNamedItem(new ItemStack(Material.PODZOL, 1), AuroraLanguage.getColorString("gui.standing"),
                                            AuroraLanguage.getColorString("gui.has-owner")
                                                    .replace("%s1", String.valueOf(residentRegion.getMembers().size()))
                                                    .replace("%s", residentRegion.getOwner().getName())
                                    ));
                                    matrix.put(54, standingSlot);
                                } else {
                                    String townBlock = AuroraLanguage.getColorString("gui.standing-town");
                                    if (standingTown.getMainChunk().equals(ChunkPair.fromChunk(((Player) sender).getLocation().getChunk()))) {
                                        townBlock = AuroraLanguage.getColorString("gui.standing-main-chunk");
                                    }
                                    standingSlot = new Slot(new SlotRunnable() {
                                        @Override
                                        public void run() {

                                        }
                                    }, Items.createNamedItem(new ItemStack(Material.GRASS_BLOCK, 1), AuroraLanguage.getColorString("gui.standing"),
                                            townBlock
                                    ));

                                    matrix.put(54, standingSlot);


                                }
                            }
                        }
                    }
                }


            }

            processToggles(matrix);

            matrix.put(23, townSlot);
            try {
                GUITable guiTable = new GUITable(resident.getTown().getName(), 6, matrix, AuroraUniverse.getInstance(), Material.GRAY_STAINED_GLASS_PANE, true);
                guiTable.open(pl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (TownNotFoundedException ignored){
            Messaging.sendPrefixedMessage(AuroraLanguage.getColorString("town-dont-belong"), sender);
        }
    }


    private void processToggles(HashMap<Integer, Slot> matrix)
    {
        String fireToggle = AuroraLanguage.getColorString("gui.state-off");

        if(town.isFire())
        {
             fireToggle = AuroraLanguage.getColorString("gui.state-on");
        }

        Slot fireToggleSlot = new Slot(new SlotRunnable() {
            @Override
            public void run() {
                String suffix = "on";
                if(town.isFire())
                {
                    suffix = "off";
                }
                player.performCommand("auntown toggle fire " + suffix);
                player.closeInventory();
            }
        }, Items.createNamedItem(new ItemStack(Material.CAMPFIRE, 1),  AuroraLanguage.getColorString("gui.toggle-fire").replace("%s", fireToggle)
        ));

        matrix.put(31, fireToggleSlot);

        String mobsToggle = AuroraLanguage.getColorString("gui.state-off");

        if(town.isMobs())
        {
            mobsToggle = AuroraLanguage.getColorString("gui.state-on");
        }

        Slot mobsToggleSlot = new Slot(new SlotRunnable() {
            @Override
            public void run() {
                String suffix = "on";
                if(town.isMobs())
                {
                    suffix = "off";
                }
                player.performCommand("auntown toggle mobs " + suffix);
                player.closeInventory();
            }
        }, Items.createNamedItem(new ItemStack(Material.ZOMBIE_HEAD, 1),  AuroraLanguage.getColorString("gui.toggle-mobs").replace("%s", mobsToggle)
        ));

        matrix.put(32, mobsToggleSlot);


        String pvpToggle = AuroraLanguage.getColorString("gui.state-off");

        if(town.isTownPvp())
        {
            pvpToggle = AuroraLanguage.getColorString("gui.state-on");
        }

        Slot pvpToggleSlot = new Slot(new SlotRunnable() {
            @Override
            public void run() {
                String suffix = "on";
                if(town.isTownPvp())
                {
                    suffix = "off";
                }
                player.performCommand("auntown toggle pvp " + suffix);
                player.closeInventory();
            }
        }, Items.createNamedItem(new ItemStack(Material.DIAMOND_SWORD, 1),  AuroraLanguage.getColorString("gui.toggle-pvp").replace("%s", pvpToggle)
        ));

        matrix.put(33, pvpToggleSlot);

        String expToggle = AuroraLanguage.getColorString("gui.state-off");

        if(town.isExplosionEnabled())
        {
            expToggle = AuroraLanguage.getColorString("gui.state-on");
        }

        Slot expToggleSlot = new Slot(new SlotRunnable() {
            @Override
            public void run() {
                String suffix = "on";
                if(town.isExplosionEnabled())
                {
                    suffix = "off";
                }
                player.performCommand("auntown toggle explosions " + suffix);
                player.closeInventory();
            }
        }, Items.createNamedItem(new ItemStack(Material.TNT, 1),  AuroraLanguage.getColorString("gui.toggle-exp").replace("%s", expToggle)
        ));

        matrix.put(41, expToggleSlot);
    }
}
