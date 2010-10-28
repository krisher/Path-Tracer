package edu.rit.krisher.raytracer;

import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
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
 * This class uses one {@link PathTracer} per available CPU to trace rays. When
 * a scene is submitted for tracing, the image pixels are divided into a number
 * of work items, which are placed in a queue for processing by one of the
 * PathTracers.
 * 
 * <p>
 * Each work item consists of a small rectangle of pixels. This serves two
 * purposes:
 * <ul>
 * <li>There are many more work items than threads. This incurs some degree of
 * load balancing, if the regions significantly vary in the amount of time
 * needed to trace them.</li>
 * <li>Each thread will be dealing with a small, contiguous subset of pixels,
 * which should help memory access coherency somewhat. This is Java, so there
 * really isn't too much we can do in this area, except provide opportunities
 * for the VM to do the right thing.</li>
 * </ul>
 * 
 * <p>
 * The results of the rendering are returned asynchronously with the
 * {@link #rayTrace(ImageBuffer, Camera, DefaultScene, int, int)} call. The appropriate
 * methods in {@link ImageBuffer} are called whenever pixel data is ready, and
 * when the rendering is complete.
 * 
 * <p>
 * Multiple scenes can be rendered simultaneously, however an ImageBuffer may
 * only be used for a single active rendering process at any time.
 * 
 * @author krisher
 * 
 */
public class RayEngine {
   // private static final int BLOCK_SIZE = 128;
   private static final int threads = Runtime.getRuntime().availableProcessors();
   private static final NumberFormat formatter = NumberFormat.getNumberInstance();
   private static final LinkedBlockingQueue<WorkItem> workQueue = new LinkedBlockingQueue<WorkItem>();
   private static final Thread[] workerThreads = new Thread[threads];
   private static final Timer timer = new Timer("Ray Trace (Thread Timing)");
   static {
      for (int i = 0; i < workerThreads.length; i++) {
         final PathTracer tracer = new PathTracer();
         workerThreads[i] = new Thread("Ray Tracing Thread - " + i) {
            /**
             * Perpetually takes items from the work queue specified in the
             * constructor and path-traces. This method does not return until
             * the host thread is interrupted, or an error occurs while
             * processing one of the work items.
             */
            @Override
            public void run() {
               try {
                  while (!Thread.interrupted()) {
                     final WorkItem item = workQueue.take();
                     timer.start();
                     tracer.pathTrace(item);
                     timer.stop();
                     timer.print();
                  }
               } catch (final InterruptedException ie) {
                  return;
               } catch (final Throwable t) {
                  t.printStackTrace();
               }
            }
         };
         workerThreads[i].setDaemon(true);
         workerThreads[i].start();
      }
   }

   private static final int BLOCK_SIZE = 64;

   /**
    * Asynchronously ray traces the specified scene given the camera position
    * and ImageBuffer to store the results in.
    * 
    * @param image
    *           A non-null ImageBuffer. The dimensions of the ray-traced image
    *           are determined from the {@link ImageBuffer#getResolution()}
    *           method synchronously with this call.
    * @param camera
    *           The non-null camera that will produce the initial set of sample
    *           rays (eye rays).
    * @param scene
    *           The non-null scene to render.
    * @param pixelSampleRate
    *           The linear super-sampling rate. This value squared is the actual
    *           number of paths traced for each image pixel. Must be greater
    *           than 0.
    * @param recursionDepth
    *           The maximum length of a ray path. 0 means trace eye rays and
    *           direct illumination only.
    */
   public static void rayTrace(final ImageBuffer image, final Camera camera, final Scene scene, final int pixelSampleRate, final int recursionDepth) {
      /*
       * Imaging parameters
       */
      final Dimension imageSize = image.getResolution();
      /*
       * Estimate for the upper bound of the number of rays that will be
       * generated, including one ray for each ray path component, and shadow
       * rays. The actual number of rays traced will likely be lower than this,
       * some rays will be absorbed before tracing to the maximum path length,
       * some rays might not hit anything (for open scenes), etc.
       */
      final double rayCount = ((double) imageSize.width * imageSize.height * pixelSampleRate * pixelSampleRate
            * recursionDepth * (1.0 + scene.getLightSources().length));
      System.out.println("Max Samples: " + formatter.format(rayCount));

      /*
       * Create ray processors.
       */
      final int xBlocks = (int) Math.ceil((imageSize.width / (double) BLOCK_SIZE));
      final int yBlocks = (int) Math.ceil((imageSize.height / (double) BLOCK_SIZE));

      /*
       * Thread-safe spin-lock based countdown latch to monitor progress for
       * this image. When this reaches 0, the ImageBuffer is notified that the
       * rendering is complete.
       */
      final AtomicInteger doneSignal = new AtomicInteger(xBlocks * yBlocks);
      /*
       * WorkItems may begin processing as soon as they are queued, so notify
       * the ImageBuffer that pixels are on their way...
       */
      timer.reset();
      image.imagingStarted();

      /*
       * Tiled work distribution...
       */
      for (int i = 0; i < xBlocks; i++) {
         final int blockStartX = i * BLOCK_SIZE;
         for (int j = 0; j < yBlocks; j++) {
            final int blockStartY = j * BLOCK_SIZE;
            workQueue.add(new WorkItem(image, scene, camera, blockStartX, blockStartY, Math.min(BLOCK_SIZE,
                                                                                                imageSize.width
                                                                                                - blockStartX),
                                                                                                Math.min(BLOCK_SIZE, imageSize.height - blockStartY), pixelSampleRate,

                                                                                                recursionDepth, doneSignal));
         }
      }

   }

   /**
    * Cancels rendering for the specified ImageBuffer (that was previously
    * passed to {@link #rayTrace(ImageBuffer, Camera, Scene, int, int)}).
    * 
    * <p>
    * Any non-started work items are removed from the work queue, but work items
    * already being processed are allowed to finish. Pixel data may still be
    * sent to the specified ImageBuffer until its
    * {@link ImageBuffer#imagingDone()} method is called.
    * 
    * @param target
    */
   public static void cancel(final ImageBuffer target) {
      final ArrayList<WorkItem> drained = new ArrayList<WorkItem>();
      workQueue.drainTo(drained);
      AtomicInteger remaining = null;
      int removed = 0;
      for (final Iterator<WorkItem> itr = drained.iterator(); itr.hasNext();) {
         final WorkItem itm = itr.next();
         if (itm.image == target) {
            itr.remove();
            remaining = itm.doneSignal;
            removed++;
         }
      }
      workQueue.addAll(drained);
      if (remaining != null) {
         if (remaining.addAndGet(-removed) == 0) {
            target.imagingDone();
         }
      }
   }
}
