package edu.rit.krisher.ui;

import javax.swing.SwingUtilities;

import edu.rit.krisher.ui.scenes.AdvRenderingScenes;

public class RTDemo {


   public static void main(final String[] args) {
      final RTFrame frame = new RTFrame();

      frame.setScenes(AdvRenderingScenes.getScenes());
//       frame.setScenes(CG2Scenes.getScenes());

      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {

            frame.setVisible(true);
         }
      });
   }
}
