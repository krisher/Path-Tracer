package edu.rit.krisher.collections;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

public class CopyOnWriteArrayList<T> implements List<T>, RandomAccess {

   public volatile T[] array;
   protected volatile boolean useObjectEquality = true;

   public CopyOnWriteArrayList(final Class<T> type) {
      array = (T[]) Array.newInstance(type, 0);
   }

   @Override
   public synchronized void add(final int idx, final T value) {
      this.array = insert(idx, value);
   }

   @Override
   public synchronized boolean add(final T value) {
      final T[] newArray = insert(size(), value);
      final boolean added = newArray == this.array;
      this.array = newArray;
      return added;
   }

   @Override
   public synchronized boolean addAll(final Collection<? extends T> arg0) {
      this.array = addAll(array.length, arg0.toArray());
      return true;
   }

   @Override
   public boolean addAll(final int arg0, final Collection<? extends T> arg1) {
      this.array = addAll(arg0, arg1.toArray());
      return true;
   }

   @Override
   public void clear() {
      set(null);
   }

   @Override
   public boolean contains(final Object o) {
      if (useObjectEquality && o != null) {
         for (final Object elt : array) {
            if (elt != null && o.equals(elt)) {
               return true;
            }
         }
      } else {
         for (final Object elt : array) {
            if (elt == o)
               return true;
         }
      }
      return false;
   }

   @Override
   public boolean containsAll(final Collection<?> arg0) {
      final T[] array = this.array;
      outerLoop: for (final Object o : arg0) {
         if (useObjectEquality && o != null) {
            for (final Object elt : array) {
               if (elt != null && o.equals(elt)) {
                  continue outerLoop;
               }
            }
         } else {
            for (final Object elt : array) {
               if (elt == o)
                  continue outerLoop;
            }
         }
         return false;
      }
      return true;
   }

   @Override
   public T get(final int arg0) {
      return array[arg0];
   }

   public synchronized T[] getAndClear() {
      final T[] oldArray = this.array;
      this.array = (T[]) Array.newInstance(getComponentType(), 0);
      return oldArray;
   }

   public Class<T> getComponentType() {
      return (Class<T>) array.getClass().getComponentType();
   }

   @Override
   public int indexOf(final Object value) {
      final int size = size();
      final T[] array = this.array;
      if (useObjectEquality && value != null) {
         for (int i = 0; i < size; i++) {
            final T elmnt = array[i];
            if (elmnt != null && value.equals(elmnt)) {
               return i;
            }
         }
      } else {
         for (int i = 0; i < size; i++) {
            final Object elmnt = array[i];
            if (elmnt == value) {
               return i;
            }
         }
      }
      return -1;
   }

   @Override
   public boolean isEmpty() {
      return array.length == 0;
   }

   @Override
   public Iterator<T> iterator() {
      return new ArrayIterator<T>(array);
   }

   @Override
   public int lastIndexOf(final Object value) {
      final int size = size();
      final T[] array = this.array;
      if (useObjectEquality && value != null) {
         for (int i = size - 1; i >= 0; i--) {
            final T elmnt = array[i];
            if (elmnt != null && value.equals(elmnt)) {
               return i;
            }
         }
      } else {
         for (int i = size - 1; i >= 0; i--) {
            final Object elmnt = array[i];
            if (elmnt == value) {
               return i;
            }
         }
      }
      return -1;
   }

   @Override
   public ListIterator<T> listIterator() {
      return new ArrayIterator<T>(array);
   }

   @Override
   public ListIterator<T> listIterator(final int arg0) {
      return new ArrayIterator<T>(array, arg0);
   }

   @Override
   public synchronized T remove(final int idx) {
      final int oldSize = size();
      final T[] array = (T[]) Array.newInstance(getComponentType(), oldSize - 1);
      if (oldSize > 1) {
         if (idx > 0)
            System.arraycopy(this.array, 0, array, 0, idx);
         if (idx < oldSize - 1)
            System.arraycopy(this.array, idx + 1, array, idx, oldSize - idx - 1);
      }
      final T oldValue = this.array[idx];
      this.array = array;
      return oldValue;
   }

   @Override
   public synchronized boolean remove(final Object value) {
      if (size() == 0)
         return false;
      final int idx = indexOf(value);
      if (idx >= 0) {
         remove(idx);
         return true;
      } else
         return false;
   }

