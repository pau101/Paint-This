package com.pau101.paintthis.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
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
import com.pau101.paintthis.painting.Painting;
import com.pau101.paintthis.painting.PaintingDrawable;
import com.pau101.paintthis.util.CubicBezier;
import com.pau101.paintthis.util.Mth;
import com.pau101.paintthis.util.OreDictUtil;
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
import net.minecraft.util.text.TextFormatting;
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

	private static final Matrix MATRIX = new Matrix(4);

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
	public boolean isLookingAtDye(EnumHand paletteHand) {
		return getLookingAtDye(paletteHand) != null;
	}

	@SubscribeEvent
	public void onJoinWorld(EntityEvent.EnteringChunk event) {
		Entity e = event.getEntity();
		if (e instanceof EntityPlayerSP) {
			World world = mc.theWorld;
			Entity palette = new ACompletelyNormalEntity(world, (EntityPlayerSP) e);
			palette.setPosition(e.posX, e.posY, e.posZ);
			world.spawnEntityInWorld(palette);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onItemTooltip(ItemTooltipEvent event) {
		if (!shouldShowRecipes() && OreDictUtil.isDye(event.getItemStack()) && shouldShowRecipesInScreen()) {
			event.getToolTip().add(TextFormatting.BLUE + I18n.format("dyeRecipes.tip"));
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
		if (event.phase == Phase.END && !Minecraft.getMinecraft().isGamePaused()) {
			ItemRenderer renderer = mc.getItemRenderer();
			ItemStack mainHand = renderer.itemStackMainHand;
			ItemStack offHand = renderer.itemStackOffHand;
			if (mainHand != prevMainHand) {
				lastEquipMainHand = prevMainHand;
			}
			if (offHand != prevOffHand) {
				lastEquipOffHand = prevOffHand;
			}
			Iterator<Entry<EnumHand, Interaction>> inters = interactions.entrySet().iterator();
			while (inters.hasNext()) {
				Entry<EnumHand, Interaction> e = inters.next();
				EnumHand hand = e.getKey();
				Interaction interaction = e.getValue();
				interaction.update(interaction.actionBarSlot == mc.thePlayer.inventory.currentItem);
				if (interaction.isDone(mc.thePlayer, hand == EnumHand.MAIN_HAND ? mainHand : offHand)) {
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
		if (!canUsePalette(player)) {
			return;
		}
		float delta = event.getPartialTicks();
		Matrix4d paletteMatrix;
		if (shouldShowInteractivePalette(player, EnumHand.MAIN_HAND)) {
			paletteMatrix = renderPalette(player, EnumHand.MAIN_HAND, renderer, delta);
		} else if (shouldShowInteractivePalette(player, EnumHand.OFF_HAND)) {
			paletteMatrix = renderPalette(player, EnumHand.OFF_HAND, renderer, delta);
		} else {
			paletteMatrix = null;
		}
		mc.entityRenderer.enableLightmap();
		RenderHelper.enableStandardItemLighting();
		for (Interaction inter : interactions.values()) {
			renderInteraction(player, inter, paletteMatrix, renderer, delta);
		}
		mc.entityRenderer.disableLightmap();
	}

	private Matrix4d renderPalette(EntityPlayerSP player, EnumHand hand, ItemRenderer renderer, float delta) {
		mc.entityRenderer.enableLightmap();
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
		float yaw = Mth.interpolate(player.prevRenderYawOffset, player.renderYawOffset, delta);
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
		float equip = 1 - (pep + (ep - pep) * delta);
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
		float equip = 1 - (pep + (ep - pep) * delta);
		ItemStack heldEquip = player.getHeldItem(interaction.hand);
		if (player.inventory.currentItem == interaction.actionBarSlot) {
			equip = 1 - (interaction.prevTransformTime + (interaction.transformTime - interaction.prevTransformTime) * delta) / Interaction.TRANSFORM_DURATION;
			equip = TRANSFORM_CURVE.eval(equip);
		} else {
			if (equip < 1 && stack != heldEquip) {
				equip = 1 - EQUIP_CURVE.eval(1 - equip);
			} else {
				equip = EQUIP_CURVE.eval(equip);
			}
		}
		if (equip == 0) {
			interaction.transform(GL_MATRIX, paletteMatrix, delta);
		} else {
			float yaw = Mth.interpolate(player.prevRenderYawOffset, player.renderYawOffset, delta);
			interpolateWorldToHeld(player, interaction.used, yaw, isLeft, mainHand, (m, p, y, l) -> interaction.transform(m, paletteMatrix, delta), equip, equip == 1 ? 1 : 0, delta);
		}
		mc.getRenderItem().renderItem(stack, player, TransformType.NONE, false);
		GlStateManager.popMatrix();
	}

	private static void interpolateWorldToHeld(EntityPlayerSP player, ItemStack stack, float yaw, boolean isLeft, boolean isMainHand, ModelViewTransform worldTransform, float t, float regularEquip, float delta) {
		MATRIX.loadIdentity();
		MATRIX.mul(getMatrix(GL11.GL_MODELVIEW_MATRIX));
		worldTransform.transform(MATRIX, player, yaw, isLeft);
		Matrix4d modelView1 = MATRIX.getTransform();
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
		MATRIX.loadIdentity();
		if (mc.gameSettings.anaglyph) {
			MATRIX.translate(-(EntityRenderer.anaglyphField * 2 - 1) * 0.07F, 0, 0);
		}
		MATRIX.perspective(fov, mc.displayWidth / (float) mc.displayHeight, 0.05F, farPlaneDistance * 2);
		Matrix4d perspective2 = MATRIX.getTransform();
		MATRIX.loadIdentity();
		if (mc.gameSettings.anaglyph) {
			MATRIX.translate((EntityRenderer.anaglyphField * 2 - 1) * 0.1F, 0, 0);
		}
		MATRIX.translate(MathHelper.sin(walked * Mth.PI) * camYaw * 0.5F, -Math.abs(MathHelper.cos(walked * Mth.PI) * camYaw), 0);
		MATRIX.rotate(MathHelper.sin(walked * Mth.PI) * camYaw * 3, 0, 0, 1);
		MATRIX.rotate(Math.abs(MathHelper.cos(walked * Mth.PI - 0.2F) * camYaw) * 5 + camPitch, 1, 0, 0);
		MATRIX.rotate((player.rotationPitch - armPitch) * 0.1F, 1, 0, 0);
		MATRIX.rotate((player.rotationYaw - armYaw) * 0.1F, 0, 1, 0);
		MATRIX.translate(side * swingX + side * 0.56F, swingY - 0.52F - regularEquip * 0.6F, swingZ - 0.72F);
		MATRIX.rotate(side * (45 + swingAmt * -20), 0, 1, 0);
		MATRIX.rotate(side * swingAng * -20, 0, 0, 1);
		MATRIX.rotate(swingAng * -80, 1, 0, 0);
		MATRIX.rotate(side * -45, 0, 1, 0);
		IBakedModel model = mc.getRenderItem().getItemModelWithOverrides(stack, mc.theWorld, mc.thePlayer);
		TransformType transform = isLeft ? TransformType.FIRST_PERSON_LEFT_HAND : TransformType.FIRST_PERSON_RIGHT_HAND;
		if (model instanceof IPerspectiveAwareModel) {
			Pair<? extends IBakedModel, Matrix4f> pair = ((IPerspectiveAwareModel) model).handlePerspective(transform);
			if (pair.getRight() != null) {
				Matrix4d matrix = new Matrix4d(pair.getRight());
				if (isLeft) {
					matrix.mul(FLIP_X, matrix);
					matrix.mul(matrix, FLIP_X);
				}
				MATRIX.mul(matrix);
			}
		} else {
			ItemTransformVec3f vec = model.getItemCameraTransforms().getTransform(transform);
			if (vec != ItemTransformVec3f.DEFAULT) {
				MATRIX.translate(side * (ItemCameraTransforms.offsetTranslateX + vec.translation.x), ItemCameraTransforms.offsetTranslateY + vec.translation.y, ItemCameraTransforms.offsetTranslateZ + vec.translation.z);
				float rx = ItemCameraTransforms.offsetRotationX + vec.rotation.x;
				float ry = ItemCameraTransforms.offsetRotationY + vec.rotation.y;
				float rz = ItemCameraTransforms.offsetRotationZ + vec.rotation.z;
				if (isLeft) {
					ry = -ry;
					rz = -rz;
				}
				MATRIX.rotate(makeQuaternion(rx, ry, rz));
				MATRIX.scale(ItemCameraTransforms.offsetScaleX + vec.scale.x, ItemCameraTransforms.offsetScaleY + vec.scale.y, ItemCameraTransforms.offsetScaleZ + vec.scale.z);
			}
		}
		Matrix4d modelView2 = MATRIX.getTransform();
		Matrix4d modelView = interpolate(modelView1, modelView2, t);
		Matrix4d perspective = interpolatePerspective(perspective1, perspective2, t);
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		multMatrix(perspective);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GlStateManager.loadIdentity();
		multMatrix(modelView);
	}

	@FunctionalInterface
	private interface ModelViewTransform {
		void transform(MatrixStack matrix, EntityPlayerSP player, float yaw, boolean isLeft);
	}

	private static Quat4d makeQuaternion(float x, float y, float z) {
		float rx = x * Mth.DEG_TO_RAD;
		float ry = y * Mth.DEG_TO_RAD;
		float rz = z * Mth.DEG_TO_RAD;
		float xs = MathHelper.sin(0.5F * rx);
		float xc = MathHelper.cos(0.5F * rx);
		float ys = MathHelper.sin(0.5F * ry);
		float yc = MathHelper.cos(0.5F * ry);
		float zs = MathHelper.sin(0.5F * rz);
		float zc = MathHelper.cos(0.5F * rz);
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

	private static Matrix4d interpolate(Matrix4d a, Matrix4d b, double t) {
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
		double scale = Mth.interpolate(aScale, bScale, t);
		Matrix4d mat = new Matrix4d();
		mat.set(aTrans);
		Matrix4d scratch = new Matrix4d();
		scratch.set(aRot);
		mat.mul(scratch);
		scratch.set(scale);
		mat.mul(scratch);
		return mat;
	}

	private static Matrix4d interpolatePerspective(Matrix4d a, Matrix4d b, double t) {
		double aFov = Math.asin(1 / Math.sqrt(a.m11 * a.m11 + 1));
		double bFov =Math.asin(1 / Math.sqrt(b.m11 * b.m11 + 1));
		double aAspect =  a.m11 / a.m00;
		double bAspect = b.m11 / b.m00;
		double aZNear = a.m23 / (a.m22 - 1);
		double aZFar = a.m23 / (a.m22 + 1);
		double bZFar =b.m23 / (b.m22 + 1);
		double bZNear = b.m23 / (b.m22 - 1);
		double fov = Mth.interpolate(aFov, bFov, t);
		double aspect = Mth.interpolate(aAspect, bAspect, t);
		double zNear = Mth.interpolate(aZNear, bZNear, t);
		double zFar = Mth.interpolate(bZFar, bZFar, t);
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

	private static Dye getLookingAtDye(EnumHand hand) {
		return getLookingAtDye(mc.thePlayer, hand);
	}

	private static Dye getLookingAtDye(EntityPlayer player, EnumHand hand) {
		int idx = getLookingAtSlot(player, hand);
		return idx == -1 ? null : ItemPalette.getDye(player.getHeldItem(hand), idx);
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
			MATRIX.loadIdentity();
			boolean isLeft = player.getPrimaryHand() == EnumHandSide.RIGHT == (hand == EnumHand.OFF_HAND);
			transformPalette(MATRIX, player, player.renderYawOffset, isLeft);
			MATRIX.translate(-0.5, -0.5, -0.5);
			float top = isLeft ? 8.5F / 16 : 7.5F / 16;
			Point3f v1 = new Point3f(0, 1, top);
			Point3f v2 = new Point3f(1, 1, top);
			Point3f v3 = new Point3f(1, 0, top);
			Point3f v4 = new Point3f(0, 0, top);
			MATRIX.transform(v1);
			MATRIX.transform(v2);
			MATRIX.transform(v3);
			MATRIX.transform(v4);
			return Mth.intersect(origin, look, asVec3(v1), asVec3(v2), asVec3(v3), asVec3(v4), false);
		}
		return Optional.empty();
	}

	private static int getPaletteSlot(Vec3d vec) {
		int px = (int) (vec.xCoord * ItemPaletteModel.textureSize), py = (int) (vec.yCoord * ItemPaletteModel.textureSize);
		byte layers = ItemPaletteModel.dyeRegions[px + py * ItemPaletteModel.textureSize];
		int slot = -1;
		for (int i = 7; i >= 0; i--) {
			if (((layers >> i) & 1) == 1) {
				slot = i;
				break;
			}
		}
		return slot;
	}

	private static Vec3d asVec3(Point3f p) {
		return new Vec3d(p.x, p.y, p.z);
	}

	private static boolean canUsePalette() {
		return canUsePalette(mc.thePlayer);
	}

	private static boolean canUsePalette(EntityPlayer player) {
		return ItemPaletteModel.textureSize > -1 && player != null && player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0 && !mc.gameSettings.hideGUI && !player.isPlayerSleeping() && !mc.playerController.isSpectator();
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
		private final EntityPlayerSP player;

		public ACompletelyNormalEntity(World world, EntityPlayerSP player) {
			super(world);
			this.player = player;
			setEntityId(-2);
			setSize(0, 0);
			noClip = true;
			forceSpawn = true;
		}

		@Override
		protected void entityInit() {}

		@Override
		public boolean canBeCollidedWith() {
			return true;
		}

		@Override
		public boolean shouldRenderInPass(int pass) {
			return false;
		}

		@Override
		public AxisAlignedBB getEntityBoundingBox() {
			if (getLookingAtSlot(player, EnumHand.MAIN_HAND) > -1 || getLookingAtSlot(player, EnumHand.OFF_HAND) > -1) {
				return player.getEntityBoundingBox();
			}
			return super.getEntityBoundingBox();
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
			if (result.isPresent()) {
				Vec3d vec = result.get();
				int slot = getPaletteSlot(vec);
				EnumHand hand = paletteHand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
				ItemStack stack = player.getHeldItem(hand);
				if (stack == null) {
					return false;
				}
				if (PaletteAction.BRUSH.isItem(stack)) {
					interactions.put(hand, new InteractionPaletteBrush(stack, player.inventory.currentItem, hand, vec, slot));
				} else if (PaletteAction.PALETTE_KNIFE.isItem(stack)) {
					interactions.put(hand, new InteractionPaletteKnife(stack, player.inventory.currentItem, hand, vec, slot));
				} else if (PaletteAction.DYE.isItem(stack)) {
					interactions.put(hand, new InteractionPaletteDye(stack, player.inventory.currentItem, hand, vec, slot));
				} else {
					return false;
				}
				return true;
			}
			return false;
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

	public static class Interaction {
		protected static final int TRANSFORM_DURATION = 6;

		protected final ItemStack used;

		protected final int actionBarSlot;

		protected final EnumHand hand;

		protected Vec3d target;

		protected int transformTime;

		protected int prevTransformTime;

		public Interaction(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target) {
			this.used = used;
			this.actionBarSlot = actionBarSlot;
			this.hand = hand;
			this.target = target;
		}

		public void update(boolean equipped) {
			prevTransformTime = transformTime;
			if (equipped && shouldProgressTransform()) {
				transformTime++;
			}
		}

		protected boolean shouldProgressTransform() {
			return transformTime < TRANSFORM_DURATION;
		}

		public boolean isDone(EntityPlayer player, ItemStack stack) {
			return stack == null || stack.getItem() != used.getItem() || player.inventory.currentItem != actionBarSlot;
		}

		public void transform(MatrixStack matrix, Matrix4d paletteMatrix, float delta) {}
	}

	public static class InteractionBrush extends Interaction {
		public InteractionBrush(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target) {
			super(used, actionBarSlot, hand, target);
		}

		@Override
		public boolean isDone(EntityPlayer player, ItemStack stack) {
			return super.isDone(player, stack);
		}
	}

	public static abstract class InteractionPalette extends Interaction {
		protected final int slot;

		protected final int duration;

		protected int prevTick;

		protected int tick;

		public InteractionPalette(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int slot, int duration) {
			super(used, actionBarSlot, hand, target);
			this.slot = slot;
			this.duration = duration;
		}

		protected abstract PaletteAction getType();

		protected abstract int getUseTick();

		@Override
		public void update(boolean equipped) {
			super.update(equipped);
			prevTick = tick;
			if (tick < duration && equipped) {
				if (tick == getUseTick()) {
					PaintThis.networkWrapper.sendToServer(new MessagePaletteInteraction(getType(), hand, slot));
				}
				tick++;
				if (duration - TRANSFORM_DURATION > TRANSFORM_DURATION && tick >= duration - TRANSFORM_DURATION && transformTime > 0) {
					transformTime--;
				}
			}
		}

		protected float getTick(float delta) {
			return prevTick + (tick - prevTick) * delta;
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
		public void transform(MatrixStack matrix, Matrix4d paletteMatrix, float delta) {
			matrix.loadIdentity();
			matrix.mul(paletteMatrix);
			matrix.translate(target.xCoord, 1 - target.yCoord, 0);
		}
	}

	public static class InteractionPaletteBrush extends InteractionPalette {
		public InteractionPaletteBrush(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int slot) {
			super(used, actionBarSlot, hand, target, slot, 16);
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
		public void transform(MatrixStack matrix, Matrix4d paletteMatrix, float delta) {
			super.transform(matrix, paletteMatrix, delta);
			float tick = getTick(delta);
			float tickSin = (MathHelper.sin(tick * Mth.PI / duration) + 1) / 2;
			matrix.translate(-0.275, -0.55, (1 - tickSin) * 3 + 0.3);
			matrix.rotate(180, 0, 0, 1);
			matrix.rotate(-60, 1, 0, 0);
			matrix.rotate(40, 0, 1, 0);
			matrix.scale(0.6, 0.6, 0.6);
		}
	}

	public static class InteractionPaletteKnife extends InteractionPalette {
		public InteractionPaletteKnife(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int slot) {
			super(used, actionBarSlot, hand, target, slot, 20);
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
		public void transform(MatrixStack matrix, Matrix4d paletteMatrix, float delta) {
			super.transform(matrix, paletteMatrix, delta);
			float tick = getTick(delta);
			float tickSin = (MathHelper.sin(tick * Mth.PI / duration) + 1) / 2;
			float laterHalf = Math.max(0, (tick / duration - 0.5F) * 2);
			matrix.translate(0.2 - tick / duration * 0.8, 0.2 - tick / duration * 1.5, (1 - tickSin) * 2 + laterHalf + 0.1);
			matrix.rotate(120, 0, 0, 1);
			matrix.rotate(10 - laterHalf * 60, 0, 1, 0);
			matrix.rotate(laterHalf * 90, 1, 0, 0);
			matrix.scale(0.6, 0.6, 0.6);
		}
	}

	public static class InteractionPaletteDye extends InteractionPalette {
		public InteractionPaletteDye(ItemStack used, int actionBarSlot, EnumHand hand, Vec3d target, int slot) {
			super(used, actionBarSlot, hand, target, slot, 20);
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
		public void transform(MatrixStack matrix, Matrix4d paletteMatrix, float delta) {
			super.transform(matrix, paletteMatrix, delta);
			float tick = getTick(delta);
			float tickSin = (MathHelper.sin(tick * Mth.PI / duration) + 1) / 2;
			matrix.translate(-0.5, -0.5, 0.0375 + (1 - tickSin) * 2);
			matrix.rotate(tick / duration * 720, 0, 0, 1);
			matrix.scale(0.3, 0.3, 0.3);
		}
	}
}
