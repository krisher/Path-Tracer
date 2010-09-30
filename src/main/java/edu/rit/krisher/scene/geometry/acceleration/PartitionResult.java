/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

/**
 *
 */
public class PartitionResult {
   public static final int NO_SPLIT = -1;

   public final int splitAxis;
   public final double splitLocation;

   public PartitionResult(final int splitAxis, final double splitLocation) {
      this.splitAxis = splitAxis;
      this.splitLocation = splitLocation;
   }

   public PartitionResult() {
      this.splitAxis = NO_SPLIT;
      this.splitLocation = 0;
   }
}
