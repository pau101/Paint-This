package com.pau101.paintthis.client.renderer.entity;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import com.pau101.paintthis.PaintThis;
import com.pau101.paintthis.client.model.entity.ModelEasel;
import com.pau101.paintthis.entity.item.EntityEasel;

public class RenderEasel extends Render<EntityEasel> {
	private static final ResourceLocation TEXTURE = new ResourceLocation(PaintThis.ID, "textures/entity/easel.png");

	private ModelEasel model = new ModelEasel();

	public RenderEasel(RenderManager manager) {
		super(manager);
		shadowSize = 0;
	}

	@Override
	public void doRender(EntityEasel entity, double x, double y, double z, float yaw, float delta) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(180 - yaw, 0, 1, 0);
		GlStateManager.scale(-1, -1, 1);
		bindEntityTexture(entity);
		model.render(entity, 0, 0, 0, 0, 0, 0.0625F);
		GlStateManager.popMatrix();
		super.doRender(entity, x, y, z, yaw, delta);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityEasel entity) {
		return TEXTURE;
	}
}
