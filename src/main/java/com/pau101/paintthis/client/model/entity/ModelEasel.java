package com.pau101.paintthis.client.model.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import com.pau101.paintthis.entity.item.EntityCanvas;
import com.pau101.paintthis.entity.item.EntityEasel;
import com.pau101.paintthis.painting.Painting;

public class ModelEasel extends ModelBase {
	private ModelRenderer tower;

	private ModelRenderer lock;

	public ModelEasel() {
		textureWidth = 64;
		textureHeight = 64;
		ModelRenderer supportRodTower = new ModelRenderer(this, 14, 4);
		supportRodTower.setRotationPoint(0, 0.5F, -0.5F);
		supportRodTower.addBox(-2, -0.5F, -0.5F, 4, 1, 1, -0.1F);
		ModelRenderer legBack = new ModelRenderer(this, 8, 12);
		legBack.setRotationPoint(0, 15.9F, 0.5F);
		legBack.addBox(-1, 0, -1, 2, 37, 2, 0);
		setRotateAngle(legBack, 0.3490658503988659F, 0, 0);
		ModelRenderer backSupport = new ModelRenderer(this, 16, 12);
		backSupport.setRotationPoint(0, 21, 0);
		backSupport.addBox(-1.5F, 0, -1, 3, 8, 1, 0);
		setRotateAngle(backSupport, 0.7155849933176751F, 0, 0);
		ModelRenderer legLeft = new ModelRenderer(this, 24, 12);
		legLeft.setRotationPoint(5.0F, -5.5F, 0);
		legLeft.addBox(-1, 0, -1, 2, 42, 2, 0.1F);
		setRotateAngle(legLeft, 0, 0, -0.06981317007977318F);
		ModelRenderer lockScrew = new ModelRenderer(this, 8, 4);
		lockScrew.setRotationPoint(0, -1, -0.5F);
		lockScrew.addBox(-1, 0, -0.5F, 2, 1, 1, 0);
		ModelRenderer legRight = new ModelRenderer(this, 32, 12);
		legRight.setRotationPoint(-5, -5.5F, 0);
		legRight.addBox(-1, 0, -1, 2, 42, 2, 0.1F);
		setRotateAngle(legRight, 0, 0, 0.06981317007977318F);
		ModelRenderer trayScrew = new ModelRenderer(this, 8, 4);
		trayScrew.setRotationPoint(0, 2, 0);
		trayScrew.addBox(-1, 0, -0.5F, 2, 1, 1, 0);
		ModelRenderer braceTop = new ModelRenderer(this, 8, 0);
		braceTop.setRotationPoint(0, 16, 0);
		braceTop.addBox(-5, 0, -1, 10, 2, 2, 0);
		lock = new ModelRenderer(this, 47, 9);
		lock.setRotationPoint(0, 3.8F, -1);
		lock.addBox(-2, 0, -1.5F, 4, 2, 3, 0);
		ModelRenderer braceBottom = new ModelRenderer(this, 30, 2);
		braceBottom.setRotationPoint(0, 43, 0);
		braceBottom.addBox(-7, 0, -1, 14, 2, 2, 0);
		ModelRenderer tray = new ModelRenderer(this, 8, 6);
		tray.setRotationPoint(0, 38, -1.5F);
		tray.addBox(-8.5F, 0, -2, 17, 2, 4, 0);
		tower = new ModelRenderer(this, 0, 0);
		tower.setRotationPoint(0, -52, 0.7F);
		tower.addBox(-1, 0, -1, 2, 44, 2, -0.1F);
		setRotateAngle(tower, -0.13962634015954636F, 0, 0);
		ModelRenderer supportRodBack = new ModelRenderer(this, 14, 4);
		supportRodBack.setRotationPoint(0, 7.5F, -0.5F);
		supportRodBack.addBox(-2, -0.5F, -0.5F, 4, 1, 1, -0.1F);
		backSupport.addChild(supportRodTower);
		tower.addChild(legBack);
		tower.addChild(backSupport);
		braceTop.addChild(legLeft);
		lock.addChild(lockScrew);
		braceTop.addChild(legRight);
		tray.addChild(trayScrew);
		tower.addChild(braceTop);
		tower.addChild(lock);
		tower.addChild(braceBottom);
		tower.addChild(tray);
		backSupport.addChild(supportRodBack);
	}

	@Override
	public void render(Entity entity, float swing, float speed, float yaw, float pitch, float age, float scale) {
		EntityEasel easel = (EntityEasel) entity;
		if (easel.riddenByEntity instanceof EntityCanvas) {
			EntityCanvas canvas = (EntityCanvas) easel.riddenByEntity;
			lock.rotationPointY = 36.1F - canvas.getHeight() * Painting.PIXELS_PER_BLOCK - (canvas.isFramed() ? 2 : 0);
		} else {
			lock.rotationPointY = 4.1F;
		}
		tower.render(scale);
	}

	private static void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}
}
