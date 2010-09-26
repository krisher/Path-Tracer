package edu.rit.krisher.collections;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class ArrayIterator<T> implements ListIterator<T> {

   private final T[] array;
   private int index = 0;

   public ArrayIterator(final T[] array) {
      this.array = array;
   }

   public ArrayIterator(final T[] array, final int startIndex) {
      this(array);
      if (startIndex < array.length)
         index = startIndex;
      else
         throw new ArrayIndexOutOfBoundsException();
   }

   @Override
   public void add(final Object arg0) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean hasNext() {
      return index < array.length;
   }

   @Override
   public boolean hasPrevious() {
      return index > 0;
   }

   @Override
   public T next() {
      if (index < array.length)
         return array[index++];
      else
         throw new NoSuchElementException();
   }

   @Override
   public int nextIndex() {
      return index;
   }

   @Override
   public T previous() {
      if (index > 0)
         return array[--index];
      throw new NoSuchElementException();
   }

   @Override
   public int previousIndex() {
      if (index > 0)
         return index - 1;
      return 0;
   }

   @Override
   public void remove() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void set(final Object arg0) {
      throw new UnsupportedOperationException();
   }

}
