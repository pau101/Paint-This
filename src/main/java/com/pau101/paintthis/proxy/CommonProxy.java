package com.pau101.paintthis.proxy;

import java.util.Locale;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.creativetab.CreativeTabsPaintThisDyes;
import com.pau101.paintthis.creativetab.CreativeTabsPaintThisTools;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.entity.item.EntityEasel;
import com.pau101.paintthis.item.ItemCanvas;
import com.pau101.paintthis.item.ItemEasel;
import com.pau101.paintthis.item.ItemPaintDye;
import com.pau101.paintthis.item.ItemPalette;
import com.pau101.paintthis.item.ItemPaletteKnife;
import com.pau101.paintthis.item.brush.ItemPaintbrush;
import com.pau101.paintthis.item.brush.ItemSigningBrush;
import com.pau101.paintthis.item.crafting.PaintThisRecipe;
import com.pau101.paintthis.item.crafting.recipes.RecipeCanvas;
import com.pau101.paintthis.item.crafting.recipes.RecipeCanvasExtend;
import com.pau101.paintthis.item.crafting.recipes.RecipeDerivePaletteAddDye;
import com.pau101.paintthis.item.crafting.recipes.RecipeDerivePaletteRemoveDye;
import com.pau101.paintthis.item.crafting.recipes.RecipePainting;
import com.pau101.paintthis.network.SelfProcessingMessage;
import com.pau101.paintthis.network.client.MessagePainterPainting;
import com.pau101.paintthis.network.client.MessageSignPainting;
import com.pau101.paintthis.network.server.MessageUpdatePainting;
import com.pau101.paintthis.network.server.MessageUpdateSign;
import com.pau101.paintthis.painting.Painting;
import com.pau101.paintthis.property.HorseShearability;
import com.pau101.paintthis.util.DyeOreDictHelper;

public class CommonProxy {
	private static final String RECIPE_DEPENDENCY = "after:forge:shapedore";

	private int nextEntityId;

	private int nextMessageId;

	public void initGUI() {
		PaintThis.toolsTab = new CreativeTabsPaintThisTools();
		PaintThis.dyesTab = new CreativeTabsPaintThisDyes();
	}

	public void initItems() {
		PaintThis.easel = register(new ItemEasel(), "easel");
		PaintThis.canvas = register(new ItemCanvas(), "canvas");
		PaintThis.paintbrushSmall = registerPaintbrush(ItemPaintbrush.Size.SMALL);
		PaintThis.paintbrushMedium = registerPaintbrush(ItemPaintbrush.Size.MEDIUM);
		PaintThis.paintbrushLarge = registerPaintbrush(ItemPaintbrush.Size.LARGE);
		PaintThis.paintbrushes = Maps.immutableEnumMap(ImmutableMap.<ItemPaintbrush.Size, ItemPaintbrush> builder().put(ItemPaintbrush.Size.SMALL, PaintThis.paintbrushSmall).put(ItemPaintbrush.Size.MEDIUM, PaintThis.paintbrushMedium).put(ItemPaintbrush.Size.LARGE, PaintThis.paintbrushLarge).build());
		PaintThis.palette = register(new ItemPalette(), "palette");
		PaintThis.paletteKnife = register(new ItemPaletteKnife(), "palette_knife");
		PaintThis.dye = register(new ItemPaintDye(), "dye", PaintThis.dyesTab);
		PaintThis.signingBrush = register(new ItemSigningBrush(), "signing_brush");
		PaintThis.horsehair = register(new Item().setUnlocalizedName("horsehair"), "horsehair");
		OreDictionary.registerOre("dye", new ItemStack(PaintThis.dye, 1, OreDictionary.WILDCARD_VALUE));
	}

	private ItemPaintbrush registerPaintbrush(ItemPaintbrush.Size size) {
		return register(new ItemPaintbrush(size), "paintbrush_" + size.getName());
	}

