package com.pau101.paintthis;

import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableMap;
import com.pau101.paintthis.server.ServerProxy;
import com.pau101.paintthis.server.item.brush.ItemBrush;
import com.pau101.paintthis.server.item.brush.ItemPaintbrush;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = PaintThis.ID, name = PaintThis.NAME, version = PaintThis.VERSION)
public final class PaintThis {
	public static final String ID = "paintthis";

	public static final String NAME = "Paint This!";

	public static final String VERSION = "0.0.4";

	public static final int MAX_CANVAS_SIZE = 4;

	public static final int CANVAS_SIZES = MAX_CANVAS_SIZE * MAX_CANVAS_SIZE;

	@Instance(ID)
	public static PaintThis instance;

	@SidedProxy(clientSide = "com.pau101.paintthis.proxy.ClientProxy", serverSide = "com.pau101.paintthis.proxy.CommonProxy")
	public static ServerProxy proxy;

	public static SimpleNetworkWrapper network;

	public static CreativeTabs toolsTab;

	public static CreativeTabs dyesTab;

	public static Item easel;

	public static ItemPaintbrush paintbrushSmall;

	public static ItemPaintbrush paintbrushMedium;

	public static ItemPaintbrush paintbrushLarge;

	public static ImmutableMap<ItemPaintbrush.Size, ItemPaintbrush> paintbrushes;

	public static Item palette;

	public static Item canvas;

	public static Item dye;

	public static Item paletteKnife;

	public static ItemBrush signingBrush;

	public static Item horsehair;

	@EventHandler
	public void init(FMLPreInitializationEvent event) {
		proxy.initSounds();
		proxy.initGUI();
		proxy.initItems();
		proxy.initEntities();
		proxy.initRenders();
		proxy.initModels();
		proxy.initNetwork();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.initRendersLater();
		proxy.initCrafting();
		proxy.initHandlers();
	}

	public static void sendToWatchingEntity(Entity entity, IMessage message, EntityPlayerMP... exclusions) {
		WorldServer world = (WorldServer) entity.worldObj;
		for (EntityPlayerMP player : (Set<EntityPlayerMP>) world.getEntityTracker().getTrackingPlayers(entity)) {
			if (!ArrayUtils.contains(exclusions, player)) {
				network.sendTo(message, player);
			}
		}
		if (entity instanceof EntityPlayerMP && !ArrayUtils.contains(exclusions, entity)) {
			network.sendTo(message, (EntityPlayerMP) entity);
		}
	}
}
