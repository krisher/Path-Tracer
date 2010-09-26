package edu.rit.krisher.raytracer.image;

import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * A set of image processing utility methods.
 */
public final class ImageUtil {

   /**
    * ToneMapper that simply clamps input values to the displayable range.
    */
   public static final ToneMapper clampTM = new ToneMapper() {

      @Override
      public void toneMap(final float[] image, final byte[] rgbOut) {
         for (int i = 0; i < image.length; i++) {
            final float clamped = Math.max(0, Math.min(1f, image[i]));
            rgbOut[i] = (byte) (0xFF & (short) (clamped * 255.0));
         }
      }
   };

   /*
    * These conversion matrices are taken from Erik Reinhard and Mike Stark's
    * tone mapping code at:
    * 
    * http://www.cs.ucf.edu/~reinhard/cdrom/
    * 
    * The numbers do not directly correspond to any RGB <-> CIE color conversion
    * models that I can find, they may be adjusted for more recent displays.
    */
   private static final double[] RGB2XYZ = { 0.5141364, 0.3238786, 0.16036376, 0.265068, 0.67023428, 0.06409157,
      0.0241188, 0.1228178, 0.84442666 };
   private static final double[] XYZ2RGB = { 2.5651, -1.1665, -0.3986, -1.0217, 1.9777, 0.0439, 0.0753, -0.2543, 1.1892 };

   private ImageUtil() {
      /*
       * Prevent Construction.
       */
   }

   /**
    * Ward tone reproduction operator.
    * 
    * @author krisher
    * 
    */
   public static final class WardTM implements ToneMapper {
      private final Double whitePoint;

      /**
       * Creates a new Ward tone mapper, with the optional max luminance value.
       * 
       * @param whitePoint
       *           An optional maximum luminance (pixels in the input with
       *           luminance >= this value are mapped to the maximum displayable
       *           luminance). May be null, in which case the maximum value is
       *           computed from the max luminance in the image.
       */
      public WardTM(final Double whitePoint) {
         this.whitePoint = whitePoint;
      }

      @Override
      public void toneMap(final float[] image, final byte[] rgbOut) {
         wardTR(image, rgbOut, whitePoint);
      }
   };

   /**
    * Reinhard tone reproduction.
    * 
    * @author krisher
    * 
    */
   public static final class ReinhardTM implements ToneMapper {
      private final Double midpoint;
      private final Double whitePoint;

      /**
       * Creates a new ToneMapper based on the Reinhard operator.
       * 
       * @param midpoint
       *           The midpoint (typically between 0 and 1). The image luminance
       *           will be scaled so that the log-average luminance value equals
       *           this value. May be null, in which case the default value 0.18
       *           is used.
       * @param whitePoint
       *           An optional maximum luminance (pixels in the input with
       *           luminance >= this value are mapped to the maximum displayable
       *           luminance). May be null, in which case the maximum value is
       *           computed from the max luminance in the image.
       */
      public ReinhardTM(final Double midpoint, final Double whitePoint) {
         this.midpoint = midpoint;
         this.whitePoint = whitePoint;
      }

      @Override
      public void toneMap(final float[] image, final byte[] rgbOut) {
         reinhardStarkTR(image, rgbOut, midpoint, whitePoint);
      }
   }

   /**
    * Performs Reinhard-Stark tone mapping on the specified array of rgb values.
    * 
    * @param rgb
    *           A non-null array of rgb values.
    * @param rgbOut
    *           the non-null array to store the results in.
    * @param midPoint
    *           The desired midpoint luminance of the output image.
    * @param whitePt
    * @return
    */
   public static void reinhardStarkTR(final float[] rgb, final byte[] rgbOut, Double midPoint, final Double whitePoint) {
      // final float[] result = new float[rgb.length];

      /*
       * Convert to Yxy color space (lum/chrom based on CIE XYZ), and compute
       * the log average of the luminance values.
       */
      double logAvg = 0;
      double maxLum = 0;
      for (int pix = 0; pix < rgb.length; pix += 3) {
         final double y = RGB2XYZ[3] * rgb[pix] + RGB2XYZ[4] * rgb[pix + 1] + RGB2XYZ[5] * rgb[pix + 2];
         logAvg += Math.log(1e-4 + y);
         if (y > maxLum)
            maxLum = y;
      }
      logAvg = Math.exp(logAvg / (rgb.length / 3.0));

      /*
       * Use 18% gray as a default midpoint value.
       */
      if (midPoint == null)
         midPoint = 0.18;
      /*
       * Scale luminance values based on specified mid-tone, and normalize
       * between 0 and 1.
       * 
       * Also convert back to RGB.
       */
      final double scale = midPoint / logAvg;
      scaleLum(scale, rgb, rgbOut, (whitePoint == null) ? maxLum : whitePoint);
   }

