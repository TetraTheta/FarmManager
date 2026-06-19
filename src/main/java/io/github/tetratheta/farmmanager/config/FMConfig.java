package io.github.tetratheta.farmmanager.config;

import io.github.tetratheta.farmmanager.crop.CropRegistry;
import io.github.tetratheta.farmmanager.region.RegionKey;
import io.github.tetratheta.farmmanager.region.RegionService;
import io.github.tetratheta.mol.config.BaseConfig;
import io.github.tetratheta.mol.message.MessageChannel;
import io.github.tetratheta.mol.message.MessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

/// Loads, normalizes, and persists FarmManager configuration values.
public final class FMConfig extends BaseConfig {
  private static final String PATH_BYPASS_SNEAK = "bypass.sneak-to-break-immature";
  private static final String PATH_CHAT_COOLDOWN = "notification.chat-cooldown-ticks";
  private static final String PATH_NOTIFICATION_CHANNEL = "notification.channel";
  private static final String PATH_CROPS = "crops.materials";
  private static final String PATH_HARVEST_ADD_TO_INVENTORY = "harvest.add-to-inventory";
  private static final String PATH_HARVEST_CREATIVE = "harvest.creative";
  private static final String PATH_HARVEST_ENABLED = "harvest.enabled";
  private static final String PATH_HARVEST_OVERFLOW = "harvest.overflow";
  private static final String PATH_LANGUAGE = "language";
  private static final String PATH_PROTECTION_FARMLAND_TRAMPLING =
      "protection.farmland.trampling.enabled";
  private static final String PATH_PROTECTION_CREATIVE = "protection.immature.creative";
  private static final String PATH_PROTECTION_ENABLED = "protection.immature.enabled";
  private static final String PATH_WATCHED_REGIONS = "regions.watched";

  /// Creates a configuration facade bound to the provided plugin instance.
  ///
  /// @param provided plugin instance that owns the Bukkit configuration
  public FMConfig(JavaPlugin provided) {
    super(provided);
  }

  /// Returns the configured language code.
  ///
  /// @return configured language code
  public String getLanguage() {
    return getString(PATH_LANGUAGE, MessageService.defaultLanguage());
  }

  /// Returns raw configured crop material names.
  ///
  /// @return configured crop material names
  public List<String> getConfiguredCropMaterials() {
    return getStringList(
        PATH_CROPS, List.of("BEETROOTS", "CARROTS", "NETHER_WART", "POTATOES", "WHEAT"));
  }

  /// Returns whether immature crop protection is enabled.
  ///
  /// @return true when immature crops should be protected
  public boolean isImmatureProtectionEnabled() {
    return getBoolean(PATH_PROTECTION_ENABLED, true);
  }

  /// Returns whether immature crop protection applies to Creative players.
  ///
  /// @return true when Creative players are protected from immature breaks
  public boolean isImmatureProtectionCreativeEnabled() {
    return getBoolean(PATH_PROTECTION_CREATIVE, false);
  }

  /// Returns whether watched-region farmland should be protected from entity trampling.
  ///
  /// @return true when farmland trampling should be cancelled
  public boolean isFarmlandTramplingProtectionEnabled() {
    return getBoolean(PATH_PROTECTION_FARMLAND_TRAMPLING, true);
  }

  /// Returns whether sneaking bypasses immature crop protection.
  ///
  /// @return true when sneaking players can bypass immature protection
  public boolean isSneakBypassEnabled() {
    return getBoolean(PATH_BYPASS_SNEAK, true);
  }

  /// Returns whether automatic harvest handling is enabled.
  ///
  /// @return true when mature crop breaks should be harvested and replanted
  public boolean isHarvestEnabled() {
    return getBoolean(PATH_HARVEST_ENABLED, true);
  }

  /// Returns whether automatic harvest applies to Creative players.
  ///
  /// @return true when Creative players should trigger automatic harvest
  public boolean isHarvestCreativeEnabled() {
    return getBoolean(PATH_HARVEST_CREATIVE, false);
  }

  /// Returns whether drops should be inserted into the player's inventory first.
  ///
  /// @return true when inventory delivery is enabled
  public boolean shouldAddToInventory() {
    return getBoolean(PATH_HARVEST_ADD_TO_INVENTORY, true);
  }

  /// Returns the normalized overflow policy.
  ///
  /// @return normalized overflow policy
  public OverflowPolicy getOverflowPolicy() {
    return OverflowPolicy.fromConfig(getString(PATH_HARVEST_OVERFLOW, "drop"));
  }

  /// Returns the normalized gameplay notification channel.
  ///
  /// @return normalized notification channel
  public MessageChannel getNotificationChannel() {
    return MessageChannel.fromConfig(getString(PATH_NOTIFICATION_CHANNEL, "action-bar"));
  }

