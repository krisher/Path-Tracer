package edu.rit.krisher.scene.geometry.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 * A buffer that holds 3-component single-precision vectors.
 * 
 * @author krisher
 * 
 */
public class Vec3fBuffer extends BaseBuffer implements Vec3Buffer {

   private final float[] buffer;

   public Vec3fBuffer(final int capacity) {
      this.buffer = new float[capacity * 3];
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
      buffer[offs] = (float) value.x;
      buffer[offs + 1] = (float) value.y;
      buffer[offs + 2] = (float) value.z;
      ++position;
      return this;
   }

   @Override
   public Vec3Buffer put(final double x, final double y, final double z) {
      if (position >= limit)
         throw new BufferOverflowException();
      final int offs = position * 3;
      buffer[offs] = (float) x;
      buffer[offs + 1] = (float) y;
      buffer[offs + 2] = (float) z;
      ++position;
      return this;
   }

   @Override
   public Vec3Buffer put(final int idx, final Vec3 value) {
      if (idx >= limit)
         throw new BufferOverflowException();
      final int offs = idx * 3;
      buffer[offs] = (float) value.x;
      buffer[offs + 1] = (float) value.y;
      buffer[offs + 2] = (float) value.z;
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
   public final AxisAlignedBoundingBox computeBounds() {
      final AxisAlignedBoundingBox bounds = new AxisAlignedBoundingBox();
      final int verts = limit() * 3;
      for (int i = 0; i < verts; i += 3) {
         for (int j = 0; j < 3; ++j) {
            if (buffer[i + j] < bounds.minXYZ[j])
               bounds.minXYZ[j] = buffer[i + j];
            if (buffer[i + j] > bounds.maxXYZ[j])
               bounds.maxXYZ[j] = buffer[i + j];
         }
      }
      return bounds;
   }

   public final Vec3fBuffer getTriangleVEE(final double[] vecs, int v0Idx, int v1Idx, int v2Idx) {
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

   public double intersectsTriangle(final Ray ray, int v0Idx, int v1Idx, int v2Idx) {
      v0Idx *= 3;
      v1Idx *= 3;
      v2Idx *= 3;
      final float v0X = buffer[v0Idx];
      final float v0Y = buffer[v0Idx + 1];
      final float v0Z = buffer[v0Idx + 2];
      return ray.intersectsTriangle(v0X, v0Y, v0Z, buffer[v1Idx] - v0X, buffer[v1Idx + 1] - v0Y, buffer[v1Idx + 2]
            - v0Z, buffer[v2Idx] - v0X, buffer[v2Idx + 1] - v0Y, buffer[v2Idx + 2] - v0Z);
   }
   
   public boolean intersectsTriangleBarycentric(final double[] tuv, final Ray ray, int v0Idx, int v1Idx, int v2Idx) {
      v0Idx *= 3;
      v1Idx *= 3;
      v2Idx *= 3;
      final float v0X = buffer[v0Idx];
      final float v0Y = buffer[v0Idx + 1];
      final float v0Z = buffer[v0Idx + 2];
      return ray.intersectsTriangleBarycentric(tuv, v0X, v0Y, v0Z, buffer[v1Idx] - v0X, buffer[v1Idx + 1] - v0Y, buffer[v1Idx + 2] - v0Z, buffer[v2Idx] - v0X, buffer[v2Idx + 1] - v0Y, buffer[v2Idx + 2] - v0Z);
   }
   
   public void getTriangleNormal(final Vec3 outNormal, int v0Idx, int v1Idx, int v2Idx) {
      v0Idx *= 3;
      v1Idx *= 3;
      v2Idx *= 3;
      final float v0X = buffer[v0Idx];
      final float v0Y = buffer[v0Idx + 1];
      final float v0Z = buffer[v0Idx + 2];
      outNormal.set(buffer[v1Idx] - v0X, buffer[v1Idx + 1] - v0Y, buffer[v1Idx + 2] - v0Z);
      outNormal.cross(buffer[v2Idx] - v0X, buffer[v2Idx + 1] - v0Y, buffer[v2Idx + 2] - v0Z).normalize();
   }

   @Override
   public AxisAlignedBoundingBox computeBounds(final int... indices) {
      final AxisAlignedBoundingBox bounds = new AxisAlignedBoundingBox();
      for (final int index : indices) {
         final int offset = index * 3;
         for (int j = 0; j < 3; ++j) {
            if (buffer[offset + j] < bounds.minXYZ[j])
               bounds.minXYZ[j] = buffer[offset + j];
            if (buffer[offset + j] > bounds.maxXYZ[j])
               bounds.maxXYZ[j] = buffer[offset + j];
         }
      }
      return bounds;
   }

}
