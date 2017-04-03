package com.pau101.paintthis.server.capability;

import java.util.Optional;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.dye.Dye;
import com.pau101.paintthis.server.entity.item.EntityCanvas;
import com.pau101.paintthis.server.item.brush.ItemPaintbrush;
import com.pau101.paintthis.server.net.serverbound.MessagePainterPainting;
import com.pau101.paintthis.server.painting.Painting;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;

public final class PainterDefault implements Painter {
	private Optional<Vec3d> lastStroke = Optional.<Vec3d> empty();

	private Optional<EntityCanvas> lastPaintedCanvas = Optional.<EntityCanvas> empty();

	@Override
	public void stroke(EntityCanvas canvas, EnumHand hand, Vec3d stroke, ItemStack paintbrush) {
		if (lastPaintedCanvas.isPresent()) {
			if (lastPaintedCanvas.get() != canvas) {
				finishStroke();
			}
		}
		Painting painting = canvas.getPainting();
		int size = ((ItemPaintbrush) paintbrush.getItem()).getSize();
		Dye dye = Dye.getDyeFromDamage(paintbrush.getMetadata() - 1);
		painting.beginRecordingChange();
		if (lastStroke.isPresent()) {
			painting.stroke(lastStroke.get(), stroke, size, dye);
		} else {
			painting.dot(stroke, size, dye);
		}
		Optional<Painting.Change> change = painting.endAndRemoveChange();
		if (change.isPresent()) {
			PaintThis.network.sendToServer(new MessagePainterPainting(canvas, hand, change.get()));
		}
		lastStroke = Optional.of(stroke);
		lastPaintedCanvas = Optional.of(canvas);
	}

	@Override
	public void finishStroke() {
		if (lastStroke.isPresent()) {
			lastStroke = Optional.<Vec3d> empty();
			lastPaintedCanvas = Optional.<EntityCanvas> empty();
		}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityHandler.PAINTER_CAP;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return hasCapability(capability, facing) ? CapabilityHandler.PAINTER_CAP.cast(this) : null;
	}
	@Override
	public NBTTagCompound serializeNBT() {
		return new NBTTagCompound();
	}

	@Override
	public void deserializeNBT(NBTTagCompound compound) {}
}
