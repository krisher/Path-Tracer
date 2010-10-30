package edu.rit.krisher.scene.acceleration;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 * Various aggregate information regarding the structure of a KD-Tree instance.
 * 
 * @author krisher
 * 
 */
public class KDTreeMetrics {

   /**
    * The maximum depth of a leaf node.
    */
   public final int maxDepth;
   /**
    * The minimum depth of a leaf node.
    */
   public final int minDepth;
   /**
    * The average depth of a leaf node.
    */
   public final float avgDepth;

   public final int maxLeafPrimitives;
   public final int minLeafPrimitives;
   public final float avgLeafPrimitives;

   public final int minEmptyDepth;
   public final int maxEmptyDepth;
   public final float avgEmptyDepth;

   /**
    * The variance in the number of primitives held by leaf nodes.
    */
   public final float primCountVariance;

   /**
    * The total number of primitives referenced by leaf nodes (geometry primitives + duplciated primitives).
    */
   public final int totalPrimitives;
   /**
    * The number of duplicated references to primitives in the leaf nodes; total primitives referenced - geometry
    * defined primitives.
    */
   public final int duplicatedPrimitives;

   /**
    * The total number of leaf nodes.
    */
   public final int leafNodes;
   /**
    * The total number of intermediate nodes.
    */
   public final int intermediateNodes;

   public final int emptyNodes;

   /**
    * The volume of the KD-Tree.
    */
   public final double treeVolume;
   /**
    * The total volume that is occupied by leaf nodes with at least one primitive.
    */
   public final double leafVolume;

   /**
    * Analyzes the specified KDTree to compute the metrics.
    * 
    * @param tree
    *           A non-null KDTree.
    */
   public KDTreeMetrics(final KDTree tree) {
      final KDTreeMetricVisitor visitor = new KDTreeMetricVisitor();
      try {
         tree.visitTreeNodes(visitor);

         this.treeVolume = tree.getBounds(-1).volume();
         this.leafVolume = visitor.cumLeafVolume;

         this.maxDepth = visitor.maxDepth;
         this.minDepth = visitor.minDepth;
         this.avgDepth = visitor.cumDepth / (float) visitor.leafNodeCount;

         this.maxLeafPrimitives = visitor.maxLeafPrimitives;
         this.minLeafPrimitives = visitor.minLeafPrimitives;
         this.avgLeafPrimitives = visitor.cumLeafPrimitives / (float) visitor.leafNodeCount;

         this.intermediateNodes = visitor.intermediateNodeCount;
         this.leafNodes = visitor.leafNodeCount;

         this.maxEmptyDepth = visitor.maxEmptyDepth;
         this.minEmptyDepth = visitor.minEmptyDepth;
         this.avgEmptyDepth = visitor.cumEmptyDepth / (float) visitor.cumEmptyNodes;
         this.emptyNodes = visitor.cumEmptyNodes;

         this.totalPrimitives = visitor.cumLeafPrimitives;
         this.duplicatedPrimitives = visitor.cumLeafPrimitives - tree.getPrimitiveCount();

         final VarianceVisitor vVisitor = new VarianceVisitor(visitor.cumLeafPrimitives
                                                              / (double) visitor.leafNodeCount);
         tree.visitTreeNodes(vVisitor);
         this.primCountVariance = (float) (vVisitor.variance / visitor.leafNodeCount);
      } catch (final Exception e) {
         /*
          * Unreachable...
          */
         assert false;
         throw new IllegalStateException(e);
      }
   }