   /**
    * Scales the specified rgb values' luminance by the specified factor.
    * 
    * @param scale
    *           The scale factor.
    * @param rgb
    *           Array of input RGB values.
    * @param rgbOut
    *           Array of output RGB values.
    * @param whiteLum
    *           Maximum (unscaled) luminance value. Any values >= this value
    *           will be mapped to a luminance of 1. May be 0, in which case max
    *           luminance is assumed to be infinite.
    */
   private static final void scaleLum(final double scale, final float[] rgb, final byte[] rgbOut, final double whiteLum) {
      final double whiteSq = whiteLum * scale * whiteLum * scale;
      for (int pix = 0; pix < rgb.length; pix += 3) {
         double Y = RGB2XYZ[3] * rgb[pix] + RGB2XYZ[4] * rgb[pix + 1] + RGB2XYZ[5] * rgb[pix + 2];
         if (Y > 0) {
            double x = RGB2XYZ[0] * rgb[pix] + RGB2XYZ[1] * rgb[pix + 1] + RGB2XYZ[2] * rgb[pix + 2];
            double z = RGB2XYZ[6] * rgb[pix] + RGB2XYZ[7] * rgb[pix + 1] + RGB2XYZ[8] * rgb[pix + 2];

            final double sum = Y + x + z;
            x = (float) (x / sum);
            final double y = (float) (Y / sum);

            Y *= scale;
            /*
             * From Reinhard paper, the Y/maxLum^2 term allows specification of
             * which luminance value corresponds to white, which can be used to
             * give a greater range to lower values, at the expense of any value
             * > maxLum being clamped to white.
             */
            if (whiteSq > 0)
               Y *= (1.0 + (Y / whiteSq)) / (1.0 + Y);
            else
               Y *= 1.0 / (1.0 + Y);
            /*
             * Convert from Yxy to CIE XYZ
             */
            if (y > 0.0 && x > 0.0 && y > 0.0) {
               x *= Y / y;
               z = (Y / y) - x - Y;
            } else {
               x = z = 0.0;
            }

            /*
             * Convert back to RGB, clamping at 1.0. Note that since we are
             * doing the scaling in the luminance channel, it is possible that
             * some r, g, b values will be clamped because the desired luminance
             * can not be achieved.
             */
            final float r = (float) Math.min(1.0, (XYZ2RGB[0] * x + XYZ2RGB[1] * Y + XYZ2RGB[2] * z));
            final float g = (float) Math.min(1.0, (XYZ2RGB[3] * x + XYZ2RGB[4] * Y + XYZ2RGB[5] * z));
            final float b = (float) Math.min(1.0, (XYZ2RGB[6] * x + XYZ2RGB[7] * Y + XYZ2RGB[8] * z));

            rgbOut[pix] = (byte) (0xFF & (short) (r * 255.0));
            rgbOut[pix + 1] = (byte) (0xFF & (short) (g * 255.0));
            rgbOut[pix + 2] = (byte) (0xFF & (short) (b * 255.0));
         } else {
            rgbOut[pix] = rgbOut[pix + 1] = rgbOut[pix + 2] = 0;
         }
      }
   }

   /**
    * Performs Ward tone mapping on the specified array of rgb values.
    * 
    * @param rgb
    *           A non-null array of rgb values.
    * @param whitePt
    */
   public static void wardTR(final float[] rgb, final byte[] rgbOut, final Double whitePoint) {
      /*
       * Compute the log average of the luminance values.
       */
      double logAvg = 0;
      double max = 0;
      for (int pix = 0; pix < rgb.length; pix += 3) {
         final double y = RGB2XYZ[3] * rgb[pix] + RGB2XYZ[4] * rgb[pix + 1] + RGB2XYZ[5] * rgb[pix + 2];
         logAvg += Math.log(1e-4 + y);
         if (y > max)
            max = y;
      }
      logAvg = Math.exp(logAvg / (rgb.length / 3.0));

      /*
       * Scale luminance values.
       */
      final double scale = Math.pow((1.219 + Math.pow(1.0 / 2.0, 0.4)) / (1.219 + Math.pow(logAvg, 0.4)), 2.5);
      scaleLum(scale, rgb, rgbOut, (whitePoint == null) ? max : whitePoint);
   }

