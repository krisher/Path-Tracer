package edu.rit.krisher.raytracer.image;

/**
 * Interface for an tone mapping operator.
 * 
 * @see ImageUtil for several implementations.
 * @author krisher
 * 
 */
public interface ToneMapper {

   /**
    * Tone-maps the specified input RGB image, and stores the result in the
    * specified output array.
    * 
    * @param image
    *           A non-null array of RGB values in the range [0, infinity)
    * @param rgbOut
    *           A non-null array with the same length as the input image.
    */
   public void toneMap(float[] image, byte[] rgbOut);
}
