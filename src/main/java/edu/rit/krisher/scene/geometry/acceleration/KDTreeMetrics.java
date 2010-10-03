package edu.rit.krisher.scene.geometry.acceleration;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;

public class KDTreeMetrics {

   public final int maxDepth;
   public final int minDepth;
   public final float avgDepth;

   public final int maxLeafPrimitives;
   public final int minLeafPrimitives;
   public final float avgLeafPrimitives;

   /**
    * 
    */
   public final float primCountVariance;

   public final int totalPrimitives;
   public final int duplicatedPrimitives;
   public final int leafNodes;
   public final int intermediateNodes;

   public KDTreeMetrics(final KDTree tree) {
      final KDTreeMetricVisitor visitor = new KDTreeMetricVisitor();
      try {
         tree.visitTreeNodes(visitor);
         this.maxDepth = visitor.maxDepth;
         this.minDepth = visitor.minDepth;
         this.avgDepth = visitor.cumDepth / (float) visitor.leafNodeCount;

         this.maxLeafPrimitives = visitor.maxLeafPrimitives;
         this.minLeafPrimitives = visitor.minLeafPrimitives;
         this.avgLeafPrimitives = visitor.cumLeafPrimitives / (float) visitor.leafNodeCount;


         this.intermediateNodes = visitor.intermediateNodeCount;
         this.leafNodes = visitor.leafNodeCount;

         this.totalPrimitives = visitor.cumLeafPrimitives;
         this.duplicatedPrimitives = visitor.cumLeafPrimitives - tree.getPrimitives().length;

         final VarianceVisitor vVisitor = new VarianceVisitor(visitor.leafNodeCount
                                                              / (double) visitor.cumLeafPrimitives);
         tree.visitTreeNodes(vVisitor);
         this.primCountVariance = (float) (vVisitor.variance / visitor.leafNodeCount);
      } catch (final Exception e) {
         /*
          * Unreachable...
          */
         assert false;
         throw new RuntimeException();
      }
   }

   @Override
   public String toString() {
      final StringBuilder builder = new StringBuilder();
      builder.append("KDTree (Leaf Nodes): " + leafNodes + "\n");
      builder.append("KDTree (Interior Nodes): " + intermediateNodes + "\n");
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
      return builder.toString();
   }

   private static class KDTreeMetricVisitor implements KDNodeVisitor {

      int maxDepth = 0;
      int minDepth = Integer.MAX_VALUE;
      int cumDepth = 0;

      int maxLeafPrimitives = 0;
      int minLeafPrimitives = Integer.MAX_VALUE;
      int cumLeafPrimitives = 0;

      int fullyPopulatedIntermediateNodes = 0;
      int intermediateNodeCount = 0;
      int leafNodeCount = 0;

      @Override
      public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
            final int childCount, final float splitLocation, final int splitAxis) throws Exception {
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
         } else {
            ++intermediateNodeCount;
            if (childCount == 2) {
               ++fullyPopulatedIntermediateNodes;
            }
         }
      }
   }

   private static class VarianceVisitor implements KDNodeVisitor {

      private final double meanPrimsPerLeaf;
      double variance;

      public VarianceVisitor(final double meanPrimsPerLeaf) {
         this.meanPrimsPerLeaf = meanPrimsPerLeaf;
      }

      @Override
      public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
            final int childCount, final float splitLocation, final int splitAxis) throws Exception {
         if (leaf) {
            final double delta = childCount - meanPrimsPerLeaf;
            variance += delta * delta;
         }
      }

   }
}
