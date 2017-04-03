package com.pau101.paintthis.server.capability;

import com.pau101.paintthis.PaintThis;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.INBTSerializable;

public final class CapabilityHandler {
	private CapabilityHandler() {}

	public static final ResourceLocation PAINTER_ID = new ResourceLocation(PaintThis.ID, "painter");

	public static final ResourceLocation HORSE_SHEARABILITY_ID = new ResourceLocation(PaintThis.ID, "horse_shearability");

	@CapabilityInject(Painter.class)
	public static Capability<Painter> PAINTER_CAP = null;

	@CapabilityInject(HorseShearability.class)
	public static Capability<HorseShearability> HORSER_SHEARABILITY_CAP = null;

	public static void init() {
		CapabilityManager.INSTANCE.register(Painter.class, new Storage(), PainterDefault::new);
		CapabilityManager.INSTANCE.register(HorseShearability.class, new Storage(), HorseShearabilityDefault::new);
	}

	public static class Storage<T extends INBTSerializable<NBTTagCompound>> implements IStorage<T> {
		@Override
		public NBTTagCompound writeNBT(Capability<T> capability, T instance, EnumFacing side) {
			return instance.serializeNBT();
		}

		@Override
		public void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt) {
			instance.deserializeNBT((NBTTagCompound) nbt);
		}
	}
}
