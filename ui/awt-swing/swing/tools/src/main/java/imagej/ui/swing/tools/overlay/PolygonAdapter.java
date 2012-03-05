//
// PolygonAdapter.java
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
import imagej.data.overlay.PolygonOverlay;
import imagej.ext.plugin.Plugin;
import imagej.ext.tool.Tool;
import imagej.ui.swing.overlay.IJBezierTool;
import imagej.ui.swing.overlay.JHotDrawOverlayAdapter;
import imagej.ui.swing.overlay.JHotDrawTool;
import imagej.ui.swing.overlay.OverlayCreatedListener;
import imagej.ui.swing.overlay.PolygonFigure;
import imagej.util.Log;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Arrays;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.PolygonRegionOfInterest;

import org.jhotdraw.draw.AttributeKeys;
import org.jhotdraw.draw.BezierFigure;
import org.jhotdraw.draw.Figure;
import org.jhotdraw.geom.BezierPath.Node;

/**
 * TODO
 * 
 * @author Lee Kamentsky
 * @author Barry DeZonia
 */
@Plugin(type = Tool.class, name = "Polygon", description = "Polygon overlays",
	iconPath = "/icons/tools/polygon.png", priority = PolygonAdapter.PRIORITY,
	enabled = true)
@JHotDrawOverlayAdapter(priority = PolygonAdapter.PRIORITY)
public class PolygonAdapter extends
	AbstractJHotDrawOverlayAdapter<PolygonOverlay, PolygonFigure>
{

	public static final int PRIORITY = EllipseAdapter.PRIORITY - 1;

	static private PolygonOverlay downcastOverlay(final Overlay overlay) {
		assert overlay instanceof PolygonOverlay;
		return (PolygonOverlay) overlay;
	}

	@Override
	public boolean supports(final Overlay overlay, final Figure figure) {
		if ((figure != null) && (!(figure instanceof PolygonFigure))) return false;
		return overlay instanceof PolygonOverlay;
	}

	@Override
	public Overlay createNewOverlay() {
		final PolygonOverlay o = new PolygonOverlay(getContext());
		return o;
	}

	@Override
	public Figure createDefaultFigure() {
		@SuppressWarnings("serial")
		final BezierFigure figure = new PolygonFigure() {

			// Make sure that the lines are always drawn 1 pixel wide
			@Override
			public void draw(final Graphics2D g) {
				set(AttributeKeys.STROKE_WIDTH, new Double(1 / g.getTransform()
					.getScaleX()));
				super.draw(g);
			}
		};
		figure.set(AttributeKeys.FILL_COLOR, getDefaultFillColor());
		figure.set(AttributeKeys.STROKE_COLOR, getDefaultStrokeColor());
		// Avoid IllegalArgumentException: miter limit < 1 on the EDT
		figure.set(AttributeKeys.IS_STROKE_MITER_LIMIT_FACTOR, false);
		return figure;
	}

	@Override
	public void updateOverlay(final PolygonFigure figure, final OverlayView overlay) {
		super.updateOverlay(figure, overlay);
		final PolygonOverlay poverlay = downcastOverlay(overlay.getData());
		final PolygonRegionOfInterest roi = poverlay.getRegionOfInterest();
		final int nodeCount = figure.getNodeCount();
		while (roi.getVertexCount() > nodeCount) {
			roi.removeVertex(nodeCount);
			Log.debug("Removed node from overlay.");
		}
		for (int i = 0; i < nodeCount; i++) {
			final Node node = figure.getNode(i);
			final double[] position = new double[] { node.x[0], node.y[0] };
			if (roi.getVertexCount() == i) {
				roi.addVertex(i, new RealPoint(position));
				Log.debug("Added node to overlay");
			}
			else {
				if ((position[0] != roi.getVertex(i).getDoublePosition(0)) ||
					(position[1] != roi.getVertex(i).getDoublePosition(1)))
				{
					Log.debug(String.format("Vertex # %d moved to %f,%f", i + 1,
						position[0], position[1]));
				}
				roi.setVertexPosition(i, position);
			}
		}
	}

	@Override
	public void updateFigure(final OverlayView overlay, final PolygonFigure figure) {
		super.updateFigure(overlay, figure);
		final PolygonOverlay pOverlay = downcastOverlay(overlay.getData());
		final PolygonRegionOfInterest roi = pOverlay.getRegionOfInterest();
		final int vertexCount = roi.getVertexCount();
		while (figure.getNodeCount() > vertexCount)
			figure.removeNode(vertexCount);
		for (int i = 0; i < vertexCount; i++) {
			final RealLocalizable vertex = roi.getVertex(i);
			if (figure.getNodeCount() == i) {
				final Node node =
					new Node(vertex.getDoublePosition(0), vertex.getDoublePosition(1));
				figure.addNode(node);
			}
			else {
				final Node node = figure.getNode(i);
				node.mask = 0;
				Arrays.fill(node.x, vertex.getDoublePosition(0));
				Arrays.fill(node.y, vertex.getDoublePosition(1));
			}
		}
	}

	@Override
	public JHotDrawTool getCreationTool(final ImageDisplay display,
		final OverlayCreatedListener listener)
	{
		return new IJBezierTool(display, this, listener);
	}

	@Override
	public Shape toShape(final PolygonFigure figure) {
		return figure.getBezierPath().toGeneralPath();
	}

}
