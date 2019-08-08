package misat11.bw.special.listener;


import misat11.bw.Main;
import misat11.bw.api.APIUtils;
import misat11.bw.api.Game;
import misat11.bw.api.GameStatus;
import misat11.bw.api.events.BedwarsApplyPropertyToBoughtItem;
import misat11.bw.api.special.SpecialItem;
import misat11.bw.game.GamePlayer;
import misat11.bw.special.ProtectionWall;
import misat11.bw.utils.MiscUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

import static misat11.lib.lang.I18n.i18nonly;

public class ProtectionWallListener implements Listener {
	private static final String PROTECTION_WALL_PREFIX = "Module:ProtectionWall:";
	private boolean isUsable = true;
	private int delay;
	
	@EventHandler
	public void onProtectionWallRegistered(BedwarsApplyPropertyToBoughtItem event) {
		if (event.getPropertyName().equalsIgnoreCase("protectionwall")) {
			ItemStack stack = event.getStack();
			APIUtils.hashIntoInvisibleString(stack, applyProperty(event));
		}
		
	}

	@EventHandler
	public void onPlayerUseItem(PlayerInteractEvent event) {
		if (event.isCancelled() && event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		if (!Main.isPlayerInGame(event.getPlayer())) {
			return;
		}

		GamePlayer gPlayer = Main.getPlayerGameProfile(event.getPlayer());
		Game game = gPlayer.getGame();
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (game.getStatus() == GameStatus.RUNNING && !gPlayer.isSpectator) {
				if (event.getItem() != null) {
					ItemStack stack = event.getItem();
					String unhidden = APIUtils.unhashFromInvisibleStringStartsWith(stack, PROTECTION_WALL_PREFIX);
					if (unhidden != null) {
						event.setCancelled(true);

						boolean isBreakable = Boolean.parseBoolean(unhidden.split(":")[2]);
						int breakTime = Integer.parseInt(unhidden.split(":")[4]);
						int width = Integer.parseInt(unhidden.split(":")[5]);
						int height = Integer.parseInt(unhidden.split(":")[6]);
						int distance = Integer.parseInt(unhidden.split(":")[7]);
						Material material = MiscUtils.getMaterialFromString(unhidden.split(":")[8], "CUT_SANDSTONE");

						ProtectionWall protectionWall = new ProtectionWall(game, event.getPlayer(),
								game.getTeamOfPlayer(event.getPlayer()), stack);

						if (event.getPlayer().getEyeLocation().getBlock().getType() != Material.AIR) {
							MiscUtils.sendActionBarMessage(event.getPlayer(), i18nonly("specials_protection_wall_not_usable_here"));
							return;
						}

						if (isUsable) {
							protectionWall.createWall(isBreakable, breakTime, width, height, distance, material);
							delay = Integer.parseInt(unhidden.split(":")[3]);
							runCountdown();
						} else {
							MiscUtils.sendActionBarMessage(event.getPlayer(), i18nonly("special_item_delay").replace("%time%", String.valueOf(delay)));
						}


					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();

		if (event.isCancelled()) {
			return;
		}

		if (!Main.isPlayerInGame(player)) {
			return;
		}

		GamePlayer gPlayer = Main.getPlayerGameProfile(player);
		Game game = gPlayer.getGame();

		if (gPlayer.isSpectator) {
			return;
		}

		Block block = event.getBlock();
		for (ProtectionWall checkedWall : getCreatedWalls(game, player)) {
			if (checkedWall != null) {
				for (Block wallBlock : checkedWall.getWallBlocks()) {
					if (wallBlock.equals(block) && !checkedWall.canBreak()) {
						event.setCancelled(true);
					}

				}
			}
		}
	}

	private ArrayList<ProtectionWall> getCreatedWalls(Game game, Player player) {
		ArrayList<ProtectionWall> createdWalls = new ArrayList<>();
		for (SpecialItem specialItem : game.getActivedSpecialItemsOfPlayer(player)) {
			if (specialItem instanceof ProtectionWall) {
				ProtectionWall wall = (ProtectionWall) specialItem;
				createdWalls.add(wall);
			}
		}
		return createdWalls;
	}

	private void runCountdown() {
		if (delay > 0) {
			isUsable = false;
			new BukkitRunnable() {

				@Override
				public void run() {
					delay--;
					if (delay == 0) {
						isUsable = true;
						this.cancel();
					}
				}
			}.runTaskTimer(Main.getInstance(), 20L, 20L);
		}
	}

	private String applyProperty(BedwarsApplyPropertyToBoughtItem event) {
		return PROTECTION_WALL_PREFIX
				+ MiscUtils.getBooleanFromProperty(
						"is-breakable", "specials.protection-wall.is-breakable", event) + ":"
				+ MiscUtils.getIntFromProperty(
				"delay", "specials.protection-wall.delay", event) + ":"
				+ MiscUtils.getIntFromProperty(
				"break-time", "specials.protection-wall.break-time", event) + ":"
				+ MiscUtils.getIntFromProperty(
				"width", "specials.protection-wall.width", event) + ":"
				+ MiscUtils.getIntFromProperty(
				"height", "specials.protection-wall.height", event) + ":"
				+ MiscUtils.getIntFromProperty(
				"distance", "specials.protection-wall.distance", event) + ":"
				+ MiscUtils.getMaterialFromProperty(
				"material", "specials.protection-wall.material", event);
	}

}
