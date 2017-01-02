package com.pau101.paintthis.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

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
import com.pau101.paintthis.item.brush.ItemBrush;
import com.pau101.paintthis.item.brush.ItemPaintbrush;
import com.pau101.paintthis.item.crafting.PositionedItemStack;
import com.pau101.paintthis.network.client.MessageDyeSelect;
import com.pau101.paintthis.painting.Painting;
import com.pau101.paintthis.painting.PaintingDrawable;
import com.pau101.paintthis.util.DyeOreDictHelper;
import com.pau101.paintthis.util.Mth;
import com.pau101.paintthis.util.Util;
import com.pau101.paintthis.util.matrix.Matrix;

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
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
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
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
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
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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

	private static int deltaWheel;

	private static int recipePage;

	private static ItemStack lastRecipeStack;

	@Override
	public void initRenders() {
		RenderingRegistry.registerEntityRenderingHandler(EntityEasel.class, RenderEasel::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityCanvas.class, RenderCanvas::new);
	}

	@Override
	public void initRendersLater() {
		ItemColors colors = Minecraft.getMinecraft().getItemColors();
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
		Minecraft mc = Minecraft.getMinecraft();
		ItemRenderer renderer = new ItemRendererPatch(mc);
		ReflectionHelper.setPrivateValue(Minecraft.class, mc, renderer, "itemRenderer");
		Field field = ReflectionHelper.findField(EntityRenderer.class, "itemRenderer");
		ReflectionHelper.setPrivateValue(Field.class, field, field.getModifiers() & ~Modifier.FINAL, "modifiers");
		try {
			field.set(mc.entityRenderer, renderer);
		} catch (Exception e) {
			e.printStackTrace();
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
		return player.worldObj.isRemote && player == Minecraft.getMinecraft().thePlayer;
	}

	@Override
	public boolean usePalette(EnumHand hand, EnumHand paletteHand) {
		if (canUsePalette()) {
			Dye chosen = getLookingAtDye(paletteHand);
			if (chosen != null) {
				PaintThis.networkWrapper.sendToServer(new MessageDyeSelect(chosen, hand));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isLookingAtDye(EnumHand paletteHand) {
		return getLookingAtDye(paletteHand) != null;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onItemTooltip(ItemTooltipEvent event) {
		if (!shouldShowRecipes() && DyeOreDictHelper.isDye(event.getItemStack()) && shouldShowRecipesInScreen()) {
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
			if (DyeOreDictHelper.isDye(stack)) {
				prepareRenderRecipes(Minecraft.getMinecraft().currentScreen, stack, event.getX(), event.getY());
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		ItemRenderer renderer = mc.getItemRenderer();
		EntityPlayerSP player = mc.thePlayer;
		if (!canUsePalette(mc, player)) {
			return;
		}
		boolean rendered = false;
		float delta = event.getPartialTicks();
		for (EnumHand hand : EnumHand.values()) {
			if (!shouldShowInteractivePalette(player, hand)) {
				continue;
			}
			if (!rendered) {
				mc.entityRenderer.enableLightmap();
				// Don't want colors washed out
				RenderHelper.disableStandardItemLighting();
				rendered = true;
			}
			boolean mainHand = hand == EnumHand.MAIN_HAND;
			ItemStack stack, other;
			if (mainHand) {
				stack = renderer.itemStackMainHand;
				other = renderer.itemStackOffHand;
			} else {
				stack = renderer.itemStackOffHand;
				other = renderer.itemStackMainHand;
			}
			GlStateManager.pushMatrix();
			float yaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * delta;
			boolean isLeft = player.getPrimaryHand() == EnumHandSide.RIGHT == (hand == EnumHand.OFF_HAND);

			float thisPrevEP, thisEP, otherPrevEP, otherEP;
			if (mainHand) {
				thisPrevEP = renderer.prevEquippedProgressMainHand;
				thisEP = renderer.equippedProgressMainHand;
				otherPrevEP = renderer.prevEquippedProgressOffHand;
				otherEP = renderer.equippedProgressOffHand;
			} else {
				thisPrevEP = renderer.prevEquippedProgressOffHand;
				thisEP = renderer.equippedProgressOffHand;
				otherPrevEP = renderer.prevEquippedProgressMainHand;
				otherEP = renderer.equippedProgressMainHand;
			}
			float pep = thisPrevEP, ep = thisEP;
			if (other != null && other.getItem() instanceof ItemBrush) {
				ItemStack heldOther = player.getHeldItem(mainHand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
				if (otherPrevEP > otherEP && heldOther != other || otherPrevEP < otherEP && heldOther == other) {
					pep = otherPrevEP;
					ep = otherEP;
				}
			}
			float equip = 1 - (pep + (ep - pep) * delta);
			MATRIX.setIdentity();
			MATRIX.mult(getMatrix(GL11.GL_MODELVIEW_MATRIX));
			MATRIX.rotate(-yaw, 0, 1, 0);
			MATRIX.translate(isLeft ? 0.25F : -0.25F, player.isSneaking() ? 1.17F : 1.25F, 0.4F);
			MATRIX.rotate(70, 1, 0, 0);
			if (isLeft) {
				MATRIX.rotate(180, 0, 1, 0);
			}
			MATRIX.scale(0.5F, 0.5F, 0.5F);
			Matrix4d modelView1 = MATRIX.getTransform();
			Matrix4d perspective1 = getMatrix(GL11.GL_PROJECTION_MATRIX);
			float fov = 70;
			Entity entity = mc.getRenderViewEntity();
			if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) {
				float deathTime = (float) ((EntityLivingBase) entity).deathTime + delta;
				fov /= (1 - 500 / (deathTime + 500)) * 2 + 1;
			}
			IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.theWorld, entity, delta);
			if (state.getMaterial() == Material.WATER) {
				fov = fov * 60 / 70;
			}
			fov = ForgeHooksClient.getFOVModifier(mc.entityRenderer, entity, state, delta, fov);
			boolean rightSide = (mainHand ? player.getPrimaryHand() : player.getPrimaryHand().opposite()) == EnumHandSide.RIGHT;
			float farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16;
			float walkDelta = player.distanceWalkedModified - player.prevDistanceWalkedModified;
			float walked = -(player.distanceWalkedModified + walkDelta * delta);
			float camYaw = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * delta;
			float camPitch = player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * delta;
			float swing = player.getSwingProgress(delta);
			float thisSwing = mainHand ? swing : 0;
			float armPitch = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * delta;
			float armYaw = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * delta;
			float swingX = -0.4F * MathHelper.sin(MathHelper.sqrt_float(thisSwing) * Mth.PI);
			float swingY = 0.2F * MathHelper.sin(MathHelper.sqrt_float(thisSwing) * Mth.TAU);
			float swingZ = -0.2F * MathHelper.sin(thisSwing * Mth.PI);
			float swingAmt = MathHelper.sin(thisSwing * thisSwing * Mth.PI);
			float swingAng = MathHelper.sin(MathHelper.sqrt_float(thisSwing) * Mth.PI);
			int side = rightSide ? 1 : -1;
			MATRIX.setIdentity();
			if (mc.gameSettings.anaglyph) {
				MATRIX.translate(-(EntityRenderer.anaglyphField * 2 - 1) * 0.07F, 0, 0);
			}
			MATRIX.perspective(fov, mc.displayWidth / (float) mc.displayHeight, 0.05F, farPlaneDistance * 2);
			Matrix4d perspective2 = MATRIX.getTransform();
			MATRIX.setIdentity();
			if (mc.gameSettings.anaglyph) {
				MATRIX.translate((EntityRenderer.anaglyphField * 2 - 1) * 0.1F, 0, 0);
			}
			MATRIX.translate(MathHelper.sin(walked * Mth.PI) * camYaw * 0.5F, -Math.abs(MathHelper.cos(walked * Mth.PI) * camYaw), 0);
			MATRIX.rotate(MathHelper.sin(walked * Mth.PI) * camYaw * 3, 0, 0, 1);
			MATRIX.rotate(Math.abs(MathHelper.cos(walked * Mth.PI - 0.2F) * camYaw) * 5 + camPitch, 1, 0, 0);
			MATRIX.rotate((player.rotationPitch - armPitch) * 0.1F, 1, 0, 0);
			MATRIX.rotate((player.rotationYaw - armYaw) * 0.1F, 0, 1, 0);
			MATRIX.translate(side * swingX + side * 0.56F, swingY - 0.52F, swingZ - 0.72F);
			MATRIX.rotate(side * (45 + swingAmt * -20), 0, 1, 0);
			MATRIX.rotate(side * swingAng * -20, 0, 0, 1);
			MATRIX.rotate(swingAng * -80, 1, 0, 0);
			MATRIX.rotate(side * -45, 0, 1, 0);
			Matrix4d modelView2 = MATRIX.getTransform();
			Matrix4d modelView = interpolate(modelView1, modelView2, equip);
			Matrix4d perspective = interpolatePerspective(perspective1, perspective2, equip);
			GlStateManager.loadIdentity();
			multMatrix(modelView);
			GlStateManager.matrixMode(GL11.GL_PROJECTION);
			GlStateManager.loadIdentity();
			multMatrix(perspective);
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
			mc.getRenderItem().renderItem(stack, TransformType.FIXED);
			GlStateManager.popMatrix();
		}
		if (rendered) {
			mc.entityRenderer.disableLightmap();
		}
	}

	private static final FloatBuffer MATRIX_BUF = BufferUtils.createFloatBuffer(16);

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
		double scale = interpolate(aScale, bScale, t);
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
		double fov = interpolate(aFov, bFov, t);
		double aspect = interpolate(aAspect, bAspect, t);
		double zNear = interpolate(aZNear, bZNear, t);
		double zFar = interpolate(bZFar, bZFar, t);
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

	private static double interpolate(double a, double b, double t) {
		return a + (b - a) * t;
	}

	@SubscribeEvent
	public void renderSpecificHand(RenderSpecificHandEvent event) {
		if (shouldShowInteractivePalette(Minecraft.getMinecraft().thePlayer, event.getHand())) {
			event.setCanceled(true);
		}
	}

	private boolean shouldShowInteractivePalette(EntityPlayer player, EnumHand hand) {
		ItemRenderer renderer = Minecraft.getMinecraft().getItemRenderer();
		ItemStack held, other;
		float ep;
		if (hand == EnumHand.MAIN_HAND) {
			held = renderer.itemStackMainHand;
			other = renderer.itemStackOffHand;
			ep = renderer.equippedProgressOffHand;
		} else {
			held = renderer.itemStackOffHand;
			other = renderer.itemStackMainHand;
			ep = renderer.equippedProgressMainHand;
		}
//		System.out.println(held);
		if (held != null && held.getItem() == PaintThis.palette) {
			return other != null && other.getItem() instanceof ItemBrush && ep > 0;
		}
		return false;
	}

	private Dye getLookingAtDye(EnumHand hand) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.thePlayer;
		ItemStack stack = player.getHeldItem(hand);
		if (stack != null && canUsePalette(mc, player) && ItemPalette.hasDyes(stack)) {
			Vec3d origin = new Vec3d(0, player.getEyeHeight(), 0);
			Vec3d look = player.getLookVec();
			MATRIX.setIdentity();
			MATRIX.rotate(-player.renderYawOffset, 0, 1, 0);
			boolean isLeft = player.getPrimaryHand() == EnumHandSide.RIGHT == (hand == EnumHand.OFF_HAND);
			MATRIX.translate(isLeft ? 0.25F : -0.25F, player.isSneaking() ? 1.17F : 1.25F, 0.4F);
			MATRIX.rotate(70, 1, 0, 0);
			if (isLeft) {
				MATRIX.rotate(180, 0, 1, 0);
			}
			MATRIX.scale(0.5F, 0.5F, 0.5F);
			MATRIX.translate(-0.5F, -0.5F, isLeft ? 0.03125F : -0.03125F);
			Point3f v1 = new Point3f(0, 1, 0);
			Point3f v2 = new Point3f(1, 1, 0);
			Point3f v3 = new Point3f(1, 0, 0);
			Point3f v4 = new Point3f(0, 0, 0);
			MATRIX.transform(v1);
			MATRIX.transform(v2);
			MATRIX.transform(v3);
			MATRIX.transform(v4);
			Optional<Vec3d> result = Mth.intersect(origin, look, getVec3(v1), getVec3(v2), getVec3(v3), getVec3(v4), false);
			if (result.isPresent()) {
				Vec3d vec = result.get();
				int px = (int) (vec.xCoord * ItemPaletteModel.textureSize), py = (int) (vec.yCoord * ItemPaletteModel.textureSize);
				byte layers = ItemPaletteModel.dyeRegions[px + py * ItemPaletteModel.textureSize];
				int slot = -1;
				for (int i = 7; i >= 0; i--) {
					if (((layers >> i) & 1) == 1) {
						slot = i;
						break;
					}
				}
				if (slot > -1) {
					return ItemPalette.getDye(stack, slot);
				}
			}
		}
		return null;
	}

	private static Vec3d getVec3(Point3f p) {
		return new Vec3d(p.x, p.y, p.z);
	}

	private boolean canUsePalette() {
		Minecraft mc = Minecraft.getMinecraft();
		return canUsePalette(mc, mc.thePlayer);		
	}

	private boolean canUsePalette(Minecraft mc, EntityPlayer player) {
		return player != null && player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0 && !mc.gameSettings.hideGUI && !player.isPlayerSleeping() && !mc.playerController.isSpectator();
	}

	private static boolean shouldShowRecipes() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	private static boolean shouldShowRecipesInScreen() {
		GuiScreen screen = Minecraft.getMinecraft().currentScreen;
		return !(screen instanceof GuiContainerCreative) || ((GuiContainerCreative) screen).getSelectedTabIndex() != CreativeTabs.SEARCH.getTabIndex();
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
		RenderItem render = Minecraft.getMinecraft().getRenderItem();
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
					TextureManager tex = Minecraft.getMinecraft().getTextureManager();
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
			TextureManager tex = Minecraft.getMinecraft().getTextureManager();
			tex.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			tex.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
		}
	}

	public static BufferedImage getImage(ResourceLocation id) throws IOException {
		return ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(id).getInputStream());
	}
}
