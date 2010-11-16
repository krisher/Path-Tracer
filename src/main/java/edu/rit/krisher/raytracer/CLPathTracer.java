/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
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

import edu.rit.krisher.raytracer.rays.SampleRay;

/**
 *
 */
public class CLPathTracer {

   private static final Logger log = Logger.getLogger("CL Path Tracer");

   public CLPathTracer() {

   }

   private static final class Intersector {
      private static final int LOCAL_WORK_SIZE = 256;

      private final CLContext context;
      private final CLProgram program;
      private final CLKernel isectKernel;
      private final CLCommandQueue commandQueue;
      private final CLBuffer<FloatBuffer> vertBuff;
      private final CLBuffer<IntBuffer> indexBuff;
      private final int triCount;
      private final boolean littleEnd;

      public Intersector(final float[] verts, final int[] triIndices) {
         context = CLContext.create(Type.GPU);
         context.addCLErrorHandler(new CLErrorHandler() {

            @Override
            public void onError(final String arg0, final ByteBuffer arg1, final long arg2) {
               System.out.println("Error: " + arg0);
            }
         });
         final CLDevice device = context.getMaxFlopsDevice();
         commandQueue = device.createCommandQueue(Mode.OUT_OF_ORDER_MODE);
         littleEnd = device.isLittleEndian();
         try {
            program = context.createProgram(CLPathTracer.class.getResourceAsStream("/edu/rit/krisher/raytracer/cl/clrt.cl")).build();
         } catch (final IOException e) {
            throw new IllegalStateException("Unable to load OpenCL kernel source.", e);
         }
         isectKernel = program.createCLKernel("find_intersections");
         System.out.println(program.getBuildStatus(device));
         System.out.println(program.getBuildLog());

         System.out.println("Kernel local mem size: " + isectKernel.getLocalMemorySize(device));
         triCount = triIndices.length / 3;

         vertBuff = context.createFloatBuffer(verts.length, Mem.READ_ONLY);
         vertBuff.getBuffer().put(verts);
         vertBuff.getBuffer().flip();
         commandQueue.putWriteBuffer(vertBuff, true);

         final IntBuffer idxB = java.nio.ByteBuffer.allocateDirect(triIndices.length * 4).order(littleEnd ? ByteOrder.LITTLE_ENDIAN
               : ByteOrder.BIG_ENDIAN).asIntBuffer();

         indexBuff = context.createBuffer(idxB, Mem.READ_ONLY);
         indexBuff.getBuffer().put(triIndices);
         indexBuff.getBuffer().flip();
         commandQueue.putWriteBuffer(indexBuff, true);

         isectKernel.setArg(3, vertBuff);
         isectKernel.setArg(4, indexBuff);
         isectKernel.setArg(5, triCount);
         log.info("Transferred Scene Geometry to CL device.");
      }

      public void processHits(final SampleRay[] rays, final int count) {
         final CLBuffer<ByteBuffer> hitBuffer = context.createByteBuffer(4 * count * 4, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
         System.out.println("Buffer capacity: " + hitBuffer.getCLCapacity());
         System.out.println("Buffer ID: " + hitBuffer.getID());
         final CLBuffer<FloatBuffer> rayBuffer = context.createFloatBuffer(8 * count, Mem.READ_ONLY);
         final FloatBuffer rayBuff = rayBuffer.getBuffer();
         for (int i = 0; i < count; ++i) {
            final SampleRay ray = rays[i];
            rayBuff.put((float) ray.origin.x);
            rayBuff.put((float) ray.origin.y);
            rayBuff.put((float) ray.origin.z);
            rayBuff.put(0);
            rayBuff.put((float) ray.direction.x);
            rayBuff.put((float) ray.direction.y);
            rayBuff.put((float) ray.direction.z);
            rayBuff.put(Float.POSITIVE_INFINITY);
         }
         rayBuff.flip();
         commandQueue.putWriteBuffer(rayBuffer, true);

         final CLEventList eList = new CLEventList(1);
         isectKernel.setArg(1, rayBuffer).setArg(2, count).setArg(0, hitBuffer);
         commandQueue.put1DRangeKernel(isectKernel, 0, (int) (Math.ceil(count / (float) LOCAL_WORK_SIZE) * LOCAL_WORK_SIZE), LOCAL_WORK_SIZE, eList);
         System.out.println("Complete: " + eList.getEvent(0).isComplete());
         System.out.flush();

         commandQueue.putReadBuffer(hitBuffer, true);

         final ByteBuffer hitB = hitBuffer.getBuffer();
         for (int i = 0; i < 2; ++i) {
            System.out.println("Hit: ");
            System.out.println(hitB.getFloat());
            System.out.println(hitB.getFloat());
            System.out.println(hitB.getFloat());
            System.out.println(hitB.getInt());
         }

         rayBuffer.release();
         hitBuffer.release();
         //
         // for (int i = 0; i < count; ++i) {
         // ray.t = Double.POSITIVE_INFINITY;
         // ray.hitGeometry = null;
         // for (final Geometry geom : geometry) {
         // geom.intersects(ray);
         // }
         // if (ray.hitGeometry != null) {
         // ray.hitGeometry.getHitData(ray, ray.intersection);
         // }
         // }
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
      final Intersector intersector = new Intersector(new float[] { -0.5f, 0f, 0f, 0.5f, 0f, 0f, 0f, 1f, 0f }, new int[] {
            0, 1, 2 });
      intersector.processHits(new SampleRay[] { new SampleRay(1), new SampleRay(1) }, 2);
   }
}
