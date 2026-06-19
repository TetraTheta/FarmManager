package io.github.tetratheta.farmmanager.region;

/// Represents the result of resolving user command input into a WorldGuard region key.
///
/// @param status resolution status
/// @param region resolved region key, or null when not resolved
public record RegionResolution(Status status, RegionKey region) {

  /// Creates a successful resolution.
  ///
  /// @param region resolved region key
  /// @return successful resolution
  public static RegionResolution resolved(RegionKey region) {
    return new RegionResolution(Status.RESOLVED, region);
  }

  /// Creates a failed resolution.
  ///
  /// @param status failed status
  /// @return failed resolution
  public static RegionResolution failed(Status status) {
    return new RegionResolution(status, null);
  }

  /// Command input resolution statuses.
  public enum Status {
    AMBIGUOUS_CONSOLE_REGION,
    INVALID_FORMAT,
    NOT_FOUND,
    RESOLVED,
    UNKNOWN_WORLD
  }
}
