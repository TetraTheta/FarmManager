package io.github.tetratheta.farmmanager.listener;

import io.github.tetratheta.farmmanager.service.FarmlandProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/// Cancels entity-caused farmland trampling in watched regions.
public final class FarmlandTramplingListener implements Listener {
  private final FarmlandProtectionService farmlandProtectionService;

  /// Creates the farmland trampling listener.
  ///
  /// @param farmlandProtectionService farmland protection decision service
  public FarmlandTramplingListener(FarmlandProtectionService farmlandProtectionService) {
    this.farmlandProtectionService = farmlandProtectionService;
  }

  /// Handles farmland trampling after other protection plugins have acted.
  ///
  /// @param event entity block change event
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    if (!farmlandProtectionService.shouldCancelTrampling(event.getBlock(), event.getTo())) return;

    event.setCancelled(true);
  }
}
