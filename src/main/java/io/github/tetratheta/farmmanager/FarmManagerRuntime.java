package io.github.tetratheta.farmmanager;

import io.github.tetratheta.farmmanager.config.FMConfig;
import io.github.tetratheta.farmmanager.crop.CropRegistry;
import io.github.tetratheta.farmmanager.listener.ComposterHopperListener;
import io.github.tetratheta.farmmanager.listener.ComposterInteractListener;
import io.github.tetratheta.farmmanager.listener.CropBreakListener;
import io.github.tetratheta.farmmanager.listener.FarmlandTramplingListener;
import io.github.tetratheta.farmmanager.region.RegionService;
import io.github.tetratheta.farmmanager.service.ComposterService;
import io.github.tetratheta.farmmanager.service.FarmlandProtectionService;
import io.github.tetratheta.farmmanager.service.HarvestService;
import io.github.tetratheta.farmmanager.service.NotificationService;
import io.github.tetratheta.mol.message.MessageService;
import io.github.tetratheta.mol.plugin.PluginRuntime;

/// Wires configuration-backed services and Bukkit resources for one plugin runtime.
@SuppressWarnings("FieldCanBeLocal")
public final class FarmManagerRuntime extends PluginRuntime {
  private final ComposterService composterService;
  private final FMConfig config;
  private final CropRegistry cropRegistry;
  private final FarmlandProtectionService farmlandProtectionService;
  private final HarvestService harvestService;
  private final MessageService messageService;
  private final NotificationService notificationService;
  private final RegionService regionService;

  /// Creates all services from the current disk configuration and registers runtime listeners.
  ///
  /// @param plugin plugin entry point that owns this runtime
  public FarmManagerRuntime(FarmManager plugin) {
    super(plugin);
    config = new FMConfig(plugin);
    messageService = new MessageService(plugin, config.getLanguage());
    cropRegistry = new CropRegistry(config.getConfiguredCropMaterials(), messageService);
    regionService = new RegionService(config.getWatchedRegions(), messageService);
    boolean changed = config.validateAndFix(messageService, cropRegistry, regionService);
    if (changed) config.saveConfig();
    notificationService = new NotificationService(messageService, config.getNotificationChannel(), config.getChatCooldownTicks());
    composterService = new ComposterService(config);
    harvestService = new HarvestService(config, notificationService);
    farmlandProtectionService = new FarmlandProtectionService(config, regionService);
    registerListener(new ComposterInteractListener(composterService));
    registerListener(new ComposterHopperListener(composterService, this::runTask));
    registerListener(new CropBreakListener(plugin, config, cropRegistry, regionService, harvestService, notificationService, this::runTask));
    registerListener(new FarmlandTramplingListener(farmlandProtectionService));
  }

  /// Returns the active configuration facade.
  ///
  /// @return active configuration facade
  public FMConfig getConfig() {
    return config;
  }

  /// Returns the active crop registry.
  ///
  /// @return active crop registry
  public CropRegistry getCropRegistry() {
    return cropRegistry;
  }

  /// Returns the active localized message service.
  ///
  /// @return localized message service
  public MessageService getMessageService() {
    return messageService;
  }

  /// Returns the active WorldGuard region service.
  ///
  /// @return active region service
  public RegionService getRegionService() {
    return regionService;
  }
}
