package atonkish.reinfshulker.block.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import atonkish.reinfshulker.util.ReinforcingMaterialSettings;

/**
 * Item transfer for hopper-upgraded reinforced shulker boxes.
 *
 * <p>A vanilla hopper is hardcoded to pull from above and push along its own {@code FACING}. A
 * shulker box instead has one {@link ShulkerBoxBlock#FACING} property naming the face its lid opens
 * out of -- the box's own "top". This generalises vanilla's rule around that axis: the box pulls
 * from the neighbour on its lid side and pushes into the neighbour on the opposite ("bottom") side,
 * so a box placed lid-up behaves exactly like a hopper, and a box placed on a wall moves items
 * sideways.
 *
 * <p>The sided-access directions match vanilla's, verified against the decompiled 26.2 {@code
 * HopperBlockEntity}: {@code ejectItems} inserts through {@code facing.getOpposite()} (the target's
 * face pointing back at the hopper) and {@code suckInItems} takes through {@code DOWN} (the
 * source's face pointing back down at the hopper). Both are "the neighbour's face that points at
 * us", which is what {@link #push} and {@link #pull} compute.
 */
public final class ShulkerHopperTransfer {
  /**
   * Ticks between transfer operations, matching vanilla's hopper. Tiers scale throughput by moving
   * more items per operation rather than by running more often -- see {@link
   * ReinforcingMaterialSettings#getHopperTransferAmount()}.
   */
  public static final int COOLDOWN = 8;

  private ShulkerHopperTransfer() {}

  /**
   * Runs one transfer operation. Returns true if any item moved, in which case the caller should
   * arm the cooldown. Mirrors vanilla: when nothing moves, no cooldown is set and the box retries
   * next tick, so it reacts immediately once a neighbour has room or stock.
   */
  public static boolean transfer(
      Level level, BlockPos pos, BlockState state, ReinforcedShulkerBoxBlockEntity blockEntity) {
    // Vanilla hoppers stop while powered; keep that so these are still redstone-clockable.
    if (level.hasNeighborSignal(pos)) {
      return false;
    }

    ReinforcingMaterialSettings settings =
        ReinforcingMaterialSettings.byMaterial(blockEntity.getMaterial());
    if (settings == null) {
      return false;
    }

    Direction lid = state.getValue(ShulkerBoxBlock.FACING);
    int amount =
        settings.hopperMovesFullStack() ? Integer.MAX_VALUE : settings.getHopperTransferAmount();

    // Both run every operation: a box in the middle of a chain should drain and fill in the same
    // tick rather than alternating, which would halve a chain's throughput.
    boolean pushed = push(level, pos, lid, blockEntity, amount);
    boolean pulled = pull(level, pos, lid, blockEntity, amount);
    return pushed || pulled;
  }

  /** Moves items out of the box into the neighbour on its "bottom" (anti-lid) side. */
  private static boolean push(
      Level level,
      BlockPos pos,
      Direction lid,
      ReinforcedShulkerBoxBlockEntity blockEntity,
      int amount) {
    Direction out = lid.getOpposite();
    Container target = HopperBlockEntity.getContainerAt(level, pos.relative(out));
    if (target == null || target == blockEntity) {
      return false;
    }

    // The face of the target that points back at us, matching vanilla ejectItems'
    // facing.getOpposite().
    Direction insertFace = lid;

    for (int slot = 0; slot < blockEntity.getContainerSize(); slot++) {
      ItemStack stack = blockEntity.getItem(slot);
      if (stack.isEmpty()) {
        continue;
      }

      int count = Math.min(amount, stack.getCount());
      ItemStack taken = blockEntity.removeItem(slot, count);
      // addItem shrinks the stack it is handed and returns that same object, so the count has to
      // be captured first -- comparing the returned stack to `taken` would compare it to itself.
      int takenCount = taken.getCount();
      ItemStack leftover = HopperBlockEntity.addItem(blockEntity, target, taken, insertFace);

      // Put whatever the target refused back first, so a slot it rejects is left exactly as found.
      returnToSlot(blockEntity, slot, leftover);

      if (leftover.getCount() < takenCount) {
        target.setChanged();
        return true;
      }

      // Nothing moved from this slot. The target may still accept a different item -- a furnace
      // fuel slot, a filtered pipe -- so keep scanning instead of giving up on the whole box.
      // Matches vanilla ejectItems, which also continues past a rejected slot.
    }

    return false;
  }

  /** Moves items into the box from the neighbour on its lid side, or from dropped items there. */
  private static boolean pull(
      Level level,
      BlockPos pos,
      Direction lid,
      ReinforcedShulkerBoxBlockEntity blockEntity,
      int amount) {
    BlockPos sourcePos = pos.relative(lid);
    Container source = HopperBlockEntity.getContainerAt(level, sourcePos);

    if (source != null && source != blockEntity) {
      // The face of the source that points back at us, matching vanilla suckInItems' DOWN.
      Direction takeFace = lid.getOpposite();

      for (int slot : slotsFor(source, takeFace)) {
        ItemStack stack = source.getItem(slot);
        if (stack.isEmpty() || !canTakeThroughFace(source, stack, slot, takeFace)) {
          continue;
        }

        int count = Math.min(amount, stack.getCount());
        ItemStack taken = source.removeItem(slot, count);
        // See push(): capture the count before addItem shrinks the stack in place.
        int takenCount = taken.getCount();
        // Destination face is null: vanilla passes null when inserting into the hopper itself,
        // since a container never restricts what it accepts from its own transfer logic.
        ItemStack leftover = HopperBlockEntity.addItem(source, blockEntity, taken, null);

        returnToSlot(source, slot, leftover);

        if (leftover.getCount() < takenCount) {
          source.setChanged();
          return true;
        }

        // This box had no room for that item; another source slot may still hold something it can
        // take, so keep scanning (matches vanilla suckInItems).
      }

      return false;
    }

    return suckInItemEntities(level, sourcePos, blockEntity);
  }

  /** Picks up dropped items floating in the block space on the box's lid side. */
  private static boolean suckInItemEntities(
      Level level, BlockPos sourcePos, ReinforcedShulkerBoxBlockEntity blockEntity) {
    List<ItemEntity> entities =
        level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos), ItemEntity::isAlive);

    for (ItemEntity entity : entities) {
      if (HopperBlockEntity.addItem(blockEntity, entity)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Puts a rejected remainder back. {@link Container#removeItem} already split the stack off, so
   * the slot is either empty (put it back) or still holds the rest of the original stack (merge the
   * count back in).
   */
  private static void returnToSlot(Container container, int slot, ItemStack leftover) {
    if (leftover.isEmpty()) {
      return;
    }

    ItemStack current = container.getItem(slot);
    if (current.isEmpty()) {
      container.setItem(slot, leftover);
    } else {
      current.grow(leftover.getCount());
      container.setItem(slot, current);
    }
  }

  private static int[] slotsFor(Container container, Direction face) {
    if (container instanceof WorldlyContainer worldlyContainer) {
      return worldlyContainer.getSlotsForFace(face);
    }

    int[] slots = new int[container.getContainerSize()];
    for (int i = 0; i < slots.length; i++) {
      slots[i] = i;
    }
    return slots;
  }

  private static boolean canTakeThroughFace(
      Container container, ItemStack stack, int slot, @Nullable Direction face) {
    if (container instanceof WorldlyContainer worldlyContainer) {
      return worldlyContainer.canTakeItemThroughFace(slot, stack, face);
    }
    return true;
  }
}