   @Override
   public synchronized boolean removeAll(final Collection<?> arg0) {
      final boolean[] removed = new boolean[size()];
      int count = 0;
      for (final Object o : arg0) {
         if (useObjectEquality && o != null) {
            for (int i = 0; i < removed.length; i++) {
               if (!removed[i]) {
                  if (null != array[i] && o.equals(array[i])) {
                     removed[i] = true;
                     count++;
                  }
               }
            }
         } else {
            for (int i = 0; i < removed.length; i++) {
               if (!removed[i]) {
                  if (o == array[i]) {
                     removed[i] = true;
                     count++;
                  }
               }
            }
         }
      }
      final T[] newArray = (T[]) Array.newInstance(getComponentType(), array.length - count);
      for (int oldIdx = 0, newIdx = 0; oldIdx < removed.length; oldIdx++) {
         if (!removed[oldIdx]) {
            newArray[newIdx++] = array[oldIdx];
         }
      }
      this.array = newArray;
      return count > 0;
   }

   @Override
   public boolean retainAll(final Collection<?> arg0) {
      final boolean[] retained = new boolean[size()];
      int count = 0;
      for (final Object o : arg0) {
         if (useObjectEquality && o != null) {
            for (int i = 0; i < retained.length; i++) {
               if (!retained[i]) {
                  if (null != array[i] && o.equals(array[i])) {
                     retained[i] = true;
                     count++;
                  }
               }
            }
         } else {
            for (int i = 0; i < retained.length; i++) {
               if (!retained[i]) {
                  if (o == array[i]) {
                     retained[i] = true;
                     count++;
                  }
               }
            }
         }
      }
      final T[] newArray = (T[]) Array.newInstance(getComponentType(), array.length - count);
      for (int oldIdx = 0, newIdx = 0; oldIdx < retained.length; oldIdx++) {
         if (retained[oldIdx]) {
            newArray[newIdx++] = array[oldIdx];
         }
      }
      final boolean changed = newArray.length != array.length;
      this.array = newArray;
      return changed;
   }

   @Override
   public synchronized T set(final int idx, final T value) {
      final T[] oldArray = this.array;
      final T[] array = (T[]) Array.newInstance(getComponentType(), oldArray.length);
      System.arraycopy(oldArray, 0, array, 0, array.length);
      array[idx] = value;
      this.array = array;
      return oldArray[idx];
   }

   public synchronized T[] set(final T[] array) {
      final T[] oldArray = this.array;
      if (array != null) {
         final T[] newArray = (T[]) Array.newInstance(getComponentType(), array.length);
         if (newArray.length > 0)
            System.arraycopy(array, 0, newArray, 0, array.length);
      } else {
         this.array = (T[]) Array.newInstance(getComponentType(), 0);
      }
      return oldArray;
   }

   @Override
   public int size() {
      return array.length;
   }

   @Override
   public List<T> subList(final int arg0, final int arg1) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object[] toArray() {
      final T[] oldArray = this.array;
      final T[] array = (T[]) Array.newInstance(getComponentType(), oldArray.length);
      System.arraycopy(oldArray, 0, array, 0, array.length);
      return array;
   }

   @Override
   public <X> X[] toArray(X[] array) {
      final T[] oldArray = this.array;
      if (array.length != oldArray.length)
         array = (X[]) Array.newInstance(array.getClass().getComponentType(), oldArray.length);
      System.arraycopy(oldArray, 0, array, 0, array.length);
      return array;
   }

   protected T[] addAll(final int idx, final Object[] array) {
      final T[] newArray = (T[]) Array.newInstance(getComponentType(), this.array.length + array.length);
      if (idx > 0)
         System.arraycopy(this.array, 0, newArray, 0, idx);
      if (array.length > 0)
         System.arraycopy(array, 0, newArray, idx, array.length);
      if (idx < this.array.length)
         System.arraycopy(this.array, idx, newArray, idx + array.length, this.array.length - idx);
      return newArray;
   }

   protected synchronized T[] insert(final int idx, final T value) {
      final int size = size();
      final T[] array = (T[]) Array.newInstance(getComponentType(), size + 1);
      if (idx > 0)
         System.arraycopy(this.array, 0, array, 0, idx);
      if (idx < size - 1)
         System.arraycopy(this.array, idx, array, idx + 1, size - idx);
      array[idx] = value;
      return array;
   }

   public boolean isUseObjectEquality() {
      return useObjectEquality;
   }

   public void setUseObjectEquality(final boolean useObjectEquality) {
      this.useObjectEquality = useObjectEquality;
   }

}
