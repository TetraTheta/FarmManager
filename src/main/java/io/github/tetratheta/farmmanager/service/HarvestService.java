package io.github.tetratheta.farmmanager.service;

import io.github.tetratheta.farmmanager.config.FMConfig;
import io.github.tetratheta.farmmanager.config.OverflowPolicy;
import io.github.tetratheta.farmmanager.crop.CropDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/// Calculates, delivers, and replants automatic crop harvests.
public final class HarvestService {
  private final FMConfig config;
  private final NotificationService notificationService;

  /// Creates a harvest service.
  ///
  /// @param config              active plugin configuration
  /// @param notificationService gameplay notification service
  public HarvestService(FMConfig config, NotificationService notificationService) {
    this.config = config;
    this.notificationService = notificationService;
  }

  /// Delivers drops, consumes one replant item from the drops, and replants the crop.
  ///
  /// @param player     player who broke the crop
  /// @param location   broken crop location
  /// @param descriptor crop descriptor
  /// @param drops      calculated block drops
  public void harvestAndReplant(Player player, Location location, CropDescriptor descriptor, Collection<ItemStack> drops) {
    List<ItemStack> adjustedDrops = cloneDrops(drops);
    consumeReplantItem(adjustedDrops, descriptor.replantItem());
    deliverDrops(player, location, adjustedDrops);
    replant(location, descriptor.cropMaterial());
  }

  /// Delivers drops according to the configured inventory and overflow policy.
  ///
  /// @param player   player who should receive the harvest
  /// @param location fallback drop location
  /// @param drops    drops after replant item consumption
  private void deliverDrops(Player player, Location location, List<ItemStack> drops) {
    if (drops.isEmpty()) return;
    if (!config.shouldAddToInventory()) {
      dropItems(location, drops);
      return;
    }
    if (config.getOverflowPolicy() == OverflowPolicy.KEEP && !canFitAll(player, drops)) {
      dropItems(location, drops);
      notificationService.send(player, "notification.harvest-overflow");
      return;
    }
    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(cloneDrops(drops).toArray(ItemStack[]::new));
    if (leftovers.isEmpty()) return;
    notificationService.send(player, "notification.harvest-overflow");
    if (config.getOverflowPolicy() == OverflowPolicy.DROP) dropItems(location, leftovers.values());
  }

  /// Drops items naturally at the broken crop location.
  ///
  /// @param location drop location
  /// @param drops    item stacks to drop
  private void dropItems(Location location, Collection<ItemStack> drops) {
    World world = location.getWorld();
    if (world == null) return;
    for (ItemStack drop : drops) {
      if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) continue;
      world.dropItemNaturally(location.toCenterLocation(), drop.clone());
    }
  }

  /// Returns whether all drops can fit into the player's storage inventory without mutation.
  ///
  /// @param player target player
  /// @param drops  drops to test
  /// @return true when all drops can fit
  private boolean canFitAll(Player player, List<ItemStack> drops) {
    ItemStack[] simulatedStorage = cloneStorage(player.getInventory());
    for (ItemStack drop : drops) {
      int remaining = drop.getAmount();
      remaining = fillExistingStacks(simulatedStorage, drop, remaining);
      remaining = fillEmptySlots(simulatedStorage, drop, remaining);
      if (remaining > 0) return false;
    }
    return true;
  }

  /// Fills compatible simulated stacks.
  ///
  /// @param storage   simulated storage contents
  /// @param drop      item to insert
  /// @param remaining remaining item count
  /// @return remaining item count after insertion
  private int fillExistingStacks(ItemStack[] storage, ItemStack drop, int remaining) {
    for (ItemStack stack : storage) {
      if (remaining <= 0) break;
      if (stack == null || !stack.isSimilar(drop)) continue;
      int space = Math.min(stack.getMaxStackSize(), drop.getMaxStackSize()) - stack.getAmount();
      if (space <= 0) continue;
      int inserted = Math.min(space, remaining);
      stack.setAmount(stack.getAmount() + inserted);
      remaining -= inserted;
    }
    return remaining;
  }

  /// Fills empty simulated storage slots.
  ///
  /// @param storage   simulated storage contents
  /// @param drop      item to insert
  /// @param remaining remaining item count
  /// @return remaining item count after insertion
  private int fillEmptySlots(ItemStack[] storage, ItemStack drop, int remaining) {
    for (int index = 0; index < storage.length && remaining > 0; index++) {
      if (storage[index] != null && !storage[index].getType().isAir()) continue;
      int inserted = Math.min(drop.getMaxStackSize(), remaining);
      ItemStack insertedStack = drop.clone();
      insertedStack.setAmount(inserted);
      storage[index] = insertedStack;
      remaining -= inserted;
    }
    return remaining;
  }

  /// Clones player storage contents for simulation.
  ///
  /// @param inventory player inventory
  /// @return cloned storage contents
  private ItemStack[] cloneStorage(PlayerInventory inventory) {
    ItemStack[] contents = inventory.getStorageContents();
    ItemStack[] clonedContents = new ItemStack[contents.length];
    for (int index = 0; index < contents.length; index++) {
      ItemStack itemStack = contents[index];
      clonedContents[index] = itemStack == null ? null : itemStack.clone();
    }
    return clonedContents;
  }

  /// Consumes one item used for replanting from the calculated drop list.
  ///
  /// @param drops       mutable drop list
  /// @param replantItem item material used to replant
  private void consumeReplantItem(List<ItemStack> drops, Material replantItem) {
    for (int index = 0; index < drops.size(); index++) {
      ItemStack drop = drops.get(index);
      if (drop.getType() != replantItem) continue;
      if (drop.getAmount() <= 1) drops.remove(index);
      else drop.setAmount(drop.getAmount() - 1);
      return;
    }
  }

  /// Replants a crop at age zero.
  ///
  /// @param location     location to replant
  /// @param cropMaterial crop block material
  private void replant(Location location, Material cropMaterial) {
    location.getBlock().setType(cropMaterial);
    if (location.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
      ageable.setAge(0);
      location.getBlock().setBlockData(ageable);
    }
  }

  /// Clones drops into a mutable list and removes empty stacks.
  ///
  /// @param drops source drops
  /// @return cloned mutable drops
  private List<ItemStack> cloneDrops(Collection<ItemStack> drops) {
    List<ItemStack> clonedDrops = new ArrayList<>();
    for (ItemStack drop : drops) {
      if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) continue;
      clonedDrops.add(drop.clone());
    }
    return clonedDrops;
  }
}
