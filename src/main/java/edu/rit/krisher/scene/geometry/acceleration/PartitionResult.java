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
   public final int splitAxis;
   /**
    * The coordinate of the split location along the selected axis.
    */
   public final double splitLocation;

   // /**
   // * The index of the last member in the (sorted) members array that should be included on the less-side of the
   // split.
   // * The members argument to
   // * {@link KDPartitionStrategy#findSplitLocation(int[], edu.rit.krisher.scene.AxisAlignedBoundingBox[],
   // edu.rit.krisher.scene.AxisAlignedBoundingBox, int)}
   // * must be sorted so that all of the less memebers are contiguous and begin at index 0 (the actual ordering of
   // * individual elements is not important).
   // */
   // public final int lessMembersEnd;
   // /**
   // * The index of the first member in the (sorted) members array that should be included on the greater-side of the
   // * split. The members argument to
   // * {@link KDPartitionStrategy#findSplitLocation(int[], edu.rit.krisher.scene.AxisAlignedBoundingBox[],
   // edu.rit.krisher.scene.AxisAlignedBoundingBox, int)}
   // * must be sorted so that all of the greater/equal memebers are contiguous and begin at this index, ending at the
   // end
   // * of the array (the actual ordering of individual elements is not important).
   // */
   // public final int greaterMembersStart;

   public PartitionResult(final int splitAxis, final double splitLocation/*
    * , final int lessMembersStart, final int
    * greaterMembersStart
    */) {
      this.splitAxis = splitAxis;
      this.splitLocation = splitLocation;
      // this.lessMembersEnd = lessMembersStart;
      // this.greaterMembersStart = greaterMembersStart;
   }

   private PartitionResult() {
      this.splitAxis = -1;
      this.splitLocation = 0;
      // this.lessMembersEnd = 0;
      // this.greaterMembersStart = 0;
   }
}
