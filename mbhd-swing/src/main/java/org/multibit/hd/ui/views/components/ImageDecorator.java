package org.multibit.hd.ui.views.components;

import com.google.common.collect.Maps;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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
   * @return A buffered image suitable for use with panels, overlays etc
   */
  public static BufferedImage toBufferedImage(Icon icon) {

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

    // Keep it smooth
    g.setRenderingHints(smoothRenderingHints());

    // Paint the icon on to it
    icon.paintIcon(null, g, 0, 0);

    g.dispose();

    return image;
  }

  /**
   * @param image The buffered image
   *
   * @return An image icon suitable for use in tables etc
   */
  public static ImageIcon toImageIcon(BufferedImage image) {

    return new ImageIcon(image);
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

    // Use buffered image as an intermediate format
    BufferedImage image = toBufferedImage(icon);

    // Convert to image icon for tables
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
   * <p>Applies a single alpha-blended color over all pixels</p>
   *
   * @param image    The source image
   * @param newColor The color to use as the replacement to non-transparent pixels
   *
   * @return The new image with color applied
   */
  public static BufferedImage applyColor(BufferedImage image, Color newColor) {

    int width = image.getWidth();
    int height = image.getHeight();

    WritableRaster raster = image.getRaster();

    int newColorRed = newColor.getRed();
    int newColorGreen = newColor.getGreen();
    int newColorBlue = newColor.getBlue();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int[] pixels = raster.getPixel(x, y, (int[]) null);

        pixels[0] = newColorRed;
        pixels[1] = newColorGreen;
        pixels[2] = newColorBlue;

        raster.setPixel(x, y, pixels);
      }
    }

    return image;
  }

  /**
   * <p>Overlay the foreground onto the background with an offset</p>
   *
   * @param foreground The foreground image
   * @param background The background image
   * @param x          The x position on the background to place the foreground image
   * @param y          The y position on the background to place the foreground image
   *
   * @return The (clipped if necessary) foreground image placed over the background image
   */
  public static BufferedImage overlayImages(BufferedImage foreground, BufferedImage background, int x, int y) {

    int bx = background.getWidth();
    int by = background.getHeight();

    // Get the graphics context
    Graphics2D g2 = background.createGraphics();

    // Blend images smoothly
    g2.setRenderingHints(smoothRenderingHints());

    // Draw background image at (0,0)
    g2.drawImage(background, 0, 0, null);

    // Draw clipped foreground image at (x,y) not exceeding background boundaries
    g2.drawImage(foreground, x, y, bx - x, by - y, null);

    // Tidy up
    g2.dispose();

    return background;
  }

  /**
   * <p>Rotate an image about its center</p>
   *
   * @param theta The number of radians to rotate (-PI rotates 180 degrees clockwise)
   *
   * @return The image rotated by the required amount
   */
  public static BufferedImage rotate(BufferedImage image, double theta) {

    // Calculate the center of rotation
    double x = image.getWidth() / 2;
    double y = image.getHeight() / 2;

    // Get the graphics context
    Graphics2D g2 = image.createGraphics();

    // Blend images smoothly
    g2.setRenderingHints(smoothRenderingHints());

    // Rotate the image about the given center
    g2.rotate(theta, x, y);

    // Draw the image
    g2.drawImage(image, 0, 0, null);

    // Tidy up
    g2.dispose();

    return image;
  }

}
