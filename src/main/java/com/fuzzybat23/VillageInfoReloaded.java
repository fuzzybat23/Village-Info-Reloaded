package com.fuzzybat23;


import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.UUID;

@net.minecraftforge.fml.common.Mod(modid = "villageinforeloaded", version = "1.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.12, 1.13]")
public class VillageInfoReloaded
{
    @net.minecraftforge.fml.common.Mod.Instance("VillageInfoReloaded")
    public static final String MODID = "villageinforeloaded";

    @Mod.Instance
    public static VillageInfoReloaded instance;

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void preInit(FMLPreInitializationEvent event)
    {
        ModMetadata data = event.getModMetadata();
        data.autogenerated = false;
        data.version = "1.0";
        data.name = "Village Info Reloaded";
        data.description = "Displays nearby village info.";
        data.authorList.add("Fuzzybat23");
        data.url = "https://minecraft.curseforge.com/members/fuzzybat23/projects";
        data.credits = "Bats everywhere.  Got this idea from the original Village Info mod, which hadn't been updated for 1.12";
        data.logoFile = "assets/logo/logo.png";

        FMLCommonHandler.instance().bus().register(instance);
        MinecraftForge.EVENT_BUS.register(instance);
    }

    @SubscribeEvent
    public void onRenderTextOverlay(RenderGameOverlayEvent.Text event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if(mc.gameSettings.showDebugInfo)
            getVillageData(event, player);
    }

    private UUID findUUID(String name, EntityPlayer player)
    {
        World world = player.getEntityWorld();
        if (world == null || world.getMinecraftServer() == null)
            return EntityPlayer.getOfflineUUID(name);
        GameProfile profile = world.getMinecraftServer().getPlayerProfileCache().getGameProfileForUsername(name);
        return profile == null ? EntityPlayer.getOfflineUUID(name) : profile.getId();
    }

    private void getVillageData(RenderGameOverlayEvent.Text event, EntityPlayer player)
    {
        if(player == null)
            return;

        WorldServer worldServer = DimensionManager.getWorld(player.dimension);
        Village village = worldServer.getVillageCollection().getNearestVillage(player.getPosition(), 5000);

        if(village == null)
        {
            event.getLeft().add("");
            event.getLeft().add("No nearby villages found..");
            return;
        }

        // data variables

        BlockPos center = village.getCenter();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        int radius = village.getVillageRadius();
        int numDoors = village.getNumVillageDoors();
        int numVillagers = village.getNumVillagers();
        int numGolems = getNumGolems(worldServer, village, radius, centerX, centerY, centerZ);
        int reputation = village.getPlayerReputation(findUUID(player.getName(), player));
        double distanceFromVillage = village.getCenter().getDistance((int)player.posX, (int)player.posY, (int)player.posZ);
        boolean areHorny = village.isMatingSeason();

        // color formatting

        TextFormatting houseColor = (numDoors > 20 ? TextFormatting.GREEN : TextFormatting.RED);
        TextFormatting repColor = (reputation <= -15 ? TextFormatting.RED : TextFormatting.GREEN);
        TextFormatting hornyColor1 = (areHorny ? TextFormatting.GREEN : TextFormatting.WHITE);
        TextFormatting hornyColor2 = (areHorny ? TextFormatting.WHITE : TextFormatting.RED);
        TextFormatting white = TextFormatting.WHITE;
        TextFormatting reset = TextFormatting.RESET;

        // add text to debug

        event.getLeft().add("");
        if((int)distanceFromVillage > 1000)
            event.getLeft().add("There are no nearby villages.");

        if((int)distanceFromVillage > 160 && (int)distanceFromVillage <= 1000)
            event.getLeft().add("There is a distant village.");

        if ((int)distanceFromVillage > radius && (int)distanceFromVillage <= 160)
        {
            event.getLeft().add("There is a " + white + "nearby" + reset + " village.");
            event.getLeft().add("Location: (" + white + centerX + reset + ", " + white + centerY + reset + ", " + white + centerZ + reset +"), Radius: " + white + radius);
            event.getLeft().add("Distance (from center): " + white + (int)distanceFromVillage);
        }

        if ((int)distanceFromVillage <= 32)
        {
            event.getLeft().add("Currently " + white + "inside" + reset + " a Village.");
            event.getLeft().add("Location: (" + white + centerX + reset + ", " + white + centerY + reset + ", " + white + centerZ + reset + "), Radius: " + white + radius);
            event.getLeft().add("Distance (from center): " + white + (int) distanceFromVillage);
            event.getLeft().add("Total number of houses: " + houseColor + numDoors);
            event.getLeft().add("Total number of villagers: " + white + numVillagers + reset + " (" + ((int) (numDoors * 0.35D)) + ")");
            event.getLeft().add("Total number of iron golems: " + white + numGolems + reset + " (" + (numVillagers / 10) + ")");
            event.getLeft().add("Your reputation with the village is: " + repColor + reputation);
            event.getLeft().add("Villages are horny right now. (" + hornyColor1 + "Yes" + reset + "/" + hornyColor2 + "No" + reset + ")");
            event.getLeft().add("Currently " + white + (isGolemSpawnArea(player, center) ? "inside" : "outside") + reset + " Iron Golem spawning area.");
        }
    }

    private static int getNumGolems(WorldServer worldServer, Village village, int radius, int x, int y, int z)
    {
        if((village != null) && (worldServer != null))
        {
            List list = worldServer.getEntitiesWithinAABB(EntityIronGolem.class, new AxisAlignedBB(x - radius, y - 4, z - radius, x + radius, y + 4, z + radius ));
            return list.size();
        }
        return 0;
    }

    private boolean isGolemSpawnArea(EntityPlayer player, BlockPos center)
    {
        return !((MathHelper.floor(player.posX) > center.getX() + 7) || (MathHelper.floor(player.posX) < center.getX() - 8) ||
                (MathHelper.floor(player.getEntityBoundingBox().minY) > center.getY() + 2) || (MathHelper.floor(player.getEntityBoundingBox().minY) < center.getY() - 3) ||
                (MathHelper.floor(player.posZ) > center.getZ() + 7) || (MathHelper.floor(player.posZ) < center.getZ() - 8));
    }
}
