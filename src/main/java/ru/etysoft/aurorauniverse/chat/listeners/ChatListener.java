package ru.etysoft.aurorauniverse.chat.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.etysoft.aurorauniverse.Logger;
import ru.etysoft.aurorauniverse.chat.AuroraChat;

public class ChatListener implements Listener {
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Logger.debug("Processing message " + event.getMessage());
        String message = event.getMessage();


        Player playerSender = event.getPlayer();

        if(!AuroraChat.processMessage(message, playerSender,  event.getRecipients()))
        {
            event.setCancelled(true);
        }
        event.getRecipients().clear();



    }


}
