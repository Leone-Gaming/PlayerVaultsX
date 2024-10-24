package com.drtshock.playervaults.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.vaultmanagement.VaultManager;
import com.drtshock.playervaults.vaultmanagement.VaultOperations;
import com.drtshock.playervaults.vaultmanagement.VaultViewInfo;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@CommandAlias("playervaults|pv|vault|chest")
@CommandPermission("playervaults.commands.use")
public class PlayerVaultsCommand extends BaseCommand {

    @Dependency
    private PlayerVaults plugin;

    @Default
    @CommandCompletion("@vaults_with_aliases @players")
    public void vault(Player player, @Default("1") String vault, @Optional OfflinePlayer target) {
        if (VaultOperations.isLocked()) {
            this.plugin.getTL().locked().title().send(player);
            return;
        }

        if (!player.hasPermission("playervaults.admin") && target != null) {
            player.sendMessage("§cError: You don't have permission to view players' vaults!");
            return;
        }

        if (PlayerVaults.getInstance().getInVault().containsKey(player.getUniqueId().toString())) {
            // don't let them open another vault.
            return;
        }

        if (target != null) {
            if (!player.hasPermission("playervaults.admin")) {
                this.plugin.getTL().noPerms().title().send(player);
            }

            int number;
            try {
                number = Integer.parseInt(vault);
            } catch (NumberFormatException e) {
                this.plugin.getTL().mustBeNumber().title().send(player);
                return;
            }

            if (VaultOperations.openOtherVault(player, target.getUniqueId().toString(), vault)) {
                PlayerVaults.getInstance().getInVault().put(player.getUniqueId().toString(), new VaultViewInfo(target.getUniqueId().toString(), number));
            } else {
                this.plugin.getTL().noOwnerFound().title().with("player", vault).send(player);
            }

            return;
        }

        if (VaultOperations.openOwnVault(player, vault, true)) {
            int id;

            if (VaultManager.getInstance().getVaultAliases().containsKey(player.getUniqueId().toString())) {
                Map<String, Integer> aliases = VaultManager.getInstance().getVaultAliases().get(player.getUniqueId().toString());

                // No vaults found with that alias, parse it as an integer
                if (!aliases.containsKey(vault)) {
                    id = Integer.parseInt(vault);
                } else {
                    id = aliases.get(vault);
                }
            } else {
                id = Integer.parseInt(vault);
            }

            PlayerVaults.getInstance().getInVault().put(player.getUniqueId().toString(), new VaultViewInfo(player.getUniqueId().toString(), id));
        }
    }

    @Subcommand("rename")
    @CommandCompletion("@vaults @none")
    public void rename(Player player, int number, @Optional String name) {
        if (!VaultOperations.checkPerms(player, number) || number < 1) {
            player.sendMessage("§cError: You don't have access to that vault!");
            return;
        }

        if (name == null) {
            Map<String, Integer> aliases = VaultManager.getInstance().getVaultAliases().get(player.getUniqueId().toString());

            if (aliases == null) {
                aliases = new HashMap<>();
            }

            aliases.entrySet().removeIf(entry -> entry.getValue() == number);
            player.sendMessage("§aRemoved all aliases for that vault!");
            return;
        }

        if (name.equalsIgnoreCase("rename")) {
            player.sendMessage("§cError: You cannot rename your vault to that!");
            return;
        }

        Map<String, Integer> aliases = VaultManager.getInstance().getVaultAliases().get(player.getUniqueId().toString());

        if (aliases == null) {
            aliases = new HashMap<>();
        }

        aliases.put(name, number);
        VaultManager.getInstance().getVaultAliases().put(player.getUniqueId().toString(), aliases);

        player.sendMessage("§aYou've renamed your vault #" + number + " to \"" + name + "\"!");
    }

}
