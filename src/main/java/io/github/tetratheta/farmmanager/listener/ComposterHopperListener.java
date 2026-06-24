package io.github.tetratheta.farmmanager.listener;

import io.github.tetratheta.farmmanager.service.ComposterService;
import io.github.tetratheta.farmmanager.service.ComposterService.CompostResult;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/// Handles configured composter items inserted by hoppers.
public final class ComposterHopperListener implements Listener {
  private final ComposterService composterService;
  private final Consumer<Runnable> taskRunner;

  /// Creates a hopper composter interaction listener.
  ///
  /// @param composterService configured composter service
  /// @param taskRunner       runtime-owned task runner
  public ComposterHopperListener(ComposterService composterService, Consumer<Runnable> taskRunner) {
    this.composterService = composterService;
    this.taskRunner = taskRunner;
  }

  /// Applies custom composter chances to hopper-fed composter inventories.
  ///
  /// @param event inventory move event
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onHopperCompost(InventoryMoveItemEvent event) {
    if (event.getDestination().getType() != InventoryType.COMPOSTER) return;
    ItemStack movedItem = event.getItem();
    if (movedItem == null || movedItem.getType().isAir() || movedItem.getAmount() <= 0) return;
    if (!composterService.isConfiguredMaterial(movedItem.getType())) return;
    Block block = event.getDestination().getLocation() == null ? null : event.getDestination().getLocation().getBlock();
    if (block == null || block.getType() != Material.COMPOSTER) return;
    if (!composterService.canAccept(block, movedItem.getType())) return;
    event.setCancelled(true);
    Inventory source = event.getSource();
    Material material = movedItem.getType();
    ItemStack itemSnapshot = movedItem.clone();
    taskRunner.accept(() -> finishHopperCompost(source, block, material, itemSnapshot));
  }

  /// Completes hopper composting after Bukkit has restored the cancelled move.
  ///
  /// @param source       source inventory
  /// @param block        target composter block
  /// @param material     accepted item material
  /// @param itemSnapshot item attempted by Bukkit
  private void finishHopperCompost(Inventory source, Block block, Material material, ItemStack itemSnapshot) {
    if (!composterService.canAccept(block, material)) return;
    if (!removeOne(source, itemSnapshot)) return;
    Optional<CompostResult> result = composterService.compost(block, material);
    if (result.isEmpty()) return;
    composterService.playCompostSound(block, result.get());
    if (result.get().successful()) composterService.spawnComposterParticles(block);
  }

  /// Removes exactly one item matching the attempted hopper transfer.
  ///
  /// @param source    source inventory
  /// @param movedItem item attempted by Bukkit
  /// @return true when one item was removed
  private boolean removeOne(Inventory source, ItemStack movedItem) {
    for (int slot = 0; slot < source.getSize(); slot++) {
      ItemStack item = source.getItem(slot);
      if (item == null || item.getType().isAir() || !item.isSimilar(movedItem)) continue;
      if (item.getAmount() <= 1) {
        source.setItem(slot, null);
      } else {
        item.setAmount(item.getAmount() - 1);
        source.setItem(slot, item);
      }
      return true;
    }
    return false;
  }
}
