//
// SwingNavigableImageCanvas.java
//

/*
ImageJ software for multidimensional image processing and analysis.

Copyright (c) 2010, ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the names of the ImageJDev.org developers nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package imagej.ui.swing.display;

import imagej.awt.AWTCursors;
import imagej.awt.AWTEventDispatcher;
import imagej.awt.AWTNavigableImageCanvas;
import imagej.display.EventDispatcher;
import imagej.display.MouseCursor;
import imagej.display.event.ZoomEvent;
import imagej.event.EventSubscriber;
import imagej.event.Events;
import imagej.tool.event.ToolActivatedEvent;
import imagej.util.IntCoords;
import imagej.util.RealCoords;
import imagej.util.Rect;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// TODO: Break out the NavigationImage from the panel

// TODO: Change firePropertyChanges to emit Events on EventBus.
// (BDZ - this was done in the zoom changing cases : see below ... ZoomEvents)

// TODO: the navigation preview is disabled. Also its zoom scaling has not
// been updated to mirror the overall image zoom scaling.

// TODO: if an image is too big to completely fit on screen then scale to fit
// within screen and set initialScale appropriately

/**
 * A Swing implementation of the navigable image canvas.
 *
 * <p>
 * This code is based on
 * <a href="http://today.java.net/article/2007/03/23/navigable-image-panel">
 * Slav Boleslawski's NavigableImagePanel</a>.
 * </p>
 * 
 * @author Grant Harris
 * @author Curtis Rueden
 * @author Barry DeZonia
 */
