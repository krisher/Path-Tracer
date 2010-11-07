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
   public float[] xSamples;
   /**
    * Samples for the y component of the grid, with values between 0 and 1.
    */
   public float[] ySamples;
   /**
    * The number of samples in each dimension. The total number of sample pairs is this value squared.
    */
   private int xSampleCount;
   private int ySampleCount;

   /**
    * Creates a new sample-point generator.
    * 
    * @param samplesPerDim
    *           The number of samples per dimension. The total number os sample pairs is this value squared.
    */
   public JitteredStratifiedRectangleSampling(final int xSampleCount, final int ySampleCount) {
      this.xSampleCount = xSampleCount;
      this.ySampleCount = ySampleCount;
      this.xSamples = new float[xSampleCount * ySampleCount];
      this.ySamples = new float[xSampleCount * ySampleCount];
   }

   public void resize(final int xSampleCount, final int ySampleCount) {
      if (xSampleCount != this.xSampleCount || ySampleCount != this.ySampleCount) {
         this.xSampleCount = xSampleCount;
         this.ySampleCount = ySampleCount;
         this.xSamples = new float[xSampleCount * ySampleCount];
         this.ySamples = new float[xSampleCount * ySampleCount];
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
      for (int sampleX = 0; sampleX < xSampleCount; ++sampleX) {
         for (int sampleY = 0; sampleY < ySampleCount; ++sampleY) {
            /*
             * Stratified jittered sampling, an eye ray is generated that passes through a random location in a small
             * square region of the pixel area for each sample.
             */
            xSamples[sampleIdx] = (sampleX) / xSampleCount + rng.nextFloat() / xSampleCount;
            assert xSamples[sampleIdx] < 1.0f;
            ySamples[sampleIdx] = (sampleY) / ySampleCount + rng.nextFloat() / ySampleCount;
            assert ySamples[sampleIdx] < 1.0f;
            ++sampleIdx;
         }
      }
   }
}
