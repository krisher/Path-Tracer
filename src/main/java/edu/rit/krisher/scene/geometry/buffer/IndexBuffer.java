package edu.rit.krisher.scene.geometry.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * A buffer that holds 3-component single-precision vectors.
 * 
 * @author krisher
 * 
 */
public class IndexBuffer extends BaseBuffer {

   private final int[] buffer;

   public IndexBuffer(final int capacity) {
      this.buffer = new int[capacity];
      limit(capacity);
   }

   @Override
   public int capacity() {
      return buffer.length;
   }

   public int get() {
      if (position >= limit)
         throw new BufferUnderflowException();
      return buffer[position++];
   }

   public int get(final int idx) {
      if (idx >= limit)
         throw new BufferUnderflowException();
      return buffer[idx];
   }

   public IndexBuffer put(final int value) {
      if (position >= limit)
         throw new BufferOverflowException();
      buffer[position++] = value;
      return this;
   }

   public IndexBuffer put(final int idx, final int value) {
      if (idx >= limit)
         throw new BufferOverflowException();
      buffer[idx] = value;
      return this;
   }

}
