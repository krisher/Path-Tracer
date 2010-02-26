package edu.rit.krisher.scene.material;

public class RingsPattern implements Texture {

    private final Color[] colors;
    double ringWidth = 1 / 16.0;

    public RingsPattern(Color c1, Color c2) {
        this.colors = new Color[] { c1, c2 };
    }

    @Override
    public Color getColor(double... coordinate) {
        double radius = 0;
        if (coordinate != null) {
            for (double coord : coordinate) {
                radius += coord * coord;
            }
        }
        return colors[((int) (Math.sqrt(radius) / ringWidth)) % 2];
    }

}
