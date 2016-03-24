package com.pau101.paintthis.util;

import java.util.Optional;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public final class Mth {
	private Mth() {}

	public static final float TAU = (float) (2 * StrictMath.PI);

	public static final float PI = (float) StrictMath.PI;

	public static final float DEG_TO_RAD = (float) (StrictMath.PI / 180);

	public static final float RAD_TO_DEG = (float) (180 / StrictMath.PI);

	public static double linearTransformd(double x, double domainMin, double domainMax, double rangeMin, double rangeMax) {
		x = x < domainMin ? domainMin : x > domainMax ? domainMax : x;
		return (rangeMax - rangeMin) * (x - domainMin) / (domainMax - domainMin) + rangeMin;
	}

	public static float linearTransformf(float x, float domainMin, float domainMax, float rangeMin, float rangeMax) {
		x = x < domainMin ? domainMin : x > domainMax ? domainMax : x;
		return (rangeMax - rangeMin) * (x - domainMin) / (domainMax - domainMin) + rangeMin;
	}

	public static int[][] permutationsOf(int x) {
		return permutationsOf(sequence(x));
	}

	public static int[][] permutationsOf(int[] x) {
		if (x.length == 1) {
			return new int[][] { x };
		}
		int[] part = new int[x.length - 1];
		System.arraycopy(x, 1, part, 0, part.length);
		int[][] perms = permutationsOf(part);
		int element = x[0];
		int[][] result = new int[perms.length * (perms[0].length + 1)][];
		for (int n = 0; n < perms.length; n++) {
			int[] perm = perms[n];
			for (int i = 0; i <= perm.length; i++) {
				int[] r = result[i + n * (perm.length + 1)] = new int[x.length];
				System.arraycopy(perm, 0, r, 0, i);
				r[i] = element;
				System.arraycopy(perm, i, r, i + 1, perm.length - i);
			}
		}
		return result;
	}

	public static int factorial(int x) {
		int factorial = 1;
		for (int i = 2; i <= x; i++) {
			factorial *= i;
		}
		return factorial;
	}

	public static int[] sequence(int n) {
		int[] sequence = new int[n];
		for (int k = 0; k < n; k++) {
			sequence[k] = k;
		}
		return sequence;
	}

	public static float wrapAngle(float angle) {
		angle %= 360;
		if (angle < 0) {
			angle += 360;
		}
		return angle;
	}

	public static String radiansToDMS(float angle) {
		angle *= RAD_TO_DEG;
		int degrees = (int) angle;
		angle -= (int) angle;
		angle *= 60;
		int minutes = (int) angle;
		angle -= (int) angle;
		angle *= 60;
		return String.format("%s\u00B0%s'%.3f\"", degrees, minutes, angle);
	}

	public static int mod(int a, int b) {
		return (a % b + b) % b;
	}

	public static float mod(float a, float b) {
		return (a % b + b) % b;
	}

	public static double mod(double a, double b) {
		return (a % b + b) % b;
	}

	public static float lerpAngle(float a, float b, float t) {
		return t * (mod(-a + b + 180, 360) - 180) + a;
	}

	public static float adjustAngleForInterpolation(float angle, float prevAngle) {
		return adjustValueForInterpolation(angle, prevAngle, -180, 180);
	}

	public static float adjustValueForInterpolation(float x, float prevX, float min, float max) {
		float range = max - min;
		while (x - prevX < min) {
			prevX -= range;
		}
		while (x - prevX >= max) {
			prevX += range;
		}
		return prevX;
	}

	public static int degToByte(float angle) {
		return MathHelper.floor_float(angle * (256F / 360));
	}

	public static int hash(int x) {
		x = (x >> 16 ^ x) * 0x45D9F3B;
		x = (x >> 16 ^ x) * 0x45D9F3B;
		x = x >> 16 ^ x;
		return x;
	}

	public static float modf(float a, float b) {
		return (a % b + b) % b;
	}

	/*
	 * http://people.cs.kuleuven.be/~ares.lagae/publications/LD05ERQIT/LD05ERQIT.pdf
	 */
	public static Optional<Vec3> intersect(Vec3 O, Vec3 D, Vec3 V_00, Vec3 V_10, Vec3 V_11, Vec3 V_01, boolean frontOnly) {
		final double e = 1e-8;
		Vec3 E_01 = V_10.subtract(V_00);
		Vec3 E_03 = V_01.subtract(V_00);
		Vec3 P = D.crossProduct(E_03);
		double det = E_01.dotProduct(P);
		if (frontOnly && det > 0) return Optional.<Vec3>empty();
		if (Math.abs(det) < e) return Optional.<Vec3>empty();
		Vec3 T = O.subtract(V_00);
		double a = T.dotProduct(P) / det;
		if (a < 0 || a > 1) return Optional.<Vec3>empty();
		Vec3 Q = T.crossProduct(E_01);
		double b = D.dotProduct(Q) / det;
		if (b < 0 || b > 1) return Optional.<Vec3>empty();

		/**
		 * In my testing this block made triangle V_10 V_11 V_01 not test correctly
		 * by the ray parameter being less than zero.
		 */
		// Reject rays using the barycentric coordinates of
		// the intersection point with respect to T'.
		/*if (a + b > 1) {
			Vec3 E_23 = V_01.subtract(V_11);
			Vec3 E_21 = V_10.subtract(V_11);
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
		if (t < 0) return Optional.<Vec3>empty();

		// Compute the barcentric coordinates of V_11.
		Vec3 E_02 = V_11.subtract(V_00);
		Vec3 N = E_01.crossProduct(E_03);
		double a_11, b_11;
		if (Math.abs(N.xCoord) >= Math.abs(N.yCoord) &&
			Math.abs(N.xCoord) >= Math.abs(N.zCoord)) {
			a_11 = (E_02.yCoord * E_03.zCoord - E_02.zCoord * E_03.yCoord) / N.xCoord;
			b_11 = (E_01.yCoord * E_02.zCoord - E_01.zCoord * E_02.yCoord) / N.xCoord;
		} else if (Math.abs(N.yCoord) >= Math.abs(N.xCoord) &&
			Math.abs(N.yCoord) >= Math.abs(N.zCoord)) {
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
			if (Math.abs(b_11 - 1) < e) v = b;
			else v = b / (u * (b_11 - 1) + 1);
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
			if (u < 0 || u > 1) u = C / q;
			v = b / (u * (b_11 - 1) + 1);
		}
		return Optional.of(new Vec3(u, v, t));
	}

	public static Vector3d getMatrixAsZYXEuler(Matrix4d matrix) {
		Vector3d result = new Vector3d();
		if (matrix.m02 < 1) {
			if (matrix.m02 > -1) {
				result.y = Math.asin(-matrix.m02);
				result.z = Math.atan2(matrix.m01, matrix.m00);
				result.x = Math.atan2(matrix.m12, matrix.m22);
			} else {
				result.y = Math.PI / 2;
				result.z = -Math.atan2(-matrix.m21, matrix.m11);
				result.x = 0;
			}
		} else {
			result.y = -Math.PI / 2;
			result.z = Math.atan2(-matrix.m21, matrix.m11);
			result.x = 0;
		}
		return result;
	}
}
