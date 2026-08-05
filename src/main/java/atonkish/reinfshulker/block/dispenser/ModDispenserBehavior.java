package atonkish.reinfshulker.block.dispenser;

import java.util.Map;

import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;

import atonkish.reinfshulker.item.ModItems;

public interface ModDispenserBehavior {
  // NOTE: the generic net.minecraft.block.dispenser.BlockPlacementDispenserBehavior used in
  // 1.21.11 no longer exists in 26.2; vanilla's own purpose-built
  // net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior (used for vanilla shulker boxes
  // themselves) is used here instead -- same or better placement behavior.
  public static void init() {
    for (Map<DyeColor, Item> materialShulkerBoxMap : ModItems.REINFORCED_SHULKER_BOX_MAP.values()) {
      for (Item item : materialShulkerBoxMap.values()) {
        DispenserBlock.registerBehavior(item, new ShulkerBoxDispenseBehavior());
      }
    }

    for (Map<DyeColor, Item> materialShulkerBoxMap :
        ModItems.HOPPER_REINFORCED_SHULKER_BOX_MAP.values()) {
      for (Item item : materialShulkerBoxMap.values()) {
        DispenserBlock.registerBehavior(item, new ShulkerBoxDispenseBehavior());
      }
    }
  }
}
