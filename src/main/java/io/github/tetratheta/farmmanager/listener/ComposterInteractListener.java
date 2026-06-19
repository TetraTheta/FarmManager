package io.github.tetratheta.farmmanager.listener;

import io.github.tetratheta.farmmanager.service.ComposterService;
import io.github.tetratheta.farmmanager.service.ComposterService.CompostResult;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/// Handles configured composter items inserted by player interaction.
public final class ComposterInteractListener implements Listener {
  private final ComposterService composterService;

  /// Creates a player composter interaction listener.
  ///
  /// @param composterService configured composter service
  public ComposterInteractListener(ComposterService composterService) {
    this.composterService = composterService;
  }

  /// Applies custom composter chances to right-click composter use.
  ///
  /// @param event player interaction event
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerCompost(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Block block = event.getClickedBlock();
    if (block == null || block.getType() != Material.COMPOSTER) return;

    ItemStack item = event.getItem();
    if (item == null || item.getType().isAir() || item.getAmount() <= 0) return;
    if (!composterService.isConfiguredMaterial(item.getType())) return;
    if (isDuplicateOffHandEvent(event)) return;
    if (!composterService.canAccept(block, item.getType())) return;

    event.setCancelled(true);
    composterService.consumeOne(item);
    Optional<CompostResult> result = composterService.compost(block, item.getType());
    if (result.isEmpty()) return;

    composterService.playCompostSound(block, result.get());
    if (result.get().successful()) composterService.spawnComposterParticles(block);
  }

  /// Avoids consuming both hands when Bukkit also fires an off-hand event.
  ///
  /// @param event player interaction event
  /// @return true when this off-hand event should be ignored
  private boolean isDuplicateOffHandEvent(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.OFF_HAND) return false;

    ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
    return composterService.isConfiguredMaterial(mainHand.getType());
  }
}
