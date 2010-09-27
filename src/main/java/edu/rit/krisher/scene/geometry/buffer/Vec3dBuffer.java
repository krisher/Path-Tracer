package edu.rit.krisher.scene.geometry.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

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
   public Vec3Buffer put(final int idx, final Vec3 value) {
      if (idx >= limit)
         throw new BufferOverflowException();
      final int offs = idx * 3;
      buffer[offs] = value.x;
      buffer[offs + 1] = value.y;
      buffer[offs + 2] = value.z;
      return this;
   }

}
