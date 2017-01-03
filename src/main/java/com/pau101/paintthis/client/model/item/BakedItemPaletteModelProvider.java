package com.pau101.paintthis.client.model.item;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pau101.paintthis.dye.Dye;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.model.TRSRTransformation;

public class BakedItemPaletteModelProvider implements IBakedModel {
	private final ItemOverrideList override = new Provider();

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
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		return Collections.EMPTY_LIST;
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
	public ItemOverrideList getOverrides() {
		return override;
	}

	private class Provider extends ItemOverrideList {
		public Provider() {
			super(Collections.EMPTY_LIST);
		}

		@Override
		public IBakedModel handleItemState(IBakedModel model, ItemStack stack, World world, EntityLivingBase entity) {
			int index = getModelIndex(stack);
			if (models[index] == null) {
				ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
				quads.addAll(BakedItemPaletteModelProvider.this.quads.get(0));
				for (int i = 0; i < 8; i++) {
					if ((index & 1 << i) != 0) {
						quads.addAll(BakedItemPaletteModelProvider.this.quads.get(i + 1));
					}
				}
				models[index] = new BakedItemPaletteModel(quads.build(), particle, format, transforms, null);
			}
			return models[index];
		}
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
