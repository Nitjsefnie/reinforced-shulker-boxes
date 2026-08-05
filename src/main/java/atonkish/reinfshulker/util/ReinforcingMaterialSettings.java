package atonkish.reinfshulker.util;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import atonkish.reinfcore.api.ReinforcedCoreRegistry;
import atonkish.reinfcore.util.ReinforcingMaterial;

public enum ReinforcingMaterialSettings {
  COPPER(
      ReinforcedCoreRegistry.registerReinforcingMaterial("copper", 45, Items.COPPER_INGOT),
      BlockBehaviour.Properties.of().strength(2.0F, 6.0F).sound(SoundType.COPPER),
      new Item.Properties(),
      1,
      false),
  IRON(
      ReinforcedCoreRegistry.registerReinforcingMaterial("iron", 54, Items.IRON_INGOT),
      BlockBehaviour.Properties.of()
          .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
          .strength(2.0F, 6.0F)
          .sound(SoundType.METAL),
      new Item.Properties(),
      2,
      false),
  GOLD(
      ReinforcedCoreRegistry.registerReinforcingMaterial("gold", 81, Items.GOLD_INGOT),
      BlockBehaviour.Properties.of()
          .instrument(NoteBlockInstrument.BELL)
          .strength(2.0F, 6.0F)
          .sound(SoundType.METAL),
      new Item.Properties(),
      4,
      false),
  DIAMOND(
      ReinforcedCoreRegistry.registerReinforcingMaterial("diamond", 108, Items.DIAMOND),
      BlockBehaviour.Properties.of().strength(2.0F, 6.0F).sound(SoundType.METAL),
      new Item.Properties(),
      8,
      false),
  NETHERITE(
      ReinforcedCoreRegistry.registerReinforcingMaterial("netherite", 108, Items.NETHERITE_INGOT),
      BlockBehaviour.Properties.of().strength(2.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK),
      new Item.Properties().fireResistant(),
      64,
      true);

  private static final Map<ReinforcingMaterial, ReinforcingMaterialSettings> BY_MATERIAL;

  private final ReinforcingMaterial material;
  private final BlockBehaviour.Properties blockSettings;
  private final Item.Properties itemSettings;
  private final int hopperTransferAmount;
  private final boolean hopperMovesFullStack;

  private ReinforcingMaterialSettings(
      ReinforcingMaterial material,
      BlockBehaviour.Properties blockSettings,
      Item.Properties itemSettings,
      int hopperTransferAmount,
      boolean hopperMovesFullStack) {
    // NOTE: ShulkerBoxBlockEntity.suffocates() no longer exists in 26.2; isClosed() is the
    // semantic replacement (a shulker box only blocks vision/suffocates while its lid is
    // shut).
    BlockBehaviour.StatePredicate contextPredicate =
        (state, world, pos) -> {
          BlockEntity blockEntity = world.getBlockEntity(pos);
          if (!(blockEntity instanceof ShulkerBoxBlockEntity)) {
            return true;
          }
          ShulkerBoxBlockEntity shulkerBoxBlockEntity = (ShulkerBoxBlockEntity) blockEntity;
          return shulkerBoxBlockEntity.isClosed();
        };

    this.material = material;
    this.blockSettings =
        blockSettings
            // NOTE: 26.2 removed the separate solid()/solidBlock(predicate) properties;
            // forceSolidOn() is the modern unified "always treat as a solid block" toggle.
            .forceSolidOn()
            .dynamicShape()
            .noOcclusion()
            .isSuffocating(contextPredicate)
            .isViewBlocking(contextPredicate)
            .pushReaction(PushReaction.DESTROY);
    this.itemSettings = itemSettings.stacksTo(1);
    this.hopperTransferAmount = hopperTransferAmount;
    this.hopperMovesFullStack = hopperMovesFullStack;
  }

  public ReinforcingMaterial getMaterial() {
    return this.material;
  }

  public BlockBehaviour.Properties getBlockSettings() {
    return this.blockSettings.mapColor(MapColor.COLOR_PURPLE);
  }

  public BlockBehaviour.Properties getColorBlockSettings(DyeColor color) {
    MapColor mapColor =
        switch (color) {
          case PURPLE -> MapColor.TERRACOTTA_PURPLE;
          default -> color.getMapColor();
        };

    return this.blockSettings.mapColor(mapColor);
  }

  public Item.Properties getItemSettings() {
    return this.itemSettings;
  }

  /**
   * Items a hopper-upgraded box of this material moves per transfer operation.
   *
   * <p>Every tier runs on the same vanilla-hopper cooldown ({@link
   * atonkish.reinfshulker.block.entity.ShulkerHopperTransfer#COOLDOWN} ticks) and scales throughput
   * by batch size rather than by ticking more often: a shorter cooldown would multiply per-tick
   * block-entity work for every placed box, while a bigger batch costs the same tick and moves
   * more.
   */
  public int getHopperTransferAmount() {
    return this.hopperTransferAmount;
  }

  /**
   * Whether a hopper-upgraded box of this material moves a whole stack per operation, ignoring
   * {@link #getHopperTransferAmount()}.
   */
  public boolean hopperMovesFullStack() {
    return this.hopperMovesFullStack;
  }

  public static ReinforcingMaterialSettings byMaterial(ReinforcingMaterial material) {
    return BY_MATERIAL.get(material);
  }

  static {
    Map<ReinforcingMaterial, ReinforcingMaterialSettings> byMaterial = new LinkedHashMap<>();
    for (ReinforcingMaterialSettings settings : values()) {
      byMaterial.put(settings.material, settings);
    }
    BY_MATERIAL = Map.copyOf(byMaterial);
  }
}
