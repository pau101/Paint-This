package com.pau101.paintthis.util;

import com.google.common.base.Preconditions;

/*
 * Based off of: http://kt8216.unixcab.org/murphy/index.html
 */
public final class LineDrawer {
	private LineDrawer() {}

	public static void draw(Stroke B, int x0, int y0, int x1, int y1) {
		// Algorithm will get stuck in an infinite loop otherwise, should probably fix that so this isn't needed...
		Preconditions.checkArgument(x0 != x1 || y0 != y1, "Line cannot be a single point! (%s, %s)", x0, y0);
		int dx = x1 - x0;
		int dy = y1 - y0;
		int xstep = 1, ystep = 1;
		if (dx < 0) {
			dx = -dx;
			xstep = -1;
		} else if (dx == 0) {
			xstep = 0;
		}
		if (dy < 0) {
			dy = -dy;
			ystep = -1;
		} else if (dy == 0) {
			ystep = 0;
		}
		int pystep = 0, pxstep = 0;
		boolean xch = false; // whether left and right get switched.
		switch (xstep + ystep * 4) {
			case -1 + -1 * 4:
				pystep = -1;
				pxstep = 1;
				xch = true;
				break;
			case -1 + 0 * 4:
				pystep = -1;
				pxstep = 0;
				xch = true;
				break;
			case -1 + 1 * 4:
				pystep = 1;
				pxstep = 1;
				break;
			case 0 + -1 * 4:
				pystep = 0;
				pxstep = -1;
				break;
			case 0 + 0 * 4:
				pystep = 0;
				pxstep = 0;
				break;
			case 0 + 1 * 4:
				pystep = 0;
				pxstep = 1;
				break;
			case 1 + -1 * 4:
				pystep = -1;
				pxstep = -1;
				break;
			case 1 + 0 * 4:
				pystep = -1;
				pxstep = 0;
				break;
			case 1 + 1 * 4:
				pystep = 1;
				pxstep = -1;
				xch = true;
				break;
		}
		WidthProvider left = B::getLeftWidth;
		WidthProvider right = B::getRightWidth;
		if (xch) {
			WidthProvider temp;
			temp = left;
			left = right;
			right = temp;
		}
		if (dx > dy) {
			drawX(B, x0, y0, dx, dy, xstep, ystep, left, right, pxstep, pystep);
		} else {
			drawY(B, x0, y0, dx, dy, xstep, ystep, left, right, pxstep, pystep);
		}
	}

	private static void drawX(Stroke B, int x0, int y0, int dx, int dy, int xstep, int ystep, WidthProvider left, WidthProvider right, int pxstep, int pystep) {
		int p_error = 0;
		int error = 0;
		int y = y0;
		int x = x0;
		int threshold = dx - 2 * dy;
		int E_diag = -2 * dx;
		int E_square = 2 * dy;
		int length = dx + 1;
		double D = Math.sqrt(dx * dx + dy * dy);
		for (int p = 0; p < length; p++) {
			int w_left = (int) (left.get(p, length) * 2 * D);
			int w_right = (int) (right.get(p, length) * 2 * D);
			drawXPerp(B, x, y, dx, dy, pxstep, pystep, p_error, w_left, w_right, error);
			if (error >= threshold) {
				y += ystep;
				error += E_diag;
				if (p_error >= threshold) {
					drawXPerp(B, x, y, dx, dy, pxstep, pystep, p_error + E_diag + E_square, w_left, w_right, error);
					p_error += E_diag;
				}
				p_error += E_square;
			}
			error += E_square;
			x += xstep;
		}
	}

	private static void drawXPerp(Stroke B, int x0, int y0, int dx, int dy, int xstep, int ystep, int einit, int w_left, int w_right, int winit) {
		int threshold = dx - 2 * dy;
		int E_diag = -2 * dx;
		int E_square = 2 * dy;
		int p = 0, q = 0;
		int y = y0;
		int x = x0;
		int error = einit;
		int tk = dx + dy - winit;
		while (tk <= w_left) {
			B.draw(x, y);
			if (error >= threshold) {
				x += xstep;
				error += E_diag;
				tk += 2 * dy;
			}
			error += E_square;
			y += ystep;
			tk += 2 * dx;
			q++;
		}
		y = y0;
		x = x0;
		error = -einit;
		tk = dx + dy + winit;
		while (tk <= w_right) {
			if (p != 0) {
				B.draw(x, y);
			}
			if (error > threshold) {
				x -= xstep;
				error += E_diag;
				tk += 2 * dy;
			}
			error += E_square;
			y -= ystep;
			tk += 2 * dx;
			p++;
		}
		if (q == 0 && p < 2) {
			B.draw(x0, y0); // we need this for very thin lines
		}
	}

	private static void drawY(Stroke B, int x0, int y0, int dx, int dy, int xstep, int ystep, WidthProvider left, WidthProvider right, int pxstep, int pystep) {
		int p_error = 0;
		int error = 0;
		int y = y0;
		int x = x0;
		int threshold = dy - 2 * dx;
		int E_diag = -2 * dy;
		int E_square = 2 * dx;
		int length = dy + 1;
		double D = Math.sqrt(dx * dx + dy * dy);
		for (int p = 0; p < length; p++) {
			int w_left = (int) (left.get(p, length) * 2 * D);
			int w_right = (int) (right.get(p, length) * 2 * D);
			drawYPerp(B, x, y, dx, dy, pxstep, pystep, p_error, w_left, w_right, error);
			if (error >= threshold) {
				x += xstep;
				error += E_diag;
				if (p_error >= threshold) {
					drawYPerp(B, x, y, dx, dy, pxstep, pystep, p_error + E_diag + E_square, w_left, w_right, error);
					p_error += E_diag;
				}
				p_error += E_square;
			}
			error += E_square;
			y += ystep;
		}
	}

	private static void drawYPerp(Stroke B, int x0, int y0, int dx, int dy, int xstep, int ystep, int einit, int w_left, int w_right, int winit) {
		int p = 0, q = 0;
		int threshold = dy - 2 * dx;
		int E_diag = -2 * dy;
		int E_square = 2 * dx;
		int y = y0;
		int x = x0;
		int error = -einit;
		int tk = dx + dy + winit;
		while (tk <= w_left) {
			B.draw(x, y);
			if (error > threshold) {
				y += ystep;
				error += E_diag;
				tk += 2 * dx;
			}
			error += E_square;
			x += xstep;
			tk += 2 * dy;
			q++;
		}
		y = y0;
		x = x0;
		error = einit;
		tk = dx + dy - winit;
		while (tk <= w_right) {
			if (p != 0) {
				B.draw(x, y);
			}
			if (error >= threshold) {
				y -= ystep;
				error += E_diag;
				tk += 2 * dx;
			}
			error += E_square;
			x -= xstep;
			tk += 2 * dy;
			p++;
		}
		if (q == 0 && p < 2) {
			B.draw(x0, y0); // we need this for very thin lines
		}
	}

	private interface WidthProvider {
		public double get(int pos, int length);
	}

	public interface Stroke {
		public void draw(int x, int y);

		public double getLeftWidth(int pos, int length);

		public double getRightWidth(int pos, int length);
	}
}
