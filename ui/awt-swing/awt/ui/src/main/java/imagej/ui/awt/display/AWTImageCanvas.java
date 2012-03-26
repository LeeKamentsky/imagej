
package imagej.ui.awt.display;

import imagej.ImageJ;
import imagej.data.display.CanvasHelper;
import imagej.data.display.ImageCanvas;
import imagej.data.display.ImageDisplay;
import imagej.event.EventHandler;
import imagej.event.EventService;
import imagej.event.EventSubscriber;
import imagej.ext.MouseCursor;
import imagej.ext.tool.event.ToolActivatedEvent;
import imagej.ui.common.awt.AWTCursors;
import imagej.util.IntCoords;
import imagej.util.IntRect;
import imagej.util.Log;
import imagej.util.RealCoords;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * An AWT implementation of {@link ImageCanvas}.
 * <p>
 * This code is based on <a
 * href="http://today.java.net/article/2007/03/23/navigable-image-panel">Slav
 * Boleslawski's NavigableImagePanel</a>.
 * </p>
 * 
 * @author Grant Harris
 * @author Curtis Rueden
 * @author Barry DeZonia
 */
public class AWTImageCanvas extends Panel implements ImageCanvas {

	private static final double MAX_SCREEN_PROPORTION = 0.85;

	private static final double HIGH_QUALITY_RENDERING_SCALE_THRESHOLD = 1.0;

	private static final Object INTERPOLATION_TYPE =
	// TODO - put this back?? //RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR; // this is like IJ1

	private final ImageDisplay display;

	private final CanvasHelper canvasHelper;

	private boolean highQualityRenderingEnabled = true;

	private BufferedImage image;

	/** Initially computed scale factor. */
	private double initialScale = 0;

	private Dimension previousPanelSize;

	@SuppressWarnings("unused")
	private List<EventSubscriber<?>> subscribers;

	/** Creates an image canvas with no default image. */
	public AWTImageCanvas() {
		display = null; // TODO
		canvasHelper = new CanvasHelper(this);
//		setOpaque(false);
		addResizeListener();
		subscribers = ImageJ.get(EventService.class).subscribe(this);
	}

	/** Creates an image canvas with the specified image. */
	public AWTImageCanvas(final BufferedImage image) {
		this();
		setImage(image);
	}

	// -- AWTImageCanvas methods --

	/**
	 * Indicates whether the high quality rendering feature is enabled.
	 * 
	 * @return true if high quality rendering is enabled, false otherwise.
	 */
	public boolean isHighQualityRenderingEnabled() {
		return highQualityRenderingEnabled;
	}

	/**
	 * Enables/disables high quality rendering.
	 * 
	 * @param enabled enables/disables high quality rendering
	 */
	public void setHighQualityRenderingEnabled(final boolean enabled) {
		highQualityRenderingEnabled = enabled;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(final BufferedImage newImage) {
		image = newImage;
		final Dimension maxDims = calcMaxAllowableDimensions();
		final Dimension dimensions =
			calcReasonableDimensions(maxDims, image.getWidth(), image.getHeight());
		setPreferredSize(dimensions);
		repaint();
	}

	// -- Component methods --

	@Override
	public void paint(final Graphics g) {
		super.paint(g); // paint the background

		if (image == null) return;

		final double scale = canvasHelper.getZoomFactor();
		if (scale == 0.0) initializeParams();

		if (isHighQualityRendering()) {
			final IntRect rect = getImageClipBounds();

			// if no part of image is displayed in the panel
			if (rect == null || rect.width == 0 || rect.height == 0) return;

			final BufferedImage subImage =
				image.getSubimage(rect.x, rect.y, rect.width, rect.height);

			final Graphics2D g2 = (Graphics2D) g;

			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, INTERPOLATION_TYPE);

			final IntCoords offset = canvasHelper.getPanOrigin();
			final int x = Math.max(0, offset.x);
			final int y = Math.max(0, offset.y);
			final int width =
				Math.min((int) (scale * subImage.getWidth()), getWidth());
			final int height =
				Math.min((int) (scale * subImage.getHeight()), getHeight());
			g2.drawImage(subImage, x, y, width, height, this);

			Log.debug("HIGH QUALITY CASE: origin=(" + x + ", " + y + "), size=(" +
				width + ", " + height + ")");
		}
		else {
			final IntCoords offset = canvasHelper.getPanOrigin();
			final int x = offset.x;
			final int y = offset.y;
			final int width = getScreenImageWidth();
			final int height = getScreenImageHeight();
			g.drawImage(image, x, y, width, height, null);

			Log.debug("LOW QUALITY CASE: origin=(" + x + ", " + y + "), size=(" +
				width + ", " + height + ")");
		}
	}

