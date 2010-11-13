package edu.rit.krisher.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.util.Timer;

/**
 * Control panel for ray tracing parameters. This panel does not provide
 * notification when the user enters/modifies each parameter, instead the user
 * may make several changes, then press a button to start the ray tracing
 * process with these parameters.
 * 
 * When the user presses the commit button, an ActionEvent is fired to
 * registered listeners with action command {@link #ACTION_CMD_START}.
 * 
 * @author krisher
 * 
 */
public class RTControlPanel extends JPanel {

   public static final String ACTION_CMD_START = "start";
   public static final String ACTION_CMD_CANCEL = "cancel";

   private final CopyOnWriteArrayList<ActionListener> actionListeners = new CopyOnWriteArrayList<ActionListener>();

   private final JList sceneList = new JList();

   private final JFormattedTextField widthField = new JFormattedTextField(NumberFormat.getIntegerInstance());
   private final JFormattedTextField heightField = new JFormattedTextField(NumberFormat.getIntegerInstance());

   private final JComboBox integratorChooser = new JComboBox();

   private final JFormattedTextField sampleRateField = new JFormattedTextField(NumberFormat.getIntegerInstance());

   private final JFormattedTextField recursionDepthField = new JFormattedTextField(NumberFormat.getIntegerInstance());

   private final JButton startRTButton = new JButton("Start");

   private final JProgressBar progress = new JProgressBar();

   private final Timer timer = new Timer("Total Ray Trace Time");

   private int workload;
   private int worked;

   private long startTime;

   public RTControlPanel() {
      setLayout(new MigLayout("wrap 2", "[align right]r[align left, grow]", "[]r[]u"));

      widthField.setValue(512);
      heightField.setValue(512);
      sampleRateField.setValue(5);
      recursionDepthField.setValue(6);

      widthField.setColumns(5);
      heightField.setColumns(5);
      sampleRateField.setColumns(3);
      recursionDepthField.setColumns(3);

      progress.setVisible(false);
      progress.setStringPainted(true);


      add(new JLabel("Width:"));
      add(widthField);
      add(new JLabel("Height:"));
      add(heightField);
      add(new JLabel("Integrator:"));
      add(integratorChooser);
      add(new JLabel("Sample Rate:"));
      add(sampleRateField);
      add(new JLabel("Max Recursion:"));
      add(recursionDepthField);

      sceneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      final JScrollPane sceneListScroll = new JScrollPane(sceneList);
      add(sceneListScroll, "spanx 2, grow");

      add(progress, "grow");
      add(startRTButton);

      startRTButton.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(final ActionEvent e) {
            if (worked < workload)
               fireActionEvent(ACTION_CMD_CANCEL);
            else
               fireActionEvent(ACTION_CMD_START);
         }

      });
   }

   public int getSelectedIntegrator() {
      return integratorChooser.getSelectedIndex();
   }

   public void setSelectedIntegrator(final int idx) {
      integratorChooser.setSelectedIndex(idx);
   }

   public void setIntegratorChoices(final String... intNames) {
      integratorChooser.removeAllItems();
      for (final String name : intNames)
         integratorChooser.addItem(name);
      if (intNames.length > 0)
         integratorChooser.setSelectedIndex(0);
   }

   public int getImageWidth() {
      return ((Number) widthField.getValue()).intValue();
   }

   public void setImageWidth(final int value) {
      widthField.setValue(value);
   }

   public int getImageHeight() {
      return ((Number) heightField.getValue()).intValue();
   }

   public void setImageHeight(final int height) {
      heightField.setValue(height);
   }

   public int getSampleRate() {
      return ((Number) sampleRateField.getValue()).intValue();
   }

   public void setSampleRate(final int sampleRate) {
      sampleRateField.setValue(sampleRate);
   }

   public int getRecursionDepth() {
      return ((Number) recursionDepthField.getValue()).intValue();
   }

   public void setRecursionDepth(final int depth) {
      recursionDepthField.setValue(depth);
   }

   public void addActionListener(final ActionListener listener) {
      this.actionListeners.addIfAbsent(listener);
   }

   public void removeActionListener(final ActionListener listener) {
      this.actionListeners.remove(listener);
   }

   public void setScenes(final Scene[] scenes) {
      sceneList.setListData(scenes);
      if (scenes.length > 0)
         sceneList.setSelectedIndex(0);
   }

   public Scene getSelectedScene() {
      return (Scene) sceneList.getSelectedValue();
   }

   public void workStarted(final int workLoad) {
      timer.start();
      this.workload = workLoad;
      this.worked = 0;

      for (final Component comp : getComponents()) {
         if (comp != startRTButton) {
            comp.setEnabled(false);
         }
      }
      sceneList.setEnabled(false);
      startRTButton.setText("Stop");
      progress.setMaximum(workLoad);
      progress.setValue(0);
      progress.setVisible(true);
      progress.setString("Estimating Time...");
      startTime = System.currentTimeMillis();
   }

   public void worked(final int amount) {
      this.worked += amount;
      progress.setValue(worked);
      final int remainingWork = workload - worked;
      final long elapsedTime = System.currentTimeMillis() - startTime;
      final int remainingTimeS = (int) (((elapsedTime / (double) worked) * remainingWork) / 1000.0);
      progress.setString("ETA: " + remainingTimeS + "s");
   }

   public void workCompleted() {
      timer.stop();
      timer.print();
      timer.reset();
      this.worked = workload;
      for (final Component comp : getComponents()) {
         if (comp != startRTButton) {
            comp.setEnabled(true);
         }
      }
      sceneList.setEnabled(true);
      progress.setVisible(false);
      startRTButton.setText("Start");
   }

   protected void fireActionEvent(final String command) {
      final ActionEvent actionE = new ActionEvent(this, 0, command);
      for (final ActionListener listener : actionListeners) {
         listener.actionPerformed(actionE);
      }
   }
}
