/**
 * 
 */
package edu.rit.krisher.raytracer;

import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.Scene;

/**
 * Interface for classes that estimate the lighting integral for a given scene and imaging configuration.
 */
public interface SceneIntegrator {

    /**
    * Asynchronously ray traces the specified scene given the camera position
    * and ImageBuffer to store the results in.
    * 
    * @param image
    *           A non-null ImageBuffer. The dimensions of the ray-traced image
    *           are determined from the {@link ImageBuffer#getResolution()} method synchronously with this call.
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
   public void integrate(final ImageBuffer image, final Scene scene, final int pixelSampleRate,
         final int recursionDepth);

   
   /**
    * Cancels rendering for the specified ImageBuffer (that was previously
    * passed to {@link #integrate(ImageBuffer, Camera, Scene, int, int)}).
    * 
    * <p>
    * Any non-started work items are removed from the work queue, but work items already being processed are allowed to
    * finish. Pixel data may still be sent to the specified ImageBuffer until its {@link ImageBuffer#imagingDone()}
    * method is called.
    * 
    * @param target
    */
   public void cancel(final ImageBuffer target);
}