	// -- ImageCanvas methods --

	@Override
	public ImageDisplay getDisplay() {
		return display;
	}

	@Override
	public int getCanvasWidth() {
		return image.getWidth();
	}

	@Override
	public int getCanvasHeight() {
		return image.getHeight();
	}

	@Override
	public int getViewportWidth() {
		return getWidth();
	}

	@Override
	public int getViewportHeight() {
		return getHeight();
	}

	@Override
	public boolean isInImage(final IntCoords point) {
		return canvasHelper.isInImage(point);
	}

	@Override
	public RealCoords panelToImageCoords(final IntCoords panelCoords) {
		return canvasHelper.panelToImageCoords(panelCoords);
	}

	@Override
	public IntCoords imageToPanelCoords(final RealCoords imageCoords) {
		return canvasHelper.imageToPanelCoords(imageCoords);
	}

	@Override
	public void setCursor(final MouseCursor cursor) {
		setCursor(AWTCursors.getCursor(cursor));
	}

	// -- Pannable methods --

	@Override
	public void pan(final IntCoords delta) {
		canvasHelper.pan(delta);
		repaint();
	}

	@Override
	public void setPan(final IntCoords origin) {
		canvasHelper.setPan(origin);
		repaint();
	}

	@Override
	public void panReset() {
		canvasHelper.panReset();
	}

	@Override
	public IntCoords getPanOrigin() {
		return canvasHelper.getPanOrigin();
	}

	// -- Zoomable methods --

	@Override
	public void setZoom(final double factor) {
		canvasHelper.setZoom(factor);
	}

	@Override
	public void setZoom(final double factor, final IntCoords center) {
		final double desiredScale = factor == 0 ? initialScale : factor;
		canvasHelper.setZoom(desiredScale, center);
		repaint();
	}

	@Override
	public void zoomIn() {
		canvasHelper.zoomIn();
	}

	@Override
	public void zoomIn(final IntCoords center) {
		canvasHelper.zoomIn(center);
	}

	@Override
	public void zoomOut() {
		canvasHelper.zoomOut();
	}

	@Override
	public void zoomOut(final IntCoords center) {
		canvasHelper.zoomOut(center);
	}

	@Override
	public void zoomToFit(final IntCoords topLeft, final IntCoords bottomRight) {
		canvasHelper.zoomToFit(topLeft, bottomRight);
	}

	@Override
	public double getZoomFactor() {
		return canvasHelper.getZoomFactor();
	}

	// -- Event handlers --

	@EventHandler
	protected void onEvent(final ToolActivatedEvent event) {
		setCursor(event.getTool().getCursor());
	}

	// -- Helper methods --

