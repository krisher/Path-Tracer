package edu.rit.krisher.raytracer.image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import edu.rit.krisher.raytracer.ImageBuffer;

/**
 * ImageBuffer implementation supporting tone mapping, and conversion of the rgb
 * pixel data to a BufferedImage.
 * 
 * @author krisher
 * 
 */
public class DefaultImageBuffer implements ImageBuffer {

   protected final float[] image;
   protected final byte[] bImage;
   protected final BufferedImage raster;

   protected final Dimension imSize;

   protected ToneMapper toneMapper = ImageUtil.clampTM;

   public DefaultImageBuffer(int width, int height) {
      imSize = new Dimension(width, height);

      bImage = new byte[imSize.width * imSize.height * 3];
      image = new float[bImage.length];
      raster = ImageUtil.createRGBImageFromBuffer(bImage, imSize);
   }

   public BufferedImage getImage() {
      return raster;
   }

   @Override
   public Dimension getResolution() {
      return imSize;
   }

   @Override
   public void imagingDone() {
      // final float[] tr = ImageUtil.wardTR(image);
      // ImageUtil.floatRGB2Byte(tr, bImage, 0, tr.length / 3);
      toneMapper.toneMap(image, bImage);
   }

   @Override
   public void imagingStarted() {
      Arrays.fill(bImage, (byte) 0);
   }

   @Override
   public void setPixels(int sx, int sy, int w, int h, float[] pixels) {
      synchronized (bImage) {
         // TODO Auto-generated method stub
         for (int y = 0; y < h; y++) {
            int dstRow = imSize.height - (sy + y) - 1;
            final int dstPixelStart = (dstRow * imSize.width + sx) * 3;
            final int srcPixelStart = y * w * 3;
            System.arraycopy(pixels, srcPixelStart, image, dstPixelStart, w * 3);
            ImageUtil.floatRGB2Byte(image, bImage, dstPixelStart, w);
         }
      }
   }

   public void setToneMapper(ToneMapper tm) {
      this.toneMapper = tm;

      toneMapper.toneMap(image, bImage);
   }

   public ToneMapper getToneMapper() {
      return this.toneMapper;
   }
}
