package io.github.tetratheta.farmmanager.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.github.tetratheta.mol.message.MessageService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/// Provides WorldGuard region lookup, validation, containment, and command suggestions.
public final class RegionService {
  private final RegionContainer regionContainer;
  private final Set<RegionKey> watchedRegions;

  /// Creates a region service from configured watched region entries.
  ///
  /// @param configuredRegions configured `world:region` entries
  /// @param messageService    localized message service used for warnings
  public RegionService(List<String> configuredRegions, MessageService messageService) {
    regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    watchedRegions = new LinkedHashSet<>();
    for (String configuredRegion : configuredRegions) {
      Optional<RegionKey> parsedRegion = RegionKey.parse(configuredRegion);
      if (parsedRegion.isEmpty()) {
        messageService.logWarning("log.config.invalid-region-format", configuredRegion);
        continue;
      }
      RegionKey region = parsedRegion.get();
      if (Bukkit.getWorld(region.worldName()) == null) {
        messageService.logWarning("log.config.unknown-world", region.worldName());
        continue;
      }
      if (!exists(region)) {
        messageService.logWarning("log.config.unknown-region", region.asString());
        continue;
      }
      watchedRegions.add(region);
    }
  }

  /// Returns whether a WorldGuard region exists.
  ///
  /// @param region region key to test
  /// @return true when the world and region exist
  public boolean exists(RegionKey region) {
    World world = Bukkit.getWorld(region.worldName());
    if (world == null) return false;
    RegionManager manager = getRegionManager(world);
    return manager != null && manager.getRegion(region.regionId()) != null;
  }

  /// Returns the WorldGuard region manager for one Bukkit world.
  ///
  /// @param world Bukkit world
  /// @return region manager, or null when unavailable
  private RegionManager getRegionManager(World world) {
    return regionContainer.get(BukkitAdapter.adapt(world));
  }

  /// Returns normalized watched region keys.
  ///
  /// @return watched region keys
  public Set<RegionKey> getWatchedRegions() {
    return new LinkedHashSet<>(watchedRegions);
  }

  /// Returns whether the provided location is inside a watched WorldGuard region.
  ///
  /// @param location location to test
  /// @return true when at least one watched region contains the location
  public boolean isWatched(Location location) {
    World world = location.getWorld();
    if (world == null) return false;
    RegionManager manager = getRegionManager(world);
    if (manager == null) return false;
    BlockVector3 point = BlockVector3.at(location.blockX(), location.blockY(), location.blockZ());
    for (RegionKey region : watchedRegions) {
      if (!region.worldName().equalsIgnoreCase(world.getName())) continue;
      ProtectedRegion protectedRegion = manager.getRegion(region.regionId());
      if (protectedRegion != null && protectedRegion.contains(point)) return true;
    }
    return false;
  }

  /// Resolves command input into a WorldGuard region.
  ///
  /// @param input  raw command input
  /// @param sender command sender
  /// @return resolution result
  public RegionResolution resolveCommandInput(String input, CommandSender sender) {
    if (input == null || input.isBlank()) return RegionResolution.failed(RegionResolution.Status.INVALID_FORMAT);
    if (input.contains(":")) return resolveExplicit(input);
    if (sender instanceof Player player) return resolveInWorld(player.getWorld(), input);
    return resolveBareConsoleRegion(input);
  }