	private void addResizeListener() {
		addComponentListener(new ComponentAdapter() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void componentResized(final ComponentEvent e) {
				final double scale = canvasHelper.getZoomFactor();
				if (scale > 0) {
					if (isFullImageInPanel()) {
						centerImage();
					}
					else if (isImageEdgeInPanel()) {
						scaleOrigin();
					}
				}
				previousPanelSize = getSize();
			}
		});
	}

	/** Tests whether the image is displayed in its entirety in the panel. */
	private boolean isFullImageInPanel() {
		final IntCoords offset = canvasHelper.getPanOrigin();
		return offset.x >= 0 && offset.x + getScreenImageWidth() < getWidth() &&
			offset.y >= 0 && offset.y + getScreenImageHeight() < getHeight();
	}

	/** Used when the image is resized. */
	private boolean isImageEdgeInPanel() {
		if (previousPanelSize == null) return false;
		final IntCoords offset = canvasHelper.getPanOrigin();
		return offset.x > 0 && offset.x < previousPanelSize.width || offset.y > 0 &&
			offset.y < previousPanelSize.height;
	}

	/** Centers the current image in the panel. */
	private void centerImage() {
		final int offsetX = (getWidth() - getScreenImageWidth()) / 2;
		final int offsetY = (getHeight() - getScreenImageHeight()) / 2;
		setPan(new IntCoords(offsetX, offsetY));
	}

	/** Used when the panel is resized. */
	private void scaleOrigin() {
		final IntCoords offset = canvasHelper.getPanOrigin();
		offset.x = offset.x * getWidth() / previousPanelSize.width;
		offset.y = offset.y * getHeight() / previousPanelSize.height;
		setPan(offset);
	}

	/**
	 * Gets the bounds of the image area currently displayed in the panel (in
	 * image coordinates).
	 */
	private IntRect getImageClipBounds() {
		final RealCoords startCoords = panelToImageCoords(new IntCoords(0, 0));
		final RealCoords endCoords =
			panelToImageCoords(new IntCoords(getWidth() - 1, getHeight() - 1));
		final int panelX1 = startCoords.getIntX();
		final int panelY1 = startCoords.getIntY();
		final int panelX2 = endCoords.getIntX();
		final int panelY2 = endCoords.getIntY();

		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();

		if (panelX1 >= imageWidth || panelX2 < 0 || panelY1 >= imageHeight ||
			panelY2 < 0)
		{
			// no intersection
			return null;
		}

		final int x1 = panelX1 < 0 ? 0 : panelX1;
		final int y1 = panelY1 < 0 ? 0 : panelY1;
		final int x2 = panelX2 >= imageWidth ? imageWidth - 1 : panelX2;
		final int y2 = panelY2 >= imageHeight ? imageHeight - 1 : panelY2;
		return new IntRect(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
	}

	/** Gets the width of the image, scaled by the zoom factor. */
	private int getScreenImageWidth() {
		final double scale = canvasHelper.getZoomFactor();
		return (int) (scale * image.getWidth());
	}

	/** Gets the height of the image, scaled by the zoom factor. */
	private int getScreenImageHeight() {
		final double scale = canvasHelper.getZoomFactor();
		return (int) (scale * image.getHeight());
	}

	/** Called from {@link #paint} when a new image is set. */
	private void initializeParams() {
		initialScale = calcInitialScale();
		canvasHelper.setPan(new IntCoords(0, 0));
		canvasHelper.setZoom(initialScale);
	}

	/**
	 * High quality rendering kicks in when when a scaled image is larger than the
	 * original image. In other words, when image decimation stops and
	 * interpolation starts.
	 */
	private boolean isHighQualityRendering() {
		return highQualityRenderingEnabled &&
			canvasHelper.getZoomFactor() > HIGH_QUALITY_RENDERING_SCALE_THRESHOLD;
	}

	private Dimension calcMaxAllowableDimensions() {
		final Dimension screenDims = Toolkit.getDefaultToolkit().getScreenSize();
		final double maxAllowedWidth = MAX_SCREEN_PROPORTION * screenDims.width;
		final double maxAllowedHeight = MAX_SCREEN_PROPORTION * screenDims.height;
		return new Dimension((int) maxAllowedWidth, (int) maxAllowedHeight);
	}

	private enum ImageShape {
		TOO_WIDE, TOO_TALL, FITS_FINE
	}

	private ImageShape checkImageShape(final Dimension maxDims, final int width,
		final int height)
	{
		// is image too big to comfortably fit on screen??
		if ((width > maxDims.width) || (height > maxDims.height)) {
			final double windowAspect = ((double) maxDims.width) / maxDims.height;
			final double imageAspect = ((double) width) / height;
			if (imageAspect > windowAspect) {
				// width is the problem dimension
				return ImageShape.TOO_WIDE;
			}
			// else imageAspect <= windowAspect
			// height is the problem dimension
			return ImageShape.TOO_TALL;
		}

		return ImageShape.FITS_FINE;
	}

	private Dimension calcReasonableDimensions(final Dimension maxDims,
		final int imageWidth, final int imageHeight)
	{
		int reasonableWidth, reasonableHeight;
		double aspectRatio;
		switch (checkImageShape(maxDims, imageWidth, imageHeight)) {
			case TOO_WIDE:
				aspectRatio = ((double) imageHeight / imageWidth);
				reasonableWidth = maxDims.width;
				reasonableHeight = (int) (aspectRatio * maxDims.width);
				break;
			case TOO_TALL:
				aspectRatio = ((double) imageWidth / imageHeight);
				reasonableHeight = maxDims.height;
				reasonableWidth = (int) (aspectRatio * maxDims.height);
				break;
			default: // fits fine
				reasonableWidth = imageWidth;
				reasonableHeight = imageHeight;
				break;
		}
		return new Dimension(reasonableWidth, reasonableHeight);
	}

	private double calcInitialScale() {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Dimension maxDims = calcMaxAllowableDimensions();
		switch (checkImageShape(maxDims, imageWidth, imageHeight)) {
			case TOO_WIDE:
				return ((double) maxDims.width) / imageWidth;
			case TOO_TALL:
				return ((double) maxDims.height) / imageHeight;
			default: // fits fine
				return 1.0;
		}
	}

}
