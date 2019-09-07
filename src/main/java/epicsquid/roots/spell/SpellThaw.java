package epicsquid.roots.spell;

import epicsquid.roots.init.HerbRegistry;
import epicsquid.roots.init.ModItems;
import epicsquid.roots.spell.modules.SpellModule;
import epicsquid.roots.util.RitualUtil;
import net.minecraft.block.BlockFarmland;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class SpellThaw extends SpellBase{

  public static String spellName = "spell_thaw";
  public static SpellThaw instance = new SpellThaw(spellName);

  public SpellThaw(String name) {
    super(name, TextFormatting.AQUA, 25F/255F, 1F, 235F/255F, 252F/255F, 166F/255F, 37F/255F);

    this.castType = EnumCastType.CONTINUOUS;
    this.cooldown = 20;

    addCost(HerbRegistry.getHerbByName("wildewheet"), 0.25F);

    addIngredients(
            new ItemStack(ModItems.bark_acacia),
            new ItemStack(Blocks.TORCH),
            new ItemStack(Blocks.TORCH),
            new ItemStack(ModItems.bark_acacia)
            //MAYBE sunflowers (?)
    );
  }

  @Override
  public boolean cast(EntityPlayer caster, List<SpellModule> modules) {

    BlockPos pos = RitualUtil.getRandomPosRadialXYZ(caster.getPosition(), 5, 2,  5);
    boolean applied = false;

      if (caster.world.getBlockState(pos).getBlock() == Blocks.SNOW_LAYER) {
        caster.world.setBlockToAir(pos);
        applied = true;
      }

      if (caster.world.getBlockState(pos).getBlock() == Blocks.SNOW || caster.world.getBlockState(pos).getBlock() == Blocks.ICE) {
        caster.world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
        applied = true;
      }

      if (caster.world.getBlockState(pos).getBlock() == Blocks.PACKED_ICE) {
        caster.world.setBlockState(pos, Blocks.ICE.getDefaultState(), 3);
        applied = true;
      }

      if ((caster.world.getBlockState(pos).getBlock() == Blocks.FARMLAND) && (caster.world.getBlockState(pos).getValue(BlockFarmland.MOISTURE) < 7)) {
        caster.world.setBlockState(pos, Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7), 3);
        applied = true;
      }

    return applied;
  }
}