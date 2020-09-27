package epicsquid.roots.event;

import com.google.common.collect.Sets;
import epicsquid.mysticalworld.MysticalWorld;
import epicsquid.roots.Roots;
import epicsquid.roots.config.GeneralConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticalWorld.MODID)
@SuppressWarnings("unused")
public class LootHandler {
  private static Set<ResourceLocation> tables = Sets.newHashSet(LootTableList.CHESTS_SIMPLE_DUNGEON, LootTableList.CHESTS_ABANDONED_MINESHAFT, LootTableList.CHESTS_DESERT_PYRAMID, LootTableList.CHESTS_JUNGLE_TEMPLE, LootTableList.CHESTS_WOODLAND_MANSION, LootTableList.CHESTS_STRONGHOLD_CORRIDOR, LootTableList.CHESTS_STRONGHOLD_CROSSING, LootTableList.CHESTS_STRONGHOLD_LIBRARY, LootTableList.CHESTS_END_CITY_TREASURE, LootTableList.CHESTS_NETHER_BRIDGE);

  @SubscribeEvent
  public static void onLootLoad(LootTableLoadEvent event) {
    if (GeneralConfig.InjectLoot) {
      RandomValueRange range = new RandomValueRange(GeneralConfig.InjectMinimum, GeneralConfig.InjectMaximum);
      if (tables.contains(event.getName())) {
        LootPool pool = new LootPool(new LootEntry[]{
            new LootEntryTable(new ResourceLocation(Roots.MODID, "chests/inject"), 1, 0, new LootCondition[0], "Roots")
        }, new LootCondition[]{}, range, range, "Roots");
        event.getTable().addPool(pool);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingDrop(LivingDropsEvent event) {
    DamageSource source = event.getSource();
    Entity entity = source.getTrueSource();
    if (entity instanceof EntityPlayer) {
      EntityLivingBase dying = event.getEntityLiving();
      NBTTagCompound data = dying.getEntityData();
      if (data.hasKey("magnetic_ticks", Constants.NBT.TAG_INT)) {
        int ticks = data.getInteger("magnetic_ticks");
        if (ticks < dying.ticksExisted || (dying.ticksExisted - ticks) < 5 * 20) {
          if (data.hasUniqueId("magnetic")) {
            UUID id = data.getUniqueId("magnetic");
            if (id != null && id.equals(entity.getUniqueID())) {
              for (EntityItem item : event.getDrops()) {
                item.setPosition(entity.posX, entity.posY, entity.posZ);
                entity.world.spawnEntity(item);
              }
              event.setCanceled(true);
            }
          }
        }
      }
    }
  }
}
