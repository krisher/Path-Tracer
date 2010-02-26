package edu.rit.krisher.scene;

import java.util.Random;

import edu.rit.krisher.vecmath.Ray;

/**
 * Interface for a camera model. This class is responsible for generating eye
 * rays in terms of an origin and direction.
 * 
 * @author krisher
 * 
 */
public interface Camera {

   /**
    * Initializes the specified Ray's direction and origin
    * 
    * @param rayOut
    *           A non-null Ray to initialize.
    * @param x
    *           A normalized [-1, 1] value describing the horizontal sample
    *           location on the image plane.
    * @param y
    *           A normalized [-1, 1] value describing the vertical sample
    *           location on the image plane.
    * @param rng
    *           A non-null random number generator that can be used for sampling
    *           of the camera aperture.
    */
   public void initializeRay(Ray rayOut, double x, double y, Random rng);

}