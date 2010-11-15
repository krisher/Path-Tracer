package edu.rit.krisher.raytracer.image;

import java.awt.Dimension;

/**
 * Interface for a buffer that will receive pixel data from some external
 * process.
 * 
 */
public interface ImageBuffer {

   /**
    * Accessor for the dimensions of the image that this buffer holds.
    * 
    * @return A non-null, non-zero dimension.
    */
   public Dimension getResolution();

   /**
    * Publication of pixel data in a specified rectangle. This method may be
    * called from multiple threads, and should be thread safe.
    * 
    * @param x
    *           The minimum x offset of the rectangle with respect to the total
    *           image dimension (0 is left edge)
    * @param y
    *           The minimum y offset of the rectangle with respect to the total
    *           image dimension (0 is top edge)
    * @param w
    *           The width of the pixel data rectangle, in pixels
    * @param h
    *           The height of the pixel data rectangle, in pixels
    * @param pixels
    *           A non-null float array containing RGB data. The values in this
    *           array are only valid for the duration of this call (the data
    *           should be copied). Length == w * h * 3.
    */
   public void setPixels(int x, int y, int w, int h, float[] pixels);


   // public void setMultisamplePixels(float[] pixelXY, float[] pixels);

   /**
    * Notification that a new image will be provided to this buffer via calls to
    * {@link #setPixels(int, int, int, int, float[])}. {@link #imagingDone()}
    * will be called at some point in the future, before any subsequent call to
    * this method.
    */
   public void imagingStarted();

   /**
    * Notification that all of the pixels of the image have been transferred via
    * {@link #setPixels(int, int, int, int, float[])}. No more pixel data will
    * be transferred until a subsequent call to {@link #imagingStarted()}
    */
   public void imagingDone();

}
