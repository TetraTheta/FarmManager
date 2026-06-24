package io.github.tetratheta.farmmanager.listener;

import io.github.tetratheta.farmmanager.FarmManager;
import io.github.tetratheta.farmmanager.config.FMConfig;
import io.github.tetratheta.farmmanager.crop.CropDescriptor;
import io.github.tetratheta.farmmanager.crop.CropRegistry;
import io.github.tetratheta.farmmanager.region.RegionService;
import io.github.tetratheta.farmmanager.service.HarvestService;
import io.github.tetratheta.farmmanager.service.NotificationService;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/// Coordinates crop break protection and automatic harvest behavior.
public final class CropBreakListener implements Listener {
  private final FMConfig config;
  private final CropRegistry cropRegistry;
  private final HarvestService harvestService;
  private final NotificationService notificationService;
  private final FarmManager plugin;
  private final RegionService regionService;
  private final Consumer<Runnable> taskRunner;

  /// Creates the crop break listener.
  ///
  /// @param plugin              plugin entry point
  /// @param config              active configuration
  /// @param cropRegistry        active crop registry
  /// @param regionService       active region service
  /// @param harvestService      harvest service
  /// @param notificationService gameplay notification service
  /// @param taskRunner          runtime-owned task runner
  public CropBreakListener(
    FarmManager plugin, FMConfig config, CropRegistry cropRegistry, RegionService regionService,
    HarvestService harvestService, NotificationService notificationService, Consumer<Runnable> taskRunner
  ) {
    this.plugin = plugin;
    this.config = config;
    this.cropRegistry = cropRegistry;
    this.regionService = regionService;
    this.harvestService = harvestService;
    this.notificationService = notificationService;
    this.taskRunner = taskRunner;
  }

  /// Handles protected and automatic crop breaks after other protection plugins have acted.
  ///
  /// @param event block break event
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    Optional<CropDescriptor> descriptor = cropRegistry.get(block.getType());
    if (descriptor.isEmpty()) return;
    if (!regionService.isWatched(block.getLocation())) return;
    if (!(block.getBlockData() instanceof Ageable ageable)) return;
    Player player = event.getPlayer();
    boolean creative = player.getGameMode() == GameMode.CREATIVE;
    boolean mature = ageable.getAge() >= ageable.getMaximumAge();
    if (!mature && shouldProtectImmature(player, creative)) {
      event.setCancelled(true);
      notificationService.send(player, "notification.immature-crop");
      return;
    }
    if (!mature || !shouldHarvest(creative)) return;
    ItemStack tool = player.getInventory().getItemInMainHand();
    Collection<ItemStack> drops = block.getDrops(tool, player);
    Location location = block.getLocation();
    CropDescriptor cropDescriptor = descriptor.get();
    event.setDropItems(false);
    taskRunner.accept(() -> finishHarvest(player, location, cropDescriptor, drops));
  }

  /// Returns whether immature crop protection should apply to this break.
  ///
  /// @param player   player who broke the crop
  /// @param creative true when the player is in Creative mode
  /// @return true when the break should be cancelled
  private boolean shouldProtectImmature(Player player, boolean creative) {
    if (!config.isImmatureProtectionEnabled()) return false;
    if (creative && !config.isImmatureProtectionCreativeEnabled()) return false;
    return !(config.isSneakBypassEnabled() && player.isSneaking());
  }

  /// Returns whether automatic harvest should apply to this break.
  ///
  /// @param creative true when the player is in Creative mode
  /// @return true when automatic harvest is enabled for this break
  private boolean shouldHarvest(boolean creative) {
    if (!config.isHarvestEnabled()) return false;
    return !creative || config.isHarvestCreativeEnabled();
  }

  /// Completes harvest only when the original crop was actually broken.
  ///
  /// @param player     player who broke the crop
  /// @param location   original crop location
  /// @param descriptor crop descriptor
  /// @param drops      calculated drops
  private void finishHarvest(Player player, Location location, CropDescriptor descriptor, Collection<ItemStack> drops) {
    if (!plugin.isEnabled()) return;
    if (!location.getBlock().getType().isAir()) return;
    harvestService.harvestAndReplant(player, location, descriptor, drops);
  }
}
