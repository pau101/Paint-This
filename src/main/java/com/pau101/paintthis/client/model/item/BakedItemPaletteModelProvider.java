package com.pau101.paintthis.client.model.item;

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.client.model.TRSRTransformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pau101.paintthis.dye.Dye;

public class BakedItemPaletteModelProvider implements IFlexibleBakedModel, ISmartItemModel {
	private final BakedItemPaletteModel[] models = new BakedItemPaletteModel[256];

	private final ImmutableList<ImmutableList<BakedQuad>> quads;

	private final TextureAtlasSprite particle;

	private final VertexFormat format;

	private final ImmutableMap<TransformType, TRSRTransformation> transforms;

	public BakedItemPaletteModelProvider(ImmutableList<ImmutableList<BakedQuad>> quads, TextureAtlasSprite particle, VertexFormat format, ImmutableMap<TransformType, TRSRTransformation> transforms) {
		this.quads = quads;
		this.particle = particle;
		this.format = format;
		this.transforms = transforms;
	}

	@Override
	public List<BakedQuad> getFaceQuads(EnumFacing facing) {
		return ImmutableList.of();
	}

	@Override
	public List<BakedQuad> getGeneralQuads() {
		return ImmutableList.of();
	}

	@Override
	public boolean isAmbientOcclusion() {
		return false;
	}

	@Override
	public boolean isGui3d() {
		return false;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return particle;
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return ItemCameraTransforms.DEFAULT;
	}

	@Override
	public VertexFormat getFormat() {
		return format;
	}

	@Override
	public IBakedModel handleItemState(ItemStack stack) {
		int index = getModelIndex(stack);
		if (models[index] == null) {
			ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
			quads.addAll(this.quads.get(0));
			for (int i = 0; i < 8; i++) {
				if ((index & 1 << i) != 0) {
					quads.addAll(this.quads.get(i + 1));
				}
			}
			models[index] = new BakedItemPaletteModel(quads.build(), particle, format, transforms, null);
		}
		return models[index];
	}

	private static int getModelIndex(ItemStack stack) {
		int index = 0;
		if (stack.hasTagCompound()) {
			byte[] dyes = stack.getTagCompound().getByteArray("dyes");
			for (int i = 0; i < dyes.length; i++) {
				if (dyes[i] != Dye.NO_DYE) {
					index |= 1 << i;
				}
			}
		}
		return index;
	}
}