  /// Returns command suggestions for region inputs.
  ///
  /// @param sender      command sender
  /// @param remaining   current argument text
  /// @param watchedOnly true when suggestions should be limited to watched regions
  /// @return suggestion strings
  public List<String> suggest(CommandSender sender, String remaining, boolean watchedOnly) {
    String input = remaining == null ? "" : remaining;
    String lowerInput = input.toLowerCase(Locale.ROOT);
    List<String> candidates = watchedOnly ? watchedRegions.stream().map(RegionKey::asString).toList() : allRegionKeys();
    if (sender instanceof Player player && !input.contains(":") && !watchedOnly) candidates = regionIdsInWorld(player.getWorld());
    if (input.contains(":") && !watchedOnly) {
      int separatorIndex = input.indexOf(':');
      String worldName = input.substring(0, separatorIndex);
      World world = Bukkit.getWorld(worldName);
      if (world != null) candidates = regionIdsInWorld(world).stream().map(regionId -> world.getName() + ":" + regionId).toList();
    }
    return candidates.stream().filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(lowerInput)).sorted(String.CASE_INSENSITIVE_ORDER)
                     .toList();
  }

  /// Adds a watched region to this runtime service.
  ///
  /// @param region normalized region key
  public void addWatchedRegion(RegionKey region) {
    watchedRegions.add(region);
  }

  /// Removes a watched region from this runtime service.
  ///
  /// @param region normalized region key
  public void removeWatchedRegion(RegionKey region) {
    watchedRegions.removeIf(existing -> existing.equalsIgnoreCase(region));
  }

  /// Returns whether a region is already watched.
  ///
  /// @param region region key
  /// @return true when the region is watched
  public boolean isWatchedRegion(RegionKey region) {
    return watchedRegions.stream().anyMatch(existing -> existing.equalsIgnoreCase(region));
  }

  /// Resolves an explicit `world:region` input.
  ///
  /// @param input explicit input
  /// @return resolution result
  private RegionResolution resolveExplicit(String input) {
    Optional<RegionKey> parsedRegion = RegionKey.parse(input);
    if (parsedRegion.isEmpty()) return RegionResolution.failed(RegionResolution.Status.INVALID_FORMAT);
    RegionKey region = parsedRegion.get();
    World world = Bukkit.getWorld(region.worldName());
    if (world == null) return RegionResolution.failed(RegionResolution.Status.UNKNOWN_WORLD);
    if (!exists(region)) return RegionResolution.failed(RegionResolution.Status.NOT_FOUND);
    return RegionResolution.resolved(region);
  }

  /// Resolves a bare region name in a specific world.
  ///
  /// @param world    world used as context
  /// @param regionId bare region ID
  /// @return resolution result
  private RegionResolution resolveInWorld(World world, String regionId) {
    RegionKey region = new RegionKey(world.getName(), regionId);
    if (!exists(region)) return RegionResolution.failed(RegionResolution.Status.NOT_FOUND);
    return RegionResolution.resolved(region);
  }

  /// Resolves a bare console region only when exactly one world contains it.
  ///
  /// @param regionId bare region ID
  /// @return resolution result
  private RegionResolution resolveBareConsoleRegion(String regionId) {
    List<RegionKey> matches = new ArrayList<>();
    for (World world : Bukkit.getWorlds()) {
      RegionKey region = new RegionKey(world.getName(), regionId);
      if (exists(region)) matches.add(region);
    }
    if (matches.isEmpty()) return RegionResolution.failed(RegionResolution.Status.NOT_FOUND);
    if (matches.size() > 1) return RegionResolution.failed(RegionResolution.Status.AMBIGUOUS_CONSOLE_REGION);
    return RegionResolution.resolved(matches.getFirst());
  }

  /// Returns all loaded region keys as `world:region` strings.
  ///
  /// @return all loaded region keys
  private List<String> allRegionKeys() {
    List<String> regions = new ArrayList<>();
    for (World world : Bukkit.getWorlds()) {
      for (String regionId : regionIdsInWorld(world)) regions.add(world.getName() + ":" + regionId);
    }
    regions.sort(Comparator.comparing(String::toLowerCase));
    return regions;
  }

  /// Returns all region IDs in one world.
  ///
  /// @param world Bukkit world
  /// @return region IDs in the world
  private List<String> regionIdsInWorld(World world) {
    RegionManager manager = getRegionManager(world);
    if (manager == null) return List.of();
    return manager.getRegions().keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
  }
}
