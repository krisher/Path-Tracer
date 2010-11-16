/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.util.Arrays;
import java.util.logging.Logger;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLPlatform;
/**
 *
 */
public class CLPathTracer {

   private static final Logger log = Logger.getLogger("CL Path Tracer");

   public static void main(final String[] args) {
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


      final CLContext context = CLContext.create(Type.GPU);
      System.out.println("Created Context: " + context);

   }

}
