package epicsquid.roots.spell;

import epicsquid.mysticallib.util.ListUtil;
import epicsquid.roots.Roots;
import epicsquid.roots.api.Herb;
import epicsquid.roots.entity.spell.EntitySpellBase;
import epicsquid.roots.init.HerbRegistry;
import epicsquid.roots.init.ModItems;
import epicsquid.roots.modifiers.*;
import epicsquid.roots.modifiers.instance.base.BaseModifierInstanceList;
import epicsquid.roots.modifiers.instance.library.LibraryModifierInstance;
import epicsquid.roots.modifiers.instance.library.LibraryModifierInstanceList;
import epicsquid.roots.modifiers.instance.staff.StaffModifierInstance;
import epicsquid.roots.modifiers.instance.staff.StaffModifierInstanceList;
import epicsquid.roots.properties.Property;
import epicsquid.roots.properties.PropertyTable;
import epicsquid.roots.recipe.IRootsRecipe;
import epicsquid.roots.spell.info.StaffSpellInfo;
import epicsquid.roots.spell.info.storage.DustSpellStorage;
import epicsquid.roots.tileentity.TileEntityMortar;
import epicsquid.roots.util.ClientHerbUtil;
import epicsquid.roots.util.ServerHerbUtil;
import epicsquid.roots.util.types.RegistryItem;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public abstract class SpellBase extends RegistryItem implements SpellMulitipliers {
  protected PropertyTable properties = new PropertyTable();

  private boolean finalised = false;

  private float red1, green1, blue1;
  private float red2, green2, blue2;
  private String name;
  protected int cooldown = 20;
  protected boolean disabled = false;

  private TextFormatting textColor;
  protected EnumCastType castType = EnumCastType.INSTANTANEOUS;
  private Object2DoubleOpenHashMap<Herb> costs = new Object2DoubleOpenHashMap<>();
  private List<Modifier> acceptedModifiers = new ArrayList<>();
  private float[] firstColours;
  private float[] secondColours;

  protected IModifier poison, fire, slow, paralysis, peaceful;

  protected Buff speedy;
  protected Buff amplifier;

  public SpellRecipe recipe = SpellRecipe.EMPTY;

  public enum EnumCastType {
    INSTANTANEOUS, CONTINUOUS
  }

  public SpellBase(ResourceLocation name, TextFormatting textColor, float r1, float g1, float b1, float r2, float g2, float b2) {
    setRegistryName(name);
    defaultModifiers();
    this.name = name.getPath();
    this.red1 = r1;
    this.green1 = g1;
    this.blue1 = b1;
    this.red2 = r2;
    this.green2 = g2;
    this.blue2 = b2;
    this.textColor = textColor;
    this.firstColours = new float[]{r1, g1, b1};
    this.secondColours = new float[]{r2, g2, b2};
  }

  public void setPoison(IModifier poison) {
    this.poison = poison;
  }

  public void setFire(IModifier fire) {
    this.fire = fire;
  }

  public void setSlow(IModifier slow) {
    this.slow = slow;
  }

  public void setParalysis(IModifier paralysis) {
    this.paralysis = paralysis;
  }

  public void setPeaceful(IModifier peaceful) {
    this.peaceful = peaceful;
  }

  protected boolean has(IModifier modifier, StaffModifierInstanceList modifiers) {
    if (modifier == null) {
      return false;
    }
    StaffModifierInstance instance = modifiers.get(modifier);
    if (instance == null) {
      return false;
    }
    if (instance.isApplied() && instance.isEnabled()) {
      return true;
    }

    return false;
  }

  public boolean poison (StaffModifierInstanceList modifiers) {
    return has(poison, modifiers);
  }

  public boolean fire(StaffModifierInstanceList modifiers) {
    return has(fire, modifiers);
  }

  public boolean slow(StaffModifierInstanceList modifiers) {
    return has(slow, modifiers);
  }

  public boolean paralysis(StaffModifierInstanceList modifiers) {
    return has(paralysis, modifiers);
  }

  public boolean peaceful(StaffModifierInstanceList modifiers) {
    return has(peaceful, modifiers);
  }

  public float[] getFirstColours() {
    return firstColours;
  }

  public float[] getSecondColours() {
    return secondColours;
  }

  public abstract void init();

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public PropertyTable getProperties() {
    return properties;
  }

  public SpellBase acceptsModifiers(Modifier... modules) {
    acceptedModifiers.addAll(Arrays.asList(modules));
    return this;
  }

  public List<Modifier> getModifiers() {
    return acceptedModifiers;
  }

  public void setRecipe(SpellRecipe recipe) {
    this.recipe = recipe;
  }

  public SpellBase addIngredients(Object... stacks) {
    this.recipe = new SpellRecipe(stacks);
    return this;
  }

  public boolean costsMet(EntityPlayer player, StaffModifierInstanceList modifiers) {
    boolean matches = true;
    for (Map.Entry<Herb, Double> entry : modifiers.apply(this.costs).entrySet()) {
      Herb herb = entry.getKey();
      double d = entry.getValue();
      if (matches) {
        double r;
        if (!player.world.isRemote) {
          r = ServerHerbUtil.getHerbAmount(player, herb);
        } else {
          r = ClientHerbUtil.getHerbAmount(herb);
        }
        matches = r >= d;
        if (!matches && !player.isCreative()) {
          if (r == -1.0) {
            if (!player.world.isRemote) {
              player.sendStatusMessage(new TextComponentTranslation("roots.info.pouch.no_pouch").setStyle(new Style().setColor(TextFormatting.RED)), true);
            }
          } else {
            if (!player.world.isRemote) {
              player.sendStatusMessage(new TextComponentTranslation("roots.info.pouch.no_herbs", new TextComponentTranslation(String.format("item.%s.name", herb.getName()))), true);
            }
          }
        }
      }
    }
    return matches && costs.size() > 0 || player.capabilities.isCreativeMode;
  }

  public void enactCosts(EntityPlayer player, StaffModifierInstanceList modifiers) {
    for (Map.Entry<Herb, Double> entry : modifiers.apply(this.costs).entrySet()) {
      Herb herb = entry.getKey();
      double d = entry.getValue();
      ServerHerbUtil.removePowder(player, herb, d);
    }
  }

  public void enactTickCosts(EntityPlayer player, StaffModifierInstanceList modifiers) {
    for (Map.Entry<Herb, Double> entry : modifiers.apply(this.costs).entrySet()) {
      Herb herb = entry.getKey();
      double d = entry.getValue();
      ServerHerbUtil.removePowder(player, herb, d / 20.0);
    }
  }

  @SideOnly(Side.CLIENT)
  public void addToolTipBase(List<String> tooltip, @Nullable BaseModifierInstanceList<?> list) {
    Object2DoubleOpenHashMap<Herb> costs = this.costs;
    if (list != null) {
      costs = list.apply(costs);
    }
    String prefix = "roots.spell." + name;
    tooltip.add("" + textColor + TextFormatting.BOLD + I18n.format(prefix + ".name") + TextFormatting.RESET);
    if (finalised()) {
      for (Map.Entry<Herb, Double> entry : costs.entrySet()) {
        Herb herb = entry.getKey();
        String d = String.format("%.4f", entry.getValue());
        tooltip.add(I18n.format(herb.getItem().getTranslationKey() + ".name") + I18n.format("roots.tooltip.pouch_divider") + d);
      }
    }
  }

  @SideOnly(Side.CLIENT)
  public void addToolTip(List<String> tooltip, @Nullable LibraryModifierInstanceList list) {
    addToolTipBase(tooltip, list);
    if (list != null) {
      if (!list.isEmpty()) {
        tooltip.add("");
      }
      for (LibraryModifierInstance modifier : list) {
        if (modifier.isApplied()) {
          tooltip.add(modifier.describeName());
        }
      }
    }
  }

  @SideOnly(Side.CLIENT)
  public void addToolTip(List<String> tooltip, @Nullable StaffModifierInstanceList list) {
    addToolTipBase(tooltip, list);
    StringJoiner basics = new StringJoiner(", ");
    if (list != null) {
      double addition = 0;
      double subtraction = 0;
      if (!GuiScreen.isShiftKeyDown()) {
        StringJoiner joiner = new StringJoiner(", ");
        for (StaffModifierInstance m : list) {
          if (!m.isApplied() || !m.isEnabled()) {
            continue;
          }

          for (IModifierCost c : m.getCosts()) {
            if (c.getCost() == CostType.ALL_COST_MULTIPLIER) {
              if (c.getValue() < 0) {
                subtraction += Math.abs(c.getValue());
              } else {
                addition += Math.abs(c.getValue());
              }
            }
          }
          if (m.isBasic()) {
            basics.add(m.describe());
          } else {
            joiner.add(m.describe());
          }
        }

        String result = joiner.toString();
        if (!result.isEmpty()) {
          tooltip.add(result);
        }
        if (GuiScreen.isShiftKeyDown()) {
          result = basics.toString();
          if (!result.isEmpty()) {
            tooltip.add(result);
          }
        }
      } else {
        for (StaffModifierInstance m : list) {
          if (!m.isApplied() || !m.isEnabled()) {
            continue;
          }

          for (IModifierCost c : m.getCosts()) {
            if (c.getCost() == CostType.ALL_COST_MULTIPLIER) {
              if (c.getValue() < 0) {
                subtraction += Math.abs(c.getValue());
              } else {
                addition += Math.abs(c.getValue());
              }
            }
          }
          if (m.isBasic()) {
            basics.add(m.describe());
          } else {
            tooltip.add(m.describe());
          }
        }
        tooltip.add(basics.toString());
      }
      double actualSub = subtraction - addition;
      double actualAdd = addition - subtraction;
      if (actualSub > 0) {
        tooltip.add(I18n.format("roots.tooltip.reduced_by", Math.floor(actualSub * 100) + "%"));
      }
      if (actualAdd > 0) {
        tooltip.add(I18n.format("roots.tooltip.increased_by", Math.floor(actualAdd * 100) + "%"));
      }
    }
  }

  private SpellBase addCost(SpellCost cost) {
    return addCost(cost.getHerb(), cost.getCost());
  }

  private SpellBase addCost(Herb herb, double amount) {
    if (herb == null) {
      System.out.println("Spell - " + this.getClass().getName() + " - added a null herb ingredient. This is a bug.");
      return this;
    }
    costs.put(herb, amount);
    return this;
  }

  public boolean matchesIngredients(List<ItemStack> ingredients) {
    return ListUtil.matchesIngredients(ingredients, this.getIngredients());
  }

  public enum CastResult {
    FAIL,
    SUCCESS,
    SUCCESS_SPEEDY,
    SUCCESS_GREATER_SPEEDY;

    public boolean isSuccess() {
      return this != FAIL;
    }

    public long modifyCooldown(long cooldown) {
      switch (this) {
        default:
        case SUCCESS:
          return 0;
        case SUCCESS_SPEEDY:
          return Math.round(cooldown * 0.1);
        case SUCCESS_GREATER_SPEEDY:
          return Math.round(cooldown * 0.3);
      }
    }
  }

  public enum Buff {
    NONE, BONUS, GREATER_BONUS;
  }

  @Override
  public double getAmplifyValue() {
    switch (amplifier) {
      default:
      case NONE:
        return 0;
      case BONUS:
        return 0.1;
      case GREATER_BONUS:
        return 0.3;
    }
  }

  @Override
  public double getSpeedValue() {
    switch (speedy) {
      default:
      case NONE:
        return 0;
      case BONUS:
        return 0.1;
      case GREATER_BONUS:
        return 0.3;
    }
  }

  public CastResult cast(EntityPlayer caster, StaffSpellInfo info, int ticks) {
    StaffModifierInstanceList modifiers = info.getModifiers();
    amplifier = Buff.NONE;
    StaffModifierInstance mod = modifiers.get(BaseModifiers.EMPOWER);
    if (mod != null && mod.isEnabled()) {
      amplifier = Buff.BONUS;
    }
    mod = modifiers.get(BaseModifiers.GREATER_EMPOWER);
    if (mod != null && mod.isEnabled()) {
      amplifier = Buff.GREATER_BONUS;
    }
    speedy = Buff.NONE;
    mod = modifiers.get(BaseModifiers.SPEEDY);
    if (mod != null && mod.isEnabled()) {
      speedy = Buff.BONUS;
    }
    mod = modifiers.get(BaseModifiers.GREATER_SPEEDY);
    if (mod != null && mod.isEnabled()) {
      speedy = Buff.GREATER_BONUS;
    }

    CastResult result = CastResult.FAIL;

    if (cast(caster, info.getModifiers(), ticks)) {
      if (speedy.equals(Buff.NONE)) {
        result = CastResult.SUCCESS;
      } else if (speedy.equals(Buff.BONUS)) {
        result = CastResult.SUCCESS_SPEEDY;
      } else {
        result = CastResult.SUCCESS_GREATER_SPEEDY;
      }
    }

    speedy = Buff.NONE;
    amplifier = Buff.NONE;

    return result;
  }

  protected abstract boolean cast(EntityPlayer caster, StaffModifierInstanceList modifiers, int ticks);

  public float getRed1() {
    return red1;
  }

  public float getGreen1() {
    return green1;
  }

  public float getBlue1() {
    return blue1;
  }

  public float getRed2() {
    return red2;
  }

  public float getGreen2() {
    return green2;
  }

  public float getBlue2() {
    return blue2;
  }

  public String getName() {
    return name;
  }

  public int getCooldown() {
    return cooldown;
  }

  public TextFormatting getTextColor() {
    return textColor;
  }

  public EnumCastType getCastType() {
    return castType;
  }

  public Object2DoubleOpenHashMap<Herb> getCosts() {
    return costs;
  }

  public List<Ingredient> getIngredients() {
    return recipe.getIngredients();
  }

  public ItemStack getResult() {
    ItemStack stack = new ItemStack(ModItems.spell_dust);
    DustSpellStorage.fromStack(stack).setSpellToSlot(this);
    return stack;
  }

  public List<ItemStack> getCostItems() {
    return costs.keySet().stream().map((herb) -> new ItemStack(herb.getItem())).collect(Collectors.toList());
  }

  public abstract void doFinalise();

  @SuppressWarnings("unchecked")
  public void finaliseCosts() {
    for (Map.Entry<String, Property<?>> entry : getProperties()) {
      if (!entry.getKey().startsWith("cost_")) {
        continue;
      }

      Property<SpellCost> prop = (Property<SpellCost>) entry.getValue();
      SpellCost cost = properties.get(prop);

      if (cost != null) {
        addCost(cost);
      }
    }
    this.finalised = true;
  }

  public void defaultModifiers() {
    acceptsModifiers(BaseModifiers.EMPOWER, BaseModifiers.GREATER_EMPOWER, BaseModifiers.SPEEDY, BaseModifiers.GREATER_SPEEDY, BaseModifiers.REDUCTION, BaseModifiers.GREATER_REDUCTION);
  }

  public void finalise() {
    doFinalise();
    finaliseCosts();
    validateProperties();
  }

  public void validateProperties() {
    List<String> values = properties.finalise();
    if (!values.isEmpty()) {
      StringJoiner join = new StringJoiner(",");
      values.forEach(join::add);
      Roots.logger.error("Spell '" + name + "' property table has the following keys inserted but not fetched: |" + join.toString() + "|");
    }
  }

  public boolean finalised() {
    return finalised;
  }

  @Nullable
  protected EntitySpellBase spawnEntity(World world, BlockPos pos, Class<? extends EntitySpellBase> entity, @Nullable EntityPlayer player, double amplifier, double speedy) {
    return spawnEntity(world, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), entity, player, amplifier, speedy);
  }

  @Nullable
  protected EntitySpellBase spawnEntity(World world, Vec3d pos, Class<? extends EntitySpellBase> entity, @Nullable EntityPlayer player, double amplifier, double speedy) {
    List<EntitySpellBase> pastRituals = world.getEntitiesWithinAABB(entity, new AxisAlignedBB(pos.x, pos.y, pos.z - 100, pos.x + 1, pos.y + 100, pos.z + 1), o -> o != null && o.getClass().equals(entity));
    if (pastRituals.isEmpty() && !world.isRemote) {
      EntitySpellBase spell = null;
      try {
        Constructor<? extends EntitySpellBase> cons = entity.getDeclaredConstructor(World.class);
        spell = cons.newInstance(world);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
        e.printStackTrace();
      }
      if (spell == null) {
        return null;
      }
      spell.setPosition(pos.x, pos.y, pos.z);
      if (player != null) {
        spell.setPlayer(player.getUniqueID());
      }
      spell.setAmplifier(amplifier);
      spell.setSpeedy(speedy);
      world.spawnEntity(spell);
      return spell;
    }
    return null;
  }

  public static class SpellCost {
    public static SpellCost EMPTY = new SpellCost(null, 0);

    private String herb;
    private double cost;

    public SpellCost(String herb, double cost) {
      this.herb = herb;
      this.cost = cost;
    }

    public Herb getHerb() {
      return HerbRegistry.getHerbByName(herb);
    }

    public double getCost() {
      return cost;
    }

    @Override
    public String toString() {
      return "SpellCost{" +
          "herb='" + herb + '\'' +
          ", cost=" + cost +
          '}';
    }
  }

  public static class SpellRecipe implements IRootsRecipe<TileEntityMortar> {
    public static SpellRecipe EMPTY = new SpellRecipe();

    private List<Ingredient> ingredients = new ArrayList<>();

    public SpellRecipe(Object... stacks) {
      for (Object stack : stacks) {
        if (stack instanceof Ingredient) {
          ingredients.add((Ingredient) stack);
        } else if (stack instanceof ItemStack) {
          ingredients.add(Ingredient.fromStacks((ItemStack) stack));
        }
      }
    }

    @Override
    public List<Ingredient> getIngredients() {
      return ingredients;
    }
  }
}