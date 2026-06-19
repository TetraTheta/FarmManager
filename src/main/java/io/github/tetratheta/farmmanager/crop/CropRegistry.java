package io.github.tetratheta.farmmanager.crop;

import io.github.tetratheta.mol.message.MessageService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;

/// Resolves configured crop materials into supported crop descriptors.
public final class CropRegistry {
  private static final Map<Material, CropDescriptor> SUPPORTED_CROPS =
      Map.of(
          Material.WHEAT, new CropDescriptor(Material.WHEAT, Material.WHEAT_SEEDS),
          Material.CARROTS, new CropDescriptor(Material.CARROTS, Material.CARROT),
          Material.POTATOES, new CropDescriptor(Material.POTATOES, Material.POTATO),
          Material.BEETROOTS, new CropDescriptor(Material.BEETROOTS, Material.BEETROOT_SEEDS),
          Material.NETHER_WART, new CropDescriptor(Material.NETHER_WART, Material.NETHER_WART));

  private final Map<Material, CropDescriptor> activeCrops;

  /// Creates a crop registry from configured material names.
  ///
  /// @param configuredMaterials configured material names
  /// @param messageService localized message service used for warnings
  public CropRegistry(List<String> configuredMaterials, MessageService messageService) {
    activeCrops = new LinkedHashMap<>();
    for (String configuredMaterial : configuredMaterials) {
      Material material = Material.matchMaterial(configuredMaterial);
      if (material == null) {
        messageService.logWarning("log.config.invalid-crop-material", configuredMaterial);
        continue;
      }

      CropDescriptor descriptor = SUPPORTED_CROPS.get(material);
      if (descriptor == null) {
        messageService.logWarning("log.config.unsupported-crop-material", material.name());
        continue;
      }
      activeCrops.put(material, descriptor);
    }
  }

  /// Returns whether a material is supported by FarmManager.
  ///
  /// @param material material to check
  /// @return true when the material has a built-in crop descriptor
  public static boolean isSupported(Material material) {
    return SUPPORTED_CROPS.containsKey(material);
  }

  /// Returns all supported crop materials.
  ///
  /// @return supported crop materials
  public static Collection<Material> getSupportedMaterials() {
    return SUPPORTED_CROPS.keySet();
  }

  /// Returns the descriptor for an active crop material.
  ///
  /// @param material block material to check
  /// @return active crop descriptor, or empty when unsupported or disabled
  public Optional<CropDescriptor> get(Material material) {
    return Optional.ofNullable(activeCrops.get(material));
  }

  /// Returns all active crop materials in deterministic configuration order.
  ///
  /// @return active crop materials
  public Set<Material> getActiveCropMaterials() {
    return new LinkedHashSet<>(activeCrops.keySet());
  }
}
