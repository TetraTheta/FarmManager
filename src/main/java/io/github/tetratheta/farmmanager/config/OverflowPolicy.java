package io.github.tetratheta.farmmanager.config;

import java.util.Locale;

/// Defines how inventory leftovers should be handled after automatic harvest.
public enum OverflowPolicy {
  DROP("drop"),
  DISCARD("discard"),
  KEEP("keep");
  //
  private final String configValue;

  OverflowPolicy(String configValue) {
    this.configValue = configValue;
  }

  /// Resolves a configured value into an overflow policy.
  ///
  /// @param value configured value
  /// @return matching policy, or `DROP` when the value is unknown
  public static OverflowPolicy fromConfig(String value) {
    if (value == null) return DROP;
    String normalized = value.strip().toLowerCase(Locale.ROOT);
    for (OverflowPolicy policy : values()) {
      if (policy.configValue.equals(normalized)) return policy;
    }
    return DROP;
  }

  /// Returns the stable configuration value for this policy.
  ///
  /// @return stable configuration value
  public String configValue() {
    return configValue;
  }
}
