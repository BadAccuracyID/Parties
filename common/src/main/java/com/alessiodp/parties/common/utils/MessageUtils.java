package com.alessiodp.parties.common.utils;

import com.alessiodp.core.common.user.OfflineUser;
import com.alessiodp.core.common.user.User;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.alessiodp.parties.common.PartiesPlugin;
import com.alessiodp.parties.common.configuration.PartiesConstants;
import com.alessiodp.parties.common.configuration.data.Messages;
import com.alessiodp.parties.common.parties.objects.PartyImpl;
import com.alessiodp.parties.common.players.objects.PartyPlayerImpl;
import com.alessiodp.parties.common.players.objects.RankImpl;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public abstract class MessageUtils {
	private final PartiesPlugin plugin;
	private final Pattern PLACEHOLDER_PATTERN = Pattern.compile("([%][^%]+[%])");
	private final Pattern PLACEHOLDER_PATTERN_LIST = Pattern.compile(PartiesConstants.PLACEHOLDER_PARTY_LIST, Pattern.CASE_INSENSITIVE);
	private final Pattern PLACEHOLDER_PATTERN_LIST_ONLINE = Pattern.compile(PartiesConstants.PLACEHOLDER_PARTY_LIST_ONLINE, Pattern.CASE_INSENSITIVE);
	private final Pattern PLACEHOLDER_PATTERN_LIST_NUMBER = Pattern.compile(PartiesConstants.PLACEHOLDER_PARTY_LIST_NUMBER, Pattern.CASE_INSENSITIVE);
	
	public String convertPartyPlaceholders(String message, PartyImpl party) {
		return convertPartyPlaceholders(message, party, "");
	}
	
	public String convertPartyPlaceholders(String message, PartyImpl party, String emptyPlaceholder) {
		String ret = message;
		String replacement;
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(ret);
		while(matcher.find()) {
			String identifier = matcher.group(0);
			switch (identifier.toLowerCase(Locale.ENGLISH)) {
				case PartiesConstants.PLACEHOLDER_PARTY_COLOR_CODE:
					replacement = party != null && party.getCurrentColor() != null ? party.getCurrentColor().getCode() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_COLOR_COMMAND:
					replacement = party != null && party.getCurrentColor() != null ? party.getCurrentColor().getCommand() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_COLOR_NAME:
					replacement = party != null && party.getCurrentColor() != null ? party.getCurrentColor().getName() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_DESC:
					replacement = party != null && !party.getDescription().isEmpty() ? party.getDescription() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_EXPERIENCE_TOTAL:
					replacement = party != null ? Integer.toString(Double.valueOf(party.getExperience()).intValue()) : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_EXPERIENCE_LEVEL:
					replacement = party != null ? Integer.toString(party.getLevel()) : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_EXPERIENCE_LEVELUP_CURRENT:
					replacement = party != null ? Integer.toString(party.getExpResult().getCurrentExperience()) : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_EXPERIENCE_LEVELUP_NECESSARY:
					replacement = party != null ? Integer.toString(party.getExpResult().getNecessaryExperience()) : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_KILLS:
					replacement = party != null ? Integer.toString(party.getKills()) : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_LEADER_NAME:
					PartyPlayerImpl leader = party != null ? plugin.getPlayerManager().getPlayer(party.getLeader()) : null;
					replacement = leader != null ? leader.getName() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_LEADER_UUID:
					replacement = party != null ? party.getLeader().toString() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_MOTD:
					replacement = party != null && !party.getMotd().isEmpty() ? party.getMotd() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_OUT:
					replacement = party == null ? Messages.PARTIES_OUT_PARTY : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_ONLINE:
					StringBuilder sb = new StringBuilder();
					replacement = emptyPlaceholder;
					if (party != null) {
						Set<PartyPlayer> list = party.getOnlineMembers(false);
						if (list.size() == 0)
							sb.append(Messages.PARTIES_LIST_EMPTY);
						else {
							for (PartyPlayer pp : list) {
								if (sb.length() > 0) {
									sb.append(Messages.PARTIES_LIST_SEPARATOR);
								}
								sb.append(plugin.getMessageUtils().convertAllPlaceholders(
										Messages.PARTIES_LIST_ONLINEFORMAT,
										party,
										(PartyPlayerImpl) pp));
							}
						}
						replacement = sb.toString();
					}
					ret = ret.replace(identifier, replacement);
					
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_ONLINE_NUMBER:
					replacement = party != null ? Integer.toString(party.getOnlineMembers(false).size()) : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PARTY_PARTY:
					replacement = party != null ? party.getName() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				default:
					// Nothing to do
					break;
				
			}
			
			// List rank
			Matcher matcherList = PLACEHOLDER_PATTERN_LIST.matcher(identifier);
			if (matcherList.find()) {
				String identifierList = matcherList.group(); // Get placeholder
				if (party == null) {
					ret = ret.replace(identifierList, emptyPlaceholder);
				} else {
					ArrayList<PartyPlayerImpl> members = getAllMembers(party, matcherList.group(1));
					StringBuilder list = new StringBuilder();
					for (PartyPlayerImpl pl : members) {
						if (list.length() > 0) {
							list.append(Messages.PARTIES_LIST_SEPARATOR);
						}
						OfflineUser offlinePlayer = plugin.getOfflinePlayer(pl.getPlayerUUID());
						if (offlinePlayer != null) {
							if (offlinePlayer.isOnline() && !pl.isVanished()) {
								list.append(
										plugin.getMessageUtils().convertAllPlaceholders(
												Messages.PARTIES_LIST_ONLINEFORMAT,
												party,
												pl)
								);
							} else {
								list.append(
										plugin.getMessageUtils().convertAllPlaceholders(
												Messages.PARTIES_LIST_OFFLINEFORMAT,
												party,
												pl)
								);
							}
						} else
							list.append(Messages.PARTIES_LIST_UNKNOWN);
					}
					ret = ret.replace(identifierList, list.toString().isEmpty() ? Messages.PARTIES_LIST_EMPTY : list.toString());
				}
			}
			// List rank online
			Matcher matcherListOnline = PLACEHOLDER_PATTERN_LIST_ONLINE.matcher(identifier);
			if (matcherListOnline.find()) {
				String identifierList = matcherListOnline.group(); // Get placeholder
				if (party == null) {
					ret = ret.replace(identifierList, emptyPlaceholder);
				} else {
					ArrayList<PartyPlayerImpl> members = getAllMembers(party, matcherListOnline.group(1));
					StringBuilder list = new StringBuilder();
					for (PartyPlayerImpl pl : members) {
						if (list.length() > 0) {
							list.append(Messages.PARTIES_LIST_SEPARATOR);
						}
						OfflineUser offlinePlayer = plugin.getOfflinePlayer(pl.getPlayerUUID());
						if (offlinePlayer != null) {
							if (offlinePlayer.isOnline() && !pl.isVanished()) {
								list.append(
										plugin.getMessageUtils().convertAllPlaceholders(
												Messages.PARTIES_LIST_ONLINEFORMAT,
												party,
												pl)
								);
							}
						} else
							list.append(Messages.PARTIES_LIST_UNKNOWN);
					}
					ret = ret.replace(identifierList, list.toString().isEmpty() ? Messages.PARTIES_LIST_EMPTY : list.toString());
				}
			}
			// List rank number
			Matcher matcherListNumber = PLACEHOLDER_PATTERN_LIST_NUMBER.matcher(identifier);
			if (matcherListNumber.find()) {
				String identifierList = matcherListNumber.group(); // Get placeholder
				if (party == null) {
					ret = ret.replace(identifierList, emptyPlaceholder);
				} else {
					ArrayList<PartyPlayerImpl> members = getAllMembers(party, matcherListNumber.group(1));
					ret = ret.replace(identifierList, Integer.toString(members.size()));
				}
			}
		}
		return ret;
	}
	
	public String convertPlayerPlaceholders(String message, PartyPlayerImpl player) {
		return convertPlayerPlaceholders(message, player, "");
	}
	
	public String convertPlayerPlaceholders(String message, PartyPlayerImpl player, String emptyPlaceholder) {
		String ret = message;
		String replacement;
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(ret);
		while(matcher.find()) {
			String identifier = matcher.group(1);
			switch (identifier.toLowerCase(Locale.ENGLISH)) {
				case PartiesConstants.PLACEHOLDER_PLAYER_NAME:
				case PartiesConstants.PLACEHOLDER_PLAYER_USER:
					replacement = player != null ? player.getName() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PLAYER_RANK_NAME:
					replacement = player != null && !player.getPartyName().isEmpty() ? plugin.getRankManager().searchRankByLevel(player.getRank()).getName() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				case PartiesConstants.PLACEHOLDER_PLAYER_RANK_CHAT:
					replacement = player != null && !player.getPartyName().isEmpty() ? plugin.getRankManager().searchRankByLevel(player.getRank()).getChat() : emptyPlaceholder;
					ret = ret.replace(identifier, replacement);
					break;
				default:
					// Nothing to do
					break;
			}
		}
		return ret;
	}
	
	public String convertAllPlaceholders(String message, PartyImpl party, PartyPlayerImpl player) {
		return convertPlayerPlaceholders(convertPartyPlaceholders(message, party), player);
	}
	
	public void sendMessage(User receiver, String message, PartyPlayerImpl victim, PartyImpl party) {
		if (receiver != null && message != null && !message.isEmpty()) {
			String formattedMessage = plugin.getMessageUtils().convertAllPlaceholders(message, party, victim);
			
			if (receiver.isPlayer())
				formattedMessage = plugin.getColorUtils().convertColors(formattedMessage);
			else
				formattedMessage = plugin.getColorUtils().removeColors(formattedMessage);
			
			receiver.sendMessage(formattedMessage, false);
		}
	}
	
	private ArrayList<PartyPlayerImpl> getAllMembers(PartyImpl party, String rankName) {
		ArrayList<PartyPlayerImpl> ret = new ArrayList<>();
		RankImpl rank = rankName != null ? plugin.getRankManager().searchRankByHardName(rankName) : null;
		for (UUID playerUUID : party.getMembers()) {
			PartyPlayerImpl pl = plugin.getPlayerManager().getPlayer(playerUUID);
			
			if (rank == null || rank.getLevel() == pl.getRank())
				ret.add(pl);
		}
		return ret;
	}
}
