package com.pau101.paintthis.util;

import java.util.Optional;

import net.minecraft.util.math.Vec3d;

public final class Mth {
	private Mth() {}

	public static final float TAU = (float) (2 * StrictMath.PI);

	public static final float PI = (float) StrictMath.PI;

	public static final float DEG_TO_RAD = (float) (StrictMath.PI / 180);

	public static final float RAD_TO_DEG = (float) (180 / StrictMath.PI);

	public static int mod(int a, int b) {
		return (a % b + b) % b;
	}

	public static float mod(float a, float b) {
		return (a % b + b) % b;
	}

	public static double mod(double a, double b) {
		return (a % b + b) % b;
	}

	/*
	 * http://people.cs.kuleuven.be/~ares.lagae/publications/LD05ERQIT/LD05ERQIT.pdf
	 */
	public static Optional<Vec3d> intersect(Vec3d O, Vec3d D, Vec3d V_00, Vec3d V_10, Vec3d V_11, Vec3d V_01, boolean frontOnly) {
		final double e = 1e-8;
		Vec3d E_01 = V_10.subtract(V_00);
		Vec3d E_03 = V_01.subtract(V_00);
		Vec3d P = D.crossProduct(E_03);
		double det = E_01.dotProduct(P);
		if (frontOnly && det > 0) {
			return Optional.<Vec3d> empty();
		}
		if (Math.abs(det) < e) {
			return Optional.<Vec3d> empty();
		}
		Vec3d T = O.subtract(V_00);
		double a = T.dotProduct(P) / det;
		if (a < 0 || a > 1) {
			return Optional.<Vec3d> empty();
		}
		Vec3d Q = T.crossProduct(E_01);
		double b = D.dotProduct(Q) / det;
		if (b < 0 || b > 1) {
			return Optional.<Vec3d> empty();
		}

		/**
		 * In my testing this block made triangle V_10 V_11 V_01 not test correctly by the ray parameter being less than zero.
		 */
		// Reject rays using the barycentric coordinates of
		// the intersection point with respect to T'.
		/*if (a + b > 1) {
			Vec3d E_23 = V_01.subtract(V_11);
			Vec3d E_21 = V_10.subtract(V_11);
			P = D.crossProduct(E_21);
			det = E_23.dotProduct(P);
			if (Math.abs(det) < e) return null;
			T = O.subtract(V_11);
			a = T.dotProduct(P) / det;
			if (a < 0) return null;
			Q = T.crossProduct(E_23);
			b = D.dotProduct(Q) / det;
			if (b < 0) return null;
		}*/

		// Compute the ray parameter of the intersection
		// point.
		double t = E_03.dotProduct(Q) / det;
		if (t < 0) {
			return Optional.<Vec3d> empty();
		}

		// Compute the barcentric coordinates of V_11.
		Vec3d E_02 = V_11.subtract(V_00);
		Vec3d N = E_01.crossProduct(E_03);
		double a_11, b_11;
		if (Math.abs(N.xCoord) >= Math.abs(N.yCoord) && Math.abs(N.xCoord) >= Math.abs(N.zCoord)) {
			a_11 = (E_02.yCoord * E_03.zCoord - E_02.zCoord * E_03.yCoord) / N.xCoord;
			b_11 = (E_01.yCoord * E_02.zCoord - E_01.zCoord * E_02.yCoord) / N.xCoord;
		} else if (Math.abs(N.yCoord) >= Math.abs(N.xCoord) && Math.abs(N.yCoord) >= Math.abs(N.zCoord)) {
			a_11 = (E_02.zCoord * E_03.xCoord - E_02.xCoord * E_03.zCoord) / N.yCoord;
			b_11 = (E_01.zCoord * E_02.xCoord - E_01.xCoord * E_02.zCoord) / N.yCoord;
		} else {
			a_11 = (E_02.xCoord * E_03.yCoord - E_02.yCoord * E_03.xCoord) / N.zCoord;
			b_11 = (E_01.xCoord * E_02.yCoord - E_01.yCoord * E_02.xCoord) / N.zCoord;
		}

		// Compute the bilinear coordinates of the
		// intersection point.
		double u, v;
		if (Math.abs(a_11 - 1) < e) {
			u = a;
			if (Math.abs(b_11 - 1) < e) {
				v = b;
			} else {
				v = b / (u * (b_11 - 1) + 1);
			}
		} else if (Math.abs(b_11 - 1) < e) {
			v = b;
			u = a / (v * (a_11 - 1) + 1);
		} else {
			double A = -(b_11 - 1);
			double B = a * (b_11 - 1) - b * (a_11 - 1) - 1;
			double C = a;
			double d = B * B - 4 * A * C;
			double q = -0.5 * (B + Math.signum(B) * Math.sqrt(d));
			u = q / A;
			if (u < 0 || u > 1) {
				u = C / q;
			}
			v = b / (u * (b_11 - 1) + 1);
		}
		return Optional.of(new Vec3d(u, v, t));
	}
}
