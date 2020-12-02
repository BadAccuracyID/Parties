package com.alessiodp.parties.bukkit.players.objects;

import java.util.List;
import java.util.UUID;

import com.alessiodp.core.common.commands.list.ADPCommand;
import com.alessiodp.core.common.scheduling.CancellableTask;
import com.alessiodp.core.common.user.User;
import com.alessiodp.parties.bukkit.addons.external.BanManagerHandler;
import com.alessiodp.parties.bukkit.addons.external.EssentialsHandler;
import com.alessiodp.parties.bukkit.commands.list.BukkitCommands;
import com.alessiodp.parties.bukkit.configuration.data.BukkitConfigMain;
import com.alessiodp.parties.bukkit.configuration.data.BukkitConfigParties;
import com.alessiodp.parties.bukkit.messaging.BukkitPartiesMessageDispatcher;
import com.alessiodp.parties.bukkit.utils.LastConfirmedCommand;
import com.alessiodp.parties.common.PartiesPlugin;
import com.alessiodp.parties.common.configuration.PartiesConstants;
import com.alessiodp.parties.common.configuration.data.ConfigParties;
import com.alessiodp.parties.common.players.objects.PartyRankImpl;
import com.alessiodp.parties.common.utils.PartiesPermission;
import com.alessiodp.parties.common.players.objects.PartyPlayerImpl;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;


import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public class BukkitPartyPlayerImpl extends PartyPlayerImpl {
	@Getter @Setter private CancellableTask homeTeleporting;
	@Getter @Setter private boolean portalPause = false;
	
	@Getter @Setter private LastConfirmedCommand lastConfirmedCommand;
	
	public BukkitPartyPlayerImpl(PartiesPlugin plugin, UUID uuid) {
		super(plugin, uuid);
	}
	
	@Override
	public void removeFromParty(boolean saveDB) {
		lock.lock();
		if (homeTeleporting != null) {
			homeTeleporting.cancel();
			homeTeleporting = null;
		}
		lock.unlock();
		super.removeFromParty(saveDB);
	}
	
	@Override
	public void playSound(String sound, double volume, double pitch) {
		User user = plugin.getPlayer(getPlayerUUID());
		if (user != null)
			user.playSound(sound, volume, pitch);
	}
	
	@Override
	public void playChatSound() {
		if (ConfigParties.GENERAL_SOUNDS_ON_CHAT_ENABLE) {
			User user = plugin.getPlayer(getPlayerUUID());
			if (user != null) {
				user.playSound(ConfigParties.GENERAL_SOUNDS_ON_CHAT_NAME, ConfigParties.GENERAL_SOUNDS_ON_CHAT_VOLUME, ConfigParties.GENERAL_SOUNDS_ON_CHAT_PITCH);
			}
		}
	}
	
	@Override
	public void playBroadcastSound() {
		if (ConfigParties.GENERAL_SOUNDS_ON_BROADCAST_ENABLE) {
			User user = plugin.getPlayer(getPlayerUUID());
			if (user != null)
				user.playSound(ConfigParties.GENERAL_SOUNDS_ON_BROADCAST_NAME, ConfigParties.GENERAL_SOUNDS_ON_BROADCAST_VOLUME, ConfigParties.GENERAL_SOUNDS_ON_BROADCAST_PITCH);
		}
	}
	
	public void playPacketSound(byte[] raw) {
		try {
			ByteArrayDataInput input = ByteStreams.newDataInput(raw);
			String sound = input.readUTF();
			double volume = input.readDouble();
			double pitch = input.readDouble();
			
			playSound(sound, volume, pitch);
		} catch (Exception ex) {
			plugin.getLoggerManager().printError(String.format(PartiesConstants.DEBUG_MESSAGING_LISTEN_PLAY_SOUND_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.toString()));
		}
	}
	
	@Override
	public List<ADPCommand> getAllowedCommands() {
		List<ADPCommand> ret = super.getAllowedCommands();
		PartyRankImpl rank = plugin.getRankManager().searchRankByLevel(getRank());
		User player = plugin.getPlayer(getPlayerUUID());
		
		if (isInParty()) {
			// Other commands
			if (BukkitConfigParties.ADDITIONAL_HOME_ENABLE) {
				if (player.hasPermission(PartiesPermission.ADMIN_HOME_OTHERS)
						|| (player.hasPermission(PartiesPermission.USER_HOME) && rank.havePermission(PartiesPermission.PRIVATE_HOME)))
					ret.add(BukkitCommands.HOME);
				if (player.hasPermission(PartiesPermission.USER_SETHOME) && rank.havePermission(PartiesPermission.PRIVATE_EDIT_HOME))
					ret.add(BukkitCommands.SETHOME);
			}
			if (BukkitConfigMain.ADDONS_GRIEFPREVENTION_ENABLE && player.hasPermission(PartiesPermission.USER_CLAIM) && rank.havePermission(PartiesPermission.PRIVATE_CLAIM))
				ret.add(BukkitCommands.CLAIM);
			
			// Admin commands
			if (BukkitConfigParties.ADDITIONAL_FRIENDLYFIRE_ENABLE
					&& BukkitConfigParties.ADDITIONAL_FRIENDLYFIRE_TYPE.equalsIgnoreCase("command")
					&& player.hasPermission(PartiesPermission.USER_PROTECTION)
					&& rank.havePermission(PartiesPermission.PRIVATE_EDIT_PROTECTION))
				ret.add(BukkitCommands.PROTECTION);
			
		}
		return ret;
	}
	
	@Override
	public boolean performPartyMessage(String message) {
		if (BukkitConfigMain.ADDONS_BANMANAGER_ENABLE
				&& BukkitConfigMain.ADDONS_BANMANAGER_PREVENTCHAT
				&& BanManagerHandler.isMuted(getPlayerUUID())) {
			return false;
		}
		
		return super.performPartyMessage(message);
	}
	
	@Override
	public void sendPacketUpdate() {
		if (plugin.isBungeeCordEnabled()) {
			((BukkitPartiesMessageDispatcher) plugin.getMessenger().getMessageDispatcher()).sendUpdatePlayer(this);
		}
	}
	
	@Override
	public boolean isVanished() {
		Player player = Bukkit.getPlayer(this.getPlayerUUID());
		if (player != null) {
			for (MetadataValue meta : player.getMetadata("vanished")) {
				if (meta.asBoolean()) return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean isChatMuted() {
		return EssentialsHandler.isPlayerMuted(getPlayerUUID());
	}
}
