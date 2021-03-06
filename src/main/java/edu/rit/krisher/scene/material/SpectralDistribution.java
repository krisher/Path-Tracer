package edu.rit.krisher.scene.material;

/**
 * Spectral power distribution.
 * 
 * @author krisher
 * 
 */
public interface SpectralDistribution {

   /**
    * Multiplies this spectrum by another distribution.
    * 
    * @param sd The spectral distribution to multiply by.
    */
   public void multiply(SpectralDistribution sd);

   /**
    * Multiplies this spectrum by a constant scale factor.
    * 
    * @param scale
    *           The factor.
    */
   public void multiply(float scale);

   /**
    * Accessor to determine whether this spectral distribution has any power.
    * 
    * @return true if this spectral distribution has no power, false if it contains at least one wavelength with
    *         non-zero power.
    */
   public boolean isZero();

   /**
    * Accessor for the first (smallest) wavelength with non-zero power.
    * 
    * @return The shortest wavelength with non-zero power.
    */
   public float getStartWavelength();

   /**
    * Accessor for the last (longest) wavelength with non-zero power.
    * 
    * @return The longest wavelength with non-zero power.
    */
   public float getEndWavelength();

   /**
    * Returns a fixed number of regularly spaced samples of the spectral power over the specified wavelength range.
    * 
    * @param samples
    *           The non-null array to store the samples in. <code>samples.length</code> samples must be generated.
    * @param startWl
    *           The wavelength to begin sampling at (and store in samples[0]).
    * @param endWl
    *           The wavelength to stop sampling at (and store in samples[samples.length - 1]).
    */
   public void getSamples(float[] samples, float startWl, float endWl);
}
