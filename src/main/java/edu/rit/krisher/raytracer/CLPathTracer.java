/**
 * 
 */
package edu.rit.krisher.raytracer;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLPlatform;
/**
 *
 */
public class CLPathTracer {

   
   public static void main(final String[] args) {
      final CLPlatform[] platforms = CLPlatform.listCLPlatforms();
      System.out.println("Found " + platforms.length + " CL Platforms:");
      for (final CLPlatform platform : platforms) {
         System.out.println("\t" + platform);
         System.out.println("\tMax FLOPS Device:" + platform.getMaxFlopsDevice());
      }
      
      
      final CLContext context = CLContext.create(Type.GPU);
      System.out.println("Created Context: " + context);
//      context.createFloatBuffer(10, Mem.READ_ONLY).
      
   }
}
