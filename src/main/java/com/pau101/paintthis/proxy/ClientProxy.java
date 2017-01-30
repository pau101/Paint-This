package com.pau101.paintthis.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.base.Throwables;
import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.capability.CapabilityHandler;
import com.pau101.paintthis.capability.Painter;
import com.pau101.paintthis.capability.PainterDefault;
import com.pau101.paintthis.client.model.item.ItemPaletteModel;
import com.pau101.paintthis.client.model.item.ItemPaletteModel.BadPaletteModelException;
import com.pau101.paintthis.client.renderer.ItemRendererPatch;
import com.pau101.paintthis.client.renderer.entity.RenderCanvas;
import com.pau101.paintthis.client.renderer.entity.RenderEasel;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.dye.DyeType;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.entity.item.EntityEasel;
import com.pau101.paintthis.item.ItemPalette;
import com.pau101.paintthis.item.PainterUsable;
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.item.brush.ItemPaintbrush;
import com.pau101.paintthis.item.crafting.PositionedItemStack;
import com.pau101.paintthis.net.serverbound.MessagePaletteInteraction;
import com.pau101.paintthis.net.serverbound.MessagePaletteInteraction.PaletteAction;
import com.pau101.paintthis.net.serverbound.MessageSignPainting;
import com.pau101.paintthis.painting.Painting;
import com.pau101.paintthis.painting.PaintingDrawable;
import com.pau101.paintthis.painting.Signature;
import com.pau101.paintthis.util.CubicBezier;
import com.pau101.paintthis.util.Mth;
import com.pau101.paintthis.util.OreDictUtil;
import com.pau101.paintthis.util.Pool;
import com.pau101.paintthis.util.Util;
import com.pau101.paintthis.util.matrix.Matrix;
import com.pau101.paintthis.util.matrix.MatrixStack;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ClientProxy extends CommonProxy {
	private static final ResourceLocation DYE_PALETTE_TEXTURE = new ResourceLocation(PaintThis.ID, "textures/dye_palette.png");

	private static final ResourceLocation RECIPE_ICONS_TEXTURE = new ResourceLocation(PaintThis.ID, "textures/gui/recipe_icons.png");

	private static final Pool<Matrix> MATRIX_POOL = new Pool<>(() -> new Matrix(4), 4);

	private static final int RECIPE_PAGE_LENGTH = 3;

	private static final int TEXT_HEIGHT = 12;

	private static final int RECIPE_HEIGHT = 18;

	private static final int RECIPE_WIDTH = 88; // 1.21 Gigawatts!

	private static final int ARROW_HEIGHT = 10;

	private static final MethodHandle RENDER_ITEM = Util.getHandle(RenderItem.class, new String[] { "func_175045_a", "renderModel" }, IBakedModel.class, int.class, ItemStack.class);

	private static final CubicBezier EQUIP_CURVE = new CubicBezier(1, 0, 0.77F, 0.96F);

	private static final CubicBezier TRANSFORM_CURVE = new CubicBezier(0.3F, 0, 0.7F, 1);

	private static final FloatBuffer MATRIX_BUF = BufferUtils.createFloatBuffer(16);

	private static final Matrix4d FLIP_X;

	static {
		FLIP_X = new Matrix4d();
		FLIP_X.setIdentity();
		FLIP_X.m00 = -1;
	}

	private static Minecraft mc = Minecraft.getMinecraft();

	private static int deltaWheel;

	private static int recipePage;

	@Nullable
	private static ItemStack lastRecipeStack;

	@Nullable
	private static ItemStack prevMainHand, prevOffHand, lastEquipMainHand, lastEquipOffHand;

	private static EnumMap<EnumHand, Interaction> interactions = new EnumMap<>(EnumHand.class);

	private static WeakReference<ACompletelyNormalEntity> paletteDummy = new WeakReference<>(null);

	@Override
	public void initRenders() {
		RenderingRegistry.registerEntityRenderingHandler(EntityEasel.class, RenderEasel::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityCanvas.class, RenderCanvas::new);
	}

	@Override
	public void initRendersLater() {
		ItemColors colors = mc.getItemColors();
		colors.registerItemColorHandler((stack, index) -> {
			if (index == 1 && stack.getMetadata() > 0) {
				return Dye.getDyeFromDamage(stack.getMetadata() - 1).getColor();
			}
			return 0xFFFFFFFF;
		}, PaintThis.paintbrushSmall, PaintThis.paintbrushMedium, PaintThis.paintbrushLarge, PaintThis.signingBrush);
		colors.registerItemColorHandler((stack, index) -> {
			if (index > 0 && index <= ItemPalette.DYE_COUNT && stack.hasTagCompound()) {
				byte[] dyes = stack.getTagCompound().getByteArray("dyes");
				byte dye;
				if (dyes.length == ItemPalette.DYE_COUNT && (dye = dyes[index - 1]) != Dye.NO_DYE) {
					return Dye.getDyeFromByte(dye).getColor();
				}
			}
			return 0xFFFFFFFF;
		}, PaintThis.palette);
		ItemRenderer renderer = new ItemRendererPatch(mc);
		ReflectionHelper.setPrivateValue(Minecraft.class, mc, renderer, "itemRenderer");
		Field field = ReflectionHelper.findField(EntityRenderer.class, "itemRenderer");
		ReflectionHelper.setPrivateValue(Field.class, field, field.getModifiers() & ~Modifier.FINAL, "modifiers");
		try {
			field.set(mc.entityRenderer, renderer);
		} catch (Exception e) {
			Throwables.propagate(e);
		}
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
			names[j] = registerItemModel(PaintThis.dye, dye.getDamage(), "dye_" + dye.name().toLowerCase(Locale.ROOT));
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
		ModelResourceLocation resource = new ModelResourceLocation(PaintThis.ID + ':' + name, "inventory");
		ModelLoader.setCustomModelResourceLocation(item, id, resource);
		return resource;
	}

	@Override
	public Painting createActivePainting(World world) {
		return world.isRemote ? new PaintingDrawable() : new Painting();
	}

	@Override
	public boolean isClientPainting(EntityPlayer player) {
		return player.worldObj.isRemote && player == mc.thePlayer;
	}

	@Override
	public void paint(EntityPlayer player, ItemStack stack, EnumHand hand) {
		EnumHand opposite = hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
		if (getLookingAtSlot(player, opposite) > 0) {
			return;
		}
		boolean hasClimaxed = true;
		Optional<Pair<EntityCanvas, Vec3d>> result = findHitCanvas(player);
		if (result.isPresent()) {
			Pair<EntityCanvas, Vec3d> hit = result.get();
			EntityCanvas canvas = hit.getLeft();
			if (canvas.isEditableBy(player)) {
				Vec3d vec = hit.getRight();
				Interaction inter = interactions.get(hand);
				if (!(inter instanceof InteractionBrush)) {
					inter = new InteractionBrush(stack, player.inventory.currentItem, hand, vec, canvas, player);
					interactions.put(hand, inter);
				}
				InteractionBrush brush = (InteractionBrush) inter;
				brush.canvas = canvas;
				brush.target = vec;
				brush.active = true;
				if (brush.canPaint()) {
					player.getCapability(CapabilityHandler.PAINTER_CAP, null).stroke(canvas, hand, vec, stack);
					hasClimaxed = false;
				}
			}
		}
		if (hasClimaxed) {
			player.getCapability(CapabilityHandler.PAINTER_CAP, null).finishStroke();	
		}
	}

	@Override
	public void sign(EntityPlayer player, ItemStack stack, EnumHand hand) {
		Optional<Pair<EntityCanvas, Vec3d>> result = findHitCanvas(player);
		if (result.isPresent()) {
			Pair<EntityCanvas, Vec3d> hit = result.get();
			EntityCanvas canvas = hit.getLeft();
			if (canvas.isEditableBy(player)) {
				interactions.put(hand, new InteractionSigningBrush(stack, player.inventory.currentItem, hand, hit.getRight(), canvas));
			}
		}
	}

	@SubscribeEvent
	public void onEnteringChunk(EntityEvent.EnteringChunk event) {
		Entity e = event.getEntity();
		ACompletelyNormalEntity oldPalette = paletteDummy.get();
		if (e instanceof EntityPlayerSP && (oldPalette == null || oldPalette.isDead || oldPalette.worldObj != e.worldObj)) {
			World world = mc.theWorld;
			ACompletelyNormalEntity palette = new ACompletelyNormalEntity(world, (EntityPlayerSP) e);
			palette.setPosition(e.posX, e.posY, e.posZ);
			world.spawnEntityInWorld(palette);
			paletteDummy = new WeakReference<>(palette);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onItemTooltip(ItemTooltipEvent event) {
		if (!shouldShowRecipes() && OreDictUtil.isDye(event.getItemStack()) && shouldShowRecipesInScreen()) {
			event.getToolTip().add(I18n.format("dyeRecipes.tip"));
		}
	}

	@SubscribeEvent
	public void onClientPlayerConstructing(AttachCapabilitiesEvent.Entity event) {
		if (event.getEntity() instanceof EntityPlayerSP) {
			event.addCapability(CapabilityHandler.PAINTER_ID, new PainterDefault());
		}
	}

	@SubscribeEvent
	public void onModelBake(ModelBakeEvent event) {
		if (ItemPaletteModel.textureSize == -1) {
			throw new BadPaletteModelException(ItemPaletteModel.failCause);
		}
	}

	@SubscribeEvent
	public void onMouseEvent(MouseInputEvent.Pre event) {
		deltaWheel = Mouse.getEventDWheel();
		if (deltaWheel != 0 && shouldShowRecipes()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onRenderTooltip(RenderTooltipEvent.Pre event) {
		if (shouldShowRecipes()) {
			ItemStack stack = event.getStack();
			if (OreDictUtil.isDye(stack)) {
				prepareRenderRecipes(mc.currentScreen, stack, event.getX(), event.getY());
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void renderSpecificHand(RenderSpecificHandEvent event) {
		EntityPlayerSP player = mc.thePlayer;
		if (canUsePalette(player) && shouldShowInteractivePalette(player, event.getHand()) || interactions.containsKey(event.getHand())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void tick(TickEvent.ClientTickEvent event) {
		if (Minecraft.getMinecraft().isGamePaused()) {
			return;
		}
		if (event.phase == Phase.START) {
			for (Interaction inter : interactions.values()) {
				inter.updatePrev();
			}
		} else {
			ItemRenderer renderer = mc.getItemRenderer();
			ItemStack mainHand = renderer.itemStackMainHand;
			ItemStack offHand = renderer.itemStackOffHand;
			if (mainHand != prevMainHand) {
				lastEquipMainHand = prevMainHand;
			}
			if (offHand != prevOffHand) {
				lastEquipOffHand = prevOffHand;
			}
			EntityPlayer player = mc.thePlayer;
			Iterator<Entry<EnumHand, Interaction>> inters = interactions.entrySet().iterator();
			while (inters.hasNext()) {
				Entry<EnumHand, Interaction> e = inters.next();
				EnumHand hand = e.getKey();
				Interaction interaction = e.getValue();
				interaction.update(player, player.getHeldItem(hand), hand == EnumHand.OFF_HAND || interaction.actionBarSlot == mc.thePlayer.inventory.currentItem);
				if (interaction.isDone(player, hand == EnumHand.MAIN_HAND ? mainHand : offHand)) {
					inters.remove();
				}
			}
			prevMainHand = mainHand;
			prevOffHand = offHand;
		}
	}

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		ItemRenderer renderer = mc.getItemRenderer();
		EntityPlayerSP player = mc.thePlayer;
		float delta = event.getPartialTicks();
		Matrix4d paletteMatrix = null;
		if (canUsePalette(player)) {
			if (shouldShowInteractivePalette(player, EnumHand.MAIN_HAND)) {
				paletteMatrix = renderPalette(player, EnumHand.MAIN_HAND, renderer, delta);
			} else if (shouldShowInteractivePalette(player, EnumHand.OFF_HAND)) {
				paletteMatrix = renderPalette(player, EnumHand.OFF_HAND, renderer, delta);
			}
		}
		if (interactions.size() > 0) {
			mc.entityRenderer.enableLightmap();
			GlStateManager.enableFog();
			RenderHelper.enableStandardItemLighting();
			for (Interaction inter : interactions.values()) {
				if (!inter.requiresPalette() || paletteMatrix != null) {
					renderInteraction(player, inter, paletteMatrix, renderer, delta);
				}
			}
			mc.entityRenderer.disableLightmap();
			GlStateManager.disableFog();
		}
	}

	private Matrix4d renderPalette(EntityPlayerSP player, EnumHand hand, ItemRenderer renderer, float delta) {
		mc.entityRenderer.enableLightmap();
		GlStateManager.enableFog();
		// Don't want colors washed out
		RenderHelper.disableStandardItemLighting();
		boolean mainHand = hand == EnumHand.MAIN_HAND;
		ItemStack stack, other, lastEquip;
		float thisPrevEP, thisEP, otherPrevEP, otherEP;
		if (mainHand) {
			stack = renderer.itemStackMainHand;
			other = renderer.itemStackOffHand;
			thisPrevEP = renderer.prevEquippedProgressMainHand;
			thisEP = renderer.equippedProgressMainHand;
			otherPrevEP = renderer.prevEquippedProgressOffHand;
			otherEP = renderer.equippedProgressOffHand;
			lastEquip = lastEquipOffHand;
		} else {
			stack = renderer.itemStackOffHand;
			other = renderer.itemStackMainHand;
			thisPrevEP = renderer.prevEquippedProgressOffHand;
			thisEP = renderer.equippedProgressOffHand;
			otherPrevEP = renderer.prevEquippedProgressMainHand;
			otherEP = renderer.equippedProgressMainHand;
			lastEquip = lastEquipMainHand;
		}
		float yaw = Mth.lerp(player.prevRenderYawOffset, player.renderYawOffset, delta);
		boolean isLeft = player.getPrimaryHand() == EnumHandSide.RIGHT != mainHand;
		float pep = thisPrevEP, ep = thisEP;
		ItemStack usingEquip = stack, heldEquip = player.getHeldItem(hand);
		ItemStack otherHeldEquip = player.getHeldItem(mainHand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
		if (thisEP == thisPrevEP && otherEP < 1 && other == otherHeldEquip ? shouldShowInteractivePalette(stack, otherHeldEquip) && !shouldShowInteractivePalette(stack, lastEquip) : !shouldShowInteractivePalette(stack, otherHeldEquip)) {
			pep = otherPrevEP;
			ep = otherEP;
			usingEquip = other;
			heldEquip = otherHeldEquip;
		}
		float equip = 1 - Mth.lerp(pep, ep, delta);
		GlStateManager.pushMatrix();
		if (equip == 0) {
			transformPalette(GL_MATRIX, player, yaw, isLeft);
		} else {
			float e;
			if (equip < 1 && usingEquip != heldEquip) {
				e = 1 - EQUIP_CURVE.eval(1 - equip);
			} else {
				e = EQUIP_CURVE.eval(equip);
			}
			interpolateWorldToHeld(player, stack, yaw, isLeft, mainHand, ClientProxy::transformPalette, e, thisEP == 1 ? 0 : 1, delta);
		}
		mc.getRenderItem().renderItem(stack, player, TransformType.NONE, false);
		Matrix4d mat = getMatrix(GL11.GL_MODELVIEW_MATRIX);
		GlStateManager.popMatrix();
		GlStateManager.disableFog();
		mc.entityRenderer.disableLightmap();
		return mat;
	}

	private void renderInteraction(EntityPlayerSP player, Interaction interaction, Matrix4d paletteMatrix, ItemRenderer renderer, float delta) {
		GlStateManager.pushMatrix();
		boolean mainHand = interaction.hand == EnumHand.MAIN_HAND;
		boolean isLeft = player.getPrimaryHand() == EnumHandSide.RIGHT != mainHand;
		ItemStack stack;
		float pep, ep;
		if (mainHand) {
			stack = renderer.itemStackMainHand;
			pep = renderer.prevEquippedProgressMainHand;
			ep = renderer.equippedProgressMainHand;
		} else {
			stack = renderer.itemStackOffHand;
			pep = renderer.prevEquippedProgressOffHand;
			ep = renderer.equippedProgressOffHand;
		}
		float equip = 1 - Mth.lerp(pep, ep, delta);
		ItemStack heldEquip = player.getHeldItem(interaction.hand);
		float renderEquip = ep == 1 ? 0 : 1;
		if (interaction.hand == EnumHand.OFF_HAND || player.inventory.currentItem == interaction.actionBarSlot) {
			renderEquip = 0;
			equip = 1 - Mth.lerp(interaction.prevTransformTime, interaction.transformTime, delta) / Interaction.TRANSFORM_DURATION;
			equip = TRANSFORM_CURVE.eval(equip);
		} else if (equip < 1 && stack != heldEquip) {
			equip = 1 - EQUIP_CURVE.eval(1 - equip);
		} else {
			equip = EQUIP_CURVE.eval(equip);
		}
		if (equip == 0) {
			interaction.transform(player, GL_MATRIX, paletteMatrix, isLeft, delta);
		} else {
			float yaw = Mth.lerp(player.prevRenderYawOffset, player.renderYawOffset, delta);
			interpolateWorldToHeld(player, stack, yaw, isLeft, mainHand, (m, p, y, l) -> interaction.transform(player, m, paletteMatrix, l, delta), equip, renderEquip, delta);
		}
		mc.getRenderItem().renderItem(stack, player, TransformType.NONE, false);
		GlStateManager.popMatrix();
	}

	public static Optional<Pair<EntityCanvas, Vec3d>> findHitCanvas(EntityPlayer player) {
		List<EntityCanvas> canvases = player.worldObj.getEntitiesWithinAABB(EntityCanvas.class, player.getEntityBoundingBox().expand(ItemBrush.REACH * 2, ItemBrush.REACH * 2, ItemBrush.REACH * 2));
		Optional<EntityCanvas> hitCanvas = Optional.empty();
		Optional<Vec3d> hitVec = Optional.of(new Vec3d(-1, -1, ItemBrush.REACH));
		Vec3d origin = player.getPositionEyes(1);
		Vec3d look = player.getLookVec();
		for (EntityCanvas canvas : canvases) {
			if (player.getDistanceToEntity(canvas) > ItemBrush.REACH * 2) {
				continue;
			}
			Optional<Vec3d> result = findHit(origin, look, canvas);
			if (result.isPresent() && result.get().zCoord < hitVec.get().zCoord) {
				hitCanvas = Optional.of(canvas);
				hitVec = result;
			}
		}
		if (hitCanvas.isPresent()) {
			return Optional.of(Pair.of(hitCanvas.get(), hitVec.get()));
		}
		return Optional.empty();
	}

	public static Optional<Vec3d> findHit(Vec3d origin, Vec3d look, EntityCanvas canvas) {
		Matrix matrix = MATRIX_POOL.getInstance();
		matrix.loadIdentity();
		matrix.rotate(-canvas.rotationYaw, 0, 1, 0);
		matrix.rotate((canvas.rotationPitch + 90), 1, 0, 0);
		float w = canvas.getWidth() / 2F, h = canvas.getHeight() / 2F;
		Point3f v1 = new Point3f(-w, 0.0625F, -h);
		Point3f v2 = new Point3f(w, 0.0625F, -h);
		Point3f v3 = new Point3f(w, 0.0625F, h);
		Point3f v4 = new Point3f(-w, 0.0625F, h);
		matrix.transform(v1);
		matrix.transform(v2);
		matrix.transform(v3);
		matrix.transform(v4);
		MATRIX_POOL.freeInstance(matrix);
		Point3f pos = new Point3f((float) canvas.posX, (float) canvas.posY, (float) canvas.posZ);
		v1.add(pos);
		v2.add(pos);
		v3.add(pos);
		v4.add(pos);
		return Mth.intersect(origin, look, getVec(v1), getVec(v2), getVec(v3), getVec(v4), true);
	}

	private static void interpolateWorldToHeld(EntityPlayerSP player, ItemStack stack, float yaw, boolean isLeft, boolean isMainHand, ModelViewTransform worldTransform, float t, float regularEquip, float delta) {
		Matrix matrix = MATRIX_POOL.getInstance();
		matrix.loadIdentity();
		matrix.mul(getMatrix(GL11.GL_MODELVIEW_MATRIX));
		worldTransform.transform(matrix, player, yaw, isLeft);
		Matrix4d modelView1 = matrix.getTransform();
		Matrix4d perspective1 = getMatrix(GL11.GL_PROJECTION_MATRIX);
		float fov = 70;
		Entity entity = mc.getRenderViewEntity();
		if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) {
			float deathTime = ((EntityLivingBase) entity).deathTime + delta;
			fov /= (1 - 500 / (deathTime + 500)) * 2 + 1;
		}
		IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.theWorld, entity, delta);
		if (state.getMaterial() == Material.WATER) {
			fov *= 6 / 7F;
		}
		fov = ForgeHooksClient.getFOVModifier(mc.entityRenderer, entity, state, delta, fov);
		float farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16;
		float walkDelta = player.distanceWalkedModified - player.prevDistanceWalkedModified;
		float walked = -(player.distanceWalkedModified + walkDelta * delta);
		float camYaw = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * delta;
		float camPitch = player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * delta;
		float swing = player.getSwingProgress(delta);
		float thisSwing = isMainHand ? swing : 0;
		float armPitch = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * delta;
		float armYaw = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * delta;
		float swingX = -0.4F * MathHelper.sin(MathHelper.sqrt_float(thisSwing) * Mth.PI);
		float swingY = 0.2F * MathHelper.sin(MathHelper.sqrt_float(thisSwing) * Mth.TAU);
		float swingZ = -0.2F * MathHelper.sin(thisSwing * Mth.PI);
		float swingAmt = MathHelper.sin(thisSwing * thisSwing * Mth.PI);
		float swingAng = MathHelper.sin(MathHelper.sqrt_float(thisSwing) * Mth.PI);
		int side = isLeft ? -1 : 1;
		matrix.loadIdentity();
		if (mc.gameSettings.anaglyph) {
			matrix.translate(-(EntityRenderer.anaglyphField * 2 - 1) * 0.07F, 0, 0);
		}
		matrix.perspective(fov, mc.displayWidth / (float) mc.displayHeight, 0.05F, farPlaneDistance * 2);
		Matrix4d perspective2 = matrix.getTransform();
		matrix.loadIdentity();
		if (mc.gameSettings.anaglyph) {
			matrix.translate((EntityRenderer.anaglyphField * 2 - 1) * 0.1F, 0, 0);
		}
		matrix.translate(MathHelper.sin(walked * Mth.PI) * camYaw * 0.5F, -Math.abs(MathHelper.cos(walked * Mth.PI) * camYaw), 0);
		matrix.rotate(MathHelper.sin(walked * Mth.PI) * camYaw * 3, 0, 0, 1);
		matrix.rotate(Math.abs(MathHelper.cos(walked * Mth.PI - 0.2F) * camYaw) * 5 + camPitch, 1, 0, 0);
		matrix.rotate((player.rotationPitch - armPitch) * 0.1F, 1, 0, 0);
		matrix.rotate((player.rotationYaw - armYaw) * 0.1F, 0, 1, 0);
		matrix.translate(side * swingX + side * 0.56F, swingY - 0.52F - regularEquip * 0.6F, swingZ - 0.72F);
		matrix.rotate(side * (45 + swingAmt * -20), 0, 1, 0);
		matrix.rotate(side * swingAng * -20, 0, 0, 1);
		matrix.rotate(swingAng * -80, 1, 0, 0);
		matrix.rotate(side * -45, 0, 1, 0);
		IBakedModel model = mc.getRenderItem().getItemModelWithOverrides(stack, mc.theWorld, mc.thePlayer);
		TransformType transform = isLeft ? TransformType.FIRST_PERSON_LEFT_HAND : TransformType.FIRST_PERSON_RIGHT_HAND;
		if (model instanceof IPerspectiveAwareModel) {
			Pair<? extends IBakedModel, Matrix4f> pair = ((IPerspectiveAwareModel) model).handlePerspective(transform);
			if (pair.getRight() != null) {
				Matrix4d mat = new Matrix4d(pair.getRight());
				if (isLeft) {
					mat.mul(FLIP_X, mat);
					mat.mul(mat, FLIP_X);
				}
				matrix.mul(mat);
			}
		} else {
			ItemTransformVec3f vec = model.getItemCameraTransforms().getTransform(transform);
			if (vec != ItemTransformVec3f.DEFAULT) {
				matrix.translate(side * (ItemCameraTransforms.offsetTranslateX + vec.translation.x), ItemCameraTransforms.offsetTranslateY + vec.translation.y, ItemCameraTransforms.offsetTranslateZ + vec.translation.z);
				float rx = ItemCameraTransforms.offsetRotationX + vec.rotation.x;
				float ry = ItemCameraTransforms.offsetRotationY + vec.rotation.y;
				float rz = ItemCameraTransforms.offsetRotationZ + vec.rotation.z;
				if (isLeft) {
					ry = -ry;
					rz = -rz;
				}
				matrix.rotate(getQuat(rx, ry, rz));
				matrix.scale(ItemCameraTransforms.offsetScaleX + vec.scale.x, ItemCameraTransforms.offsetScaleY + vec.scale.y, ItemCameraTransforms.offsetScaleZ + vec.scale.z);
			}
		}
		Matrix4d modelView2 = matrix.getTransform();
		Matrix4d modelView = lerp(modelView1, modelView2, t);
		Matrix4d perspective = lerpPerspective(perspective1, perspective2, t);
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		multMatrix(perspective);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GlStateManager.loadIdentity();
		multMatrix(modelView);
		MATRIX_POOL.freeInstance(matrix);
	}

	private static Quat4d getQuat(float x, float y, float z) {
		float rx = x * Mth.DEG_TO_RAD;
		float ry = y * Mth.DEG_TO_RAD;
		float rz = z * Mth.DEG_TO_RAD;
		float xs = MathHelper.sin(rx / 2);
		float xc = MathHelper.cos(rx / 2);
		float ys = MathHelper.sin(ry / 2);
		float yc = MathHelper.cos(ry / 2);
		float zs = MathHelper.sin(rz / 2);
		float zc = MathHelper.cos(rz / 2);
		return new Quat4d(xs * yc * zc + xc * ys * zs, xc * ys * zc - xs * yc * zs, xs * ys * zc + xc * yc * zs, xc * yc * zc - xs * ys * zs);
	}

	private static void transformPalette(MatrixStack matrix, EntityPlayer player, float yaw, boolean isLeft) {
		matrix.rotate(-yaw, 0, 1, 0);
		matrix.translate(isLeft ? 0.22 : -0.22, player.isSneaking() ? 1.17 : 1.25, 0.38);
		matrix.rotate(70, 1, 0, 0);
		if (isLeft) {
			matrix.rotate(180, 0, 1, 0);
		}
		matrix.scale(0.45, 0.45, 0.45);
	}

	private static Matrix4d getMatrix(int matrix) {
		GL11.glGetFloat(matrix, MATRIX_BUF);
		Matrix4d mat = new Matrix4d();
		for (int i = 0; i < 16; i++) {
			mat.setElement(i % 4, i / 4, MATRIX_BUF.get(i));
		}
		return mat;
	}

	private static void multMatrix(Matrix4d mat) {
		for (int i = 0; i < 16; i++) {
			MATRIX_BUF.put(i, (float) mat.getElement(i % 4, i / 4));
		}
		GlStateManager.multMatrix(MATRIX_BUF);
	}

	private static Matrix4d lerp(Matrix4d a, Matrix4d b, double t) {
		Quat4d aRot = new Quat4d();
		Quat4d bRot = new Quat4d();
		a.get(aRot);
		b.get(bRot);
		Vector3d aTrans = new Vector3d();
		Vector3d bTrans = new Vector3d();
		a.get(aTrans);
		b.get(bTrans);
		double aScale = a.getScale(); // only a uniform scale is used the rendering so this'll do
		double bScale = b.getScale();
		aRot.interpolate(bRot, t);
		aTrans.interpolate(bTrans, t);
		double scale = Mth.lerp(aScale, bScale, t);
		Matrix4d mat = new Matrix4d();
		mat.set(aTrans);
		Matrix4d scratch = new Matrix4d();
		scratch.set(aRot);
		mat.mul(scratch);
		scratch.set(scale);
		mat.mul(scratch);
		return mat;
	}

	private static Matrix4d lerpPerspective(Matrix4d a, Matrix4d b, double t) {
		double aFov = Math.asin(1 / Math.sqrt(a.m11 * a.m11 + 1));
		double bFov =Math.asin(1 / Math.sqrt(b.m11 * b.m11 + 1));
		double aAspect =  a.m11 / a.m00;
		double bAspect = b.m11 / b.m00;
		double aZNear = a.m23 / (a.m22 - 1);
		double aZFar = a.m23 / (a.m22 + 1);
		double bZFar =b.m23 / (b.m22 + 1);
		double bZNear = b.m23 / (b.m22 - 1);
		double fov = Mth.lerp(aFov, bFov, t);
		double aspect = Mth.lerp(aAspect, bAspect, t);
		double zNear = Mth.lerp(aZNear, bZNear, t);
		double zFar = Mth.lerp(bZFar, bZFar, t);
		double deltaZ = zFar - zNear;
		double cotangent = 1 / Math.tan(fov);
		Matrix4d mat = new Matrix4d();
		mat.m00 = cotangent / aspect;
		mat.m11 = cotangent;
		mat.m22 = -(zFar + zNear) / deltaZ;
		mat.m32 = -1;
		mat.m23 = -2 * zNear * zFar / deltaZ;
		return mat;
	}

	private static boolean shouldShowInteractivePalette(EntityPlayer player, EnumHand hand) {
		ItemRenderer renderer = mc.getItemRenderer();
		ItemStack held, other;
		if (hand == EnumHand.MAIN_HAND) {
			held = renderer.itemStackMainHand;
			other = renderer.itemStackOffHand;
		} else {
			held = renderer.itemStackOffHand;
			other = renderer.itemStackMainHand;
		}
		return shouldShowInteractivePalette(held, other);
	}

	private static boolean shouldShowInteractivePalette(ItemStack held, ItemStack other) {
		return held != null && held.getItem() == PaintThis.palette && other != null && (other.getItem() instanceof PainterUsable || OreDictUtil.isDye(other));
	}

	private static int getLookingAtSlot(EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		Optional<Vec3d> result = intersectPalette(player, stack, hand);
		if (result.isPresent()) {
			return getPaletteSlot(result.get());
		}
		return -1;
	}

	private static Optional<Vec3d> intersectPalette(EntityPlayer player, ItemStack stack, EnumHand hand) {
		if (stack != null && canUsePalette(player)) {
			Vec3d origin = new Vec3d(0, player.getEyeHeight(), 0);
			Vec3d look = player.getLookVec();
			Matrix matrix = MATRIX_POOL.getInstance();
			matrix.loadIdentity();
			boolean isLeft = player.getPrimaryHand() == EnumHandSide.RIGHT == (hand == EnumHand.OFF_HAND);
			transformPalette(matrix, player, player.renderYawOffset, isLeft);
			matrix.translate(-0.5, -0.5, -0.5);
			float top = isLeft ? 8.5F / 16 : 7.5F / 16;
			Point3f v1 = new Point3f(0, 1, top);
			Point3f v2 = new Point3f(1, 1, top);
			Point3f v3 = new Point3f(1, 0, top);
			Point3f v4 = new Point3f(0, 0, top);
			matrix.transform(v1);
			matrix.transform(v2);
			matrix.transform(v3);
			matrix.transform(v4);
			MATRIX_POOL.freeInstance(matrix);
			return Mth.intersect(origin, look, getVec(v1), getVec(v2), getVec(v3), getVec(v4), false);
		}
		return Optional.empty();
	}

	private static int getPaletteSlot(Vec3d vec) {
		int px = (int) (vec.xCoord * ItemPaletteModel.textureSize), py = (int) (vec.yCoord * ItemPaletteModel.textureSize);
		return ItemPaletteModel.dyeRegions[px + py * ItemPaletteModel.textureSize] - 1;
	}

	private static Vec3d getVec(Point3f p) {
		return new Vec3d(p.x, p.y, p.z);
	}

	private static boolean canUsePalette() {
		return canUsePalette(mc.thePlayer);
	}

	private static boolean canUsePalette(EntityPlayer player) {
		return player != null && player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0 && !mc.gameSettings.hideGUI && !player.isPlayerSleeping() && !mc.playerController.isSpectator() && ItemPaletteModel.textureSize > -1;
	}

	private static boolean shouldShowRecipes() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	private static boolean shouldShowRecipesInScreen() {
		GuiScreen screen = mc.currentScreen;
		return !(screen instanceof GuiContainerCreative) || ((GuiContainerCreative) screen).getSelectedTabIndex() != CreativeTabs.SEARCH.getTabIndex();
	}

	private static void prepareRenderRecipes(GuiScreen screen, ItemStack stack, int x, int y) {
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.color(1, 1, 1);
		screen.zLevel = screen.itemRender.zLevel = 300;
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
		int height = TEXT_HEIGHT - 2 + (hasParents ? RECIPE_HEIGHT : 0) + (hasUsages ? TEXT_HEIGHT + (hasPrior ? ARROW_HEIGHT : 0) + page.size() * RECIPE_HEIGHT + (hasLater ? ARROW_HEIGHT : 0) : 0);
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
		RenderItem render = mc.getRenderItem();
		InventoryCrafting inventory = (InventoryCrafting) slot.inventory;
		ItemStack stack = slot.getStack();
		byte[] dyes = stack.getTagCompound().getByteArray("dyes");
		int w = inventory.getWidth();
		int h = inventory.getHeight();
		int slotIndex = slot.getSlotIndex();
		int sx = slotIndex % w;
		int sy = slotIndex / w;
		boolean rendered = false;
		for (int i = 0; i < dyes.length; i++) {
			if (dyes[i] == Dye.NO_DYE) {
				continue;
			}
			int mi = PositionedItemStack.getXYIndex(i);
			int nx = sx + mi / 3 - 1;
			int ny = sy + mi % 3 - 1;
			if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
				continue;
			}
			Slot neighbor = container.inventorySlots.getSlotFromInventory(inventory, nx + ny * w);
			if (neighbor != null && !neighbor.getHasStack()) {
				if (!rendered) {
					TextureManager tex = mc.getTextureManager();
					tex.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
					tex.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
					GlStateManager.enableRescaleNormal();
					GlStateManager.enableAlpha();
					GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
					GlStateManager.enableBlend();
					GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
					GlStateManager.color(1, 1, 1);
					rendered = true;
				}
				GlStateManager.pushMatrix();
				GlStateManager.translate(neighbor.xDisplayPosition, neighbor.yDisplayPosition + 16, 92 + render.zLevel);
				GlStateManager.scale(16, -16, 16);
				IBakedModel model = render.getItemModelWithOverrides(Dye.getDyeFromByte(dyes[i]).createItemStack(), null, null);
				if (model.isGui3d()) {
					GlStateManager.enableLighting();
				} else {
					GlStateManager.disableLighting();
				}
				model = ForgeHooksClient.handleCameraTransforms(model, TransformType.GUI, false);
				try {
					RENDER_ITEM.invoke(render, model, 0x55FFFFFF, stack);
				} catch (Throwable e) {
					Throwables.propagate(e);
				}
				GlStateManager.popMatrix();
			}
		}
		if (rendered) {
			GlStateManager.disableAlpha();
			GlStateManager.disableRescaleNormal();
			GlStateManager.disableLighting();
			TextureManager tex = mc.getTextureManager();
			tex.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			tex.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
		}
	}

	public static BufferedImage getImage(ResourceLocation id) throws IOException {
		return ImageIO.read(mc.getResourceManager().getResource(id).getInputStream());
	}

	@FunctionalInterface
	private interface ModelViewTransform {
		void transform(MatrixStack matrix, EntityPlayerSP player, float yaw, boolean isLeft);
	}

	private static final MatrixStack GL_MATRIX = new MatrixStack() {
		@Override
		public void push() {
			GlStateManager.pushMatrix();
		}

		@Override
		public void pop() {
			GlStateManager.popMatrix();
		}

		@Override
		public void translate(double x, double y, double z) {
			GlStateManager.translate(x, y, z);
		}

		@Override
		public void rotate(double angle, double x, double y, double z) {
			GlStateManager.rotate((float) angle, (float) x, (float) y, (float) z);
		}

		@Override
		public void scale(double x, double y, double z) {
			GlStateManager.scale(x, y, z);
		}

		@Override
		public void mul(Matrix4d matrix) {
			multMatrix(matrix);
		}

		@Override
		public void loadIdentity() {
			GlStateManager.loadIdentity();
		}
	};

	private final class ACompletelyNormalEntity extends Entity {
		private final EntityPlayer player;

		public ACompletelyNormalEntity(World world, EntityPlayer player) {
			super(world);
			this.player = player;
			setEntityId(-45981); // 1485720303664
			setSize(0, 0);
			noClip = true;
			forceSpawn = true;
		}

		@Override
		protected void entityInit() {}

		@Override
		public boolean canBeCollidedWith() {
			return shouldShowInteractivePalette(player, EnumHand.MAIN_HAND) && getLookingAtSlot(player, EnumHand.MAIN_HAND) > -1 || shouldShowInteractivePalette(player, EnumHand.OFF_HAND) && getLookingAtSlot(player, EnumHand.OFF_HAND) > -1;
		}

		@Override
		public boolean shouldRenderInPass(int pass) {
			return false;
		}

		@Override
		public AxisAlignedBB getEntityBoundingBox() {
			return canBeCollidedWith() ? player.getEntityBoundingBox() : super.getEntityBoundingBox();
		}

		@Override
		public void onUpdate() {
			if (player.isDead) {
				setDead();
			} else {
				setPosition(player.posX, player.posY, player.posZ);
			}
			firstUpdate = false;
		}

		@Override
		public boolean processInitialInteract(EntityPlayer player, ItemStack palette, EnumHand paletteHand) {
			Optional<Vec3d> result = intersectPalette(player, palette, paletteHand);
			if (!result.isPresent()) {
				return false;
			}
			Vec3d vec = result.get();
			int slot = getPaletteSlot(vec);
			// Shouldn't happen under normal conditions
			if (slot == -1) {
				return false;
			}
			EnumHand hand = paletteHand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
			if (interactions.containsKey(hand)) {
				return false;
			}
			ItemStack stack = player.getHeldItem(hand);
			if (stack == null) {
				return false;
			}
			Vec3d target = ItemPaletteModel.dyeCenters[slot];
			if (PaletteAction.BRUSH.isItem(stack)) {
				interactions.put(hand, new InteractionPaletteBrush(stack, player.inventory.currentItem, hand, target, slot));
			} else if (PaletteAction.PALETTE_KNIFE.isItem(stack)) {
				interactions.put(hand, new InteractionPaletteKnife(stack, player.inventory.currentItem, hand, target, slot));
			} else if (PaletteAction.DYE.isItem(stack)) {
				interactions.put(hand, new InteractionPaletteDye(stack, player.inventory.currentItem, hand, target, slot));
			} else {
				return false;
			}
			return true;
		}

		@Override
		protected void readEntityFromNBT(NBTTagCompound compound) {}

		@Override
		protected void writeEntityToNBT(NBTTagCompound compound) {}

		@Override
		public String toString() {
			return player.getName() + "'s palette";
		}
	}

	private abstract static class Interaction {
		protected static final int TRANSFORM_DURATION = 6;

		protected final ItemStack used;

		protected final int actionBarSlot;

		protected final EnumHand hand;

		protected Vec3d target;

		protected int transformTime;

		protected int prevTransformTime;

		public Interaction(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target) {
			this.used = used;
			this.actionBarSlot = hand == EnumHand.OFF_HAND ? -1 : actionBarSlot;
			this.hand = hand;
			this.target = target;
		}

		public void updatePrev() {
			prevTransformTime = transformTime;
		}

		public void update(EntityPlayer player, ItemStack stack, boolean equipped) {
			if (equipped && shouldProgressTransform()) {
				transformTime++;
			}
		}

		public boolean requiresPalette() {
			return true;
		}

		protected boolean shouldProgressTransform() {
			return transformTime < TRANSFORM_DURATION;
		}

		public boolean isDone(EntityPlayer player, ItemStack stack) {
			if (stack == null) {
				return true;
			}
			if (hand == EnumHand.MAIN_HAND) {
				return player.inventory.currentItem != actionBarSlot && isDifferentItem(used, stack);
			}
			return isDifferentItem(used, stack);
		}

		protected boolean isDifferentItem(ItemStack first, ItemStack second) {
			return first.getItem() != second.getItem();
		}

		public abstract void transform(EntityPlayer player, MatrixStack matrix, Matrix4d paletteMatrix, boolean isRight, float delta);
	}

	private static class InteractionBrush extends Interaction {
		private int useTick;

		private boolean isDone;

		private boolean active;

		private boolean reachedCanvas;

		private EntityCanvas canvas;

		private Vec3d prevTilt;

		private Vec3d tilt;

		private Vec3d targetTilt;

		public InteractionBrush(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, EntityCanvas canvas, EntityPlayer player) {
			super(used, actionBarSlot, hand, target);
			this.canvas = canvas;
			prevTilt = tilt = targetTilt = computeTilt(canvas.getLook(1), player.getLook(1));
		}

		@Override
		public boolean requiresPalette() {
			return false;
		}

		@Override
		protected boolean shouldProgressTransform() {
			return super.shouldProgressTransform() && (active || !reachedCanvas);
		}

		@Override
		public boolean isDone(EntityPlayer player, ItemStack stack) {
			return super.isDone(player, stack) || isDone;
		}

		public boolean canPaint() {
			return transformTime == TRANSFORM_DURATION;
		}

		@Override
		public void updatePrev() {
			super.updatePrev();
			if (canPaint()) {
				prevTilt = tilt;	
			}
		}

		@Override
		public void update(EntityPlayer player, ItemStack stack, boolean equipped) {
			if (equipped && shouldProgressTransform()) {
				transformTime++;
				if (transformTime == TRANSFORM_DURATION) {
					reachedCanvas = true;
					if (!active) {
						Painter painter = player.getCapability(CapabilityHandler.PAINTER_CAP, null);
						if (canvas.isEditableBy(player)) {
							painter.stroke(canvas, hand, target, stack);
							painter.finishStroke();
						}
					}
				}
			}
			if (canPaint() && !tilt.equals(targetTilt)) {
				tilt = Mth.lerp(tilt, targetTilt, 0.4).normalize();
				if (tilt.dotProduct(targetTilt) > 0.999) {
					tilt = targetTilt;
				}
			}
			if (reachedCanvas && !active && transformTime > 0) {
				transformTime--;
				if (transformTime == 0) {
					isDone = true;
				}
			}
			active = false;
		}

		@Override
		public void transform(EntityPlayer player, MatrixStack matrix, Matrix4d paletteMatrix, boolean isRight, float delta) {
			double px = Mth.lerp(player.lastTickPosX, player.posX, delta);
			double py = Mth.lerp(player.lastTickPosY, player.posY, delta);
			double pz = Mth.lerp(player.lastTickPosZ, player.posZ, delta);
			float yaw = Mth.lerp(canvas.prevRotationYaw, canvas.rotationYaw, delta);
			float pitch = Mth.lerp(canvas.prevRotationPitch, canvas.rotationPitch, delta);
			Vec3d look = player.getLook(delta);
			findHit(player.getPositionEyes(delta), look, canvas).ifPresent(p -> target = p);
			if (canPaint()) {
				targetTilt = computeTilt(canvas.getLook(delta), look);
			}
			matrix.translate(Mth.lerp(canvas.prevPosX, canvas.posX, delta) - px, Mth.lerp(canvas.prevPosY, canvas.posY, delta) - py, Mth.lerp(canvas.prevPosZ, canvas.posZ, delta) - pz);
			matrix.rotate(-yaw, 0, 1, 0);
			matrix.rotate((pitch + 90), 1, 0, 0);
			matrix.translate(target.xCoord * canvas.getWidth() - canvas.getWidth() / 2D, 1D / 32, target.yCoord * canvas.getHeight() - canvas.getHeight() / 2D);
			matrix.rotate(-135, 1, 0, 0);
			matrix.rotate(90, 0, 1, 0);
			Vec3d ti = canPaint() ? Mth.lerp(prevTilt, tilt, delta) : tilt;
			matrix.rotate(45, -ti.xCoord, ti.yCoord, -ti.zCoord);
			matrix.scale(0.5, 0.5, 0.5);
			matrix.translate(-0.5, -0.5 + 0.0625, 0);
		}

		private Vec3d computeTilt(Vec3d normal, Vec3d look) {
			Vec3d posX = new Vec3d(1, 0, 0);
			// http://math.stackexchange.com/a/476311
			Vec3d cross = normal.crossProduct(posX);
			Matrix3d transform = new Matrix3d();
			transform.setIdentity();
			if (cross.lengthSquared() > 0) {
				Matrix3d vx = new Matrix3d();
				vx.m01 = -cross.zCoord;
				vx.m02 = cross.yCoord;
				vx.m10 = cross.zCoord;
				vx.m12 = -cross.xCoord;
				vx.m20 = -cross.yCoord;
				vx.m21 =  cross.xCoord;
				transform.add(vx);
				vx.mul(vx);
				vx.mul((1 - normal.dotProduct(posX)) / cross.lengthSquared());
				transform.add(vx);
			}
			Vec3d ti = normal.crossProduct(look.subtract(normal.scale(2 * look.dotProduct(normal)))).normalize();
			Vector3d tilt = new Vector3d(ti.xCoord, ti.yCoord, ti.zCoord);
			transform.transform(tilt);
			return new Vec3d(tilt.x, tilt.y, tilt.z);
		}
	}

	private static abstract class InteractionDurated extends Interaction {
		protected final int duration;

		protected int prevTick;

		protected int tick;

		public InteractionDurated(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int duration) {
			super(used, actionBarSlot, hand, target);
			this.duration = duration;
		}

		protected abstract int getUseTick();

		protected abstract void use();

		protected float getTick(float delta) {
			return Mth.lerp(prevTick, tick, delta);
		}

		@Override
		protected boolean shouldProgressTransform() {
			return super.shouldProgressTransform() && tick < TRANSFORM_DURATION;
		}

		@Override
		public boolean isDone(EntityPlayer player, ItemStack stack) {
			return super.isDone(player, stack) || tick >= duration;
		}

		@Override
		public void updatePrev() {
			super.updatePrev();
			prevTick = tick;
		}

		@Override
		public void update(EntityPlayer player, ItemStack stack, boolean equipped) {
			super.update(player, stack, equipped);
			if (tick < duration && equipped) {
				if (tick == getUseTick()) {
					use();
				}
				tick++;
				if (duration - TRANSFORM_DURATION > TRANSFORM_DURATION && tick >= duration - TRANSFORM_DURATION && transformTime > 0) {
					transformTime--;
				}
			}
		}
	}

	private static class InteractionSigningBrush extends InteractionDurated {
		private final EntityCanvas canvas;

		private final Signature.Side side;

		public InteractionSigningBrush(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, EntityCanvas canvas) {
			super(used, actionBarSlot, hand, target, 15);
			this.canvas = canvas;
			side = Signature.Side.forHit(target);
		}

		@Override
		protected int getUseTick() {
			return 6;
		}

		@Override
		protected void use() {
			PaintThis.network.sendToServer(new MessageSignPainting(canvas, hand, target));
		}

		@Override
		public void transform(EntityPlayer player, MatrixStack matrix, Matrix4d paletteMatrix, boolean isRight, float delta) {
			double px = Mth.lerp(player.lastTickPosX, player.posX, delta);
			double py = Mth.lerp(player.lastTickPosY, player.posY, delta);
			double pz = Mth.lerp(player.lastTickPosZ, player.posZ, delta);
			float yaw = Mth.lerp(canvas.prevRotationYaw, canvas.rotationYaw, delta);
			float pitch = Mth.lerp(canvas.prevRotationPitch, canvas.rotationPitch, delta);
			matrix.translate(Mth.lerp(canvas.prevPosX, canvas.posX, delta) - px, Mth.lerp(canvas.prevPosY, canvas.posY, delta) - py, Mth.lerp(canvas.prevPosZ, canvas.posZ, delta) - pz);
			matrix.rotate(-yaw, 0, 1, 0);
			matrix.rotate((pitch + 90), 1, 0, 0);
			double dx = side == Signature.Side.LEFT ? -0.15 : canvas.getWidth() - 0.6;
			matrix.translate((dx + getTick(delta) / duration * 0.7) - canvas.getWidth() / 2D, 1D / 32, canvas.getHeight() - 0.1 - canvas.getHeight() / 2D);
			matrix.rotate(-135, 1, 0, 0);
			matrix.rotate(120, 0, 1, 0);
			matrix.scale(0.5, 0.5, 0.5);
			matrix.translate(-0.5, -0.5 + 0.0625, 0);
		}
	}

	private static abstract class InteractionPalette extends InteractionDurated {
		protected final int slot;

		public InteractionPalette(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int duration, int slot) {
			super(used, actionBarSlot, hand, target, duration);
			this.slot = slot;
		}

		protected abstract PaletteAction getType();

		protected abstract void transform(EntityPlayer player, MatrixStack matrix, float tick, float delta);

		@Override
		protected void use() {
			PaintThis.network.sendToServer(new MessagePaletteInteraction(getType(), hand, slot));
		}

		@Override
		public final void transform(EntityPlayer player, MatrixStack matrix, Matrix4d paletteMatrix, boolean isRight, float delta) {
			matrix.loadIdentity();
			matrix.mul(paletteMatrix);
			if (isRight) {
				matrix.scale(1, 1, -1);
			}
			matrix.translate(target.xCoord, 1 - target.yCoord, 0);
			transform(player, matrix, getTick(delta), delta);
			if (isRight) {
				matrix.scale(1, 1, -1);
			}
		}
	}

	private static class InteractionPaletteBrush extends InteractionPalette {
		public InteractionPaletteBrush(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int slot) {
			super(used, actionBarSlot, hand, target, 16, slot);
		}

		@Override
		protected PaletteAction getType() {
			return PaletteAction.BRUSH;
		}

		@Override
		protected int getUseTick() {
			return 6;
		}

		@Override
		public void transform(EntityPlayer player, MatrixStack matrix, float tick, float delta) {
			float tickSin = (MathHelper.sin(tick * Mth.PI / duration) + 1) / 2;
			matrix.translate(-0.275, -0.55, (1 - tickSin) * 3 + 0.3);
			matrix.rotate(180, 0, 0, 1);
			matrix.rotate(-60, 1, 0, 0);
			matrix.rotate(40, 0, 1, 0);
			matrix.scale(0.6, 0.6, 0.6);
		}
	}

	private static class InteractionPaletteKnife extends InteractionPalette {
		public InteractionPaletteKnife(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int slot) {
			super(used, actionBarSlot, hand, target, 20, slot);
		}

		@Override
		protected PaletteAction getType() {
			return PaletteAction.PALETTE_KNIFE;
		}

		@Override
		protected int getUseTick() {
			return 11;
		}

		@Override
		public void transform(EntityPlayer player, MatrixStack matrix, float tick, float delta) {
			float tickSin = (MathHelper.sin(tick * Mth.PI / duration) + 1) / 2;
			float laterHalf = Math.max(0, (tick / duration - 0.5F) * 2);
			matrix.translate(0.2 - tick / duration * 0.8, 0.2 - tick / duration * 1.5, (1 - tickSin) * 2 + laterHalf + 0.1);
			matrix.rotate(120, 0, 0, 1);
			matrix.rotate(10 - laterHalf * 60, 0, 1, 0);
			matrix.rotate(laterHalf * 90, 1, 0, 0);
			matrix.scale(0.6, 0.6, 0.6);
		}
	}

	private static class InteractionPaletteDye extends InteractionPalette {
		public InteractionPaletteDye(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int slot) {
			super(used, actionBarSlot, hand, target, 20, slot);
		}

		@Override
		protected PaletteAction getType() {
			return PaletteAction.DYE;
		}

		@Override
		protected int getUseTick() {
			return 6;
		}

		@Override
		protected boolean isDifferentItem(ItemStack first, ItemStack second) {
			return super.isDifferentItem(first, second) || first.getMetadata() != second.getMetadata();
		}

		@Override
		public void transform(EntityPlayer player, MatrixStack matrix, float tick, float delta) {
			float tickSin = (MathHelper.sin(tick * Mth.PI / duration) + 1) / 2;
			matrix.translate(-0.5, -0.5, 0.0375 + (1 - tickSin) * 2);
			matrix.rotate(tick / duration * 720, 0, 0, 1);
			matrix.scale(0.3, 0.3, 0.3);
		}
	}
}
