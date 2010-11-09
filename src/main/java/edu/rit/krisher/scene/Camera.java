package edu.rit.krisher.scene;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;

/**
 * Interface for a camera model. This class is responsible for generating eye rays in terms of an origin and direction.
 * 
 * @author krisher
 * 
 */
public interface Camera {

   /**
    * Initializes the specified Rays' directions and origins.
    * 
    * @param rayOut
    *           A list of non-null Rays to initialize. The pixelX and pixelY fields of the SampleRays indicate the
    *           image-plane location to initialize the ray for. The values of all other fields are undefined. Camera
    *           implementations should populate the ray direction and origin fields for each ray.
    * @param imageWidth
    *           The width (in pixels) of the image plane.
    * @param imageHeight
    *           The height (in pixels) of the image plane
    * @param rng
    *           A non-null random number generator that can be used for sampling of the camera aperture.
    */
   public void sample(SampleRay[] rayOut, int imageWidth, int imageHeight, int pixelOffsetX, int pixelOffsetY,
         Random rng);

}