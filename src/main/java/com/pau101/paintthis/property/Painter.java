package com.pau101.paintthis.property;

import java.util.Optional;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.dye.Dye;
import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.item.brush.ItemPaintbrush;
import com.pau101.paintthis.network.client.MessagePainterPainting;
import com.pau101.paintthis.painting.Painting;

public class Painter implements IExtendedEntityProperties {
	public static final String IDENTIFIER = PaintThis.MODID + ':' + "painter";

	private Optional<Vec3> lastStroke = Optional.<Vec3> empty();

	private Optional<EntityCanvas> lastPaintedCanvas = Optional.<EntityCanvas> empty();

	@Override
	public void init(Entity entity, World world) {}

	@Override
	public void saveNBTData(NBTTagCompound compound) {}

	@Override
	public void loadNBTData(NBTTagCompound compound) {}

	public void finishStroke() {
		if (lastStroke.isPresent()) {
			lastStroke = Optional.<Vec3> empty();
			lastPaintedCanvas = Optional.<EntityCanvas> empty();
		}
	}

	public void stroke(EntityCanvas canvas, Vec3 stroke, ItemStack paintbrush) {
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
			PaintThis.networkWrapper.sendToServer(new MessagePainterPainting(canvas, change.get()));
		}
		lastStroke = Optional.of(stroke);
		lastPaintedCanvas = Optional.of(canvas);
	}
}
