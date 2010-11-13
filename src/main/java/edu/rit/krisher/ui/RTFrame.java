package edu.rit.krisher.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;
import edu.rit.krisher.raytracer.PathTracer;
import edu.rit.krisher.raytracer.PhotonTracer;
import edu.rit.krisher.raytracer.SurfaceIntegrator;
import edu.rit.krisher.raytracer.image.DisplayableImageBuffer;
import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.raytracer.image.ImageUtil;
import edu.rit.krisher.scene.Scene;

/**
 * Main RayTracer UI window.
 * 
 * TODO: Add controls to load a .ply file and specify a material...
 */
public class RTFrame extends JFrame {

   private final RTControlPanel rtControls = new RTControlPanel();
   private final ToneMapPanel imageControls = new ToneMapPanel();
   private final JPanel rtImagePanel = new JPanel(new GridLayout(1, 1));
   private DisplayableImageBuffer rtImage = new DisplayableImageBuffer(512, 512);

   final JButton saveButton = new JButton("Save Image");
   final JFileChooser saveChooser = new JFileChooser(".");

   private final SurfaceIntegrator[] integrators = new SurfaceIntegrator[] { new PathTracer(), new PhotonTracer() };

   public RTFrame() {
      final Container contentPane = getContentPane();

      rtControls.setBorder(BorderFactory.createTitledBorder("Ray Tracer"));
      rtControls.setIntegratorChoices("Path Tracer", "Photon Tracer");
      rtControls.addActionListener(rtControlListener);
      contentPane.setLayout(new MigLayout("", "[fill]u[grow]"));
      contentPane.add(rtControls, "ay top");

      final JComponent display = rtImage.getDisplayComponent();
      rtImagePanel.add(display);
      display.setToolTipText("Double-click to select white-point luminance value");
      display.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(final MouseEvent evt) {
            if (evt.getClickCount() == 2) {
               final float[] rgb = new float[3];
               rtImage.getPixel(rgb, evt.getX(), evt.getY());
               imageControls.setReinhardWhitepoint(ImageUtil.luminance(rgb[0], rgb[1], rgb[2]));
               rtImage.setToneMapper(imageControls.getSelectedToneMapper());
            }
         }
      });
      contentPane.add(new JScrollPane(rtImagePanel), "spany 3, wrap, align 50% 50%");

      imageControls.setBorder(BorderFactory.createTitledBorder("Tone"));
      imageControls.addActionListener(tmControlListener);
      contentPane.add(imageControls, "ay top, wrap");

      saveChooser.setAcceptAllFileFilterUsed(false);
      saveChooser.setFileFilter(new FileFilter() {

         @Override
         public String getDescription() {
            return "PNG Files";
         }

         @Override
         public boolean accept(final File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".png");
         }
      });
      saveButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent evt) {
            if (saveChooser.showSaveDialog(RTFrame.this) == JFileChooser.APPROVE_OPTION) {
               File selected = saveChooser.getSelectedFile();
               if (!selected.getName().toLowerCase().endsWith(".png")) {
                  selected = new File(selected.getPath() + ".png");
               }
               try {
                  ImageIO.write(rtImage.getImage(), "png", selected);
               } catch (final Exception e) {
                  JOptionPane.showMessageDialog(RTFrame.this, "Save Failed!", "Error", JOptionPane.ERROR_MESSAGE);

               }
            }
         }
      });
      contentPane.add(saveButton);

      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      pack();
   }

   public void setScenes(final Scene[] scenes) {
      rtControls.setScenes(scenes);
   }

   private final ActionListener rtControlListener = new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
         final String cmd = e.getActionCommand();
         final int selectedIntegrator = Math.max(0, rtControls.getSelectedIntegrator());
         if (RTControlPanel.ACTION_CMD_START == cmd) {
            /*
             * Update ray-tracer settings.
             */
            final Dimension oldSize = rtImage.getResolution();
            if (oldSize.width != rtControls.getImageWidth() || oldSize.height != rtControls.getImageHeight()) {
               rtImagePanel.remove(rtImage.getDisplayComponent());
               rtImage = new DisplayableImageBuffer(rtControls.getImageWidth(), rtControls.getImageHeight());
               rtImage.setToneMapper(imageControls.getSelectedToneMapper());
               rtImagePanel.add(rtImage.getDisplayComponent());
               rtImagePanel.revalidate();
            }

            final Scene selectedScene = rtControls.getSelectedScene();
            integrators[selectedIntegrator].integrate(progressBuffer, selectedScene, rtControls.getSampleRate(), rtControls.getRecursionDepth());
         } else {
            integrators[selectedIntegrator].cancel(progressBuffer);
         }
      }

   };

   private final ActionListener tmControlListener = new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
         rtImage.setToneMapper(imageControls.getSelectedToneMapper());
      }

   };

   private final ImageBuffer progressBuffer = new ImageBuffer() {

      @Override
      public void setPixels(final int x, final int y, final int w, final int h, final float[] pixels) {
         rtImage.setPixels(x, y, w, h, pixels);
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               rtControls.worked(w * h);
            }
         });
      }

      @Override
      public void imagingStarted() {
         final Dimension size = getResolution();
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               rtControls.workStarted(size.width * size.height);
            }
         });
      }

      @Override
      public void imagingDone() {
         rtImage.imagingDone();
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               rtControls.workCompleted();
            }
         });
      }

      @Override
      public Dimension getResolution() {
         return rtImage.getResolution();
      }
   };
}
