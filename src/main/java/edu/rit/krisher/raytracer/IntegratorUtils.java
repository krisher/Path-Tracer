package edu.rit.krisher.raytracer;

import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.DefaultScene;
import edu.rit.krisher.scene.Geometry;
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
public abstract class IntegratorUtils implements SceneIntegrator {
   // private static final int BLOCK_SIZE = 128;
   public static final int BLOCK_SIZE = 64;
   protected static final int threads = Runtime.getRuntime().availableProcessors();
   protected static final NumberFormat formatter = NumberFormat.getNumberInstance();
   protected static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
   protected final Timer timer = new Timer("Ray Trace (Thread Timing)");

   public IntegratorUtils() {
      
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



   public static void processIntersections(final SampleRay[] rays, final int count, final Geometry[] geometry) {
      for (int i=0; i < count; ++i) {
         final SampleRay ray = rays[i];
         ray.intersection.t = Double.POSITIVE_INFINITY;
         ray.intersection.hitGeometry = null;
         ray.intersection.primitiveID = -1;
         for (final Geometry geom : geometry) {
            geom.intersects(ray, ray.intersection);
         }
      }
   }
   
   /**
    * Initializes the pixelX and pixelY values of the specified SampleRays based on the specified pixel rectangle,
    * multi-sampling rate, and random number generator for jittering the sample locations.
    * 
    * @param sampleRays An array of pixelRect.width * pixelRect.height * msGridSize * msGridSize sample rays.
    * @param pixelRect The pixels for which to initialize rays.
    * @param msGridSize The multi-sample rate for each pixel.  msGridSize * msGridSize rays will be initialized for each pixel (with different locations within the pixel.
    * @param rng A random number generator.
    */
   public static final void generatePixelSamples(final SampleRay[] sampleRays, final Rectangle pixelRect,
         final int msGridSize, final Random rng) {
      /*
       * Imaging ray generation.
       */
      int sampleIdx = 0;
      for (int pixelY = 0; pixelY < pixelRect.height; pixelY++) {
         for (int pixelX = 0; pixelX < pixelRect.width; pixelX++) {
            for (int sampleX = 0; sampleX < msGridSize; ++sampleX) {
               for (int sampleY = 0; sampleY < msGridSize; ++sampleY) {
                  /*
                   * Stratified jittered sampling, an eye ray is generated that passes through a random location in a
                   * small square region of the pixel area for each sample.
                   */
                  sampleRays[sampleIdx].pixelX = pixelRect.x + pixelX + (sampleX) / msGridSize + rng.nextFloat()
                        / msGridSize;
                  sampleRays[sampleIdx].pixelY = pixelRect.y + pixelY + (sampleY) / msGridSize + rng.nextFloat()
                        / msGridSize;
                  ++sampleIdx;
               }
            }
         }
      }
   }

}
