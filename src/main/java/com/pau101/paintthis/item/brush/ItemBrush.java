package com.pau101.paintthis.item.brush;

import java.util.List;
import java.util.Optional;

import javax.vecmath.Point3f;

import org.apache.commons.lang3.tuple.Pair;

import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.item.PainterUsable;
import com.pau101.paintthis.util.Mth;
import com.pau101.paintthis.util.matrix.Matrix;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;

public abstract class ItemBrush extends Item implements PainterUsable {
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
			String dyeName = I18n.translateToLocal(dye.getCompleteUnlocalizedName());
			paintbrush = I18n.translateToLocalFormatted("item.brushDyed", paintbrush, dyeName);
		}
		return paintbrush;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return !(newStack.getItem() instanceof ItemBrush);
	}

	public static Optional<Pair<EntityCanvas, Vec3d>> findHitCanvas(EntityPlayer player) {
		List<EntityCanvas> canvases = player.worldObj.getEntitiesWithinAABB(EntityCanvas.class, player.getEntityBoundingBox().expand(REACH * 2, REACH * 2, REACH * 2));
		Optional<EntityCanvas> hitCanvas = Optional.<EntityCanvas> empty();
		Optional<Vec3d> hitVec = Optional.of(new Vec3d(-1, -1, REACH));
		Vec3d origin = player.getPositionEyes(1);
		Vec3d look = player.getLookVec();
		for (EntityCanvas canvas : canvases) {
			if (player.getDistanceToEntity(canvas) > REACH * 2) {
				continue;
			}
			MATRIX.loadIdentity();
			MATRIX.rotate(-canvas.rotationYaw, 0, 1, 0);
			MATRIX.rotate((canvas.rotationPitch + 90), 1, 0, 0);
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
			Optional<Vec3d> result = Mth.intersect(origin, look, getVec3(v1), getVec3(v2), getVec3(v3), getVec3(v4), true);
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

	private static Vec3d getVec3(Point3f p) {
		return new Vec3d(p.x, p.y, p.z);
	}
}
