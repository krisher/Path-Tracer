package edu.rit.krisher.scene.geometry.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 * A buffer that holds 3-component double-precision vectors.
 * 
 * @author krisher
 * 
 */
public class Vec3dBuffer extends BaseBuffer implements Vec3Buffer {

   private final double[] buffer;

   public Vec3dBuffer(final int capacity) {
      this.buffer = new double[capacity * 3];
      limit(capacity);
   }

   @Override
   public int capacity() {
      return buffer.length / 3;
   }

   @Override
   public Vec3Buffer get(final Vec3 value) {
      if (position >= limit)
         throw new BufferUnderflowException();
      final int offs = position * 3;
      value.x = buffer[offs];
      value.y = buffer[offs + 1];
      value.z = buffer[offs + 2];
      ++position;
      return this;
   }

   @Override
   public Vec3Buffer get(final int idx, final Vec3 value) {
      if (idx >= limit)
         throw new BufferUnderflowException();
      final int offs = idx * 3;
      value.x = buffer[offs];
      value.y = buffer[offs + 1];
      value.z = buffer[offs + 2];
      return this;
   }

   @Override
   public Vec3Buffer put(final Vec3 value) {
      if (position >= limit)
         throw new BufferOverflowException();
      final int offs = position * 3;
      buffer[offs] = value.x;
      buffer[offs + 1] = value.y;
      buffer[offs + 2] = value.z;
      ++position;
      return this;
   }

   @Override
   public Vec3Buffer put(final double x, final double y, final double z) {
      if (position >= limit)
         throw new BufferOverflowException();
      final int offs = position * 3;
      buffer[offs] = x;
      buffer[offs + 1] = y;
      buffer[offs + 2] = z;
      ++position;
      return this;
   }

   @Override
   public Vec3Buffer put(final int idx, final Vec3 value) {
      if (idx >= limit)
         throw new BufferOverflowException();
      final int offs = idx * 3;
      buffer[offs] = value.x;
      buffer[offs + 1] = value.y;
      buffer[offs + 2] = value.z;
      return this;
   }

   @Override
   public Vec3Buffer get(final double[] value, final int... indices) {
      int outOffs = 0;
      for (final int index : indices) {
         final int offs = index * 3;
         value[outOffs] = buffer[offs];
         value[outOffs + 1] = buffer[offs + 1];
         value[outOffs + 2] = buffer[offs + 2];
         outOffs += 3;
      }
      return this;
   }

   @Override
   public double intersectsTriangle(final Ray ray, int v0Idx, int v1Idx, int v2Idx) {
      v0Idx *= 3;
      v1Idx *= 3;
      v2Idx *= 3;
      final double v0X = buffer[v0Idx];
      final double v0Y = buffer[v0Idx + 1];
      final double v0Z = buffer[v0Idx + 2];
      return ray.intersectsTriangle(v0X, v0Y, v0Z, buffer[v1Idx] - v0X, buffer[v1Idx + 1] - v0Y, buffer[v1Idx + 2] - v0Z, buffer[v2Idx] - v0X, buffer[v2Idx + 1] - v0Y, buffer[v2Idx + 2] - v0Z);
   }

   @Override
   public boolean intersectsTriangleBarycentric(final double[] tuv, final Ray ray, int v0Idx, int v1Idx, int v2Idx) {
      v0Idx *= 3;
      v1Idx *= 3;
      v2Idx *= 3;
      final double v0X = buffer[v0Idx];
      final double v0Y = buffer[v0Idx + 1];
      final double v0Z = buffer[v0Idx + 2];
      return ray.intersectsTriangleBarycentric(tuv, v0X, v0Y, v0Z, buffer[v1Idx] - v0X, buffer[v1Idx + 1] - v0Y, buffer[v1Idx + 2] - v0Z, buffer[v2Idx] - v0X, buffer[v2Idx + 1] - v0Y, buffer[v2Idx + 2] - v0Z);
   }

   @Override
   public void getTriangleNormal(final Vec3 outNormal, int v0Idx, int v1Idx, int v2Idx) {
      v0Idx *= 3;
      v1Idx *= 3;
      v2Idx *= 3;
      final double v0X = buffer[v0Idx];
      final double v0Y = buffer[v0Idx + 1];
      final double v0Z = buffer[v0Idx + 2];
      outNormal.set(buffer[v1Idx] - v0X, buffer[v1Idx + 1] - v0Y, buffer[v1Idx + 2] - v0Z);
      outNormal.cross(buffer[v2Idx] - v0X, buffer[v2Idx + 1] - v0Y, buffer[v2Idx + 2] - v0Z);
   }

   /**
    * @param vecs
    */
   @Override
   public final Vec3dBuffer getTriangleVEE(final double[] vecs, int v0Idx, int v1Idx, int v2Idx) {
      v0Idx *= 3;
      vecs[0] = buffer[v0Idx];
      vecs[1] = buffer[v0Idx + 1];
      vecs[2] = buffer[v0Idx + 2];

      v1Idx *= 3;
      vecs[3] = buffer[v1Idx] - vecs[0];
      vecs[4] = buffer[v1Idx + 1] - vecs[1];
      vecs[5] = buffer[v1Idx + 2] - vecs[2];

      v2Idx *= 3;
      vecs[6] = buffer[v2Idx] - vecs[0];
      vecs[7] = buffer[v2Idx + 1] - vecs[1];
      vecs[8] = buffer[v2Idx + 2] - vecs[2];

      return this;
   }

   @Override
   public final AxisAlignedBoundingBox computeBounds() {
      final AxisAlignedBoundingBox bounds = new AxisAlignedBoundingBox();
      final int verts = limit() * 3;
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

   @Override
   public AxisAlignedBoundingBox computeBounds(final int... indices) {
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