   public static void floatRGB2Byte(final float[] frgb, final byte[] rgb, final int offset, final int count) {
      for (int i = 0; i < count; i++) {
         final int pixOffs = offset + i * 3;
         for (int comp = 0; comp < 3; comp++) {
            final float pixComp = frgb[pixOffs + comp];
            if (pixComp > 1.0)
               rgb[pixOffs + comp] = (byte) (255 | 0xFF);
            else if (pixComp < 0.0)
               rgb[pixOffs + comp] = (byte) 0;
            else {
               rgb[pixOffs + comp] = (byte) ((short) (pixComp * 255.0) & 0xFF);
            }
         }
      }
   }

   public static BufferedImage getLuminanceImage(final float[] rgb, final Dimension size) {
      final byte[] bImage = new byte[rgb.length / 3];
      for (int isrc = 0, idst = 0; isrc < rgb.length; isrc += 3, idst++) {
         final double y = RGB2XYZ[3] * rgb[isrc] + RGB2XYZ[4] * rgb[isrc + 1] + RGB2XYZ[5] * rgb[isrc + 2];
         bImage[idst] = (byte) ((short) (Math.min(y, 1.0) * 255.0) & 0xFF);
      }
      return createGrayImageFromBuffer(bImage, size);
   }

   /**
    * Creates a BufferedImage with the specified size, that is backed by the
    * specified RGB byte array.
    * 
    * @param bImage
    *           A non-null byte array used as the backing store for the created
    *           BufferedImage. Must have capacity >= size.width * size.height *
    *           3.
    * @param size
    *           The dimensions of the image to create.
    * @return A non-null BufferedImage that wraps the specified byte array.
    */
   public static final BufferedImage createRGBImageFromBuffer(final byte[] bImage, final Dimension size) {

      final DataBuffer buffer = new DataBufferByte(bImage, size.width * size.height);

      final int pixelStride = 3; // assuming r, g, b, skip, r, g, b, skip...
      final int scanlineStride = 3 * size.width; // no extra padding
      final int[] bandOffsets = { 0, 1, 2 }; // r, g, b
      final WritableRaster raster = Raster.createInterleavedRaster(buffer, size.width, size.height, scanlineStride, pixelStride, bandOffsets, null);

      final ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
      final boolean hasAlpha = false;
      final boolean isAlphaPremultiplied = false;
      final int transparency = Transparency.OPAQUE;
      final int transferType = DataBuffer.TYPE_BYTE;
      final ColorModel colorModel = new ComponentColorModel(colorSpace, hasAlpha, isAlphaPremultiplied, transparency, transferType);

      return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
   }

   /**
    * Creates a BufferedImage with the specified size, that is backed by the
    * specified RGB byte array.
    * 
    * @param bImage
    *           A non-null byte array used as the backing store for the created
    *           BufferedImage. Must have capacity >= size.width * size.height *
    *           3.
    * @param size
    *           The dimensions of the image to create.
    * @return A non-null BufferedImage that wraps the specified byte array.
    */
   public static final BufferedImage createGrayImageFromBuffer(final byte[] bImage, final Dimension size) {

      final DataBuffer buffer = new DataBufferByte(bImage, size.width * size.height);

      final int pixelStride = 1;
      final int scanlineStride = size.width; // no extra padding
      final int[] bandOffsets = { 0 }; // r, g, b
      final WritableRaster raster = Raster.createInterleavedRaster(buffer, size.width, size.height, scanlineStride, pixelStride, bandOffsets, null);

      final ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
      final boolean hasAlpha = false;
      final boolean isAlphaPremultiplied = false;
      final int transparency = Transparency.OPAQUE;
      final int transferType = DataBuffer.TYPE_BYTE;
      final ColorModel colorModel = new ComponentColorModel(colorSpace, hasAlpha, isAlphaPremultiplied, transparency, transferType);

      return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
   }

}