   @Override
   public String toString() {
      final StringBuilder builder = new StringBuilder();
      builder.append("KDTree (Leaf Nodes): " + leafNodes + "\n");
      builder.append("KDTree (Interior Nodes): " + intermediateNodes + "\n");
      builder.append("\n");
      builder.append("KDTree (Total Volume): " + treeVolume + "\n");
      builder.append("KDTree (Leaf Volume %): " + leafVolume / treeVolume + "\n");
      builder.append("\n");
      builder.append("KDTree (Max Depth): " + maxDepth + "\n");
      builder.append("KDTree (Min Depth): " + minDepth + "\n");
      builder.append("KDTree (Avg Depth): " + avgDepth + "\n");
      builder.append("\n");
      builder.append("KDTree (Distinct Primitives): " + (totalPrimitives - duplicatedPrimitives) + "\n");
      builder.append("KDTree (Total Primitives): " + (totalPrimitives) + "\n");
      builder.append("KDTree (Duplicated Primitives): " + (duplicatedPrimitives) + "\n");
      builder.append("\n");
      builder.append("KDTree (Max Primitives/Leaf): " + maxLeafPrimitives + "\n");
      builder.append("KDTree (Min Primitives/Leaf): " + minLeafPrimitives + "\n");
      builder.append("KDTree (Avg Primitives/Leaf): " + avgLeafPrimitives + "\n");
      builder.append("KDTree (Leaf Primitives Count Variance): " + primCountVariance + "\n");
      builder.append("\n");
      builder.append("KDTree (Empty Nodes): " + emptyNodes + "\n");
      builder.append("KDTree (Min Empty Node Depth): " + minEmptyDepth + "\n");
      builder.append("KDTree (Max Empty Node Depth): " + maxEmptyDepth + "\n");
      builder.append("KDTree (Avg Empty Node Depth): " + avgEmptyDepth + "\n");
      builder.append("\n");
      return builder.toString();
   }

   private static class KDTreeMetricVisitor implements KDNodeVisitor {

      int maxDepth = 0;
      int minDepth = Integer.MAX_VALUE;
      int cumDepth = 0;

      int maxLeafPrimitives = 0;
      int minLeafPrimitives = Integer.MAX_VALUE;
      int cumLeafPrimitives = 0;

      int cumEmptyNodes = 0;
      int minEmptyDepth = Integer.MAX_VALUE;
      int maxEmptyDepth = -1;
      int cumEmptyDepth = 0;

      int fullyPopulatedIntermediateNodes = 0;
      int intermediateNodeCount = 0;
      int leafNodeCount = 0;

      double cumLeafVolume = 0;

      @Override
      public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
            final int childCount, final double splitLocation, final int splitAxis) throws Exception {
         if (leaf) {
            ++leafNodeCount;
            if (depth > maxDepth)
               maxDepth = depth;
            if (depth < minDepth)
               minDepth = depth;
            cumDepth += depth;
            if (childCount > maxLeafPrimitives)
               maxLeafPrimitives = childCount;
            if (childCount < minLeafPrimitives)
               minLeafPrimitives = childCount;
            cumLeafPrimitives += childCount;
            cumLeafVolume += bounds.volume();
         } else {
            ++intermediateNodeCount;
            if (childCount == 2) {
               ++fullyPopulatedIntermediateNodes;
            }
            if (childCount < 2) {
               cumEmptyNodes += 2 - childCount;
               final int emptyDepth = depth + 1;
               if (emptyDepth < minEmptyDepth)
                  minEmptyDepth = emptyDepth;
               if (emptyDepth > maxEmptyDepth)
                  maxEmptyDepth = emptyDepth;
               cumEmptyDepth += emptyDepth * (2 - childCount);
            }
         }
      }
   }

   private static class VarianceVisitor implements KDNodeVisitor {

      private final double meanPrimsPerLeaf;
      private double variance = 0;

      public VarianceVisitor(final double meanPrimsPerLeaf) {
         this.meanPrimsPerLeaf = meanPrimsPerLeaf;
      }

      @Override
      public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
            final int childCount, final double splitLocation, final int splitAxis) throws Exception {
         if (leaf) {
            final double delta = childCount - meanPrimsPerLeaf;
            variance += delta * delta;
         }
      }

   }
}
