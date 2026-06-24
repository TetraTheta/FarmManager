package io.github.tetratheta.farmmanager.crop;

import org.bukkit.Material;

/// Describes one supported crop and the item consumed from its drops for replanting.
///
/// @param cropMaterial crop block material
/// @param replantItem  item material consumed from drops before delivery
public record CropDescriptor(Material cropMaterial, Material replantItem) {}
