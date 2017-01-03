package com.pau101.paintthis.util.matrix;

public interface MatrixStack {
	void push();

	void pop();

	void translate(double x, double y, double z);

	void rotate(double angle, double x, double y, double z);

	void scale(double x, double y, double z);
}
