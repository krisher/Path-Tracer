/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLErrorHandler;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;

import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.raytracer.image.ImageUtil;
import edu.rit.krisher.raytracer.rays.GeometryRay;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.SamplingUtils;
import edu.rit.krisher.raytracer.sampling.UnsafePRNG;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.util.Timer;
import edu.rit.krisher.vecmath.Constants;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class CLPathTracer implements SurfaceIntegrator {
   private static final int ILLUMINATION_SAMPLES = 4;
   private static final Logger log = Logger.getLogger("CL Path Tracer");
   private static final Map<ImageBuffer, AtomicInteger> active = new ConcurrentHashMap<ImageBuffer, AtomicInteger>();
   private static final String FIND_INTERSECTIONS_KERNEL = "find_intersections";
   private static final String TEST_INTERSECTIONS_KERNEL = "test_intersections";

   private final Timer timer = new Timer("CL Path Trace (Thread Timing)");

   private final CLContext context;
   private final CLDevice device;
   private final CLProgram program;
   private final boolean littleEnd;

   public CLPathTracer() {
      printPlatformInfo();
      final boolean cpu = "cpu".equalsIgnoreCase(System.getProperty("opencl.device"));
      log.info("Using " + (cpu ? "CPU" : "GPU") + " for kernel execution (opencl.device == "
               + System.getProperty("opencl.device") + ")");
      context = CLContext.create(cpu ? Type.CPU : Type.GPU);

      // context = CLContext.create(CLPlatform.listCLPlatforms()[0], Type.GPU);

      context.addCLErrorHandler(new CLErrorHandler() {

         @Override
         public void onError(final String arg0, final ByteBuffer arg1, final long arg2) {
            System.out.println("Error: " + arg0);
         }
      });
      device = context.getMaxFlopsDevice();
      littleEnd = device.isLittleEndian();
      try {
         program = context.createProgram(CLPathTracer.class.getResourceAsStream("/edu/rit/krisher/raytracer/cl/clrt.cl")).build();
      } catch (final IOException e) {
         throw new IllegalStateException("Unable to load OpenCL kernel source.", e);
      }

   }

   private class Intersector {
      private final int localWorkSize;
      private final CLKernel isectKernel;
      private final CLKernel itestKernel;
      private final CLCommandQueue commandQueue;
      private final CLBuffer<FloatBuffer> vertBuff;
      private final CLBuffer<IntBuffer> indexBuff;
      private final int triCount;

      public Intersector(final float[] verts, final int[] indices) {

         log.info("Creating CL ray intersector with " + (verts.length / 3) + " vertices, " + (indices.length / 3)
                  + " triangles.");

         commandQueue = device.createCommandQueue(Mode.OUT_OF_ORDER_MODE);
         isectKernel = program.createCLKernel(FIND_INTERSECTIONS_KERNEL);
         itestKernel = program.createCLKernel(TEST_INTERSECTIONS_KERNEL);
         log.info("Program compilation status: " + program.getBuildStatus(device));
         log.info("Compilation info-log:\n" + program.getBuildLog());

         localWorkSize = (int) isectKernel.getWorkGroupSize(device);
         log.fine(FIND_INTERSECTIONS_KERNEL + " kernel local mem size: " + isectKernel.getLocalMemorySize(device));
         log.fine(FIND_INTERSECTIONS_KERNEL + " kernel local workgroup size: " + isectKernel.getWorkGroupSize(device));
         triCount = indices.length / 3;

         vertBuff = context.createFloatBuffer(verts.length, Mem.READ_ONLY);
         vertBuff.getBuffer().put(verts);
         vertBuff.getBuffer().flip();
         commandQueue.putWriteBuffer(vertBuff, true);

         final IntBuffer idxB = java.nio.ByteBuffer.allocateDirect(indices.length * 4).order(littleEnd ? ByteOrder.LITTLE_ENDIAN
               : ByteOrder.BIG_ENDIAN).asIntBuffer();

         indexBuff = context.createBuffer(idxB, Mem.READ_ONLY);
         indexBuff.getBuffer().put(indices);
         indexBuff.getBuffer().flip();
         commandQueue.putWriteBuffer(indexBuff, true);


         isectKernel.setArg(3, vertBuff);
         isectKernel.setArg(4, indexBuff);
         isectKernel.setArg(5, triCount);

         itestKernel.setArg(3, vertBuff);
         itestKernel.setArg(4, indexBuff);
         itestKernel.setArg(5, triCount);
         log.info("Transferred Scene Geometry to CL device.");
      }

      public synchronized void processHits(final GeometryRay[] rays, final int count) {
         final CLBuffer<ByteBuffer> hitBuffer = context.createByteBuffer(count * 4 * 4, Mem.WRITE_ONLY);
         final CLBuffer<FloatBuffer> rayBuffer = context.createFloatBuffer(8 * count, Mem.READ_ONLY);

         final FloatBuffer rayBuff = rayBuffer.getBuffer();
         for (int i = 0; i < count; ++i) {
            final GeometryRay ray = rays[i];
            rayBuff.put((float) ray.origin.x);
            rayBuff.put((float) ray.origin.y);
            rayBuff.put((float) ray.origin.z);
            rayBuff.put((float) ray.direction.x);
            rayBuff.put((float) ray.direction.y);
            rayBuff.put((float) ray.direction.z);
            rayBuff.put(0);
            rayBuff.put(Float.POSITIVE_INFINITY);
         }
         rayBuff.flip();
         commandQueue.putWriteBuffer(rayBuffer, false);

         final CLEventList eList = new CLEventList(1);
         isectKernel.setArg(0, hitBuffer).setArg(1, rayBuffer).setArg(2, count);
         final int workItems = (int) (Math.ceil(count / (float) localWorkSize) * localWorkSize);
         commandQueue.put1DRangeKernel(isectKernel, 0, workItems, localWorkSize, eList);
         // System.out.println(eList.getEvent(0).isComplete());
         commandQueue.putReadBuffer(hitBuffer, true);

         final ByteBuffer hitB = hitBuffer.getBuffer();
         for (int i = 0; i < count; ++i) {
            rays[i].t = hitB.getFloat();
            final double u = hitB.getFloat();
            final double v = hitB.getFloat();

            rays[i].primitiveID = hitB.getInt();
            if (rays[i].primitiveID >= triCount) {
               rays[i].primitiveID = -1;
            }
         }

         rayBuffer.release();
         hitBuffer.release();
      }
   }

   private Intersector createGeometryIntersector(final TriangleMesh... geometry) {
      // this.geometry = geometry;

      final int[] geometryPrimOffset = new int[geometry.length]; // Map from geometry index to the starting primitive
      // index for that geometry.

      int vertComponentTotal = 0; // Total number of vertex components.
      int indicesTotal = 0; // Total number of vertex indices (3 per triangle)
      for (int i = 0; i < geometry.length; ++i) {
         final TriangleMesh mesh = geometry[i];
         geometryPrimOffset[i] = indicesTotal / 3;
         vertComponentTotal += mesh.getVertices().length;
         indicesTotal += mesh.getTriIndices().length;
      }
      final float[] verts = new float[vertComponentTotal];
      final int[] indices = new int[indicesTotal];
      final int[] primToGeometry = new int[indicesTotal / 3]; // Map of triangle ID to the geometry instance that it
      // came from.
      vertComponentTotal = 0;
      indicesTotal = 0;
      for (int i = 0; i < geometry.length; ++i) {
         final TriangleMesh mesh = geometry[i];
         final float[] meshVerts = mesh.getVertices();
         System.arraycopy(meshVerts, 0, verts, vertComponentTotal, meshVerts.length);
         final int[] meshIndices = mesh.getTriIndices();
         Arrays.fill(primToGeometry, indicesTotal / 3, indicesTotal / 3 + meshIndices.length / 3, i);
         for (int j = 0; j < meshIndices.length; ++j) { // Offset the vertex indices by the running total of vertices
            // from other geometry.
            indices[j + indicesTotal] = meshIndices[j] + vertComponentTotal / 3;
         }
         indicesTotal += meshIndices.length;
         vertComponentTotal += meshVerts.length;

      }

      return new Intersector(verts, indices) {
         @Override
         public synchronized void processHits(final GeometryRay[] rays, final int count) {
            super.processHits(rays, count);
            for (int i = 0; i < count; ++i) {
               final GeometryRay ray = rays[i];
               /*
                * Map primitive ID back to real primID and geometry instance
                */
               if (ray.primitiveID >= 0) {
                  ray.hitGeometry = geometry[primToGeometry[ray.primitiveID]];
                  ray.primitiveID -= geometryPrimOffset[primToGeometry[ray.primitiveID]];
               } else {
                  ray.hitGeometry = null;
               }
            }
         }
      };
   }

   /**
    * Asynchronously ray traces the specified scene given the camera position and ImageBuffer to store the results in.
    * 
    * @param image
    *           A non-null ImageBuffer. The dimensions of the ray-traced image are determined from the
    *           {@link ImageBuffer#getResolution()} method synchronously with this call.
    * @param scene
    *           The non-null scene to render.
    * @param pixelSampleRate
    *           The linear super-sampling rate. This value squared is the actual number of paths traced for each image
    *           pixel. Must be greater than 0.
    * @param recursionDepth
    *           The maximum length of a ray path. 0 means trace eye rays and direct illumination only.
    */
   @Override
   public void integrate(final ImageBuffer image, final Scene scene, final int pixelSampleRate, final int recursionDepth) {

      /*
       * Imaging parameters
       */
      final Dimension imageSize = image.getResolution();
      /*
       * Estimate for the upper bound of the number of rays that will be generated, including one ray for each ray path
       * component, and shadow rays. The actual number of rays traced will likely be lower than this, some rays will be
       * absorbed before tracing to the maximum path length, some rays might not hit anything (for open scenes), etc.
       */
      final double rayCount = ((double) imageSize.width * imageSize.height * pixelSampleRate * pixelSampleRate
            * recursionDepth * (1.0 + ILLUMINATION_SAMPLES));
      System.out.println("Max Samples: " + IntegratorUtils.formatter.format(rayCount));
      /*
       * WorkItems may begin processing as soon as they are queued, so notify the ImageBuffer that pixels are on their
       * way...
       */
      timer.reset();
      image.imagingStarted();

      /*
       * Tiled work distribution...
       */
      final Rectangle[] imageChunks = IntegratorUtils.chunkRectangle(imageSize.width, imageSize.height, Math.max(2, 64 / pixelSampleRate));
      /*
       * Thread-safe spin-lock based countdown latch to monitor progress for this image. When this reaches 0, the
       * ImageBuffer is notified that the rendering is complete.
       */
      final AtomicInteger doneSignal = new AtomicInteger(imageChunks.length);
      final ConcurrentLinkedQueue<Rectangle> blocks = new ConcurrentLinkedQueue<Rectangle>();
      for (final Rectangle chunk : imageChunks) {
         blocks.add(chunk);
      }
      active.put(image, doneSignal);
      final Intersector intersector = createIntersector(scene);

      for (int i = 0; i < IntegratorUtils.threads; i++)
         IntegratorUtils.threadPool.submit(new CLPathIntegrator(scene, image, blocks, pixelSampleRate, recursionDepth, doneSignal, intersector));
   }

   private final Intersector createIntersector(final Scene scene) {
      final ArrayList<TriangleMesh> meshes = new ArrayList<TriangleMesh>();
      for (final Geometry geom : scene.getGeometry()) {
         // TODO: pass all geometry down.
         if (geom instanceof TriangleMesh) {
            meshes.add((TriangleMesh) geom);
         }
      }
      System.out.println("Passing " + meshes.size() + " meshes to OpenCL Ray Intersector.");
      return createGeometryIntersector(meshes.toArray(new TriangleMesh[meshes.size()]));
   }

   static class CLPathIntegrator implements Runnable {

      // private static final double gaussFalloffControl = 4.0;
      // private static final double gaussFalloffConstant = Math.exp(-gaussFalloffControl * 0.5 * 0.5);
      /**
       * Overridden to remove thread safety overhead
       */
      private final Random rng = new UnsafePRNG();

      private final int pixelSampleRate;
      private final int recursionDepth;
      private final ImageBuffer imageBuffer;
      private final Scene scene;
      private final Queue<Rectangle> workQueue;
      private final AtomicInteger doneSignal;
      private final IntegratorUtils.DirectIlluminationSampler illumSampler;
      private final Intersector intersector;
      /*
       * Buffer to collect rgb pixel data
       * 
       * Values will always be >= 0, but are unbounded in magnitude.
       */
      private float[] pixels;
      // private float[] pixelNormalization;
      private Rectangle rect;

      public CLPathIntegrator(final Scene scene, final ImageBuffer image, final Queue<Rectangle> workQueue,
            final int pixelSampleRate, final int recursionDepth, final AtomicInteger doneSignal,
            final Intersector intersector) {
         this.recursionDepth = recursionDepth;
         this.imageBuffer = image;
         this.scene = scene;
         this.doneSignal = doneSignal;
         this.workQueue = workQueue;
         this.pixelSampleRate = pixelSampleRate;
         illumSampler = new IntegratorUtils.DirectIlluminationSampler(rng, scene.getLightSources(), scene.getGeometry());
         this.intersector = intersector;
      }

      /*
       * @see edu.rit.krisher.raytracer.RayIntegrator#integrate(edu.rit.krisher.raytracer.WorkItem)
       */
      @Override
      public void run() {
         final Dimension imageSize = imageBuffer.getResolution();
         SampleRay[] rays = new SampleRay[0];
         while ((rect = workQueue.poll()) != null) {
            try {
               final int pixelCount = rect.width * rect.height * 3;
               if (pixels == null || pixels.length < pixelCount) {
                  pixels = new float[pixelCount];
                  // pixelNormalization = new float[pixelCount / 3];
               } else {
                  Arrays.fill(pixels, 0);
                  // Arrays.fill(pixelNormalization, 0);
               }

               final int rayCount = pixelSampleRate * pixelSampleRate * rect.width * rect.height;
               if (rays.length < rayCount) {
                  rays = new SampleRay[rayCount];
                  for (int rayIdx = 0; rayIdx < rayCount; ++rayIdx) {
                     rays[rayIdx] = new SampleRay(1);
                  }
               } else {
                  for (int i = 0; i < rayCount; ++i) {
                     rays[i].throughput.set(1);
                     rays[i].specularBounce = true;
                     rays[i].extinction.clear();
                  }
               }

               /* Generate Eye Rays */
               SamplingUtils.generatePixelSamples(rays, new Rectangle(0, 0, rect.width, rect.height), pixelSampleRate, rng);
               scene.getCamera().sample(rays, imageSize.width, imageSize.height, rect.x, rect.y, rng);

               /*
                * Compute filter normalization constants for each pixel.
                * 
                * TODO: Gaussian filter really needs multi-pixel support, and doesn't work well without it.
                * 
                * TODO: This should be implemented in the Image Buffer (it should generate a sequence of multi-sample
                * buffer chunks (with pixel sample locations) that are processed in the tracing threads)
                */
               // for (final SampleRay ray : rays) {
               // final int dst = (((int) ray.pixelY) * rect.width + (int) ray.pixelX);
               // final double x = ray.pixelX - (int) ray.pixelX - 0.5;
               // final double y = ray.pixelY - (int) ray.pixelY - 0.5;
               // final double filter = Math.max(0, Math.exp(-gaussFalloffControl * x * x) - gaussFalloffConstant)
               // * Math.max(0, Math.exp(-gaussFalloffControl * y * y) - gaussFalloffConstant);
               // ray.throughput.set(filter);
               // pixelNormalization[dst] += filter;
               // }



               /* Trace Rays */
               integrateIrradiance(rays, rays.length);

               /* Put results back into image buffer */
               final float pixelNormalization = 1.0f / (pixelSampleRate * pixelSampleRate);
               for (int i = 0; i < pixels.length; ++i) {
                  pixels[i] *= pixelNormalization;
               }
               imageBuffer.setPixels(rect.x, rect.y, rect.width, rect.height, pixels);
            } catch (final Throwable e) {
               e.printStackTrace();
            } finally {
               final int remaining = doneSignal.decrementAndGet();
               if (remaining == 0) {
                  imageBuffer.imagingDone();
                  active.remove(imageBuffer);
                  return;
               } else if (remaining < 0) {
                  return; // Process was canceled.
               }
            }
         }
      }

      private final void updateImage(final int x, final int y, final double r, final double g, final double b) {
         final int dst = 3 * (y * rect.width + x);
         pixels[dst] += r;
         pixels[dst + 1] += g;
         pixels[dst + 2] += b;
      }

      private final void integrateIrradiance(final SampleRay[] rays, int rayCount) {
         final Color directIllumContribution = new Color(0, 0, 0);
         final Geometry[] geometry = scene.getGeometry();
         final Color bg = scene.getBackground();

         /*
          * All active rays are at the same depth into the path (# of bounces from the initial eye ray). Process until
          * we reach the maximum depth, or all rays have terminated.
          */
         for (int rayDepth = 0; rayDepth <= recursionDepth && rayCount > 0; rayDepth++) {
            /* Visibility pass */
            intersector.processHits(rays, rayCount);
            for (int i = 0; i < rayCount; ++i) {
               final SampleRay ray = rays[i];
               if (ray.hitGeometry != null) {
                  ray.hitGeometry.getHitData(ray, ray.intersection);
               }
            }
            /*
             * Number of rays that will be processed in the next iteration
             */
            int outRayCount = 0;
            for (int processRayIdx = 0; processRayIdx < rayCount; processRayIdx++) {
               final SampleRay ray = rays[processRayIdx];

               if (ray.hitGeometry == null) {
                  /*
                   * No intersection, process for the scene background color.
                   */
                  updateImage((int) ray.pixelX, (int) ray.pixelY, bg.r * ray.throughput.r, bg.g * ray.throughput.g, bg.b
                              * ray.throughput.b);
                  /*
                   * This path is terminated.
                   */
                  continue;
               }

               /*
                * Diffuse surfaces with a wide distribution of reflectivity are relatively unlikely to bounce to a small
                * emissive object, which introduces significant variance without an extremely large number of samples.
                * Diffuse surfaces will be tested for direct illumination explicitly below, so we ignore the emissive
                * component of an object reflecting off a diffuse surface.
                * 
                * For more information, see:
                * 
                * ﻿Shirley, Peter, Changyaw Wang, and Kurt Zimmerman. 1996. Monte Carlo techniques for direct lighting
                * calculations. ACM Transactions on Graphics 15, no. 1: 1-36.
                * 
                * 
                * or
                * 
                * P. Shirley, R. Morley, Realistic Ray Tracing, 2nd Ed. 2003. AK Peters.
                */
               if (ray.specularBounce) {
                  ray.intersection.material.getEmissionColor(directIllumContribution, ray, ray.intersection);
               } else
                  directIllumContribution.clear();

               /*
                * Save the transmission weights, they may be overwritten if the ray is reused for the next path segment
                * below.
                */
               final double throughputR = ray.throughput.r
               * (ray.extinction.r == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.r) * ray.t));
               final double throughputG = ray.throughput.g
               * (ray.extinction.g == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.g) * ray.t));
               final double throughputB = ray.throughput.b
               * (ray.extinction.b == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.b) * ray.t));

               /*
                * Specular and refractive materials do not benefit from direct illuminant sampling, since their
                * relfection/refraction distributions are typically constrained to a small solid angle, they only
                * respond to light coming from directions that will be sampled via bounce rays.
                */
               if (ray.intersection.material.isDiffuse()) {
                  final Vec3 illumRayOrigin = ray.getPointOnRay(ray.t).scaleAdd(ray.intersection.surfaceNormal, Constants.EPSILON_F);
                  illumSampler.sampleDirectIllumination(illumRayOrigin, ray.intersection, ray.direction.inverted(), directIllumContribution, ILLUMINATION_SAMPLES);
               }

               /*
                * Add the contribution to the pixel, modulated by the transmission across all previous bounces in this
                * path.
                */
               updateImage((int) ray.pixelX, (int) ray.pixelY, throughputR * directIllumContribution.r, throughputG
                           * directIllumContribution.g, throughputB * directIllumContribution.b);

               /*
                * If we have not reached the maximum recursion depth, generate a new reflection/refraction ray for the
                * next path segment.
                */
               if (rayDepth < recursionDepth
                     && (rayDepth < 2 || rng.nextFloat() >= Math.min(1.0 / (recursionDepth + 1), 1.0 - ImageUtil.luminance((float) throughputR, (float) throughputG, (float) throughputB)))) {
                  final SampleRay irradSampleRay = rays[outRayCount];
                  /*
                   * Preserve the current extinction, this is only modified when the ray passes through a refractive
                   * interface, at which point the extinction is changed in the Material model.
                   */
                  irradSampleRay.extinction.set(ray.extinction);
                  irradSampleRay.origin.set(ray.getPointOnRay(ray.t));

                  irradSampleRay.reset();
                  final double pdf = ray.intersection.material.sampleBRDF(irradSampleRay, ray.direction.inverted(), ray.intersection, rng);
                  if (pdf > 0 && !irradSampleRay.throughput.isZero()) {
                     // Scale transmission by inverse probability of reaching this depth due to RR.
                     if (rayDepth >= 2)
                        irradSampleRay.throughput.multiply(1 / (1 - Math.min(1.0 / (recursionDepth + 1), 1.0 - ImageUtil.luminance((float) throughputR, (float) throughputG, (float) throughputB))));
                     irradSampleRay.throughput.multiply(throughputR, throughputG, throughputB);
                     irradSampleRay.throughput.multiply(Math.abs(ray.intersection.surfaceNormal.dot(irradSampleRay.direction))
                                                        / pdf);

                     irradSampleRay.pixelX = ray.pixelX;
                     irradSampleRay.pixelY = ray.pixelY;
                     /*
                      * Avoid precision issues when processing the ray for the next intersection.
                      */
                     irradSampleRay.origin.scaleAdd(irradSampleRay.direction, Constants.EPSILON_F);
                     ++outRayCount;

                  }
               }

            }
            rayCount = outRayCount;

         }
      }

   }

   /**
    * Cancels rendering for the specified ImageBuffer (that was previously passed to
    * {@link #integrate(ImageBuffer, Scene, int, int)}).
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
      final AtomicInteger remaining = active.get(target);
      if (remaining != null) {
         final int prevRemaining = remaining.getAndSet(0);
         if (prevRemaining != 0) {
            active.remove(target);
            target.imagingDone();
         }
      }
   }

   public static void printPlatformInfo() {
      final CLPlatform[] platforms = CLPlatform.listCLPlatforms();
      for (final CLPlatform platform : platforms) {
         final StringBuilder builder = new StringBuilder();
         builder.append('\n');
         builder.append("Platform: " + platform.toString()).append('\n');
         builder.append("=============================").append('\n');
         for (final CLDevice device : platform.listCLDevices()) {
            builder.append("Device: " + device).append('\n');
            builder.append("\tFreq: " + device.getMaxClockFrequency()).append('\n');
            builder.append("\tComput Units: " + device.getMaxComputeUnits()).append('\n');
            builder.append("\tGlobal Mem: " + device.getGlobalMemSize()).append('\n');
            builder.append("\tLocal Mem: " + device.getLocalMemSize()).append('\n');

            builder.append("\tMax Workgroup Size: " + device.getMaxWorkGroupSize()).append('\n');
            builder.append("\tMax Workitem Size: " + Arrays.toString(device.getMaxWorkItemSizes())).append('\n');

         }
         builder.append('\n');
         builder.append("Max FLOPS Device: " + platform.getMaxFlopsDevice()).append('\n');
         builder.append("-----------------------------").append('\n');
         log.info(builder.toString());
      }
   }

   public static void main(final String[] args) {
      printPlatformInfo();
   }
}