	public void initCrafting() {
		GameRegistry.addRecipe(new ShapedOreRecipe(PaintThis.easel, " S ", "SSS", "S S", 'S', "stickWood"));
		GameRegistry.addRecipe(new ShapedOreRecipe(PaintThis.palette, "P", 'P', "slabWood"));
		GameRegistry.addRecipe(new ShapedOreRecipe(PaintThis.paletteKnife, "F", "I", "S", 'F', Items.flint, 'I', "ingotIron", 'S', "stickWood"));
		GameRegistry.addRecipe(new ShapedOreRecipe(PaintThis.paintbrushSmall, "H", "S", 'H', PaintThis.horsehair, 'S', "stickWood"));
		GameRegistry.addShapedRecipe(new ItemStack(PaintThis.paintbrushMedium), "H", "S", 'H', PaintThis.horsehair, 'S', PaintThis.paintbrushSmall);
		GameRegistry.addShapedRecipe(new ItemStack(PaintThis.paintbrushLarge), "H", "M", 'H', PaintThis.horsehair, 'M', PaintThis.paintbrushMedium);
		GameRegistry.addShapelessRecipe(new ItemStack(PaintThis.signingBrush), PaintThis.paintbrushSmall);
		addRecipe(new RecipeCanvas());
		addRecipe(new RecipeCanvasExtend());
		addRecipe(new RecipePainting());
		addRecipe(new RecipeDerivePaletteAddDye());
		addRecipe(new RecipeDerivePaletteRemoveDye());
		initDyeCrafting();
	}

	private void initDyeCrafting() {
		for (Dye dye : Dye.values()) {
			if (!dye.isVanilla()) {
				Object p1 = getIngredient(dye.getParentOne());
				Object p2 = getIngredient(dye.getParentTwo());
				GameRegistry.addRecipe(new ShapelessOreRecipe(dye.createItemStack(2), p1, p2));
			}
		}
	}

	private Object getIngredient(Dye dye) {
		if (dye.isVanilla()) {
			return DyeOreDictHelper.getDyeNameFromDamage(dye.getDamage());
		}
		return dye.createItemStack();
	}

	private void addRecipe(PaintThisRecipe recipe) {
		GameRegistry.addRecipe(recipe);
		String name = PaintThis.MODID + ':' + recipe.getRegistryName();
		RecipeSorter.register(name, recipe.getClass(), RecipeSorter.Category.SHAPED, RECIPE_DEPENDENCY);
	}

	public void initEntities() {
		registerEntity(EntityEasel.class, "Easel", 160, 3, true);
		registerEntity(EntityCanvas.class, "Canvas", 160, Integer.MAX_VALUE, false);
	}

	public void initRenders() {}

	public void initModels() {}

	public void initEventHandlers() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void initNetwork() {
		PaintThis.networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(PaintThis.MODID);
		registerMessage(MessagePainterPainting.class, Side.SERVER);
		registerMessage(MessageUpdatePainting.class, Side.CLIENT);
		registerMessage(MessageSignPainting.class, Side.SERVER);
		registerMessage(MessageUpdateSign.class, Side.CLIENT);
	}

	@SubscribeEvent
	public void onHorseConstructing(EntityConstructing event) {
		if (isHorse(event.entity)) {
			event.entity.registerExtendedProperties(HorseShearability.IDENTIFIER, new HorseShearability());
		}
	}

	@SubscribeEvent
	public void onInteract(EntityInteractEvent event) {
		if (isHorse(event.target)) {
			EntityPlayer player = event.entityPlayer;
			if (player.getHeldItem() != null && player.getHeldItem().getItem() == Items.shears) {
				if (!event.target.worldObj.isRemote) {
					EntityLiving horse = (EntityLiving) event.target;
					HorseShearability tolerance = (HorseShearability) horse.getExtendedProperties(HorseShearability.IDENTIFIER);
					tolerance.shear(horse, player);
				}
				event.setCanceled(true);
			}
		}
	}

	private boolean isHorse(Entity entity) {
		if (entity instanceof EntityHorse) {
			return !((EntityHorse) entity).isUndead();
		}
		String id = EntityList.getEntityString(entity);
		if (id != null && id.toLowerCase(Locale.ROOT).contains("horse")) {
			return entity instanceof EntityLiving;
		}
		return false;
	}

	private <M extends SelfProcessingMessage> void registerMessage(Class<M> messageType, Side toSide) {
		PaintThis.networkWrapper.registerMessage((m, ctx) -> m.process(ctx), messageType, nextMessageId++, toSide);
	}

	private void registerEntity(Class<? extends Entity> clazz, String name, int trackingRange, int updateFrequency, boolean sendsVelocityUpdates) {
		EntityRegistry.registerModEntity(clazz, name, nextEntityId++, PaintThis.instance, trackingRange, updateFrequency, sendsVelocityUpdates);
	}

	private <I extends Item> I register(I item, String name) {
		return register(item, name, PaintThis.toolsTab);
	}

	private <I extends Item> I register(I item, String name, CreativeTabs tab) {
		item.setCreativeTab(tab);
		GameRegistry.registerItem(item, name);
		return item;
	}

	public Painting createActivePainting(World world) {
		return new Painting();
	}

	public boolean isClientPainting(EntityPlayer player) {
		return false;
	}
}
