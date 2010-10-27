/**
 * 
 */
package edu.rit.krisher.scene.material;

import edu.rit.krisher.util.MathUtils;

/**
 * Spectrum implementation that is modeled as a set of one or more regularly spaced wavelength/power samples.
 */
public class RegularSampledSpectrum implements SpectralDistribution {

   private final float[] samples;
   private final float wlStart;
   private final float wlEnd;

   /**
    * Creates a new spectrum over the specified wavelength range, with the specified regular power samples.
    * 
    * @param wlStart
    *           The shortest wavelength.
    * @param wlEnd
    *           The longest wavelength.
    * @param samples
    *           One sample value for each wavelength, where samples[0] is the power of wavelength wlStart, and
    *           samples[samples.length - 1] is the power of wavelength wlEnd.
    */
   public RegularSampledSpectrum(final float wlStart, final float wlEnd, final float... samples) {
      this.samples = samples;
      this.wlStart = wlStart;
      this.wlEnd = wlEnd;
   }

   /*
    * @see edu.rit.krisher.scene.material.SpectralDistribution#multiply(float[], float, float)
    */
   @Override
   public void multiply(final SpectralDistribution sd) {
      // TODO Auto-generated method stub

   }

   /*
    * @see edu.rit.krisher.scene.material.SpectralDistribution#multiply(float)
    */
   @Override
   public void multiply(final float scale) {
      for (int i = 0; i < samples.length; ++i) {
         samples[i] *= scale;
      }
   }

   /*
    * @see edu.rit.krisher.scene.material.SpectralDistribution#isZero()
    */
   @Override
   public boolean isZero() {
      for (final float sample : samples) {
         if (sample > 0)
            return false;
      }
      return true;
   }

   /*
    * @see edu.rit.krisher.scene.material.SpectralDistribution#getStartWavelength()
    */
   @Override
   public float getStartWavelength() {
      return wlStart;
   }

   /*
    * @see edu.rit.krisher.scene.material.SpectralDistribution#getEndWavelength()
    */
   @Override
   public float getEndWavelength() {
      return wlEnd;
   }

   /*
    * @see edu.rit.krisher.scene.material.SpectralDistribution#getSamples(float[], float, float)
    */
   @Override
   public void getSamples(final float[] samples, final float startWl, final float endWl) {
      // TODO Auto-generated method stub

   }

   /**
    * Resamples the specified spectral distribution that is defined by a set of arbitrary wavelength/power pairs.
    * 
    * @param resampled
    *           Output parameter for a regularly spaced (in wavelength) set of nSamples sample values, with the first
    *           entry corresponding to the shortest wavelength, and the last corresponding to the longest wavelength.
    * @param wavelengths
    *           The wavelengths of the corresponding power value in samples. Must be sorted in increasing wavelength
    *           order.
    * @param samples
    *           The sample values for each wavelength.
    */
   public static void resample(final float[] resampled, final float[] wavelengths, final float[] samples) {
      for (int i = 0; i < resampled.length; ++i) {
         final double intStartWl = MathUtils.lerp(i / (float) resampled.length, wavelengths[0], wavelengths[wavelengths.length - 1]);
         final double intEndWl = MathUtils.lerp(i / (float) resampled.length, wavelengths[0], wavelengths[wavelengths.length - 1]);

         resampled[i] = (float) (MathUtils.integrateLinearFunction(wavelengths, samples, (float) intStartWl, (float) intEndWl) / (intEndWl - intStartWl));
      }
   }

   
}
