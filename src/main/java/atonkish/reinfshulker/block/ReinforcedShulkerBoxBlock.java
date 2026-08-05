package atonkish.reinfshulker.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import atonkish.reinfcore.util.ReinforcingMaterial;
import atonkish.reinfshulker.block.entity.ModBlockEntityType;
import atonkish.reinfshulker.block.entity.ReinforcedShulkerBoxBlockEntity;
import atonkish.reinfshulker.stat.ModStats;

public class ReinforcedShulkerBoxBlock extends ShulkerBoxBlock {
  private final ReinforcingMaterial material;
  private final boolean hopper;

  public ReinforcedShulkerBoxBlock(
      ReinforcingMaterial material, @Nullable DyeColor color, BlockBehaviour.Properties settings) {
    this(material, color, false, settings);
  }

  public ReinforcedShulkerBoxBlock(
      ReinforcingMaterial material,
      @Nullable DyeColor color,
      boolean hopper,
      BlockBehaviour.Properties settings) {
    super(color, settings);
    this.material = material;
    this.hopper = hopper;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ReinforcedShulkerBoxBlockEntity(this.material, this.getColor(), pos, state);
  }

  @Override
  @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level world, BlockState state, BlockEntityType<T> type) {
    return BaseEntityBlock.createTickerHelper(
        type,
        ModBlockEntityType.REINFORCED_SHULKER_BOX_MAP.get(this.material),
        ReinforcedShulkerBoxBlockEntity::tick);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    if (world instanceof ServerLevel serverWorld) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
        if (canOpen(state, world, pos, shulkerBoxBlockEntity)) {
          player.openMenu(shulkerBoxBlockEntity);
          player.awardStat(ModStats.OPEN_REINFORCED_SHULKER_BOX_MAP.get(this.material));
          PiglinAi.angerNearbyPiglins(serverWorld, player, true);
        }
      }
    }

    return InteractionResult.SUCCESS;
  }

  private static boolean canOpen(
      BlockState state, Level world, BlockPos pos, ShulkerBoxBlockEntity entity) {
    if (entity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
      return true;
    } else {
      AABB box =
          Shulker.getProgressDeltaAabb(
                  1.0F, state.getValue(FACING), 0.0F, 0.5F, Vec3.atBottomCenterOf(pos))
              .deflate(1.0E-6D);
      return world.noCollision(box);
    }
  }

  public static Block get(ReinforcingMaterial material, @Nullable DyeColor color) {
    return ModBlocks.REINFORCED_SHULKER_BOX_MAP.get(material).get(color);
  }

  public static Block get(ReinforcingMaterial material, @Nullable DyeColor color, boolean hopper) {
    return hopper
        ? ModBlocks.HOPPER_REINFORCED_SHULKER_BOX_MAP.get(material).get(color)
        : ModBlocks.REINFORCED_SHULKER_BOX_MAP.get(material).get(color);
  }

  public ReinforcingMaterial getMaterial() {
    return this.material;
  }

  /** Whether this block moves items on its own, i.e. whether it has been hopper-upgraded. */
  public boolean isHopper() {
    return this.hopper;
  }

  public static ItemStack getItemStack(ReinforcingMaterial material, @Nullable DyeColor color) {
    return new ItemStack(get(material, color));
  }
}
