package edu.rit.krisher.raytracer.sampling;

import java.util.Random;

/**
 * Non-threadsafe sampling pattern generator for a 2D rectangular area.
 * 
 * @author krisher
 * 
 */
public final class JitteredStratifiedRectangleSampling {

   /**
    * Samples for the x component of the grid, with values between 0 and 1.
    */
   public double[] xSamples;
   /**
    * Samples for the y component of the grid, with values between 0 and 1.
    */
   public double[] ySamples;
   /**
    * The number of samples in each dimension. The total number of sample pairs is this value squared.
    */
   private int samplesPerDim;

   /**
    * Creates a new sample-point generator.
    * 
    * @param samplesPerDim
    *           The number of samples per dimension. The total number os sample pairs is this value squared.
    */
   public JitteredStratifiedRectangleSampling(final int samplesPerDim) {
      this.samplesPerDim = samplesPerDim;
      this.xSamples = new double[samplesPerDim * samplesPerDim];
      this.ySamples = new double[samplesPerDim * samplesPerDim];
   }

   public void resize(final int samplesPerDim) {
      if (samplesPerDim != this.samplesPerDim) {
         this.samplesPerDim = samplesPerDim;
         this.xSamples = new double[samplesPerDim * samplesPerDim];
         this.ySamples = new double[samplesPerDim * samplesPerDim];
      }
   }

   /**
    * Populates the sample arrays with new samples.
    * 
    * @param rng
    *           A non-null random number generator to generate jittered sample locations with.
    */
   public void generateSamples(final Random rng) {
      int sampleIdx = 0;
      for (int sampleX = 0; sampleX < samplesPerDim; ++sampleX) {
         for (int sampleY = 0; sampleY < samplesPerDim; ++sampleY) {
            /*
             * Stratified jittered sampling, an eye ray is generated that passes through a random location in a small
             * square region of the pixel area for each sample.
             */
            xSamples[sampleIdx] = (sampleX + rng.nextDouble()) / samplesPerDim;
            ySamples[sampleIdx] = (sampleY + rng.nextDouble()) / samplesPerDim;
            ++sampleIdx;
         }
      }
   }
}
