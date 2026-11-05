package rush.gui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Keeps a pre-drawn image of the parts of the scene that only change once a
 * turn, so each frame copies one image instead of redrawing the walls and lights.
 */
final class SceneCache {

    private BufferedImage image;
    private double cachedEvening = -1;
    private double cachedScale = -1;
    private int cachedVariant = -1;

    void draw(Graphics2D g, double evening, double deviceScale, int variant, Consumer<Graphics2D> painter) {
        if (image == null || evening != cachedEvening || deviceScale != cachedScale || variant != cachedVariant) {
            int w = (int) Math.ceil(Layout.SCENE_W * deviceScale);
            int h = (int) Math.ceil(Layout.SCENE_H * deviceScale);
            image = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = image.createGraphics();
            cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            cg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            cg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            cg.scale(deviceScale, deviceScale);
            painter.accept(cg);
            cg.dispose();
            cachedEvening = evening;
            cachedScale = deviceScale;
            cachedVariant = variant;
        }
        AffineTransform saved = g.getTransform();
        g.scale(1 / deviceScale, 1 / deviceScale);
        g.drawImage(image, 0, 0, null);
        g.setTransform(saved);
    }
}
