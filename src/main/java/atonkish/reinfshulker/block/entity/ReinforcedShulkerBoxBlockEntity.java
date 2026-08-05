package atonkish.reinfshulker.block.entity;

import java.util.stream.IntStream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import atonkish.reinfcore.screen.ReinforcedStorageScreenHandler;
import atonkish.reinfcore.util.ReinforcingMaterial;
import atonkish.reinfshulker.block.ReinforcedShulkerBoxBlock;
import atonkish.reinfshulker.mixin.BlockEntityAccessor;

public class ReinforcedShulkerBoxBlockEntity extends ShulkerBoxBlockEntity {
  private static final String COOLDOWN_KEY = "TransferCooldown";

  private final ReinforcingMaterial cachedMaterial;
  private int transferCooldown;

  public ReinforcedShulkerBoxBlockEntity(
      ReinforcingMaterial material, @Nullable DyeColor color, BlockPos pos, BlockState state) {
    super(color, pos, state);
    ((BlockEntityAccessor) this)
        .setType(ModBlockEntityType.REINFORCED_SHULKER_BOX_MAP.get(material));
    this.setItems(NonNullList.withSize(material.getSize(), ItemStack.EMPTY));
    this.cachedMaterial = material;
  }

  public ReinforcedShulkerBoxBlockEntity(
      ReinforcingMaterial material, BlockPos pos, BlockState state) {
    super(pos, state);
    ((BlockEntityAccessor) this)
        .setType(ModBlockEntityType.REINFORCED_SHULKER_BOX_MAP.get(material));
    this.setItems(NonNullList.withSize(material.getSize(), ItemStack.EMPTY));
    this.cachedMaterial = material;
  }

  /**
   * Ticker for every reinforced shulker box. Hopper-upgraded and plain boxes share one block entity
   * type per material (they differ only in which block owns them), so this runs vanilla's lid
   * animation for all of them and adds item transfer only for the hopper-upgraded blocks.
   *
   * <p>This intentionally hides {@link ShulkerBoxBlockEntity#tick} rather than overriding it --
   * block entity tickers are static method references, not virtual calls.
   */
  public static void tick(
      Level world, BlockPos pos, BlockState state, ReinforcedShulkerBoxBlockEntity blockEntity) {
    ShulkerBoxBlockEntity.tick(world, pos, state, blockEntity);

    if (world.isClientSide() || !(state.getBlock() instanceof ReinforcedShulkerBoxBlock block)) {
      return;
    }

    if (!block.isHopper()) {
      return;
    }

    if (blockEntity.transferCooldown > 0) {
      blockEntity.transferCooldown--;
      return;
    }

    if (ShulkerHopperTransfer.transfer(world, pos, state, blockEntity)) {
      blockEntity.transferCooldown = ShulkerHopperTransfer.COOLDOWN;
      blockEntity.setChanged();
    }
  }

  @Override
  protected void saveAdditional(ValueOutput view) {
    super.saveAdditional(view);
    if (this.transferCooldown > 0) {
      view.putInt(COOLDOWN_KEY, this.transferCooldown);
    }
  }

  @Override
  protected void loadAdditional(ValueInput view) {
    super.loadAdditional(view);
    this.transferCooldown = view.getIntOr(COOLDOWN_KEY, 0);
  }

  @Override
  protected Component getDefaultName() {
    String namespace = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()).getNamespace();
    String material = this.cachedMaterial.getName();
    String key =
        this.isHopper()
            ? "container." + namespace + ".hopper." + material + "ShulkerBox"
            : "container." + namespace + "." + material + "ShulkerBox";
    return Component.translatable(key);
  }

  /** Whether this box is a hopper-upgraded variant, i.e. whether it moves items on its own. */
  public boolean isHopper() {
    return this.getBlockState().getBlock() instanceof ReinforcedShulkerBoxBlock block
        && block.isHopper();
  }

  @Override
  public int[] getSlotsForFace(Direction side) {
    return IntStream.range(0, this.getContainerSize()).toArray();
  }

  public ReinforcingMaterial getMaterial() {
    return this.cachedMaterial;
  }

  @Override
  protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
    return ReinforcedStorageScreenHandler.createShulkerBoxScreen(
        this.cachedMaterial, syncId, playerInventory, this);
  }
}
