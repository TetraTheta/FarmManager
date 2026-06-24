package io.github.tetratheta.farmmanager.region;

import java.util.Locale;
import java.util.Optional;

/// Identifies a WorldGuard region by Bukkit world name and region ID.
///
/// @param worldName Bukkit world name
/// @param regionId  WorldGuard region ID
public record RegionKey(String worldName, String regionId) {
  /// Creates a normalized region key.
  ///
  /// @param worldName Bukkit world name
  /// @param regionId  WorldGuard region ID
  public RegionKey {
    worldName = worldName.strip();
    regionId = regionId.strip();
  }

  /// Parses a `world:region` key.
  ///
  /// @param value configured value
  /// @return parsed key, or empty when the value is malformed
  public static Optional<RegionKey> parse(String value) {
    if (value == null) return Optional.empty();
    int separatorIndex = value.indexOf(':');
    if (separatorIndex <= 0 || separatorIndex == value.length() - 1) return Optional.empty();
    String worldName = value.substring(0, separatorIndex).strip();
    String regionId = value.substring(separatorIndex + 1).strip();
    if (worldName.isBlank() || regionId.isBlank()) return Optional.empty();
    return Optional.of(new RegionKey(worldName, regionId));
  }

  /// Returns whether this key has the same world and region ignoring case.
  ///
  /// @param other other region key
  /// @return true when both keys match ignoring case
  public boolean equalsIgnoreCase(RegionKey other) {
    return worldName.equalsIgnoreCase(other.worldName) && regionId.equalsIgnoreCase(other.regionId);
  }

  /// Returns a lower-case lookup key.
  ///
  /// @return lower-case lookup key
  public String lookupKey() {
    return asString().toLowerCase(Locale.ROOT);
  }

  /// Returns the stable configuration representation.
  ///
  /// @return `world:region` string
  public String asString() {
    return worldName + ":" + regionId;
  }
}
