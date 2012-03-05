//
// PointAdapter.java
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

package imagej.ui.swing.tools.overlay;

import imagej.data.display.ImageDisplay;
import imagej.data.display.OverlayView;
import imagej.data.overlay.Overlay;
import imagej.data.overlay.PointOverlay;
import imagej.ext.MouseCursor;
import imagej.ext.plugin.Plugin;
import imagej.ext.tool.Tool;
import imagej.ui.swing.overlay.IJCreationTool;
import imagej.ui.swing.overlay.JHotDrawOverlayAdapter;
import imagej.ui.swing.overlay.JHotDrawTool;
import imagej.ui.swing.overlay.OverlayCreatedListener;
import imagej.ui.swing.tools.AngleTool;

import java.awt.Shape;

import net.imglib2.RealPoint;

import org.jhotdraw.draw.AttributeKeys;
import org.jhotdraw.draw.Figure;

/**
 * TODO
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Tool.class, name = "Point", description = "Point overlays",
	iconPath = "/icons/tools/point.png", priority = PointAdapter.PRIORITY,
	enabled = true)
@JHotDrawOverlayAdapter(priority = PointAdapter.PRIORITY)
public class PointAdapter extends AbstractJHotDrawOverlayAdapter<PointOverlay, PointFigure> {

	public static final int PRIORITY = AngleTool.PRIORITY - 1;

	@Override
	public boolean supports(final Overlay overlay, final Figure figure) {
		if (!(overlay instanceof PointOverlay)) return false;
		return (figure == null) || (figure instanceof PointFigure);
	}

	@Override
	public PointOverlay createNewOverlay() {
		return new PointOverlay(getContext());
	}

	@Override
	public Figure createDefaultFigure() {
		final PointFigure figure = new PointFigure();
		figure.set(AttributeKeys.STROKE_COLOR, getDefaultStrokeColor());
		// Avoid IllegalArgumentException: miter limit < 1 on the EDT
		figure.set(AttributeKeys.IS_STROKE_MITER_LIMIT_FACTOR, false);
		return figure;
	}

	@Override
	public void updateFigure(final OverlayView overlayView, final PointFigure figure) {
		super.updateFigure(overlayView, figure);
		assert figure instanceof PointFigure;
		final PointFigure point = (PointFigure) figure;
		final Overlay overlay = overlayView.getData();
		assert overlay instanceof PointOverlay;
		final PointOverlay pointOverlay = (PointOverlay) overlay;
		point.set(
				pointOverlay.getPoint().getDoublePosition(0),
				pointOverlay.getPoint().getDoublePosition(1)
				);
	}

	@Override
	public void updateOverlay(final PointFigure figure, final OverlayView overlayView)
	{
		super.updateOverlay(figure, overlayView);
		assert figure instanceof PointFigure;
		final PointFigure point = (PointFigure) figure;
		final Overlay overlay = overlayView.getData();
		assert overlay instanceof PointOverlay;
		final PointOverlay pointOverlay = (PointOverlay) overlay;
		pointOverlay.setPoint(new RealPoint(new double[] {
			point.getX(), point.getY() }));
	}
	
	@Override
	public MouseCursor getCursor() {
		return MouseCursor.CROSSHAIR;
	}
	
	/*
	@Override
	public void onMouseDown(final MsPressedEvent evt) {
		if (evt.getButton() != MsButtonEvent.LEFT_BUTTON) return;
		evt.consume();
	}

	@Override
	public void onMouseDrag(final MsDraggedEvent evt) {
		if (evt.getButton() != MsButtonEvent.LEFT_BUTTON) return;
		evt.consume();
	}

	@Override
	public void onMouseClick(imagej.ext.display.event.input.MsClickedEvent evt) {
		if (evt.getButton() != MsButtonEvent.LEFT_BUTTON) return;
		evt.consume();
	}
	*/

	@Override
	public JHotDrawTool getCreationTool(final ImageDisplay display,
		final OverlayCreatedListener listener)
	{
		return new IJCreationTool(display, this, listener);
	}
	
	public Shape toShape(final PointFigure figure) {
		throw new UnsupportedOperationException();
	}

}
