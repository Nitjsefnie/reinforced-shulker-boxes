package atonkish.reinfshulker.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import atonkish.reinfshulker.block.ReinforcedShulkerBoxBlock;
import atonkish.reinfshulker.block.entity.ReinforcedShulkerBoxBlockEntity;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
  // NOTE: onBreak(World, BlockPos, BlockState, PlayerEntity) was renamed
  // playerWillDestroy(Level, BlockPos, BlockState, Player) in 26.2, and
  // ItemStack.applyComponentsFrom(ComponentMap) was renamed
  // applyComponents(DataComponentMap); verified via javap bytecode inspection of
  // playerWillDestroy that the local variable shape (BlockEntity, ShulkerBoxBlockEntity,
  // ItemStack captured after the 4 method params) is otherwise unchanged.
  @Inject(
      method = "playerWillDestroy",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/item/ItemStack;applyComponents(Lnet/minecraft/core/component/DataComponentMap;)V"),
      locals = LocalCapture.CAPTURE_FAILHARD)
  public void playerWillDestroy(
      Level world,
      BlockPos pos,
      BlockState state,
      Player player,
      CallbackInfoReturnable<BlockState> cir,
      BlockEntity blockEntity,
      ShulkerBoxBlockEntity shulkerBoxBlockEntity,
      ItemStack itemStack) {
    if (blockEntity instanceof ReinforcedShulkerBoxBlockEntity) {
      ReinforcedShulkerBoxBlockEntity entity = (ReinforcedShulkerBoxBlockEntity) blockEntity;
      ((ItemStackAccessor) (Object) itemStack)
          .setItem(
              ReinforcedShulkerBoxBlock.get(entity.getMaterial(), entity.getColor())
                  .asItem()
                  .builtInRegistryHolder());
    }
  }
}
