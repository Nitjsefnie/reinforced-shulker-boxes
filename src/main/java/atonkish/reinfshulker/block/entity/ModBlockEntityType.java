package atonkish.reinfshulker.block.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

import atonkish.reinfcore.util.ReinforcingMaterial;
import atonkish.reinfshulker.block.ModBlocks;
import atonkish.reinfshulker.mixin.BlockEntityTypeAccessor;

public class ModBlockEntityType {
  public static final Map<ReinforcingMaterial, BlockEntityType<ReinforcedShulkerBoxBlockEntity>>
      REINFORCED_SHULKER_BOX_MAP = new LinkedHashMap<>();

  public static BlockEntityType<ReinforcedShulkerBoxBlockEntity> registerMaterial(
      String namespace, ReinforcingMaterial material) {
    if (!REINFORCED_SHULKER_BOX_MAP.containsKey(material)) {
      String id = material.getName() + "_shulker_box";
      // Hopper-upgraded boxes share the plain box's block entity type: they hold the same inventory
      // and render identically, and differ only in the block that owns them (which the ticker and
      // the block entity both read back off the block state). One type per material keeps the
      // renderer registration and the shulkerboxtooltip provider lookup unchanged.
      List<Block> blocks =
          new ArrayList<>(ModBlocks.REINFORCED_SHULKER_BOX_MAP.get(material).values());
      Map<DyeColor, Block> hopperBlocks = ModBlocks.HOPPER_REINFORCED_SHULKER_BOX_MAP.get(material);
      if (hopperBlocks != null) {
        blocks.addAll(hopperBlocks.values());
      }
      BlockEntityType<ReinforcedShulkerBoxBlockEntity> blockEntityType =
          Registry.register(
              BuiltInRegistries.BLOCK_ENTITY_TYPE,
              Identifier.fromNamespaceAndPath(namespace, id),
              new BlockEntityType<>(
                  (blockPos, blockState) ->
                      new ReinforcedShulkerBoxBlockEntity(material, blockPos, blockState),
                  Set.copyOf(blocks)));
      REINFORCED_SHULKER_BOX_MAP.put(material, blockEntityType);

      ((BlockEntityTypeAccessor) BlockEntityTypes.SHULKER_BOX).getValidBlocks().addAll(blocks);
    }

    return REINFORCED_SHULKER_BOX_MAP.get(material);
  }
}
