package com.pau101.paintthis.item.brush;

import java.util.List;
import java.util.Optional;

import javax.vecmath.Point3f;

import org.apache.commons.lang3.tuple.Pair;

import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.util.Mth;
import com.pau101.paintthis.util.matrix.Matrix;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;

public abstract class ItemBrush extends Item {
	public static final double REACH = 3;

	private static final Matrix MATRIX = new Matrix(4);

	public ItemBrush() {
		setHasSubtypes(true);
	}

	@Override
	public boolean isFull3D() {
		return true;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		String paintbrush = super.getItemStackDisplayName(stack);
		if (stack.getMetadata() > 0) {
			Dye dye = Dye.getDyeFromDamage(stack.getMetadata() - 1);
			String dyeName = StatCollector.translateToLocal(dye.getCompleteUnlocalizedName());
			paintbrush = StatCollector.translateToLocalFormatted("item.brushDyed", paintbrush, dyeName);
		}
		return paintbrush;
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int renderPass) {
		if (renderPass == 0 || stack.getMetadata() == 0) {
			return super.getColorFromItemStack(stack, renderPass);
		}
		return Dye.getDyeFromDamage(stack.getMetadata() - 1).getColor();
	}

	protected static Optional<Pair<EntityCanvas, Vec3>> findHitCanvas(EntityPlayer player) {
		List<EntityCanvas> canvases = player.worldObj.getEntitiesWithinAABB(EntityCanvas.class, player.getEntityBoundingBox().expand(REACH * 2, REACH * 2, REACH * 2));
		Optional<EntityCanvas> hitCanvas = Optional.<EntityCanvas> empty();
		Optional<Vec3> hitVec = Optional.of(new Vec3(-1, -1, REACH));
		Vec3 origin = player.getPositionEyes(1);
		Vec3 look = player.getLookVec();
		for (EntityCanvas canvas : canvases) {
			if (player.getDistanceToEntity(canvas) > REACH * 2) {
				continue;
			}
			MATRIX.setIdentity();
			MATRIX.rotate(-canvas.rotationYaw * Mth.DEG_TO_RAD, 0, 1, 0);
			MATRIX.rotate((canvas.rotationPitch + 90) * Mth.DEG_TO_RAD, 1, 0, 0);
			float w = canvas.getWidth() / 2F, h = canvas.getHeight() / 2F;
			Point3f v1 = new Point3f(-w, 0.0625F, -h);
			Point3f v2 = new Point3f(w, 0.0625F, -h);
			Point3f v3 = new Point3f(w, 0.0625F, h);
			Point3f v4 = new Point3f(-w, 0.0625F, h);
			MATRIX.transform(v1);
			MATRIX.transform(v2);
			MATRIX.transform(v3);
			MATRIX.transform(v4);
			Point3f pos = new Point3f((float) canvas.posX, (float) canvas.posY, (float) canvas.posZ);
			v1.add(pos);
			v2.add(pos);
			v3.add(pos);
			v4.add(pos);
			Optional<Vec3> result = Mth.intersect(origin, look, getVec3(v1), getVec3(v2), getVec3(v3), getVec3(v4), true);
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

	private static Vec3 getVec3(Point3f p) {
		return new Vec3(p.x, p.y, p.z);
	}

	public static void setDyeIndex(ItemStack brush, int dyeIndex) {
		brush.setTagInfo("dyeIndex", new NBTTagInt(dyeIndex));
	}

	public static int getDyeIndex(ItemStack brush) {
		if (brush == null) {
			return -1;
		}
		if (brush.getItem() instanceof ItemBrush) {
			NBTTagCompound compound = brush.getTagCompound();
			if (compound == null || !compound.hasKey("dyeIndex", 3)) {
				return -1;
			}
			return compound.getInteger("dyeIndex");
		}
		return -1;
	}
}
