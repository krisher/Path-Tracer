package edu.rit.krisher.raytracer;

import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Utility methods for ray tracing.
 * 
 * @author krisher
 * 
 */
public final class IntegratorUtils  {
   public static final int DEFAULT_PIXEL_BLOCK_SIZE = 8;
   public static final int threads = Runtime.getRuntime().availableProcessors();
   public static final NumberFormat formatter = NumberFormat.getNumberInstance();
   public static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

   private IntegratorUtils() {
      /*
       * Prevent construction.
       */
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
         final int blockStartY = j * blockSize;
         for (int i = 0; i < xBlocks; i++) {
            final int blockStartX = i * blockSize;
            result[j * xBlocks + i] = new Rectangle(blockStartX, blockStartY, Math.min(blockSize, width - blockStartX), Math.min(blockSize, height
                                                                                                                                 - blockStartY));
         }
      }
      return result;
   }

   /**
    * Processes intersections of each of the 'count' rays with the specified scene geometry. Upon completion,
    * the {@link IntersectionInfo} for each ray will be updated indicating the geometry that was hit (or null if nothing
    * was hit),
    * the primitiveID of the hit geometry, and the parametric hit location.
    * 
    * @param rays
    *           A non-null array of at least 'count' non-null SampleRays. The origin and direction of the rays must be
    *           initialized prior to this call.
    * @param count
    *           The first 'count' rays in the rays array are processed for intersection.
    * @param geometry
    *           The non-null list of geometry to test for intersection.
    */
   public static void processIntersections(final SampleRay[] rays, final int count, final Geometry[] geometry) {
      for (int i = 0; i < count; ++i) {
         final SampleRay ray = rays[i];
         ray.intersection.t = Double.POSITIVE_INFINITY;
         ray.intersection.hitGeometry = null;
         for (final Geometry geom : geometry) {
            geom.intersects(ray, ray.intersection);
         }
      }
   }

   /**
    * Initializes the pixelX and pixelY values of the specified SampleRays based on the specified pixel rectangle,
    * multi-sampling rate, and random number generator for jittering the sample locations.
    * 
    * @param sampleRays
    *           An array of pixelRect.width * pixelRect.height * msGridSize * msGridSize sample rays.
    * @param pixelRect
    *           The pixels for which to initialize rays.
    * @param msGridSize
    *           The multi-sample rate for each pixel. msGridSize * msGridSize rays will be initialized for each pixel
    *           (with different locations within the pixel.
    * @param rng
    *           A random number generator.
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

   /**
    * Initializes the specified vector to a randomly selected direction with equal probability for any direction.
    * 
    * @param vec
    *           A non-null vector to initialize.
    * @param rng
    *           A random number generator.
    */
   public static final void rejectionSphereSample(final Vec3 vec, final Random rng) {
      do {
         vec.x = rng.nextFloat();
         vec.y = rng.nextFloat();
         vec.z = rng.nextFloat();
      } while (vec.lengthSquared() > 1);
      vec.normalize();
   }

}
