package edu.rit.krisher.scene.geometry.buffer;

abstract class BaseBuffer implements Buffer {
   int limit;
   int position;

   public BaseBuffer() {
      super();
   }

   @Override
   public int limit() {
      return limit;
   }

   @Override
   public Buffer limit(final int newLimit) {
      if (newLimit < 0 || newLimit > capacity())
         throw new IllegalArgumentException();
      this.limit = newLimit;
      if (position > limit)
         position = limit;
      return this;
   }

   @Override
   public int position() {
      return position;
   }

   @Override
   public Buffer clear() {
      this.position = 0;
      this.limit = capacity();
      return this;
   }

   @Override
   public Buffer flip() {
      limit = position;
      position = 0;
      return this;
   }

   @Override
   public Buffer position(final int newPosition) {
      if ((newPosition > limit) || (newPosition < 0))
         throw new IllegalArgumentException();
      position = newPosition;
      return this;
   }

}