public class SwingNavigableImageCanvas extends JPanel implements
	AWTNavigableImageCanvas, EventSubscriber<ToolActivatedEvent>
{
	private static final double HIGH_QUALITY_RENDERING_SCALE_THRESHOLD = 1.0;
	private static final Object INTERPOLATION_TYPE =
		// TODO - put this back?? //RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;  // this is like IJ1
	private boolean highQualityRenderingEnabled = true;
	private double zoomMultiplier = 1.2;
	private BufferedImage image;
	private double initialScale = 0;
	private double scale = 0;
	private double originX = 0;
	private double originY = 0;
	private double centerX = 0;
	private double centerY = 0;
	private Point mousePosition;
	private Dimension previousPanelSize;

	/**
	 * <p>
	 * Creates a new navigable image panel with no default image and the mouse
	 * scroll wheel as the zooming device.
	 * </p>
	 */
	public SwingNavigableImageCanvas() {
		setOpaque(false);
		addResizeListener();
		addMouseListeners();
		setZoomDevice(ZoomDevice.MOUSE_WHEEL);
		// setZoomDevice(ZoomDevice.MOUSE_BUTTON);
	}

	/**
	 * <p>
	 * Creates a new navigable image panel with the specified image and the mouse
	 * scroll wheel as the zooming device.
	 * </p>
	 */
	public SwingNavigableImageCanvas(final BufferedImage image) {
		this();
		setImage(image);
		setOrigin(0, 0);
		setCenter(image.getWidth()/2.0, image.getHeight()/2.0);
	}

	private void setOrigin(double x, double y) {
		originX = x;
		originY = y;
	}
	
	private void setCenter(double x, double y) {
		centerX = x;
		centerY = y;
	}

	// -- BASIC ACCESSORS --
	
	@Override
	public int getImageWidth() {
		return image.getWidth();
	}

	@Override
	public int getImageHeight() {
		return image.getHeight();
	}

	// -- PRIVATE HELPERS -- 
	
	private int getScreenImageWidth() {
		return (int) (scale * image.getWidth());
	}

	private int getScreenImageHeight() {
		return (int) (scale * image.getHeight());
	}

	// Called from paintComponent() when a new image is set.
	private void initializeParams() {
		final double imgWidth = image.getWidth();
		final double imgHeight = image.getWidth();
		final double xScale = getWidth() / imgWidth;
		final double yScale = getHeight() / imgHeight;
		initialScale = Math.min(xScale, yScale);
		scale = initialScale;
		setOrigin(0,0);
		setCenter(imgWidth / 2.0, imgHeight / 2.0);

		if (isNavigationImageEnabled()) {
			createNavigationImage();
		}
	}

	/** Centers the current image in the panel. */
	private void centerImage() {
		double newCenterX = getWidth() / 2.0;
		double newCenterY = getHeight() / 2.0;
		setCenter(newCenterX, newCenterY);
		double newOriginX = newCenterX - (getScreenImageWidth() / 2.0);
		double newOriginY = newCenterY - (getScreenImageHeight() / 2.0);
		setOrigin(newOriginX, newOriginY);
	}

	private void clipToImageBoundaries(RealCoords coords) {
		if (coords.x < 0.0) {
			coords.x = 0.0;
		}
		if (coords.y < 0.0) {
			coords.y = 0.0;
		}
		if (coords.x >= image.getWidth()) {
			coords.x = image.getWidth() - 1.0;
		}
		if (coords.y >= image.getHeight()) {
			coords.y = image.getHeight() - 1.0;
		}
	}
	
	// -- PRIVATE ZOOM CODE HELPERS --
	
	/** Converts the specified zoom level to scale. */
	private double zoomToScale(final double zoom) {
		return initialScale * zoom;
	}

	private boolean scaleOutOfBounds(double desiredScale) {
		// check if trying to zoom in too close
		if (desiredScale > scale)
		{
			int maxDimension = Math.max(image.getWidth(), image.getHeight());

			// if zooming the image would show less than one pixel of image data
			if ((maxDimension / getZoom()) < 1)
				return true;
		}
		
		// check if trying to zoom out too far
		if (desiredScale < scale)
		{
			// get boundaries of image in panel coords
			final RealCoords nearCorner = imageToPanelCoords(new RealCoords(0,0));
			final RealCoords farCorner = imageToPanelCoords(new RealCoords(image.getWidth(),image.getHeight()));

			// if boundaries take up less than 25 pixels in either dimension 
			if (((farCorner.x - nearCorner.x) < 25) || ((farCorner.y - nearCorner.y) < 25))
				return true;
		}
		
		return false;
	}

	private void doZoom(double newScale, double ctrX, double ctrY) {
		if (scaleOutOfBounds(newScale))
			return;  // DO NOT ZOOM ANY FARTHER
		final double oldZoom = getZoom();
		scale = newScale;
		setCenter(ctrX, ctrY);
		double newOriginX = centerX - (scale * (image.getWidth()/2.0));  // TODO - divide by scale ???? 
		double newOriginY = centerY - (scale * (image.getHeight()/2.0)); 
		setOrigin(newOriginX, newOriginY);
		Events.publish(new ZoomEvent(this, oldZoom, getZoom()));
		repaint();
	}
	
	// Zooms an image in the panel by repainting it at the new zoom level.
	// The current mouse position is the zooming center.
	private void zoomOverMousePoint(double newScale) {
		final RealCoords imageP = panelToImageCoords(ptToCoords(mousePosition));
		doZoom(newScale, imageP.x, imageP.y);
	}

	// -- PUBLIC ZOOM CODE METHODS --

	public double getZoomCtrX() {
		return centerX;
	}

	public double getZoomCtrY() {
		return centerY;
	}

	/**
	 * <p>
	 * Gets the current zoom level.
	 * </p>
	 * 
	 * @return the current zoom level
	 */
	@Override
	public double getZoom() {
		return scale / initialScale;
	}

	@Override
	public double getZoomMultiplier() {
		return zoomMultiplier;
	}

	/**
	 * <p>
	 * Sets a new zooming scale multiplier value.
	 * </p>
	 * 
	 * @param newZoomMultiplier new zoom multiplier value
	 */
	@Override
	public void setZoomMultiplier(double newZoomMultiplier) {
		if (newZoomMultiplier <= 1)
			throw new IllegalArgumentException("zoom multiplier must be > 1");
		
		zoomMultiplier = newZoomMultiplier;
	}

	public void zoomIn() {
		doZoom(scale * zoomMultiplier, centerX, centerY);
	}

	public void zoomIn(double newCenterX, double newCenterY) {
		doZoom(scale * zoomMultiplier, newCenterX, newCenterY);
	}

	public void zoomOut() {
		doZoom(scale / zoomMultiplier, centerX, centerY);
	}

	public void zoomOut(double newCenterX, double newCenterY) {
		doZoom(scale / zoomMultiplier, newCenterX, newCenterY);
	}

	@Override
	public void zoomToFit(Rect region) {
		double xRatio = image.getWidth() / region.width;
		double yRatio = image.getHeight() / region.height;
		double newScale = Math.max(xRatio, yRatio);
		double newCtrX = region.x + 0.5 * region.width;
		double newCtrY = region.y + 0.5 * region.height;
		doZoom(newScale, newCtrX, newCtrY);
	}

	/**
	 * <p>
	 * Sets the zoom level used to display the image.
	 * </p>
	 * <p>
	 * This method is used in programmatic zooming. The zooming center is the
	 * point of the image closest to the center of the panel. After a new zoom
	 * level is set the image is repainted.
	 * </p>
	 * 
	 * @param newZoom the zoom level used to display this panel's image.
	 */
	@Override
	public void setZoom(final double newZoom) {
		if (newZoom < 0)
			throw new IllegalArgumentException("zoom cannot be negative");
		
		double newCenterX = image.getWidth() / 2.0;
		double newCenterY = image.getHeight() / 2.0;
		
		double zoom = newZoom;
		if (newZoom == 0)
			zoom = initialScale;
		
		setZoom(zoom, newCenterX, newCenterY);
	}

	/**
	 * <p>
	 * Sets the zoom level used to display the image, and the zooming center,
	 * around which zooming is done.
	 * </p>
	 * <p>
	 * This method is used in programmatic zooming. After a new zoom level is set
	 * the image is repainted.
	 * </p>
	 * 
	 * @param newZoom the zoom level used to display this panel's image.
	 */
	@Override
	public void setZoom(final double newZoom, final double ctrX, double ctrY) {
		final RealCoords zoomingCenter = new RealCoords(ctrX, ctrY);
		RealCoords imageP = panelToImageCoords(zoomingCenter);
		clipToImageBoundaries(imageP); 
		//double calculatedScale = zoomToScale(newZoom);
		//doZoom(calculatedScale, imageP.x, imageP.y);
		doZoom(newZoom, imageP.x, imageP.y);
	}

	// -- PAN CODE --
	
	@Override
	public double getPanX() {
		return originX;
	}

	@Override
	public double getPanY() {
		return originY;
	}

	/** Pans the image by the given (X, Y) amount. */
	@Override
	public void pan(final double xDelta, final double yDelta) {
		setOrigin(originX + xDelta, originY + yDelta);
		setCenter(centerX + xDelta, centerY + yDelta);
		repaint();
	}

	@Override
	public void setPan(double ox, double oy) {
		pan(ox - originX, oy - originY);
	}
	
	private void addMouseListeners() {
		addMouseListener(new MouseAdapter() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void mousePressed(final MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (isInNavigationImage(e.getPoint())) {
						final Point p = e.getPoint();
						final RealCoords realCoords = ptToCoords(p);
						final IntCoords intCoords = new IntCoords(realCoords.getIntX(), realCoords.getIntY());
						displayImageAt(intCoords);
					}
				}
			}

		});

		addMouseMotionListener(new MouseMotionListener() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void mouseDragged(final MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) &&
					!isInNavigationImage(e.getPoint()))
				{
					// TODO - clean up this section, to fully remove pan support
					// in favor of the pan tool way of doing things
//	  				Point p = e.getPoint();
//	  				moveImage(p);
				}
			}

			@SuppressWarnings("synthetic-access")
			@Override
			public void mouseMoved(final MouseEvent e) {
				// we need the mouse position so that after zooming
				// that position of the image is maintained
				mousePosition = e.getPoint();
//				Coords coord = panelToImageCoords(mousePosition);
//
//				// Display current pixel value in StatusBar
//				//sampleImage.getRaster().getDataBuffer(). .getPixel(coord.getIntX(), coord.getIntY(), dArray)
//				if (isInImage(mousePosition)) {
//					int value = image.getRGB(coord.getIntX(), coord.getIntY());
//					Events.publish(new StatusEvent("Pixel: (" + coord.getIntX() + "," + coord.getIntY() + ") "
//							+ pixelARGBtoString(value)));
//				}
			}

		});
	}

	public String pixelARGBtoString(final int pixel) {
		final int alpha = (pixel >> 24) & 0xff;
		final int red = (pixel >> 16) & 0xff;
		final int green = (pixel >> 8) & 0xff;
		final int blue = (pixel) & 0xff;
		return "" + alpha + ", " + red + ", " + green + ", " + blue;
	}

	/**
	 * Sets an image for display in the panel.
	 * 
	 * @param newImage an image to be set in the panel
	 */
	@Override
	public void setImage(final BufferedImage newImage) {
		final BufferedImage oldImage = image;
		image = toCompatibleImage(newImage);
		int imageW = image.getWidth();
		int imageH = image.getHeight();
		setPreferredSize(new Dimension(imageW, imageH));
		firePropertyChange(IMAGE_CHANGED_PROPERTY, oldImage, image);
		repaint();
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	/** Tests whether an image uses the standard RGB color space. */
	public static boolean isStandardRGBImage(final BufferedImage bImage) {
		return bImage.getColorModel().getColorSpace().isCS_sRGB();
	}

	private static BufferedImage toCompatibleImage(final BufferedImage image) {
		if (image.getColorModel().equals(CONFIGURATION.getColorModel())) {
			return image;
		}
		final BufferedImage compatibleImage =
			CONFIGURATION.createCompatibleImage(image.getWidth(), image.getHeight(),
				image.getTransparency());
		final Graphics g = compatibleImage.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return compatibleImage;
	}

	private static final GraphicsConfiguration CONFIGURATION =
		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration();

	@Override
	public void updateImage() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void addEventDispatcher(final EventDispatcher dispatcher) {
		addKeyListener((AWTEventDispatcher) dispatcher);
		addMouseListener((AWTEventDispatcher) dispatcher);
		addMouseMotionListener((AWTEventDispatcher) dispatcher);
		addMouseWheelListener((AWTEventDispatcher) dispatcher);
	}

	/*
	 * Handles setting of the cursor depending on the activated tool
	 */
	@Override
	public void subscribeToToolEvents() {
		Events.subscribe(ToolActivatedEvent.class, this);
	}

	@Override
	public void setCursor(final MouseCursor cursor) {
		final int cursorCode = AWTCursors.getCursorCode(cursor);
		setCursor(Cursor.getPredefinedCursor(cursorCode));
	}

	// Converts this panel's coordinates into the original image coordinates
	@Override
	public RealCoords panelToImageCoords(final RealCoords p) {
		return new RealCoords((p.x - originX) / scale, (p.y - originY) / scale);
	}

	// Converts the original image coordinates into this panel's coordinates
	@Override
	public RealCoords imageToPanelCoords(final RealCoords p) {
		return new RealCoords((p.x * scale) + originX, (p.y * scale) + originY);
	}

	// Tests whether a given point in the panel falls within the image boundaries.
	@Override
	public boolean isInImage(final RealCoords p) {
		final RealCoords coords = panelToImageCoords(p);
		final int x = coords.getIntX();
		final int y = coords.getIntY();
		return (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight());
	}

	// Tests whether the image is displayed in its entirety in the panel.
	private boolean isFullImageInPanel() {
		return (originX >= 0 && (originX + getScreenImageWidth()) < getWidth() &&
			originY >= 0 && (originY + getScreenImageHeight()) < getHeight());
	}


	/**
	 * <p>
	 * Indicates whether the high quality rendering feature is enabled.
	 * </p>
	 * 
	 * @return true if high quality rendering is enabled, false otherwise.
	 */
	@Override
	public boolean isHighQualityRenderingEnabled() {
		return highQualityRenderingEnabled;
	}

	/**
	 * <p>
	 * Enables/disables high quality rendering.
	 * </p>
	 * 
	 * @param enabled enables/disables high quality rendering
	 */
	@Override
	public void setHighQualityRenderingEnabled(final boolean enabled) {
		highQualityRenderingEnabled = enabled;
	}

	// High quality rendering kicks in when when a scaled image is larger
	// than the original image. In other words,
	// when image decimation stops and interpolation starts.
	private boolean isHighQualityRendering() {
		return (highQualityRenderingEnabled && scale > HIGH_QUALITY_RENDERING_SCALE_THRESHOLD);
	}

	/**
	 * Gets the bounds of the image area currently displayed in the panel (in
	 * image coordinates).
	 */
	private Rectangle getImageClipBounds() {
		final RealCoords startCoords = panelToImageCoords(new RealCoords(0, 0));
		final RealCoords endCoords =
			panelToImageCoords(new RealCoords(getWidth() - 1, getHeight() - 1));
		final int panelX1 = startCoords.getIntX();
		final int panelY1 = startCoords.getIntY();
		final int panelX2 = endCoords.getIntX();
		final int panelY2 = endCoords.getIntY();
		// No intersection?
		if (panelX1 >= image.getWidth() || panelX2 < 0 ||
			panelY1 >= image.getHeight() || panelY2 < 0)
		{
			return null;
		}

		final int x1 = (panelX1 < 0) ? 0 : panelX1;
		final int y1 = (panelY1 < 0) ? 0 : panelY1;
		final int x2 =
			(panelX2 >= image.getWidth()) ? image.getWidth() - 1 : panelX2;
		final int y2 =
			(panelY2 >= image.getHeight()) ? image.getHeight() - 1 : panelY2;
		return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
	}

	/**
	 * Paints the panel and its image at the current zoom level, location, and
	 * interpolation method dependent on the image scale.</p>
	 * 
	 * @param g the <code>Graphics</code> context for painting
	 */
	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g); // Paints the background
		if (image == null) {
			return;
		}
		if (scale == 0.0) {
			initializeParams();
		}
		if (isHighQualityRendering()) {
			final Rectangle rect = getImageClipBounds();
			if (rect == null || rect.width == 0 || rect.height == 0) { // no part of
																																	// image is
																																	// displayed
																																	// in the
																																	// panel
				return;
			}
			final BufferedImage subimage =
				image.getSubimage(rect.x, rect.y, rect.width, rect.height);
			final Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, INTERPOLATION_TYPE);
			g2.drawImage(subimage, (int)Math.max(0, originX), (int)Math.max(0, originY), Math
				.min((int) (subimage.getWidth() * scale), getWidth()), Math.min(
				(int) (subimage.getHeight() * scale), getHeight()), null);
		}
		else {
			g.drawImage(image, (int)originX, (int)originY, getScreenImageWidth(),
				getScreenImageHeight(), null);
		}
		drawNavigationImage(g);
	}

	private void addResizeListener() {
		// Handle component resizing
		addComponentListener(new ComponentAdapter() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void componentResized(final ComponentEvent e) {
				if (scale > 0.0) {
					if (isFullImageInPanel()) {
						centerImage();
					}
					else if (isImageEdgeInPanel()) {
						scaleOrigin();
					}
					if (isNavigationImageEnabled()) {
						createNavigationImage();
					}
					repaint();
				}
				previousPanelSize = getSize();
			}

		});
	}

	// Used when the image is resized.
	private boolean isImageEdgeInPanel() {
		if (previousPanelSize == null) {
			return false;
		}
		return (originX > 0 && originX < previousPanelSize.width || originY > 0 &&
			originY < previousPanelSize.height);
	}

	// Used when the panel is resized
	private void scaleOrigin() {
		double newOriginX = originX * getWidth() / previousPanelSize.width;
		double newOriginY = originY * getHeight() / previousPanelSize.height;
		setOrigin(newOriginX, newOriginY);
		repaint();
	}

	private static final double SCREEN_NAV_IMAGE_FACTOR = 0.15; // 15% of panel's
																															// width
	private static final double NAV_IMAGE_FACTOR = 0.3; // 30% of panel's width
	private double navZoomFactor = 1.2;
	private double navScale = 0;
	private boolean navigationImageEnabled = false;  // TODO - enable with a hotkey?????
	private BufferedImage navigationImage;
	private int navImageWidth;
	private int navImageHeight;

	// Creates and renders the navigation image in the upper let corner of the
	// panel.
	private void createNavigationImage() {
		// We keep the original navigation image larger than initially
		// displayed to allow for zooming into it without pixellation effect.
		navImageWidth = (int) (getWidth() * NAV_IMAGE_FACTOR);
		navImageHeight = navImageWidth * image.getHeight() / image.getWidth();
		final int scrNavImageWidth = (int) (getWidth() * SCREEN_NAV_IMAGE_FACTOR);
		// int scrNavImageHeight = scrNavImageWidth * image.getHeight() /
		// image.getWidth();
		navScale = (double) scrNavImageWidth / navImageWidth;

		final GraphicsEnvironment ge =
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsDevice gs = ge.getDefaultScreenDevice();
		final GraphicsConfiguration gc = gs.getDefaultConfiguration();
		navigationImage =
			gc.createCompatibleImage(navImageWidth, navImageHeight,
				Transparency.OPAQUE);

		final Graphics g = navigationImage.getGraphics();
		g.drawImage(image, 0, 0, navImageWidth, navImageHeight, null);
	}

	private int getScreenNavImageWidth() {
		return (int) (navScale * navImageWidth);
	}

	private int getScreenNavImageHeight() {
		return (int) (navScale * navImageHeight);
	}

	// Converts the navigation image coordinates into the zoomed image coordinates
	private IntCoords navToZoomedImageCoords(final IntCoords p) {
		final int x = p.x * getScreenImageWidth() / getScreenNavImageWidth();
		final int y = p.y * getScreenImageHeight() / getScreenNavImageHeight();
		return new IntCoords(x, y);
	}

	// The user clicked within the navigation image and this part of the image
	// is displayed in the panel. The clicked point of the image is centered in
	// the panel.
	private void displayImageAt(final IntCoords p) {
		final IntCoords scrImagePoint = navToZoomedImageCoords(p);
		originX = -(scrImagePoint.x - getWidth() / 2);
		originY = -(scrImagePoint.y - getHeight() / 2);
		// TODO - fix me for center
		repaint();
	}

	private RealCoords ptToCoords(final Point p) {
		return new RealCoords(p.x, p.y);
	}

	/**
	 * <p>
	 * Indicates whether navigation image is enabled.
	 * <p>
	 * 
	 * @return true when navigation image is enabled, false otherwise.
	 */
	@Override
	public boolean isNavigationImageEnabled() {
		return navigationImageEnabled;
	}

	/**
	 * <p>
	 * Enables/disables navigation with the navigation image.
	 * </p>
	 * <p>
	 * Navigation image should be disabled when custom, programmatic navigation is
	 * implemented.
	 * </p>
	 * 
	 * @param enabled true when navigation image is enabled, false otherwise.
	 */
	@Override
	public void setNavigationImageEnabled(final boolean enabled) {
		navigationImageEnabled = enabled;
		repaint();
	}

	// Tests whether a given point in the panel falls within the navigation image
	// boundaries.
	private boolean isInNavigationImage(final Point p) {
		return (isNavigationImageEnabled() && p.x < getScreenNavImageWidth() && p.y < getScreenNavImageHeight());
	}

	// Zooms the navigation image

	private void zoomNavigationImage() {
		navScale *= navZoomFactor;
		repaint();
	}

	private void drawNavigationImage(final Graphics g) {
		// Draw navigation image
		if (isNavigationImageEnabled()) {
			g.drawImage(navigationImage, 0, 0, getScreenNavImageWidth(),
				getScreenNavImageHeight(), null);
			g.setColor(Color.blue);
			g.drawRect(0, 0, getScreenNavImageWidth(), getScreenNavImageHeight());
			drawZoomAreaOutline(g);
		}
	}

	// Paints a white outline over the navigation image indicating
	// the area of the image currently displayed in the panel.
	private void drawZoomAreaOutline(final Graphics g) {
		if (isFullImageInPanel()) {
			return;
		}
		final int x = (int)
			(-originX * getScreenNavImageWidth() / getScreenImageWidth());
		final int y = (int)
			(-originY * getScreenNavImageHeight() / getScreenImageHeight());
		final int width =
			getWidth() * getScreenNavImageWidth() / getScreenImageWidth();
		final int height =
			getHeight() * getScreenNavImageHeight() / getScreenImageHeight();
		g.setColor(Color.white);
		g.drawRect(x, y, width, height);
	}

	// -- Zooming --

	private WheelZoomDevice wheelZoomDevice = null;
	private ButtonZoomDevice buttonZoomDevice = null;

	@SuppressWarnings("synthetic-access")
	private void addWheelZoomDevice() {
		if (wheelZoomDevice == null) {
			wheelZoomDevice = new WheelZoomDevice();
			addMouseWheelListener(wheelZoomDevice);
		}
	}

	@SuppressWarnings("synthetic-access")
	private void addButtonZoomDevice() {
		if (buttonZoomDevice == null) {
			buttonZoomDevice = new ButtonZoomDevice();
			addMouseListener(buttonZoomDevice);
		}
	}

	private void removeWheelZoomDevice() {
		if (wheelZoomDevice != null) {
			removeMouseWheelListener(wheelZoomDevice);
			wheelZoomDevice = null;
		}
	}

	private void removeButtonZoomDevice() {
		if (buttonZoomDevice != null) {
			removeMouseListener(buttonZoomDevice);
			buttonZoomDevice = null;
		}
	}

	/**
	 * <p>
	 * Sets a new zoom device.
	 * </p>
	 * 
	 * @param newZoomDevice specifies the type of a new zoom device.
	 */
	public void setZoomDevice(final ZoomDevice newZoomDevice) {
		if (newZoomDevice == ZoomDevice.NONE) {
			removeWheelZoomDevice();
			removeButtonZoomDevice();
		}
		else if (newZoomDevice == ZoomDevice.MOUSE_BUTTON) {
			removeWheelZoomDevice();
			addButtonZoomDevice();
		}
		else if (newZoomDevice == ZoomDevice.MOUSE_WHEEL) {
			removeButtonZoomDevice();
			addWheelZoomDevice();
		}
	}

	/**
	 * <p>
	 * Gets the current zoom device.
	 * </p>
	 */
	public ZoomDevice getZoomDevice() {
		if (buttonZoomDevice != null) {
			return ZoomDevice.MOUSE_BUTTON;
		}
		else if (wheelZoomDevice != null) {
			return ZoomDevice.MOUSE_WHEEL;
		}
		else {
			return ZoomDevice.NONE;
		}
	}


	// -- EventSubscriber methods --

	@Override
	public void onEvent(final ToolActivatedEvent event) {
		setCursor(event.getTool().getCursor());
	}

	// -- Helper classes --

	/**
	 * <p>
	 * Defines zoom devices.
	 * </p>
	 */
	public static class ZoomDevice {

		/**
		 * <p>
		 * Identifies that the panel does not implement zooming, but the component
		 * using the panel does (programmatic zooming method).
		 * </p>
		 */
		public static final ZoomDevice NONE = new ZoomDevice("none");
		/**
		 * <p>
		 * Identifies the left and right mouse buttons as the zooming device.
		 * </p>
		 */
		public static final ZoomDevice MOUSE_BUTTON =
			new ZoomDevice("mouseButton");
		/**
		 * <p>
		 * Identifies the mouse scroll wheel as the zooming device.
		 * </p>
		 */
		public static final ZoomDevice MOUSE_WHEEL = new ZoomDevice("mouseWheel");
		private final String zoomDevice;

		private ZoomDevice(final String zoomDevice) {
			this.zoomDevice = zoomDevice;
		}

		@Override
		public String toString() {
			return zoomDevice;
		}

	}

	private class WheelZoomDevice implements MouseWheelListener {

		@SuppressWarnings("synthetic-access")
		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			final Point p = e.getPoint();
			final boolean zoomIn = (e.getWheelRotation() < 0);

			if (isInNavigationImage(p)) {
				if (zoomIn)
					navZoomFactor = 1.2;
				else
					navZoomFactor = 0.8;
				zoomNavigationImage();
			}
			else if (isInImage(ptToCoords(p))) {
				double newScale;
				if (zoomIn)
					newScale = scale * zoomMultiplier;
				else
					newScale = scale / zoomMultiplier;
				zoomOverMousePoint(newScale);
			}
		}

	}

	private class ButtonZoomDevice extends MouseAdapter {

		@SuppressWarnings("synthetic-access")
		@Override
		public void mouseClicked(final MouseEvent e) {
			final Point p = e.getPoint();
			final boolean zoomIn = SwingUtilities.isLeftMouseButton(e);
			
			if (isInNavigationImage(p)) {
				if (zoomIn)
					navZoomFactor = 1.2;
				else
					navZoomFactor = 0.8;
				zoomNavigationImage();
			}
			else if (isInImage(ptToCoords(p))) {
				double newScale;
				if (zoomIn)
					newScale = scale * zoomMultiplier;
				else
					newScale = scale / zoomMultiplier;
				zoomOverMousePoint(newScale);
			}
		}

	}
}
