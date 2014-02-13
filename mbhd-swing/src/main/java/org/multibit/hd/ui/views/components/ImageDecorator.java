package org.multibit.hd.ui.views.components;

import com.google.common.collect.Maps;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * <p>Decorator to provide the following to UI:</p>
 * <ul>
 * <li>Various image effects</li>
 * <li>Consistent rendering hints</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class ImageDecorator {

  /**
   * Utilities have no public constructor
   */
  private ImageDecorator() {
  }

  /**
   * @param image        The original image
   * @param cornerRadius The required corner radius in pixels
   *
   * @return A new image with the required cornering
   */
  public static BufferedImage applyRoundedCorners(BufferedImage image, int cornerRadius) {

    int w = image.getWidth();
    int h = image.getHeight();

    BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

    Graphics2D g2 = output.createGraphics();

    // Perform soft-clipping in fully opaque mask with standard hints
    g2.setComposite(AlphaComposite.Src);
    g2.setColor(Color.MAGENTA);
    g2.setRenderingHints(smoothRenderingHints());

    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

    // Use the mask as an alpha source and apply the image
    g2.setComposite(AlphaComposite.SrcAtop);
    g2.drawImage(image, 0, 0, null);

    g2.dispose();

    return output;
  }

  /**
   * @param icon The icon
   *
   * @return An image icon suitable for use in tables etc
   */
  public static ImageIcon toImageIcon(Icon icon) {

    if (icon instanceof ImageIcon) {
      return (ImageIcon) icon;
    }

    // Get the size
    int w = icon.getIconWidth();
    int h = icon.getIconHeight();

    // Set up the graphics environment
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();
    GraphicsConfiguration gc = gd.getDefaultConfiguration();

    // Create the buffered image
    BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);

    Graphics2D g = image.createGraphics();
    g.setRenderingHints(ImageDecorator.smoothRenderingHints());

    // Paint the icon on to it
    icon.paintIcon(null, g, 0, 0);

    g.dispose();

    return toImageIcon(image);

  }

  /**
   * @return Rendering hints for anti-aliased and symmetrical output (smooth)
   */
  public static Map<RenderingHints.Key, ?> smoothRenderingHints() {

    Map<RenderingHints.Key, Object> hints = Maps.newHashMap();

    // Anti-aliasing to ensure smooth edges
    hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Pure strokes to ensure symmetrical corners
    hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    return hints;
  }

  /**
   * @param image The buffered image
   *
   * @return An image icon suitable for use in tables etc
   */
  public static ImageIcon toImageIcon(BufferedImage image) {

    return new ImageIcon(image);
  }
}
