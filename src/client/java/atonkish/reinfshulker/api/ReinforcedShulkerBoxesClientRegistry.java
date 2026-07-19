package atonkish.reinfshulker.api;

import java.util.List;

import net.minecraft.client.resources.model.sprite.SpriteId;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import atonkish.reinfcore.util.ReinforcingMaterial;
import atonkish.reinfshulker.client.render.ModTexturedRenderLayers;

// NOTE: the @Deprecated registerMaterialAtlasTexture/registerMaterialRenderLayer methods
// (already unused internally) were dropped for this port: 26.2 replaced
// TexturedRenderLayers/RenderLayer's "get the render layer for a texture atlas" concept
// with the new extract/submit rendering pipeline (see ReinforcedShulkerBoxBlockEntityRenderer),
// so there is no direct equivalent to keep them pointing at.
@Environment(EnvType.CLIENT)
public class ReinforcedShulkerBoxesClientRegistry {
  public static SpriteId registerMaterialDefaultSprite(
      String namespace, ReinforcingMaterial material) {
    return ModTexturedRenderLayers.registerMaterialDefaultSprite(namespace, material);
  }

  public static List<SpriteId> registerMaterialColoringSprites(
      String namespace, ReinforcingMaterial material) {
    return ModTexturedRenderLayers.registerMaterialColoringSprites(namespace, material);
  }
}
