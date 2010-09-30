package edu.rit.krisher.scene.geometry.acceleration;

import edu.rit.krisher.scene.Geometry;

public interface Partitionable extends Geometry {

   public Geometry[] getPrimitives();

}
