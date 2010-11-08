package edu.rit.krisher.raytracer.image;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;


/**
 * Combination ImageBuffer (to recieve pixel data from a Ray Tracing process)
 * and GUI Panel to display the image. This could have been seperated into two
 * classes, but keeping them together makes it very easy to update only the
 * changed portions of the image on the display panel.
 * 
 * @author krisher
 * 
 */
public class DisplayableImageBuffer extends DefaultImageBuffer implements ImageBuffer {

   private final JComponent imageComponent = new JComponent() {
      @Override
      protected void paintComponent(final Graphics g) {
         super.paintComponent(g);
         synchronized (bImage) {
            ((Graphics2D) g).drawImage(raster, new AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);
         }
      }
   };
   
   private final AtomicInteger updatedPixelCount = new AtomicInteger(0);

   public DisplayableImageBuffer(final int width, final int height) {
      super(width, height);
      imageComponent.setPreferredSize(new Dimension(imSize));
      imageComponent.setMaximumSize(new Dimension(imSize));
      imageComponent.setMinimumSize(new Dimension(imSize));
   }

   public JComponent getDisplayComponent() {
      return imageComponent;
   }

   @Override
   public void imagingDone() {
      super.imagingDone();
      imageComponent.repaint();
   }

   @Override
   public void imagingStarted() {
      super.imagingStarted();
      imageComponent.repaint();
   }

   @Override
   public void setPixels(final int sx, final int sy, final int w, final int h, final float[] pixels) {
      super.setPixels(sx, sy, w, h, pixels);
      final int pixelsUpdated = updatedPixelCount.addAndGet(w * h);
      if (pixelsUpdated > 10000) {
         updatedPixelCount.addAndGet(-pixelsUpdated);
//         imageComponent.repaint(sx, imSize.height - sy - h, w, h);
         imageComponent.repaint();
      }
   }

   @Override
   public void setToneMapper(final ToneMapper tm) {
      super.setToneMapper(tm);
      imageComponent.repaint();
   }
}
