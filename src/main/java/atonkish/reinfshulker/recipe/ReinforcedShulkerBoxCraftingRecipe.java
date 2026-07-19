package atonkish.reinfshulker.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

// NOTE: 26.2 substantially reworked the recipe hierarchy: ShapedRecipe's constructor now
// takes Recipe.CommonInfo/CraftingRecipe.CraftingBookInfo records instead of separate
// group/category/showNotification params, RawShapedRecipe was renamed ShapedRecipePattern,
// the crafting result is now an ItemStackTemplate (not a plain ItemStack), craft() was
// renamed assemble(CraftingInput), and RecipeSerializer<T> became a final invariant-generic
// record (codec, streamCodec) instead of an interface with codec()/packetCodec() methods to
// implement. This class now extends NormalCraftingRecipe directly (instead of ShapedRecipe,
// as in 1.21.11): ShapedRecipe.getSerializer() locks its return type to the concrete
// RecipeSerializer<ShapedRecipe>, and RecipeSerializer's invariant generics mean a subclass
// cannot override that with RecipeSerializer<ReinforcedShulkerBoxCraftingRecipe> (unlike the
// old interface-based RecipeSerializer, which allowed free covariant narrowing).
// NormalCraftingRecipe.getSerializer() is still abstract with a wildcard bound
// (RecipeSerializer<? extends NormalCraftingRecipe>), so it can be overridden freely.
// One cosmetic gap from this: ShapedRecipe's recipe-book display() (showing the actual grid
// pattern) is no longer inherited; Recipe's default display() is used instead (an empty
// list), so this recipe won't show its shape in the recipe book. Functionally unaffected.
public class ReinforcedShulkerBoxCraftingRecipe extends NormalCraftingRecipe {
  public static final MapCodec<ReinforcedShulkerBoxCraftingRecipe> CODEC =
      RecordCodecBuilder.mapCodec(
          (instance) -> {
            return instance
                .group(
                    Codec.STRING
                        .optionalFieldOf("group", "")
                        .forGetter(
                            (recipe) -> {
                              return recipe.group();
                            }),
                    CraftingBookCategory.CODEC
                        .fieldOf("category")
                        .orElse(CraftingBookCategory.MISC)
                        .forGetter(
                            (recipe) -> {
                              return recipe.category();
                            }),
                    ShapedRecipePattern.MAP_CODEC.forGetter(
                        (recipe) -> {
                          return recipe.pattern;
                        }),
                    ItemStackTemplate.CODEC
                        .fieldOf("result")
                        .forGetter(
                            (recipe) -> {
                              return recipe.result;
                            }),
                    Codec.BOOL
                        .optionalFieldOf("show_notification", true)
                        .forGetter(
                            (recipe) -> {
                              return recipe.showNotification();
                            }))
                .apply(instance, ReinforcedShulkerBoxCraftingRecipe::new);
          });
  public static final StreamCodec<RegistryFriendlyByteBuf, ReinforcedShulkerBoxCraftingRecipe>
      STREAM_CODEC =
          StreamCodec.of(
              ReinforcedShulkerBoxCraftingRecipe::write, ReinforcedShulkerBoxCraftingRecipe::read);

  final ShapedRecipePattern pattern;
  final ItemStackTemplate result;

  public ReinforcedShulkerBoxCraftingRecipe(
      String group,
      CraftingBookCategory category,
      ShapedRecipePattern pattern,
      ItemStackTemplate result,
      boolean showNotification) {
    super(
        new Recipe.CommonInfo(showNotification),
        new CraftingRecipe.CraftingBookInfo(category, group));
    this.pattern = pattern;
    this.result = result;
  }

  public ReinforcedShulkerBoxCraftingRecipe(
      String group,
      CraftingBookCategory category,
      ShapedRecipePattern pattern,
      ItemStackTemplate result) {
    this(group, category, pattern, result, true);
  }

  @Override
  public RecipeSerializer<ReinforcedShulkerBoxCraftingRecipe> getSerializer() {
    return ModRecipeSerializer.REINFORCED_SHULKER_BOX;
  }

  @Override
  protected PlacementInfo createPlacementInfo() {
    return PlacementInfo.createFromOptionals(this.pattern.ingredients());
  }

  @Override
  public boolean matches(CraftingInput craftingInput, Level level) {
    return this.pattern.matches(craftingInput);
  }

  @Override
  public ItemStack assemble(CraftingInput craftingInput) {
    Item item = this.result.create().getItem();
    ItemStack itemStack = craftingInput.getItem(4);
    return itemStack.transmuteCopy(item, 1);
  }

  private static ReinforcedShulkerBoxCraftingRecipe read(RegistryFriendlyByteBuf buf) {
    String string = buf.readUtf();
    CraftingBookCategory craftingBookCategory = buf.readEnum(CraftingBookCategory.class);
    ShapedRecipePattern shapedRecipePattern = ShapedRecipePattern.STREAM_CODEC.decode(buf);
    ItemStackTemplate itemStackTemplate = ItemStackTemplate.STREAM_CODEC.decode(buf);
    boolean bl = buf.readBoolean();
    return new ReinforcedShulkerBoxCraftingRecipe(
        string, craftingBookCategory, shapedRecipePattern, itemStackTemplate, bl);
  }

  private static void write(
      RegistryFriendlyByteBuf buf, ReinforcedShulkerBoxCraftingRecipe recipe) {
    buf.writeUtf(recipe.group());
    buf.writeEnum(recipe.category());
    ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.pattern);
    ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.result);
    buf.writeBoolean(recipe.showNotification());
  }
}
