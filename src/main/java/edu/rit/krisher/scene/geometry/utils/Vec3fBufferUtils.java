package edu.rit.krisher.scene.geometry.utils;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Vec3;

/**
 * A buffer that holds 3-component single-precision vectors.
 * 
 * @author krisher
 * 
 */
public final class Vec3fBufferUtils {

   private Vec3fBufferUtils() {
   }

   public static void get(final Vec3 value, final float[] buffer, final int idx) {
      final int offs = idx * 3;
      value.x = buffer[offs];
      value.y = buffer[offs + 1];
      value.z = buffer[offs + 2];
   }

   public static void put(final Vec3 value, final float[] buffer, final int idx) {
      final int offs = idx * 3;
      buffer[offs] = (float) value.x;
      buffer[offs + 1] = (float) value.y;
      buffer[offs + 2] = (float) value.z;
   }

   public static void add(final Vec3 value, final float[] buffer, final int idx) {
      final int offs = idx * 3;
      buffer[offs] += (float) value.x;
      buffer[offs + 1] += (float) value.y;
      buffer[offs + 2] += (float) value.z;
   }

   public static void normalize(final float[] buffer, final int idx) {
      final int offs = idx * 3;
      final double l = Math.sqrt(buffer[offs] * buffer[offs] + buffer[offs + 1] * buffer[offs + 1] + buffer[offs + 2]
            * buffer[offs + 2]);

      buffer[offs] /= l;
      buffer[offs + 1] /= l;
      buffer[offs + 2] /= l;
   }

   public static final AxisAlignedBoundingBox computeBounds(final float[] buffer) {
      final AxisAlignedBoundingBox bounds = new AxisAlignedBoundingBox();
      final int verts = buffer.length;
      for (int i = 0; i < verts; i += 3) {
         for (int j = 0; j < 3; ++j) {
            if (buffer[i + j] < bounds.xyzxyz[j])
               bounds.xyzxyz[j] = buffer[i + j];
            if (buffer[i + j] > bounds.xyzxyz[j + 3])
               bounds.xyzxyz[j + 3] = buffer[i + j];
         }
      }
      return bounds;
   }
   
   public static AxisAlignedBoundingBox computeBounds(final float[] buffer, final int... indices) {
      final AxisAlignedBoundingBox bounds = new AxisAlignedBoundingBox();
      for (final int index : indices) {
         final int offset = index * 3;
         for (int j = 0; j < 3; ++j) {
            if (buffer[offset + j] < bounds.xyzxyz[j])
               bounds.xyzxyz[j] = buffer[offset + j];
            if (buffer[offset + j] > bounds.xyzxyz[j + 3])
               bounds.xyzxyz[j + 3] = buffer[offset + j];
         }
      }
      return bounds;
   }


   
}
