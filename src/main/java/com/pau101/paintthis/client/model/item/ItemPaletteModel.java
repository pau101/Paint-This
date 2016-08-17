package com.pau101.paintthis.client.model.item;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelPart;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pau101.paintthis.PaintThis;

public class ItemPaletteModel implements IModel {
	private final ImmutableList<ResourceLocation> textures;

	private final ImmutableMap<TransformType, TRSRTransformation> transforms;

	public ItemPaletteModel(ImmutableList<ResourceLocation> textures, ImmutableMap<TransformType, TRSRTransformation> transforms) {
		this.textures = textures;
		this.transforms = transforms;
	}

	private static ImmutableList<ResourceLocation> getTextures(ModelBlock model) {
		ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
		for (int i = 0; model.isTexturePresent("layer" + i); i++) {
			builder.add(new ResourceLocation(model.resolveTextureName("layer" + i)));
		}
		return builder.build();
	}

	@Override
	public IFlexibleBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		ImmutableList.Builder<ImmutableList<BakedQuad>> builder = ImmutableList.builder();
		Optional<TRSRTransformation> transform = state.apply(Optional.<IModelPart> absent());
		for (int i = 0; i < textures.size(); i++) {
			TextureAtlasSprite sprite = bakedTextureGetter.apply(textures.get(i));
			builder.add(getQuadsForSprite(i, sprite, format, transform));
		}
		TextureAtlasSprite particle = bakedTextureGetter.apply(textures.isEmpty() ? new ResourceLocation("missingno") : textures.get(0));
		return new BakedItemPaletteModelProvider(builder.build(), particle, format, transforms);
	}

	@Override
	public IModelState getDefaultState() {
		return TRSRTransformation.identity();
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.of();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return textures;
	}

	public ImmutableList<BakedQuad> getQuadsForSprite(int tint, TextureAtlasSprite sprite, VertexFormat format, Optional<TRSRTransformation> transform) {
		ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
		int uMax = sprite.getIconWidth();
		int vMax = sprite.getIconHeight();
		BitSet faces = new BitSet((uMax + 1) * (vMax + 1) * 4);
		for (int f = 0; f < sprite.getFrameCount(); f++) {
			int[] pixels = sprite.getFrameTextureData(f)[0];
			boolean ptu;
			boolean[] ptv = new boolean[uMax];
			Arrays.fill(ptv, true);
			for (int v = 0; v < vMax; v++) {
				ptu = true;
				for (int u = 0; u < uMax; u++) {
					boolean t = isTransparent(pixels, uMax, vMax, u, v);
					// left - transparent, right - opaque
					if (ptu && !t) {
						addSideQuad(builder, faces, format, transform, EnumFacing.WEST, tint, sprite, uMax, vMax, u, v);
					}
					// left - opaque, right - transparent
					if (!ptu && t) {
						addSideQuad(builder, faces, format, transform, EnumFacing.EAST, tint, sprite, uMax, vMax, u, v);
					}
					// up - transparent, down - opaque
					if (ptv[u] && !t) {
						addSideQuad(builder, faces, format, transform, EnumFacing.UP, tint, sprite, uMax, vMax, u, v);
					}
					// up - opaque, down - transparent
					if (!ptv[u] && t) {
						addSideQuad(builder, faces, format, transform, EnumFacing.DOWN, tint, sprite, uMax, vMax, u, v);
					}
					ptu = t;
					ptv[u] = t;
				}
				// last - opaque
				if (!ptu) {
					addSideQuad(builder, faces, format, transform, EnumFacing.EAST, tint, sprite, uMax, vMax, uMax, v);
				}
			}
			// last line
			for (int u = 0; u < uMax; u++) {
				if (!ptv[u]) {
					addSideQuad(builder, faces, format, transform, EnumFacing.DOWN, tint, sprite, uMax, vMax, u, vMax);
				}
			}
		}
		// front
		builder.add(buildQuad(format, transform, EnumFacing.NORTH, tint, 0, 0, 7.5F / 16, sprite.getMinU(), sprite.getMaxV(), 0, 1, 7.5F / 16, sprite.getMinU(), sprite.getMinV(), 1, 1, 7.5F / 16, sprite.getMaxU(), sprite.getMinV(), 1, 0, 7.5F / 16, sprite.getMaxU(), sprite.getMaxV()));
		// back
		builder.add(buildQuad(format, transform, EnumFacing.SOUTH, tint, 0, 0, 8.5F / 16, sprite.getMinU(), sprite.getMaxV(), 1, 0, 8.5F / 16, sprite.getMaxU(), sprite.getMaxV(), 1, 1, 8.5F / 16, sprite.getMaxU(), sprite.getMinV(), 0, 1, 8.5F / 16, sprite.getMinU(), sprite.getMinV()));
		return builder.build();
	}

	protected boolean isTransparent(int[] pixels, int uMax, int vMax, int u, int v) {
		return (pixels[u + (vMax - 1 - v) * uMax] >> 24 & 0xFF) == 0;
	}

	private static void addSideQuad(ImmutableList.Builder<BakedQuad> builder, BitSet faces, VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing side, int tint, TextureAtlasSprite sprite, int uMax, int vMax, int u, int v) {
		int si = side.ordinal();
		if (si > 4) {
			si -= 2;
		}
		int index = (vMax + 1) * ((uMax + 1) * si + u) + v;
		if (!faces.get(index)) {
			faces.set(index);
			builder.add(buildSideQuad(format, transform, side, tint, sprite, u, v));
		}
	}

	private static BakedQuad buildSideQuad(VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing side, int tint, TextureAtlasSprite sprite, int u, int v) {
		final float eps0 = 30e-5F;
		final float eps1 = 45e-5F;
		final float eps2 = 0.5F;
		final float eps3 = 0.5F;
		float x0 = (float) u / sprite.getIconWidth();
		float y0 = (float) v / sprite.getIconHeight();
		float x1 = x0, y1 = y0;
		float z1 = 7.5F / 16 - eps1, z2 = 8.5F / 16 + eps1;
		switch (side) {
			case WEST:
				z1 = 8.5F / 16 + eps1;
				z2 = 7.5F / 16 - eps1;
			case EAST:
				y1 = (v + 1F) / sprite.getIconHeight();
				break;
			case DOWN:
				z1 = 8.5F / 16 + eps1;
				z2 = 7.5F / 16 - eps1;
			case UP:
				x1 = (u + 1F) / sprite.getIconWidth();
				break;
			default:
				throw new IllegalArgumentException("can't handle z-oriented side");
		}
		float u0 = 16 * (x0 - side.getDirectionVec().getX() * eps3 / sprite.getIconWidth());
		float u1 = 16 * (x1 - side.getDirectionVec().getX() * eps3 / sprite.getIconWidth());
		float v0 = 16 * (1f - y0 - side.getDirectionVec().getY() * eps3 / sprite.getIconHeight());
		float v1 = 16 * (1f - y1 - side.getDirectionVec().getY() * eps3 / sprite.getIconHeight());
		switch (side) {
			case WEST:
			case EAST:
				y0 -= eps1;
				y1 += eps1;
				v0 -= eps2 / sprite.getIconHeight();
				v1 += eps2 / sprite.getIconHeight();
				break;
			case DOWN:
			case UP:
				x0 -= eps1;
				x1 += eps1;
				u0 += eps2 / sprite.getIconWidth();
				u1 -= eps2 / sprite.getIconWidth();
				break;
			default:
				throw new IllegalArgumentException("can't handle z-oriented side");
		}
		switch (side) {
			case WEST:
				x0 += eps0;
				x1 += eps0;
				break;
			case EAST:
				x0 -= eps0;
				x1 -= eps0;
				break;
			case DOWN:
				y0 -= eps0;
				y1 -= eps0;
				break;
			case UP:
				y0 += eps0;
				y1 += eps0;
				break;
			default:
				throw new IllegalArgumentException("can't handle z-oriented side");
		}
		return buildQuad(format, transform, side.getOpposite(), tint, x0, y0, z1, sprite.getInterpolatedU(u0), sprite.getInterpolatedV(v0), x1, y1, z1, sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v1), x1, y1, z2, sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v1), x0, y0, z2, sprite.getInterpolatedU(u0), sprite.getInterpolatedV(v0));
	}

	private static final BakedQuad buildQuad(VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing side, int tint, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3) {
		UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
		builder.setQuadTint(tint);
		builder.setQuadOrientation(side);
		putVertex(builder, format, transform, side, x0, y0, z0, u0, v0);
		putVertex(builder, format, transform, side, x1, y1, z1, u1, v1);
		putVertex(builder, format, transform, side, x2, y2, z2, u2, v2);
		putVertex(builder, format, transform, side, x3, y3, z3, u3, v3);
		return builder.build();
	}

	private static void putVertex(UnpackedBakedQuad.Builder builder, VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing side, float x, float y, float z, float u, float v) {
		Vector4f vec = new Vector4f();
		Vector3f normal = new Vector3f();
		for (int e = 0; e < format.getElementCount(); e++) {
			switch (format.getElement(e).getUsage()) {
				case POSITION:
					if (transform.isPresent()) {
						vec.x = x;
						vec.y = y;
						vec.z = z;
						vec.w = 1;
						transform.get().getMatrix().transform(vec);
						builder.put(e, vec.x, vec.y, vec.z, vec.w);
					} else {
						builder.put(e, x, y, z, 1);
					}
					break;
				case COLOR:
					builder.put(e, 1, 1, 1, 1);
					break;
				case UV:
					if (format.getElement(e).getIndex() == 0) {
						builder.put(e, u, v, 0, 1);
					}
					break;
				case NORMAL:
					if (transform.isPresent()) {
						normal.x = side.getFrontOffsetX();
						normal.y = side.getFrontOffsetY();
						normal.z = side.getFrontOffsetZ();
						transform.get().getMatrix().transform(normal);
						builder.put(e, normal.x, normal.y, normal.z, 0);
					} else {
						builder.put(e, side.getFrontOffsetX(), side.getFrontOffsetY(), side.getFrontOffsetZ(), 0);
					}
					break;
				default:
					builder.put(e);
					break;
			}
		}
	}

	public enum Loader implements ICustomModelLoader {
		INSTANCE;

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager) {}

		@Override
		public boolean accepts(ResourceLocation modelLocation) {
			return modelLocation.getResourceDomain().equals(PaintThis.MODID) && modelLocation.getResourcePath().equals("models/item/palette");
		}

		@Override
		public IModel loadModel(ResourceLocation modelLocation) {
			IModel model = null;
			try {
				ResourceLocation path = new ResourceLocation(modelLocation.toString() + ".json");
				IResource iresource = Minecraft.getMinecraft().getResourceManager().getResource(path);
				Reader reader = new InputStreamReader(iresource.getInputStream(), Charsets.UTF_8);
				ModelBlock modelBlock;
				try {
					modelBlock = ModelBlock.deserialize(reader);
					modelBlock.name = modelBlock.toString();
				} finally {
					reader.close();
				}
				ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
				int layer = 0;
				String tex;
				while ((tex = modelBlock.textures.get("layer" + layer++)) != null) {
					builder.add(new ResourceLocation(tex));
				}
				model = new ItemPaletteModel(builder.build(), IPerspectiveAwareModel.MapWrapper.getTransforms(modelBlock.func_181682_g()));
			} catch (IOException e) {
				Throwables.propagate(e);
			}
			return model;
		}
	}
}
