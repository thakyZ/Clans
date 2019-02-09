package the_fireplace.clans.commands.raiding;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;
import the_fireplace.clans.Clans;
import the_fireplace.clans.util.MinecraftColors;
import the_fireplace.clans.clan.Clan;
import the_fireplace.clans.clan.ClanCache;
import the_fireplace.clans.commands.RaidSubCommand;
import the_fireplace.clans.raid.Raid;
import the_fireplace.clans.raid.RaidingParties;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandCreateRaid extends RaidSubCommand {
	@Override
	public int getMinArgs() {
		return 2;
	}

	@Override
	public int getMaxArgs() {
		return 2;
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/raid create <name> <target clan>";
	}

	@Override
	public void run(@Nullable MinecraftServer server, EntityPlayerMP sender, String[] args) {
		Clan target = ClanCache.getClan(args[1]);
		if(target == null)
			sender.sendMessage(new TextComponentString(MinecraftColors.RED+"Target clan not found."));
		else {
			String name = args[0];
			if(!RaidingParties.getRaidingPlayers().contains(sender)) {
				if (!RaidingParties.getRaids().containsKey(name)) {
					if(!target.isShielded()) {
						if (target.getOnlineMembers(FMLCommonHandler.instance().getMinecraftServerInstance(), sender).size() > 0) {
							if (!RaidingParties.getRaidedClans().contains(target)) {
								long raidCost = Clans.cfg.startRaidCost;
								if (Clans.cfg.startRaidMultiplier)
									raidCost *= target.getClaimCount();
								if (Clans.getPaymentHandler().deductAmount(raidCost, sender.getUniqueID())) {
									new Raid(name, sender, target, raidCost);
									sender.sendMessage(new TextComponentString(MinecraftColors.GREEN + "Raiding party created!"));
								} else
									sender.sendMessage(new TextComponentString(MinecraftColors.RED + "Insufficient funds to form raiding party against " + target.getClanName() + ". It costs " + raidCost + ' ' + Clans.getPaymentHandler().getCurrencyName(raidCost)));
							} else
								sender.sendMessage(new TextComponentString(MinecraftColors.RED + "Target clan already has a party preparing to raid it or currently raiding it!"));
						} else
							sender.sendMessage(new TextComponentString(MinecraftColors.RED + "Target clan has no online members!"));
					} else
						sender.sendMessage(new TextComponentString(MinecraftColors.RED + "Target clan is currently shielded!"));
				} else
					sender.sendMessage(new TextComponentString(MinecraftColors.RED + "Raid name is already taken!"));
			} else
				sender.sendMessage(new TextComponentString(MinecraftColors.RED + "You are already in a raid!"));
		}
	}
}