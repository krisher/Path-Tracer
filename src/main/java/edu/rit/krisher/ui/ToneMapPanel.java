package edu.rit.krisher.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;
import edu.rit.krisher.raytracer.image.ImageUtil;
import edu.rit.krisher.raytracer.image.ToneMapper;

public class ToneMapPanel extends JPanel {

   private final CopyOnWriteArrayList<ActionListener> actionListeners = new CopyOnWriteArrayList<ActionListener>();

   private final ButtonGroup toneMappingChoice = new ButtonGroup();

   private final JRadioButton noTRButton = new JRadioButton("None");
   private final JRadioButton wardTRButton = new JRadioButton("Ward");
   private final JRadioButton reinhardTRButton = new JRadioButton("Reinhard");
   private final JFormattedTextField midpointValue = new JFormattedTextField(NumberFormat.getPercentInstance());
   private final JFormattedTextField maxLumValue = new JFormattedTextField(NumberFormat.getNumberInstance());

   public ToneMapPanel() {

      toneMappingChoice.add(noTRButton);
      toneMappingChoice.add(wardTRButton);
      toneMappingChoice.add(reinhardTRButton);

      noTRButton.setSelected(true);
      final ActionListener delegatingActionListener = new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent evt) {
            fireActionEvent();
         }
      };
      noTRButton.addActionListener(delegatingActionListener);
      wardTRButton.addActionListener(delegatingActionListener);
      reinhardTRButton.addActionListener(delegatingActionListener);

      midpointValue.setColumns(4);
      midpointValue.setValue(0.18);
      midpointValue.addActionListener(delegatingActionListener);

      maxLumValue.setValue(0.0);
      maxLumValue.setColumns(6);
      maxLumValue.addActionListener(delegatingActionListener);
      // midpointValue.addFocusListener(new FocusAdapter() {
      //
      // @Override
      // public void focusLost(FocusEvent e) {
      // super.focusLost(e);
      // }
      //         
      // });

      setLayout(new MigLayout("wrap 2", "[align right, grow]r[align left]", "[]r[]r[]r[]r[]u[]r"));

      add(new JLabel("Tone Mapping:"));
      add(noTRButton);
      add(wardTRButton, "skip 1");
      add(reinhardTRButton, "skip 1");
      add(new JLabel("Target Lum:"), "skip 1");
      add(midpointValue, "skip 1");
      add(new JLabel("White Luminance: "));
      add(maxLumValue);
      add(new JLabel("(0=>auto)"), "skip 1");
   }

   public void disableTonemapping() {
      noTRButton.setSelected(true);
   }

   public void enableWardTonemapping() {
      wardTRButton.setSelected(true);
   }

   public void enableReinhardTonemapping() {
      reinhardTRButton.setSelected(true);
   }

   public ToneMapper getSelectedToneMapper() {
      final ButtonModel model = toneMappingChoice.getSelection();
      final double maxLum = ((Number)maxLumValue.getValue()).doubleValue();
      if (model == wardTRButton.getModel()) {
         return new ImageUtil.WardTM(maxLum == 0 ? null:maxLum);
      } else if (model == reinhardTRButton.getModel()) {
         return new ImageUtil.ReinhardTM(((Number) midpointValue.getValue()).doubleValue(), maxLum == 0 ? null:maxLum);
      }

      return ImageUtil.clampTM;
   }

   public double getReinhardMidpoint() {
      return ((Number) midpointValue.getValue()).doubleValue();
   }

   public void setReinhardMidpoint(final double value) {
      midpointValue.setValue(value);
   }

   public void addActionListener(final ActionListener listener) {
      this.actionListeners.addIfAbsent(listener);
   }

   public void removeActionListener(final ActionListener listener) {
      this.actionListeners.remove(listener);
   }

   protected void fireActionEvent() {
      final ActionEvent actionE = new ActionEvent(this, 0, null);
      for (final ActionListener listener : actionListeners) {
         listener.actionPerformed(actionE);
      }
   }
}
