package com.pau101.paintthis.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.client.model.item.ItemPaletteModel;
import com.pau101.paintthis.client.renderer.entity.RenderCanvas;
import com.pau101.paintthis.client.renderer.entity.RenderEasel;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.dye.DyeType;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.entity.item.EntityEasel;
import com.pau101.paintthis.item.ItemPalette;
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.item.brush.ItemPaintbrush;
import com.pau101.paintthis.item.crafting.PositionedItemStack;
import com.pau101.paintthis.painting.Painting;
import com.pau101.paintthis.painting.PaintingDrawable;
import com.pau101.paintthis.property.Painter;
import com.pau101.paintthis.util.DyeOreDictHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientProxy extends CommonProxy {
	private static final ResourceLocation DYE_PALETTE_TEXTURE = new ResourceLocation(PaintThis.MODID, "textures/dye_palette.png");

	private static final ResourceLocation RECIPE_ICONS_TEXTURE = new ResourceLocation(PaintThis.MODID, "textures/gui/recipe_icons.png");

	private static final int RECIPE_PAGE_LENGTH = 3;

	private static final int TEXT_HEIGHT = 12;

	private static final int RECIPE_HEIGHT = 18;

	private static final int RECIPE_WIDTH = 88; // 1.21 Gigawatts!

	private static final int ARROW_HEIGHT = 10;

	private static int deltaWheel;

	private static int recipePage;

	private static ItemStack lastRecipeStack;

	private static boolean isGhost;

	@Override
	public void initRenders() {
		RenderingRegistry.registerEntityRenderingHandler(EntityEasel.class, RenderEasel::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityCanvas.class, RenderCanvas::new);
	}

	@Override
	public void initModels() {
		ModelLoaderRegistry.registerLoader(ItemPaletteModel.Loader.INSTANCE);
		registerItemModel(PaintThis.easel, "easel");
		registerItemModel(PaintThis.palette, "palette");
		registerItemModel(PaintThis.paletteKnife, "palette_knife");
		registerItemModel(PaintThis.signingBrush, "signing_brush");
		registerItemModel(PaintThis.horsehair, "horsehair");
		initDyeModels();
		initPaintbrushModels();
		ModelResourceLocation canvasId = registerItemModel(PaintThis.canvas, "canvas");
		ModelResourceLocation canvasFramedId = registerItemModel(PaintThis.canvas, 1, "canvas_framed");
		ModelBakery.registerItemVariants(PaintThis.canvas, canvasId, canvasFramedId);
	}

	private void initDyeModels() {
		Dye[] dyes = Dye.values();
		ModelResourceLocation[] names = new ModelResourceLocation[dyes.length - Dye.VANILLA_DYE_COUNT];
		for (int i = Dye.VANILLA_DYE_COUNT, j = 0; j < names.length; i++, j++) {
			Dye dye = dyes[i];
			names[j] = registerItemModel(PaintThis.dye, dye.getDamage(), "dye_" + dye.name().toLowerCase());
		}
		ModelBakery.registerItemVariants(PaintThis.dye, names);
	}

	private void initPaintbrushModels() {
		for (ItemPaintbrush.Size size : ItemPaintbrush.Size.values()) {
			initPaintbrushModels(size);
		}
		initBrushModels(PaintThis.signingBrush, "signing_brush");
	}

	private void initPaintbrushModels(ItemPaintbrush.Size size) {
		initBrushModels(PaintThis.paintbrushes.get(size), "paintbrush_" + size.getName());
	}

	private void initBrushModels(ItemBrush brush, String name) {
		Dye[] dyes = Dye.values();
		ModelResourceLocation[] names = new ModelResourceLocation[dyes.length + 1];
		names[0] = registerItemModel(brush, name);
		String nameDyed = name + "_dyed";
		for (int i = 1; i <= dyes.length; i++) {
			names[i] = registerItemModel(brush, i, nameDyed);
		}
		ModelBakery.registerItemVariants(brush, names);
	}

	private ModelResourceLocation registerItemModel(Item item, String name) {
		return registerItemModel(item, 0, name);
	}

	private ModelResourceLocation registerItemModel(Item item, int id, String name) {
		ModelResourceLocation resource = new ModelResourceLocation(PaintThis.MODID + ':' + name, "inventory");
		ModelLoader.setCustomModelResourceLocation(item, id, resource);
		return resource;
	}

	@Override
	public Painting createActivePainting(World world) {
		return world.isRemote ? new PaintingDrawable() : new Painting();
	}

	@Override
	public boolean isClientPainting(EntityPlayer player) {
		return player.worldObj.isRemote && player == Minecraft.getMinecraft().thePlayer;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onItemTooltip(ItemTooltipEvent event) {
		if (!shouldShowRecipes() && DyeOreDictHelper.isDye(event.itemStack) && shouldShowRecipesInScreen()) {
			event.toolTip.add(EnumChatFormatting.BLUE + I18n.format("dyeRecipes.tip"));
		}
	}

	@SubscribeEvent
	public void onClientPlayerConstructing(EntityConstructing event) {
		if (event.entity instanceof EntityPlayerSP) {
			event.entity.registerExtendedProperties(Painter.IDENTIFIER, new Painter());
		}
	}

	@SubscribeEvent
	public void onMouseEvent(MouseInputEvent.Pre event) {
		deltaWheel = Mouse.getEventDWheel();
		if (deltaWheel != 0 && shouldShowRecipes()) {
			event.setCanceled(true);
		}
	}

	private static boolean shouldShowRecipes() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	private static boolean shouldShowRecipesInScreen() {
		GuiScreen screen = Minecraft.getMinecraft().currentScreen;
		return !(screen instanceof GuiContainerCreative) || ((GuiContainerCreative) screen).getSelectedTabIndex() != CreativeTabs.tabAllSearch.getTabIndex();
	}

	public static boolean renderToolTip(GuiScreen screen, ItemStack stack, int x, int y) {
		if (shouldShowRecipes() && DyeOreDictHelper.isDye(stack)) {
			prepareRenderRecipes(screen, stack, x, y);
			return true;
		}
		return false;
	}

	private static void prepareRenderRecipes(GuiScreen screen, ItemStack stack, int x, int y) {
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.color(1, 1, 1);
		screen.zLevel = screen.itemRender.zLevel = 300;
		Minecraft mc = Minecraft.getMinecraft();
		renderRecipes(screen, stack, x, y, mc.getRenderItem(), mc.getTextureManager(), mc.fontRendererObj);
		screen.zLevel = screen.itemRender.zLevel = 0;
		GlStateManager.enableLighting();
		GlStateManager.enableDepth();
		RenderHelper.enableStandardItemLighting();
		GlStateManager.enableRescaleNormal();
	}

	private static void renderRecipes(GuiScreen screen, ItemStack stack, int x, int y, RenderItem render, TextureManager texturer, FontRenderer font) {
		String name = I18n.format(stack.getUnlocalizedName() + ".name");
		Dye dye = Dye.getDyeFromDyeItemStack(stack);
		List<Dye> usages = dye.getUses();
		int maxRecipePage = handlePageTurn(stack, usages);
		int pageStart = recipePage * RECIPE_PAGE_LENGTH;
		int pageEnd = Math.min(pageStart + RECIPE_PAGE_LENGTH, usages.size());
		List<Dye> page = usages.subList(pageStart, pageEnd);
		boolean hasParents = dye.getType() != DyeType.PRIMARY;
		boolean hasUsages = page.size() > 0;
		boolean hasPrior = recipePage > 0;
		boolean hasLater = recipePage < maxRecipePage;
		int bx = x + 12;
		int by = y - 12;
		int width = Math.max(RECIPE_WIDTH, font.getStringWidth(name));
		int height =
			TEXT_HEIGHT - 2 +
			(hasParents ? RECIPE_HEIGHT : 0) +
			(hasUsages ? TEXT_HEIGHT +
				(hasPrior ? ARROW_HEIGHT : 0) +
				page.size() * RECIPE_HEIGHT +
				(hasLater ? ARROW_HEIGHT : 0)
			: 0);
		if (bx + width > screen.width) {
			bx -= 28 + width;
		}
		if (by + height + 6 > screen.height) {
			by = screen.height - height - 6;
		}
		renderToolTipBackground(screen, bx, by, width, height);
		font.drawString(name, bx, by, 0xFFFFFFFF, true);
		by += TEXT_HEIGHT;
		if (hasParents) {
			renderRecipe(screen, bx, by, null, dye, render, texturer);
			by += RECIPE_HEIGHT;
		}
		if (hasUsages) {
			font.drawString(I18n.format("dyeRecipes.uses"), bx, by, 0xFFFFFFFF, true);
			by += TEXT_HEIGHT;
			if (hasPrior) {
				texturer.bindTexture(RECIPE_ICONS_TEXTURE);
				drawShadowedTexturedModelRect(screen, bx, by - 4, 32, 0, 16, 16);
				font.drawString(I18n.format("dyeRecipes.prior", pageStart), bx + 18, by, 0xFFFFFFFF, true);
				by += ARROW_HEIGHT;
			}
			for (Dye usage : page) {
				renderRecipe(screen, bx, by, dye, usage, render, texturer);
				by += RECIPE_HEIGHT;
			}
			if (hasLater) {
				texturer.bindTexture(RECIPE_ICONS_TEXTURE);
				drawShadowedTexturedModelRect(screen, bx, by - 4, 48, 0, 16, 16);
				font.drawString(I18n.format("dyeRecipes.later", usages.size() - pageEnd), bx + 18, by, 0xFFFFFFFF, true);
				by += ARROW_HEIGHT;
			}
		}
	}

	private static int handlePageTurn(ItemStack stack, List<Dye> usages) {
		if (lastRecipeStack != stack) {
			recipePage = 0;
			lastRecipeStack = stack;
		}
		int maxRecipePage = (usages.size() - 1) / RECIPE_PAGE_LENGTH;
		if (deltaWheel != 0) {
			recipePage -= Integer.signum(deltaWheel);
			if (recipePage > maxRecipePage) {
				recipePage = maxRecipePage;
			}
			if (recipePage < 0) {
				recipePage = 0;
			}
		}
		deltaWheel = 0;
		return maxRecipePage;
	}

	private static void renderToolTipBackground(GuiScreen screen, int x, int y, int width, int height) {
		int fill = 0xF0100010;
		screen.drawGradientRect(x - 3, y - 4, x + width + 3, y - 3, fill, fill);
		screen.drawGradientRect(x - 3, y + height + 3, x + width + 3, y + height + 4, fill, fill);
		screen.drawGradientRect(x - 3, y - 3, x + width + 3, y + height + 3, fill, fill);
		screen.drawGradientRect(x - 4, y - 3, x - 3, y + height + 3, fill, fill);
		screen.drawGradientRect(x + width + 3, y - 3, x + width + 4, y + height + 3, fill, fill);
		int outlineBright = 0x505000FF;
		int outlineDark = (outlineBright & 0xFEFEFE) >> 1 | outlineBright & 0xFF000000;
		screen.drawGradientRect(x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, outlineBright, outlineDark);
		screen.drawGradientRect(x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, outlineBright, outlineDark);
		screen.drawGradientRect(x - 3, y - 3, x + width + 3, y - 3 + 1, outlineBright, outlineBright);
		screen.drawGradientRect(x - 3, y + height + 2, x + width + 3, y + height + 3, outlineDark, outlineDark);
	}

	private static void renderRecipe(GuiScreen screen, int x, int y, Dye first, Dye dye, RenderItem render, TextureManager texturer) {
		Dye p1 = dye.getParentOne();
		Dye p2 = dye.getParentTwo();
		if (p2 == first) {
			Dye t = p1;
			p1 = p2;
			p2 = t;
		}
		render.renderItemIntoGUI(p1.createItemStack(), x, y);
		render.renderItemIntoGUI(p2.createItemStack(), x + 36, y);
		render.renderItemIntoGUI(dye.createItemStack(), x + 72, y);
		texturer.bindTexture(RECIPE_ICONS_TEXTURE);
		drawShadowedTexturedModelRect(screen, x + 18, y, 0, 0, 16, 16);
		drawShadowedTexturedModelRect(screen, x + 54, y, 16, 0, 16, 16);
	}

	private static void drawShadowedTexturedModelRect(GuiScreen screen, int x, int y, int u, int v, int width, int height) {
		final float shadow = 63 / 255F;
		GlStateManager.color(shadow, shadow, shadow);
		screen.drawTexturedModalRect(x + 1, y + 1, u, v, width, height);
		GlStateManager.color(1, 1, 1);
		screen.drawTexturedModalRect(x, y, u, v, width, height);
	}

	private static boolean isGoodSlot(Slot slot) {
		return slot != null && slot.inventory instanceof InventoryCrafting;
	}

	private static boolean hasGoodPalette(Slot slot) {
		if (slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			return stack.getItem() == PaintThis.palette && ItemPalette.hasDyes(stack);
		}
		return false;
	}

	public static void renderPalette(GuiContainer container, Slot slot) {
		if (!isGoodSlot(slot) || !hasGoodPalette(slot)) {
			return;
		}
		RenderItem render = Minecraft.getMinecraft().getRenderItem();
		InventoryCrafting inventory = (InventoryCrafting) slot.inventory;
		ItemStack stack = slot.getStack();
		byte[] dyes = stack.getTagCompound().getByteArray("dyes");
		int w = inventory.getWidth();
		int h = inventory.getHeight();
		int slotIndex = slot.getSlotIndex();
		int sx = slotIndex % w;
		int sy = slotIndex / w;
		try {
			isGhost = true;
			for (int i = 0; i < dyes.length; i++) {
				if (dyes[i] == Dye.NO_DYE) {
					continue;
				}
				int mi = PositionedItemStack.getXYIndex(i);
				int nx = sx + (mi / 3 - 1);
				int ny = sy + (mi % 3 - 1);
				if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
					continue;
				}
				Slot neighbor = container.inventorySlots.getSlotFromInventory(inventory, nx + ny * w);
				if (neighbor != null && !neighbor.getHasStack()) {
					render.renderItemAndEffectIntoGUI(Dye.getDyeFromByte(dyes[i]).createItemStack(), neighbor.xDisplayPosition, neighbor.yDisplayPosition);
				}
			}
		} finally {
			/*
			 * Hopefully permanent ghosting can't happen...
			 * ... Hey, who turned out the lights?
			 */
			isGhost = false;
		}
	}

	public static int tweakPutColor4(int argb) {
		return isGhost ? argb & 0xFFFFFF | 0x55000000 : argb;
	}

	public static BufferedImage getImage(ResourceLocation id) throws IOException {
		return ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(id).getInputStream());
	}
}
