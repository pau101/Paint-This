package com.pau101.paintthis.client.renderer.entity;

import org.lwjgl.opengl.GL11;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.server.entity.item.EntityCanvas;
import com.pau101.paintthis.server.painting.PaintingDrawable;
import com.pau101.paintthis.server.painting.Signature;
import com.pau101.paintthis.server.painting.Signature.Side;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public final class RenderCanvas extends Render<EntityCanvas> {
	public static final ResourceLocation TEXTURE = new ResourceLocation(PaintThis.ID, "textures/entity/canvas.png");

	public static final int TILE_SIZE = 8;

	private static final int BACK_U_OFFSET = 24;

	private static final double PIXEL = 1 / 16.0;

	private static final double HALF_PIXEL = PIXEL / 2;

	private static final float TEXTURE_WIDTH = 64;

	private static final float TEXTURE_HEIGHT = 32;

	private static final float Y_SIDE_MIN_V = 24 / TEXTURE_HEIGHT;

	private static final float Y_SIDE_MAX_V = 25 / TEXTURE_HEIGHT;

	private static final float X_SIDE_MIN_U = 48 / TEXTURE_WIDTH;

	private static final float X_SIDE_MAX_U = 49 / TEXTURE_WIDTH;

	private static final float CAP_MIN_U = 49 / TEXTURE_WIDTH;

	private static final float CAP_MIN_V = 0 / TEXTURE_HEIGHT;

	private static final double FRAME_Y_OFFSET = HALF_PIXEL / 2;

	private int backDisplayList;

	private int frontDisplayList;

	private int frameDisplayList;

	public RenderCanvas(RenderManager manager) {
		super(manager);
		shadowSize = 0;
	}

	@Override
	public void doRender(EntityCanvas canvas, double x, double y, double z, float yaw, float delta) {
		if (!(canvas.isOnBlock() || canvas.isOnEasel()) || canvas.isBeingPlacedOnEasel()) {
			return;
		}
		if (backDisplayList == 0) {
			generateModels();
		}
		float pitch = canvas.prevRotationPitch + (canvas.rotationPitch - canvas.prevRotationPitch) * delta;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(-yaw, 0, 1, 0);
		GlStateManager.rotate(pitch + 90, 1, 0, 0);
		int size = canvas.getSize();
		bindEntityTexture(canvas);
		renderBack(size);
		if (canvas.isFramed()) {
			renderFrame(size);
		}
		PaintingDrawable painting = (PaintingDrawable) canvas.getPainting();
		drawPainting(painting, size);
		GlStateManager.popMatrix();
		super.doRender(canvas, x, y, z, yaw, delta);
	}

	private void drawPainting(PaintingDrawable painting, int size) {
		bindTexture(painting.getResource());
		renderFront(size);
		if (painting.isSigned()) {
			drawSignature(painting);
		}
	}

	private void drawSignature(PaintingDrawable painting) {
		Signature signature = painting.getSignature();
		FontRenderer renderer = Minecraft.getMinecraft().fontRendererObj;
		int color = signature.getDye().getColor();
		GlStateManager.enableRescaleNormal();
		GlStateManager.pushMatrix();
		float scale = 1 / 8F;
		GlStateManager.scale(0.0625F, 0.0625F, 0.0625F);
		String name = signature.getSignerName();
		float sx;
		if (signature.getSide() == Side.RIGHT) {
			sx = painting.getPixelWidth() / 2 - renderer.getStringWidth(name) * scale - 0.5F;
		} else {
			sx = -painting.getPixelWidth() / 2 + 0.5F;
		}
		float sy = painting.getPixelHeight() / 2 - 1.5F;
		GlStateManager.translate(sx, 0.6F, sy);
		GlStateManager.scale(scale, scale, scale);
		GlStateManager.glNormal3f(0, 0, -1);
		GlStateManager.rotate(90, 1, 0, 0);
		renderer.drawString(name, 0, 0, color);
		GlStateManager.popMatrix();
		GlStateManager.disableRescaleNormal();
		GlStateManager.color(1, 1, 1);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityCanvas entity) {
		return TEXTURE;
	}

	private void renderBack(int size) {
		GlStateManager.callList(backDisplayList + size);
	}

	private void renderFront(int size) {
		GlStateManager.callList(frontDisplayList + size);
	}

	private void renderFrame(int size) {
		GlStateManager.callList(frameDisplayList + size);
	}

	private void generateModels() {
		if (backDisplayList != 0) {
			GLAllocation.deleteDisplayLists(backDisplayList, PaintThis.CANVAS_SIZES);
			GLAllocation.deleteDisplayLists(frontDisplayList, PaintThis.CANVAS_SIZES);
			GLAllocation.deleteDisplayLists(frameDisplayList, PaintThis.CANVAS_SIZES);
		}
		backDisplayList = GLAllocation.generateDisplayLists(PaintThis.CANVAS_SIZES);
		frontDisplayList = GLAllocation.generateDisplayLists(PaintThis.CANVAS_SIZES);
		frameDisplayList = GLAllocation.generateDisplayLists(PaintThis.CANVAS_SIZES);
		Tessellator tes = Tessellator.getInstance();
		VertexBuffer renderer = tes.getBuffer();
		double pixel = 1 / 16D, halfPixel = pixel / 2;
		for (int size = 0; size < PaintThis.CANVAS_SIZES; size++) {
			int width = size % PaintThis.MAX_CANVAS_SIZE + 1;
			int height = size / PaintThis.MAX_CANVAS_SIZE + 1;
			int twiceWidth = width * 2, twiceHeight = height * 2;
			double halfWidth = width / 2.0, halfHeight = height / 2.0;
			GlStateManager.glNewList(backDisplayList + size, GL11.GL_COMPILE);
			renderBack(renderer, twiceWidth, twiceHeight, halfWidth, halfHeight);
			GlStateManager.glEndList();
			GlStateManager.glNewList(frontDisplayList + size, GL11.GL_COMPILE);
			renderFront(renderer, width, height, halfWidth, halfHeight);
			GlStateManager.glEndList();
			GlStateManager.glNewList(frameDisplayList + size, GL11.GL_COMPILE);
			renderFrame(renderer, width, height, halfWidth, halfHeight);
			GlStateManager.glEndList();
		}
	}

	private void renderBack(VertexBuffer buf, int twiceWidth, int twiceHeight, double halfWidth, double halfHeight) {
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
		buf.setTranslation(-halfWidth, -HALF_PIXEL, -halfHeight);
		for (int x = 0; x < twiceWidth; x++) {
			for (int y = 0; y < twiceHeight; y++) {
				double minX = x * 0.5, minY = y * 0.5;
				double maxX = minX + 0.5, maxY = minY + 0.5;
				float minU = ((x == 0 ? 0 : x == twiceWidth - 1 ? 2 : 1) * TILE_SIZE + BACK_U_OFFSET) / TEXTURE_WIDTH;
				float minV = (y == 0 ? 0 : y == twiceHeight - 1 ? 2 : 1) * TILE_SIZE / TEXTURE_HEIGHT;
				float maxU = minU + TILE_SIZE / TEXTURE_WIDTH;
				float maxV = minV + TILE_SIZE / TEXTURE_HEIGHT;
				buf.pos(minX, 0, maxY).tex(minU, maxV).normal(0, -1, 0).endVertex();
				buf.pos(minX, 0, minY).tex(minU, minV).normal(0, -1, 0).endVertex();
				buf.pos(maxX, 0, minY).tex(maxU, minV).normal(0, -1, 0).endVertex();
				buf.pos(maxX, 0, maxY).tex(maxU, maxV).normal(0, -1, 0).endVertex();
				if (x == 0) {
					buf.pos(minX, 0, minY).tex(X_SIDE_MIN_U, minV).normal(-1, 0, 0).endVertex();
					buf.pos(minX, 0, maxY).tex(X_SIDE_MIN_U, maxV).normal(-1, 0, 0).endVertex();
					buf.pos(minX, PIXEL, maxY).tex(X_SIDE_MAX_U, maxV).normal(-1, 0, 0).endVertex();
					buf.pos(minX, PIXEL, minY).tex(X_SIDE_MAX_U, minV).normal(-1, 0, 0).endVertex();
				} else if (x == twiceWidth - 1) {
					buf.pos(maxX, PIXEL, minY).tex(X_SIDE_MIN_U, minV).normal(1, 0, 0).endVertex();
					buf.pos(maxX, PIXEL, maxY).tex(X_SIDE_MIN_U, maxV).normal(1, 0, 0).endVertex();
					buf.pos(maxX, 0, maxY).tex(X_SIDE_MAX_U, maxV).normal(1, 0, 0).endVertex();
					buf.pos(maxX, 0, minY).tex(X_SIDE_MAX_U, minV).normal(1, 0, 0).endVertex();
				}
				if (y == 0) {
					buf.pos(maxX, 0, minY).tex(maxU, Y_SIDE_MIN_V).normal(0, 0, -1).endVertex();
					buf.pos(minX, 0, minY).tex(minU, Y_SIDE_MIN_V).normal(0, 0, -1).endVertex();
					buf.pos(minX, PIXEL, minY).tex(minU, Y_SIDE_MAX_V).normal(0, 0, -1).endVertex();
					buf.pos(maxX, PIXEL, minY).tex(maxU, Y_SIDE_MAX_V).normal(0, 0, -1).endVertex();
				} else if (y == twiceHeight - 1) {
					buf.pos(maxX, PIXEL, maxY).tex(maxU, Y_SIDE_MAX_V).normal(0, 0, 1).endVertex();
					buf.pos(minX, PIXEL, maxY).tex(minU, Y_SIDE_MAX_V).normal(0, 0, 1).endVertex();
					buf.pos(minX, 0, maxY).tex(minU, Y_SIDE_MIN_V).normal(0, 0, 1).endVertex();
					buf.pos(maxX, 0, maxY).tex(maxU, Y_SIDE_MIN_V).normal(0, 0, 1).endVertex();
				}
			}
		}
		Tessellator.getInstance().draw();
		buf.setTranslation(0, 0, 0);
	}

	private void renderFront(VertexBuffer buf, int width, int height, double halfWidth, double halfHeight) {
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
		buf.pos(-halfWidth, HALF_PIXEL, -halfHeight).tex(0, 0).normal(0, 1, 0).endVertex();
		buf.pos(-halfWidth, HALF_PIXEL, halfHeight).tex(0, 1).normal(0, 1, 0).endVertex();
		buf.pos(halfWidth, HALF_PIXEL, halfHeight).tex(1, 1).normal(0, 1, 0).endVertex();
		buf.pos(halfWidth, HALF_PIXEL, -halfHeight).tex(1, 0).normal(0, 1, 0).endVertex();
		Tessellator.getInstance().draw();
	}

	private void renderFrame(VertexBuffer buf, int width, int height, double halfWidth, double halfHeight) {
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
		for (int y = 0; y < height; y++) {
			int first = y == 0 ? 1 : 0;
			int last = y == height - 1 ? 1 : 0;
			double minY = y - halfHeight - first * PIXEL;
			double maxY = minY + 1 + (first + last) * PIXEL;
			double minV = (1 - first) / TEXTURE_HEIGHT;
			double maxV = (17 + last) / TEXTURE_HEIGHT;
			drawVertEdgeSegment(buf, halfWidth, minY, maxY, minV, maxV, 1);
			drawVertEdgeSegment(buf, halfWidth, minY, maxY, minV, maxV, -1);
		}
		for (int x = 0; x < width; x++) {
			double minX = x - halfWidth;
			double maxX = minX + 1;
			double minV = 1 / TEXTURE_HEIGHT;
			double maxV = 17 / TEXTURE_HEIGHT;
			drawHoriEdgeSegment(buf, halfHeight, minX, maxX, minV, maxV, 1);
			drawHoriEdgeSegment(buf, halfHeight, minX, maxX, minV, maxV, -1);
		}
		drawSideCap(buf, halfWidth, halfHeight, 1, 1);
		drawSideCap(buf, halfWidth, halfHeight, -1, 1);
		drawSideCap(buf, halfWidth, halfHeight, 1, -1);
		drawSideCap(buf, halfWidth, halfHeight, -1, -1);
		Tessellator.getInstance().draw();
	}

	private void drawVertEdgeSegment(VertexBuffer buf, double halfWidth, double minY, double maxY, double minV, double maxV, int side) {
		double innerX = halfWidth * side;
		double outerX = (halfWidth + PIXEL) * side;
		double minX = Math.min(innerX, outerX);
		double maxX = Math.max(innerX, outerX);
		double minU = (49 + (side == -1 ? 0 : 1)) / TEXTURE_WIDTH;
		double maxU = minU + 1 / TEXTURE_WIDTH;
		// top
		buf.pos(minX, HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(minU, minV).normal(0, 1, 0).endVertex();
		buf.pos(minX, HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(minU, maxV).normal(0, 1, 0).endVertex();
		buf.pos(maxX, HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(maxU, maxV).normal(0, 1, 0).endVertex();
		buf.pos(maxX, HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(maxU, minV).normal(0, 1, 0).endVertex();
		// outer side
		buf.pos(outerX, HALF_PIXEL * side + FRAME_Y_OFFSET, minY).tex(minU, minV).normal(-side, 0, 0).endVertex();
		buf.pos(outerX, HALF_PIXEL * side + FRAME_Y_OFFSET, maxY).tex(minU, maxV).normal(-side, 0, 0).endVertex();
		buf.pos(outerX, -HALF_PIXEL * side + FRAME_Y_OFFSET, maxY).tex(maxU, maxV).normal(-side, 0, 0).endVertex();
		buf.pos(outerX, -HALF_PIXEL * side + FRAME_Y_OFFSET, minY).tex(maxU, minV).normal(-side, 0, 0).endVertex();
		// inner side
		buf.pos(innerX, -HALF_PIXEL * side + FRAME_Y_OFFSET, minY).tex(minU, minV).normal(side, 0, 0).endVertex();
		buf.pos(innerX, -HALF_PIXEL * side + FRAME_Y_OFFSET, maxY).tex(minU, maxV).normal(side, 0, 0).endVertex();
		buf.pos(innerX, HALF_PIXEL * side + FRAME_Y_OFFSET, maxY).tex(maxU, maxV).normal(side, 0, 0).endVertex();
		buf.pos(innerX, HALF_PIXEL * side + FRAME_Y_OFFSET, minY).tex(maxU, minV).normal(side, 0, 0).endVertex();
		// bottom
		buf.pos(maxX, -HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(maxU, minV).normal(0, -1, 0).endVertex();
		buf.pos(maxX, -HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(maxU, maxV).normal(0, -1, 0).endVertex();
		buf.pos(minX, -HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(minU, maxV).normal(0, -1, 0).endVertex();
		buf.pos(minX, -HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(minU, minV).normal(0, -1, 0).endVertex();
	}

	private void drawHoriEdgeSegment(VertexBuffer buf, double halfHeight, double minX, double maxX, double minV, double maxV, int side) {
		double innerY = halfHeight * side;
		double outerY = (halfHeight + PIXEL) * side;
		double minY = Math.min(innerY, outerY);
		double maxY = Math.max(innerY, outerY);
		double minU = (51 + (side == -1 ? 0 : 1)) / TEXTURE_WIDTH;
		double maxU = minU + 1 / TEXTURE_WIDTH;
		// top
		buf.pos(minX, HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(minU, minV).normal(0, 1, 0).endVertex();
		buf.pos(maxX, HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(minU, maxV).normal(0, 1, 0).endVertex();
		buf.pos(maxX, HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(maxU, maxV).normal(0, 1, 0).endVertex();
		buf.pos(minX, HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(maxU, minV).normal(0, 1, 0).endVertex();
		// outer side
		buf.pos(minX, -HALF_PIXEL * side + FRAME_Y_OFFSET, outerY).tex(minU, minV).normal(0, 0, side).endVertex();
		buf.pos(maxX, -HALF_PIXEL * side + FRAME_Y_OFFSET, outerY).tex(minU, maxV).normal(0, 0, side).endVertex();
		buf.pos(maxX, HALF_PIXEL * side + FRAME_Y_OFFSET, outerY).tex(maxU, maxV).normal(0, 0, side).endVertex();
		buf.pos(minX, HALF_PIXEL * side + FRAME_Y_OFFSET, outerY).tex(maxU, minV).normal(0, 0, side).endVertex();
		// inner side
		buf.pos(minX, HALF_PIXEL * side + FRAME_Y_OFFSET, innerY).tex(minU, minV).normal(0, 0, -side).endVertex();
		buf.pos(maxX, HALF_PIXEL * side + FRAME_Y_OFFSET, innerY).tex(minU, maxV).normal(0, 0, -side).endVertex();
		buf.pos(maxX, -HALF_PIXEL * side + FRAME_Y_OFFSET, innerY).tex(maxU, maxV).normal(0, 0, -side).endVertex();
		buf.pos(minX, -HALF_PIXEL * side + FRAME_Y_OFFSET, innerY).tex(maxU, minV).normal(0, 0, -side).endVertex();
		// bottom
		buf.pos(minX, -HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(minU, minV).normal(0, -1, 0).endVertex();
		buf.pos(maxX, -HALF_PIXEL + FRAME_Y_OFFSET, minY).tex(minU, maxV).normal(0, -1, 0).endVertex();
		buf.pos(maxX, -HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(maxU, maxV).normal(0, -1, 0).endVertex();
		buf.pos(minX, -HALF_PIXEL + FRAME_Y_OFFSET, maxY).tex(maxU, minV).normal(0, -1, 0).endVertex();
	}

	private void drawSideCap(VertexBuffer buf, double halfWidth, double halfHeight, int x, int y) {
		double innerX = halfWidth * x;
		double outerX = (halfWidth + PIXEL) * x;
		double minX = Math.min(innerX, outerX);
		double maxX = Math.max(innerX, outerX);
		double outerY = (halfHeight + PIXEL) * y;
		buf.pos(maxX, HALF_PIXEL * y + FRAME_Y_OFFSET, -outerY).tex(CAP_MIN_U + 1 / TEXTURE_WIDTH, CAP_MIN_V).normal(0, 0, -y).endVertex();
		buf.pos(maxX, -HALF_PIXEL * y + FRAME_Y_OFFSET, -outerY).tex(CAP_MIN_U + 1 / TEXTURE_WIDTH, CAP_MIN_V + 1 / TEXTURE_HEIGHT).normal(0, 0, -y).endVertex();
		buf.pos(minX, -HALF_PIXEL * y + FRAME_Y_OFFSET, -outerY).tex(CAP_MIN_U, CAP_MIN_V).normal(0, 0, -y).endVertex();
		buf.pos(minX, HALF_PIXEL * y + FRAME_Y_OFFSET, -outerY).tex(CAP_MIN_U, CAP_MIN_V + 1 / TEXTURE_HEIGHT).normal(0, 0, -y).endVertex();
	}
}
