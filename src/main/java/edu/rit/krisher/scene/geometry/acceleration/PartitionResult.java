/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

/**
 * Results of a KDNode partitioning as specified by a KDPartitionStrategy. This is a value class to hold multiple return
 * values for KDPartitionStrategy.
 */
public class PartitionResult {
   /**
    * Constant indicating that the {@link KDPartitionStrategy} determined that a node should not be split (it should be
    * a leaf node) in the KD Tree.
    */
   public static final PartitionResult LEAF = new PartitionResult();

   /**
    * The axis (0=>x, 1=>y, or 2=>z) of the split.
    */
   final int splitAxis;
   /**
    * The coordinate of the split location along the selected axis.
    */
   final double splitLocation;

   final int lessMemberCount;
   final int greaterEqMemberCount;

   public PartitionResult(final int splitAxis, final double splitLocation, final int lessMemberCount,
         final int greaterMemberCount) {
      this.splitAxis = splitAxis;
      this.splitLocation = splitLocation;
      this.lessMemberCount = lessMemberCount;
      this.greaterEqMemberCount = greaterMemberCount;
   }

   public PartitionResult(final int splitAxis, final double splitLocation) {
      this(splitAxis, splitLocation, -1, -1);
   }

   private PartitionResult() {
      this(-1, 0);
   }

}
