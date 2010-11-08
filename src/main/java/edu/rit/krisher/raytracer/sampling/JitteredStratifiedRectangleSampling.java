package edu.rit.krisher.raytracer.sampling;

import java.util.Random;


/**
 * Thread-safe sampling pattern generator for a 2D rectangular area.
 * 
 * @author krisher
 * 
 */
public final class JitteredStratifiedRectangleSampling {

   /**
    * Creates a new sample-point generator.
    * 
    */
   public JitteredStratifiedRectangleSampling() {
   }

   /**
    * Populates the sample arrays with new samples.
    * 
    * @param sampleValues
    *           A non-null array with size at least xGridSize * yGridSize * 2 to store the results in. Upon exit, the
    *           array
    *           will contain pairs of x,y sample offsets in the range [0,1) where each sample location is randomly
    *           chosen from each cell in a regular grid (the unit square divided into xGridSize x yGridSize cells).
    * @param rng
    *           A non-null random number generator to generate jittered sample locations with.
    */
   public void generateSamples(final float[] sampleValues, final int xGridSize, final int yGridSize, final Random rng) {
      int sampleIdx = 0;
      for (int sampleX = 0; sampleX < xGridSize; ++sampleX) {
         for (int sampleY = 0; sampleY < yGridSize; ++sampleY) {
            /*
             * Stratified jittered sampling, an eye ray is generated that passes through a random location in a small
             * square region of the pixel area for each sample.
             */
            sampleValues[sampleIdx] = (sampleX) / xGridSize + rng.nextFloat() / xGridSize;
            assert sampleValues[sampleIdx] < 1.0f;
            sampleValues[sampleIdx + 1] = (sampleY) / yGridSize + rng.nextFloat() / yGridSize;
            assert sampleValues[sampleIdx + 1] < 1.0f;
            sampleIdx += 2;
         }
      }
   }
}
