package io.github.tetratheta.farmmanager.service;

import io.github.tetratheta.farmmanager.config.FMConfig;
import io.github.tetratheta.farmmanager.region.RegionService;
import org.bukkit.Material;
import org.bukkit.block.Block;

/// Decides whether protected farmland block changes should be cancelled.
public final class FarmlandProtectionService {
  private final FMConfig config;
  private final RegionService regionService;

  /// Creates a farmland protection service.
  ///
  /// @param config active configuration
  /// @param regionService active region service
  public FarmlandProtectionService(FMConfig config, RegionService regionService) {
    this.config = config;
    this.regionService = regionService;
  }

  /// Returns whether an entity-caused block change should preserve farmland.
  ///
  /// @param block source block that may be trampled
  /// @param targetMaterial material the event wants to change into
  /// @return true when the block change should be cancelled
  public boolean shouldCancelTrampling(Block block, Material targetMaterial) {
    if (!config.isFarmlandTramplingProtectionEnabled()) return false;
    if (block.getType() != Material.FARMLAND) return false;
    if (targetMaterial != Material.DIRT) return false;
    return regionService.isWatched(block.getLocation());
  }
}
