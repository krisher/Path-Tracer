package edu.rit.krisher.raytracer.rays;

import edu.rit.krisher.scene.Material;
import edu.rit.krisher.vecmath.Vec3;

public class HitData {
    public Material material;
    public double[] materialCoords;
    public Vec3 surfaceNormal;

}
