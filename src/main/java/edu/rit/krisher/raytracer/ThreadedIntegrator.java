package edu.rit.krisher.raytracer;

import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.DefaultScene;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.util.Timer;

/**
 * Class to manage ray tracing process in multiple threads.
 * 
 * <p>
 * This class uses one {@link PathTracer} per available CPU to trace rays. When a scene is submitted for tracing, the
 * image pixels are divided into a number of work items, which are placed in a queue for processing by one of the
 * PathTracers.
 * 
 * <p>
 * Each work item consists of a small rectangle of pixels. This serves two purposes:
 * <ul>
 * <li>There are many more work items than threads. This incurs some degree of load balancing, if the regions
 * significantly vary in the amount of time needed to trace them.</li>
 * <li>Each thread will be dealing with a small, contiguous subset of pixels, which should help memory access coherency
 * somewhat. This is Java, so there really isn't too much we can do in this area, except provide opportunities for the
 * VM to do the right thing.</li>
 * </ul>
 * 
 * <p>
 * The results of the rendering are returned asynchronously with the
 * {@link #integrate(ImageBuffer, Camera, DefaultScene, int, int)} call. The appropriate methods in {@link ImageBuffer}
 * are called whenever pixel data is ready, and when the rendering is complete.
 * 
 * <p>
 * Multiple scenes can be rendered simultaneously, however an ImageBuffer may only be used for a single active rendering
 * process at any time.
 * 
 * @author krisher
 * 
 */
public abstract class ThreadedIntegrator implements SceneIntegrator {
   // private static final int BLOCK_SIZE = 128;
   public static final int BLOCK_SIZE = 64;
   protected static final int threads = Runtime.getRuntime().availableProcessors();
   protected static final NumberFormat formatter = NumberFormat.getNumberInstance();
   protected static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
   protected final Timer timer = new Timer("Ray Trace (Thread Timing)");

   public ThreadedIntegrator() {

   }



   public static Rectangle[] chunkRectangle(final int width, final int height, final int blockSize) {
      /*
       * Create ray processors.
       */
      final int xBlocks = (int) Math.ceil((width / (double) blockSize));
      final int yBlocks = (int) Math.ceil((height / (double) blockSize));
      final Rectangle[] result = new Rectangle[xBlocks * yBlocks];
      /*
       * Tiled work distribution...
       */
      for (int j = 0; j < yBlocks; j++) {
         final int blockStartY = j * BLOCK_SIZE;
         for (int i = 0; i < xBlocks; i++) {
            final int blockStartX = i * BLOCK_SIZE;
            result[j * xBlocks + i] = new Rectangle(blockStartX, blockStartY, Math.min(blockSize, width - blockStartX), Math.min(blockSize, height
                                                                                                                                 - blockStartY));
         }
      }
      return result;
   }

   /**
    * Cancels rendering for the specified ImageBuffer (that was previously passed to
    * {@link #integrate(ImageBuffer, Camera, Scene, int, int)}).
    * 
    * <p>
    * Any non-started work items are removed from the work queue, but work items already being processed are allowed to
    * finish. Pixel data may still be sent to the specified ImageBuffer until its {@link ImageBuffer#imagingDone()}
    * method is called.
    * 
    * @param target
    */
   @Override
   public void cancel(final ImageBuffer target) {

      final ArrayList<Runnable> drained = new ArrayList<Runnable>();
      threadPool.getQueue().drainTo(drained);
      AtomicInteger remaining = null;
      int removed = 0;
      for (final Iterator<ImageBlock> itr = drained.iterator(); itr.hasNext();) {
         final ImageBlock itm = itr.next();
         if (itm.image == target) {
            itr.remove();
            remaining = itm.doneSignal;
            removed++;
         }
      }
      for (final Runnable r : drained) {
         threadPool.submit(r);
      }
      if (remaining != null) {
         if (remaining.addAndGet(-removed) == 0) {
            target.imagingDone();
         }
      }
   }

}
