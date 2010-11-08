/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.util.concurrent.atomic.AtomicInteger;

import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.scene.Scene;

/**
 * A unit of work for a scene being ray traced. The {@link #workDone()} method
 * should be called after this work has been completed, and must be called
 * exactly once.
 * 
 * @author krisher
 * 
 */
public final class ImageBlock {
   public final int blockStartX;
   public final int blockStartY;
   public final int blockWidth;
   public final int blockHeight;

   public final Scene scene;
   public final ImageBuffer image;

   public final int pixelSampleRate;

   public final int recursionDepth;

   final AtomicInteger doneSignal;

   public ImageBlock(final ImageBuffer image, final Scene scene, final int blockStartX, final int blockStartY, final int blockWidth,
         final int blockHeight, final int pixelSampleRate, final int recursionDepth, final AtomicInteger doneSignal) {
      super();
      this.image = image;
      this.scene = scene;
      this.blockStartX = blockStartX;
      this.blockStartY = blockStartY;
      this.blockWidth = blockWidth;
      this.blockHeight = blockHeight;

      this.pixelSampleRate = pixelSampleRate;

      this.recursionDepth = recursionDepth;

      this.doneSignal = doneSignal;
   }

   public void workDone() {
      if (0 == doneSignal.decrementAndGet())
         image.imagingDone();
   }
}