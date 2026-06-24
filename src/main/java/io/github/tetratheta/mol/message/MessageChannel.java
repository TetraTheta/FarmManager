package io.github.tetratheta.mol.message;

import java.util.Locale;

/// Represents where localized player-facing messages are shown.
public enum MessageChannel {
  CHAT("chat"),
  ACTION_BAR("action-bar");
  //
  private final String configValue;

  MessageChannel(String configValue) {
    this.configValue = configValue;
  }

  /// Resolves a configured value into a message channel.
  ///
  /// @param value configured value
  /// @return matching channel, or `ACTION_BAR` when the value is unsupported
  public static MessageChannel fromConfig(String value) {
    if (value == null) return ACTION_BAR;
    String normalized = normalizeConfigValue(value);
    if ("actionbar".equals(normalized)) return ACTION_BAR;
    for (MessageChannel channel : values()) {
      if (channel.configValue.equals(normalized)) return channel;
    }
    return ACTION_BAR;
  }

  /// Normalizes free-form configuration input before matching aliases.
  ///
  /// @param value configured value
  /// @return normalized value
  private static String normalizeConfigValue(String value) {
    return value.strip().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
  }

  /// Returns whether the raw value clearly means a supported message channel.
  ///
  /// @param value configured value
  /// @return true when the value is a valid channel or alias
  public static boolean isSupportedConfigValue(String value) {
    if (value == null) return false;
    String normalized = normalizeConfigValue(value);
    if ("actionbar".equals(normalized)) return true;
    for (MessageChannel channel : values()) {
      if (channel.configValue.equals(normalized)) return true;
    }
    return false;
  }

  /// Returns the stable configuration value for this channel.
  ///
  /// @return stable configuration value
  public String configValue() {
    return configValue;
  }
}
