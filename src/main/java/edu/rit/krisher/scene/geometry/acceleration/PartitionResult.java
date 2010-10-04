/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

/**
 *
 */
public class PartitionResult {
   public static final PartitionResult LEAF = new PartitionResult();;

   public final int splitAxis;
   public final double splitLocation;

   public PartitionResult(final int splitAxis, final double splitLocation) {
      this.splitAxis = splitAxis;
      this.splitLocation = splitLocation;
   }

   private PartitionResult() {
      this.splitAxis = -1;
      this.splitLocation = 0;
   }
}
