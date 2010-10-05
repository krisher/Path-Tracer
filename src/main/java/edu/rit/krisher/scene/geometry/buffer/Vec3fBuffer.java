package edu.rit.krisher.scene.geometry.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;
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

}
