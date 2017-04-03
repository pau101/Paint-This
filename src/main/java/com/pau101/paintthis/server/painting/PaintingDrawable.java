package com.pau101.paintthis.server.painting;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import com.google.common.base.Throwables;
import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.client.ClientProxy;
import com.pau101.paintthis.client.renderer.entity.RenderCanvas;
import com.pau101.paintthis.server.dye.Dye;
import com.pau101.paintthis.server.entity.item.EntityCanvas;
import com.pau101.paintthis.server.util.LineDrawer;
import com.pau101.paintthis.server.util.LineDrawer.Stroke;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public final class PaintingDrawable extends Painting {
	private static final ResourceLocation PAINT_TEXTURE = new ResourceLocation(PaintThis.ID, "textures/entity/canvas_paint.png");

	private static final double SQRT_2_OVER_2 = Math.sqrt(2) / 2;

	private static int[][] canvasTextures;

	private static int[] paintTexture;

	private static int paintTextureWidth;

	private static int paintTextureHeight;

	private static TextureManager textureManager;

	private PaintingStroke stroke = new PaintingStroke();

	private DynamicTexture texture;

	private ResourceLocation resource;

	private int[] pixels;

	private int paintXOffset;

	private int paintYOffset;

	@Override
	protected void initData(int width, int height) {
		super.initData(width, height);
		if (texture != null) {
			disposeTexture();
		}
		texture = new DynamicTexture(getPixelWidth(), getPixelHeight());
		resource = getTextureManager().getDynamicTextureLocation(getUUID().toString(), texture);
		pixels = texture.getTextureData();
		updateTexture();
	}

	@Override
	public void assignUUID(UUID uuid) {
		super.assignUUID(uuid);
		Random random = new Random(uuid.hashCode());
		getPaintTexture();
		paintXOffset = random.nextInt(paintTextureWidth);
		paintYOffset = random.nextInt(paintTextureHeight);
	}

	@Override
	protected void finalize() {
		disposeTexture();
	}

	private void disposeTexture() {
		TextureManager textureManager = getTextureManager();
		textureManager.deleteTexture(resource);
		textureManager.mapTextureObjects.remove(resource);
		textureManager.mapTextureCounters.remove(resource);
		texture = null;
		resource = null;
		pixels = null;
	}

	public ResourceLocation getResource() {
		return resource;
	}

	@Override
	public void stroke(Vec3d from, Vec3d to, int size, Dye dye) {
		byte[] data = getData();
		int x0 = (int) Math.floor(from.xCoord * getPixelWidth());
		int y0 = (int) Math.floor(from.yCoord * getPixelHeight());
		int x1 = (int) Math.floor(to.xCoord * getPixelWidth());
		int y1 = (int) Math.floor(to.yCoord * getPixelHeight());
		if (x0 == x1 && y0 == y1) {
			dot(from, size, dye);
			return;
		}
		LineDrawer.draw(stroke.with(dye.getByteValue(), size), x0, y0, x1, y1);
		updateTexture();
	}

	@Override
	public void dot(Vec3d pos, int size, Dye dye) {
		int cx = (int) Math.floor(pos.xCoord * getPixelWidth());
		int cy = (int) Math.floor(pos.yCoord * getPixelHeight());
		byte value = dye.getByteValue();
		if (size <= 1) {
			set(cx, cy, value);
			updateTextureRegion(cx, cy, 1, 1);
		} else {
			double radiusSq = (size - SQRT_2_OVER_2) * (size - SQRT_2_OVER_2);
			for (int x = -size + 1; x < size; x++) {
				for (int y = -size + 1; y < size; y++) {
					if (x * x + y * y < radiusSq) {
						set(cx + x, cy + y, value);
					}
				}
			}
			updateTextureRegion(cx - size + 1, cy - size + 1, size * 2 - 1, size * 2 - 1);
		}
	}

	@Override
	public void update(int x, int y, int width, byte[] data) {
		super.update(x, y, width, data);
		updateTextureRegion(x, y, width, data.length / width);
	}

	public void updateTexture() {
		updateTextureRegion(0, 0, getPixelWidth(), getPixelHeight());
	}

	public void updateTextureRegion(int minX, int minY, int width, int height) {
		int[] canvasTexture = getCanvasTexture(EntityCanvas.getSizeIndex(getWidth(), getHeight()));
		int[] paintTexture = getPaintTexture();
		byte[] dyes = getData();
		int maxX = minX + width;
		int maxY = minY + height;
		if (minX < 0) {
			minX = 0;
		}
		if (minY < 0) {
			minY = 0;
		}
		if (maxX > getPixelWidth()) {
			maxX = getPixelWidth();
		}
		if (maxY > getPixelHeight()) {
			maxY = getPixelHeight();
		}
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				int i = x + y * getPixelWidth();
				byte dye = dyes[i];
				if (dye == Dye.NO_DYE) {
					pixels[i] = canvasTexture[i];
				} else {
					int v = paintTexture[(x + paintXOffset) % paintTextureWidth + (y + paintYOffset) % paintTextureHeight * paintTextureWidth] & 0xFF;
					int rgb = Dye.getDyeFromByte(dye).getColor();
					rgb = rgb & 0xFF00FFFF | v * (rgb >> 16 & 0xFF) / 0xFF << 16;
					rgb = rgb & 0xFFFF00FF | v * (rgb >> 8 & 0xFF) / 0xFF << 8;
					rgb = rgb & 0xFFFFFF00 | v * (rgb & 0xFF) / 0xFF;
					pixels[i] = rgb;
				}
			}
		}
		texture.updateDynamicTexture();
	}

	@Override
	public void readFromBuffer(ByteBuf buffer) {
		super.readFromBuffer(buffer);
		updateTexture();
	}

	private static int[] getCanvasTexture(int size) {
		if (canvasTextures == null) {
			try {
				canvasTextures = createCanvasTextures();
			} catch (IOException e) {
				Throwables.propagate(e);
			}
		}
		return canvasTextures[size];
	}

	private static int[] getPaintTexture() {
		if (paintTexture == null) {
			try {
				BufferedImage paintImg = ClientProxy.getImage(PAINT_TEXTURE);
				paintTextureWidth = paintImg.getWidth();
				paintTextureHeight = paintImg.getHeight();
				paintTexture = new int[paintTextureWidth * paintTextureHeight];
				paintImg.getRGB(0, 0, paintTextureWidth, paintTextureHeight, paintTexture, 0, paintTextureWidth);
			} catch (IOException e) {
				Throwables.propagate(e);
			}
		}
		return paintTexture;
	}

	private static int[][] createCanvasTextures() throws IOException {
		int[][] canvasTextures = new int[PaintThis.CANVAS_SIZES][];
		BufferedImage canvasImg = ClientProxy.getImage(RenderCanvas.TEXTURE);
		for (int size = 0; size < PaintThis.CANVAS_SIZES; size++) {
			int width = size % PaintThis.MAX_CANVAS_SIZE + 1;
			int height = size / PaintThis.MAX_CANVAS_SIZE + 1;
			canvasTextures[size] = createCanvasTexture(canvasImg, width, height);
		}
		return canvasTextures;
	}

	private static int[] createCanvasTexture(BufferedImage canvasImg, int width, int height) {
		int twiceWidth = width * 2, twiceHeight = height * 2;
		int pixelWidth = width * Painting.PIXELS_PER_BLOCK;
		int[] pixels = new int[pixelWidth * height * PIXELS_PER_BLOCK];
		for (int x = 0; x < twiceWidth; x++) {
			for (int y = 0; y < twiceHeight; y++) {
				int texU = (x == 0 ? 0 : x == twiceWidth - 1 ? 2 : 1) * RenderCanvas.TILE_SIZE;
				int texV = (y == 0 ? 0 : y == twiceHeight - 1 ? 2 : 1) * RenderCanvas.TILE_SIZE;
				int blockX = x * RenderCanvas.TILE_SIZE;
				int blockY = y * RenderCanvas.TILE_SIZE;
				canvasImg.getRGB(texU, texV, RenderCanvas.TILE_SIZE, RenderCanvas.TILE_SIZE, pixels, blockX + blockY * pixelWidth, pixelWidth);
			}
		}
		return pixels;
	}

	private static TextureManager getTextureManager() {
		if (textureManager == null) {
			textureManager = Minecraft.getMinecraft().getTextureManager();
		}
		return textureManager;
	}

	private class PaintingStroke implements Stroke {
		private byte dye;

		private int width;

		public PaintingStroke with(byte dye, int width) {
			this.dye = dye;
			this.width = width;
			return this;
		}

		@Override
		public void draw(int x, int y) {
			set(x, y, dye);
		}

		@Override
		public double getLeftWidth(int pos, int length) {
			return width;
		}

		@Override
		public double getRightWidth(int pos, int length) {
			return width;
		}
	}
}