  /// Returns the listener chat cooldown in ticks.
  ///
  /// @return chat cooldown in ticks
  public int getChatCooldownTicks() {
    return getInt(PATH_CHAT_COOLDOWN, 40, 0, 20 * 60 * 60);
  }

  /// Returns configured watched region entries.
  ///
  /// @return configured watched region entries
  public List<String> getWatchedRegions() {
    return getStringList(PATH_WATCHED_REGIONS, List.of());
  }

  /// Stores normalized watched region entries.
  ///
  /// @param regions normalized watched region keys
  public void setWatchedRegions(Set<RegionKey> regions) {
    getConfig().set(PATH_WATCHED_REGIONS, regions.stream().map(RegionKey::asString).toList());
  }

  /// Adds one normalized watched region entry and saves it in memory.
  ///
  /// @param region normalized region key
  public void addWatchedRegion(RegionKey region) {
    List<String> regions = new ArrayList<>(getConfig().getStringList(PATH_WATCHED_REGIONS));
    if (!regions.contains(region.asString())) regions.add(region.asString());
    getConfig().set(PATH_WATCHED_REGIONS, regions);
  }

  /// Removes one normalized watched region entry and saves it in memory.
  ///
  /// @param region normalized region key
  public void removeWatchedRegion(RegionKey region) {
    List<String> regions = new ArrayList<>(getConfig().getStringList(PATH_WATCHED_REGIONS));
    regions.removeIf(entry -> RegionKey.parse(entry).map(region::equals).orElse(false));
    getConfig().set(PATH_WATCHED_REGIONS, regions);
  }

  /// Normalizes recoverable configuration values before runtime services use them.
  ///
  /// @param messageService localized message service used for warnings
  /// @param cropRegistry active crop registry
  /// @param regionService active region service
  /// @return true when configuration values were changed and should be saved
  public boolean validateAndFix(
      MessageService messageService, CropRegistry cropRegistry, RegionService regionService) {
    boolean changed = validateOverflowPolicy(messageService);
    changed |= validateNotificationChannel(messageService);
    changed |= validateCropMaterials(cropRegistry);
    changed |= validateWatchedRegions(regionService);
    return changed;
  }

  /// Ensures the overflow policy is supported.
  ///
  /// @param messageService localized message service used for warnings
  /// @return true when the configuration was changed
  private boolean validateOverflowPolicy(MessageService messageService) {
    String configured = getConfig().getString(PATH_HARVEST_OVERFLOW, "drop");
    OverflowPolicy policy = OverflowPolicy.fromConfig(configured);
    if (policy != OverflowPolicy.DROP || "drop".equalsIgnoreCase(configured.strip())) {
      getConfig().set(PATH_HARVEST_OVERFLOW, policy.configValue());
      return !configured.equals(policy.configValue());
    }

    messageService.logWarning("log.config.invalid-overflow-policy", configured);
    getConfig().set(PATH_HARVEST_OVERFLOW, OverflowPolicy.DROP.configValue());
    return true;
  }

  /// Ensures the notification channel is supported.
  ///
  /// @param messageService localized message service used for warnings
  /// @return true when the configuration was changed
  private boolean validateNotificationChannel(MessageService messageService) {
    String configured = getConfig().getString(PATH_NOTIFICATION_CHANNEL, "action-bar");
    MessageChannel channel = MessageChannel.fromConfig(configured);
    if (MessageChannel.isSupportedConfigValue(configured)) {
      getConfig().set(PATH_NOTIFICATION_CHANNEL, channel.configValue());
      return !configured.equals(channel.configValue());
    }

    messageService.logWarning("log.config.invalid-notification-channel", configured);
    getConfig().set(PATH_NOTIFICATION_CHANNEL, MessageChannel.ACTION_BAR.configValue());
    return true;
  }

  /// Writes back the active crop material list after invalid entries have been ignored.
  ///
  /// @param cropRegistry active crop registry
  /// @return true when the configuration was changed
  private boolean validateCropMaterials(CropRegistry cropRegistry) {
    List<String> normalized =
        cropRegistry.getActiveCropMaterials().stream().map(Material::name).toList();
    List<String> configured =
        getConfig().getStringList(PATH_CROPS).stream()
            .map(value -> value.strip().toUpperCase(Locale.ROOT))
            .toList();
    if (configured.equals(normalized)) return false;

    getConfig().set(PATH_CROPS, normalized);
    return true;
  }

  /// Writes back only watched regions that exist in WorldGuard.
  ///
  /// @param regionService active region service
  /// @return true when the configuration was changed
  private boolean validateWatchedRegions(RegionService regionService) {
    Set<RegionKey> validRegions = regionService.getWatchedRegions();
    List<String> normalized = validRegions.stream().map(RegionKey::asString).toList();
    if (getConfig().getStringList(PATH_WATCHED_REGIONS).equals(normalized)) return false;

    getConfig().set(PATH_WATCHED_REGIONS, normalized);
    return true;
  }
}
