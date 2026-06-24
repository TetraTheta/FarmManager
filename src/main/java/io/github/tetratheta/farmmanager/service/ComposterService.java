package io.github.tetratheta.farmmanager.service;

import io.github.tetratheta.farmmanager.config.FMConfig;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.inventory.ItemStack;

/// Applies configured composter chances to accepted item insertions.
///
/// Empty composters are filled once before chance-based behavior starts, matching vanilla expectations for the first accepted item.
public final class ComposterService {
  private static final int BONE_MEAL_READY_LEVEL = 8;
  private static final int GUARANTEED_FILL_LEVEL = 0;
  private static final int LAST_FILLABLE_LEVEL = 6;
  private final FMConfig config;
  private final Map<Material, Double> chances;

  /// Creates a composter service from the active configuration.
  ///
  /// @param config active plugin configuration
  public ComposterService(FMConfig config) {
    this.config = config;
    chances = config.getComposterChances();
  }

  /// Attempts to compost one configured item into the target block.
  ///
  /// @param block    target composter block
  /// @param material accepted item material
  /// @return compost result, or empty when the block cannot accept the item
  public Optional<CompostResult> compost(Block block, Material material) {
    if (!canAccept(block, material)) return Optional.empty();
    BlockData blockData = block.getBlockData();
    if (!(blockData instanceof Levelled composter)) return Optional.empty();
    boolean successful = shouldIncreaseLevel(composter.getLevel(), chances.get(material));
    if (!successful) return Optional.of(CompostResult.failed());
    composter.setLevel(composter.getLevel() + 1);
    block.setBlockData(composter);
    if (composter.getLevel() == LAST_FILLABLE_LEVEL + 1) {
      composter.setLevel(BONE_MEAL_READY_LEVEL);
      block.setBlockData(composter);
      return Optional.of(CompostResult.ready());
    }
    return Optional.of(CompostResult.filled());
  }

  /// Returns whether the target block can accept one configured compost item now.
  ///
  /// @param block    target composter block
  /// @param material accepted item material
  /// @return true when one item can be consumed for custom composting
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean canAccept(Block block, Material material) {
    if (!isConfiguredMaterial(material)) return false;
    if (block.getType() != Material.COMPOSTER) return false;
    BlockData blockData = block.getBlockData();
    if (!(blockData instanceof Levelled composter)) return false;
    return composter.getLevel() <= LAST_FILLABLE_LEVEL;
  }

  /// Returns whether FarmManager should handle this material for composting.
  ///
  /// @param material item material
  /// @return true when the material has a configured composter chance
  public boolean isConfiguredMaterial(Material material) {
    return config.isComposterEnabled() && chances.containsKey(material);
  }

  /// Returns whether this insertion should raise the composter level.
  ///
  /// @param currentLevel current composter level
  /// @param chance       configured chance from 0.0 to 1.0
  /// @return true when the level should increase
  private boolean shouldIncreaseLevel(int currentLevel, double chance) {
    return currentLevel == GUARANTEED_FILL_LEVEL || ThreadLocalRandom.current().nextDouble() <= chance;
  }

  /// Plays the vanilla-like sound for a compost result.
  ///
  /// @param block  target composter block
  /// @param result compost result
  public void playCompostSound(Block block, CompostResult result) {
    if (!result.successful()) {
      block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COMPOSTER_FILL, 1.0f, 1.0f);
      return;
    }
    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COMPOSTER_FILL_SUCCESS, 1.0f, 1.0f);
    if (result.completed()) block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COMPOSTER_READY, 1.0f, 1.0f);
  }

  /// Spawns vanilla-like composter particles above the target block.
  ///
  /// @param block target composter block
  public void spawnComposterParticles(Block block) {
    Random random = new Random();
    double centerHeight = 0.5 + 0.03125;
    for (int i = 0; i < 10; i++) {
      double xa = random.nextGaussian() * 0.02;
      double ya = random.nextGaussian() * 0.02;
      double za = random.nextGaussian() * 0.02;
      double x = block.getX() + 0.1875 + 0.625 * random.nextFloat();
      double y = block.getY() + centerHeight + random.nextFloat() * (1.0 - centerHeight);
      double z = block.getZ() + 0.1875 + 0.625 * random.nextFloat();
      block.getWorld().spawnParticle(Particle.COMPOSTER, x, y, z, 1, xa, ya, za, 0.0);
    }
  }

  /// Consumes exactly one item from a mutable stack.
  ///
  /// @param itemStack stack to decrement
  public void consumeOne(ItemStack itemStack) {
    itemStack.setAmount(itemStack.getAmount() - 1);
  }

  /// Describes the outcome of one accepted compost item.
  ///
  /// @param successful true when the level increased
  /// @param completed  true when the composter became ready to harvest
  public record CompostResult(boolean successful, boolean completed) {
    /// Returns a failed fill result.
    ///
    /// @return failed result
    public static CompostResult failed() {
      return new CompostResult(false, false);
    }

    /// Returns a successful fill result.
    ///
    /// @return fill result
    public static CompostResult filled() {
      return new CompostResult(true, false);
    }

    /// Returns a result that made the composter ready.
    ///
    /// @return completed result
    public static CompostResult ready() {
      return new CompostResult(true, true);
    }
  }
}
