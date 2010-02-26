package edu.rit.krisher.scene.material;

public class CheckerboardPattern implements Texture {

    private final Color[] colors;

    public CheckerboardPattern(Color c1, Color c2) {
        this.colors = new Color[] { c1, c2 };
    }

    @Override
    public Color getColor(double... coordinate) {
        int colorIdx = 0;
        if (coordinate != null) {
            for (double coord : coordinate) {
                if (coord < 0)
                    colorIdx = ((int) (colorIdx - coord + 1)) % 2;
                else
                    colorIdx = ((int) (colorIdx + coord)) % 2;
            }
        }
        return colors[colorIdx];
    }

}
