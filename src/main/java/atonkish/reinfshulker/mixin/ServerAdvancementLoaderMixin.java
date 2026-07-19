package atonkish.reinfshulker.mixin;

import java.util.ArrayList;
import java.util.Map;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import atonkish.reinfshulker.ReinforcedShulkerBoxesMod;

@Mixin(ServerAdvancementManager.class)
public class ServerAdvancementLoaderMixin {
  // NOTE: ServerAdvancementLoader was renamed ServerAdvancementManager in 26.2, and the
  // map passed to apply() now holds already-parsed Advancement objects (this reload
  // listener's earlier prepare() stage now does JSON parsing) instead of raw JsonElement;
  // the filtering here only inspects keys (Identifiers), so the value type change doesn't
  // affect this mixin's behavior.
  @Inject(method = "apply", at = @At("HEAD"))
  private void removeMissingIdentifier(
      Map<Identifier, Advancement> map,
      ResourceManager resourceManager,
      ProfilerFiller profiler,
      CallbackInfo info) {
    if (ReinforcedShulkerBoxesMod.IS_REINFCHEST_LOADED) {
      return;
    }

    ArrayList<Identifier> missingIdentifiers = new ArrayList<>();
    for (Identifier id : map.keySet()) {
      if (!id.getNamespace().equals(ReinforcedShulkerBoxesMod.MOD_ID)) {
        continue;
      }

      if (id.getPath().contains("chest")) {
        missingIdentifiers.add(id);
      }
    }

    missingIdentifiers.forEach(map::remove);
  }
}
