package com.pau101.paintthis.client.model.item;

import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.common.model.TRSRTransformation;

public final class BakedItemPaletteModel implements IBakedModel, IPerspectiveAwareModel {
	private final ImmutableList<BakedQuad> quads;

	private final TextureAtlasSprite particle;

	private final VertexFormat format;

	private final ImmutableMap<TransformType, TRSRTransformation> transforms;

	private final BakedItemPaletteModel otherModel;

	private final boolean isCulled;

	public BakedItemPaletteModel(ImmutableList<BakedQuad> quads, TextureAtlasSprite particle, VertexFormat format, ImmutableMap<TransformType, TRSRTransformation> transforms, BakedItemPaletteModel otherModel) {
		this.quads = quads;
		this.particle = particle;
		this.format = format;
		this.transforms = transforms;
		if (otherModel != null) {
			this.otherModel = otherModel;
			isCulled = true;
		} else {
			ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
			for (BakedQuad quad : quads) {
				if (quad.getFace() == EnumFacing.SOUTH) {
					builder.add(quad);
				}
			}
			this.otherModel = new BakedItemPaletteModel(builder.build(), particle, format, transforms, this);
			isCulled = false;
		}
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		return quads;
	}

	@Override
	public boolean isAmbientOcclusion() {
		return true;
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
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
		Pair<? extends IBakedModel, Matrix4f> pair = MapWrapper.handlePerspective(this, transforms, type);
		if (type == TransformType.GUI && !isCulled && pair.getRight() == null) {
			return Pair.of(otherModel, null);
		} else if (type != TransformType.GUI && isCulled) {
			return Pair.of(otherModel, pair.getRight());
		}
		return pair;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}
}
