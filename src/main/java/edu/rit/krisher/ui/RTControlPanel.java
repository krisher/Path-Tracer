package edu.rit.krisher.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;

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

   private final JFormattedTextField sampleRateField = new JFormattedTextField(NumberFormat.getIntegerInstance());

   private final JFormattedTextField recursionDepthField = new JFormattedTextField(NumberFormat.getIntegerInstance());

   private final JButton startRTButton = new JButton("Start");

   private final JProgressBar progress = new JProgressBar();

   private int workload;
   private int worked;

   private long startTime;

   public RTControlPanel() {
      setLayout(new MigLayout("wrap 2", "[align right, grow]r[align left]", "[]r[]u"));

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
      add(new JLabel("Sample Rate:"));
      add(sampleRateField);
      add(new JLabel("Max Recursion:"));
      add(recursionDepthField);

      sceneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      final JScrollPane sceneListScroll = new JScrollPane(sceneList);
      add(sceneListScroll, "spanx 2, grow");

      add(progress);
      add(startRTButton);

      startRTButton.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            if (worked < workload)
               fireActionEvent(ACTION_CMD_CANCEL);
            else
               fireActionEvent(ACTION_CMD_START);
         }

      });
   }

   public int getImageWidth() {
      return ((Number) widthField.getValue()).intValue();
   }

   public void setImageWidth(int value) {
      widthField.setValue(value);
   }

   public int getImageHeight() {
      return ((Number) heightField.getValue()).intValue();
   }

   public void setImageHeight(int height) {
      heightField.setValue(height);
   }

   public int getSampleRate() {
      return ((Number) sampleRateField.getValue()).intValue();
   }

   public void setSampleRate(int sampleRate) {
      sampleRateField.setValue(sampleRate);
   }

   public int getRecursionDepth() {
      return ((Number) recursionDepthField.getValue()).intValue();
   }

   public void setRecursionDepth(int depth) {
      recursionDepthField.setValue(depth);
   }

   public void addActionListener(ActionListener listener) {
      this.actionListeners.addIfAbsent(listener);
   }

   public void removeActionListener(ActionListener listener) {
      this.actionListeners.remove(listener);
   }

   public void setScenes(SceneDescription[] scenes) {
      sceneList.setListData(scenes);
      if (scenes.length > 0)
         sceneList.setSelectedIndex(0);
   }

   public SceneDescription getSelectedScene() {
      return (SceneDescription) sceneList.getSelectedValue();
   }

   public void workStarted(int workLoad) {
      this.workload = workLoad;
      this.worked = 0;

      for (Component comp : getComponents()) {
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

   public void worked(int amount) {
      this.worked += amount;
      progress.setValue(worked);
      final int remainingWork = workload - worked;
      final long elapsedTime = System.currentTimeMillis() - startTime;
      final int remainingTimeS = (int) (((elapsedTime / (double) worked) * remainingWork) / 1000.0);
      progress.setString("ETA: " + remainingTimeS + "s");
   }

   public void workCompleted() {
      this.worked = workload;
      for (Component comp : getComponents()) {
         if (comp != startRTButton) {
            comp.setEnabled(true);
         }
      }
      sceneList.setEnabled(true);
      progress.setVisible(false);
      startRTButton.setText("Start");
   }

   protected void fireActionEvent(String command) {
      final ActionEvent actionE = new ActionEvent(this, 0, command);
      for (ActionListener listener : actionListeners) {
         listener.actionPerformed(actionE);
      }
   }
}
