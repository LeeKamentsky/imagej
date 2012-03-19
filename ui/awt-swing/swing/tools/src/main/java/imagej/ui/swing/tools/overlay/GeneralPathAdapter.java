//GeneralPathAdapter.java

package imagej.ui.swing.tools.overlay;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Stack;

import net.imglib2.roi.GeneralPathRegionOfInterest;
import net.imglib2.roi.GeneralPathSegmentHandler;

import org.jhotdraw.draw.AttributeKeys;
import org.jhotdraw.draw.BezierFigure;
import org.jhotdraw.draw.Figure;
import org.jhotdraw.geom.BezierPath;

import imagej.data.display.ImageDisplay;
import imagej.data.display.OverlayView;
import imagej.data.overlay.GeneralPathOverlay;
import imagej.data.overlay.Overlay;
import imagej.ext.plugin.Plugin;
import imagej.ext.tool.Tool;
import imagej.ui.swing.overlay.IJBezierTool;
import imagej.ui.swing.overlay.JHotDrawOverlayAdapter;
import imagej.ui.swing.overlay.JHotDrawTool;
import imagej.ui.swing.overlay.OverlayCreatedListener;
import imagej.ui.swing.overlay.PolygonFigure;

/**
* TODO
* 
* @author Lee Kamentsky
* @author Barry DeZonia
*/
@Plugin(type = Tool.class, name = "General Path", description = "General path overlays",
iconPath = "/icons/tools/polygon.png", priority = PolygonAdapter.PRIORITY,
enabled = true)
@JHotDrawOverlayAdapter(priority = GeneralPathAdapter.PRIORITY)
public class GeneralPathAdapter extends
AbstractJHotDrawOverlayAdapter<GeneralPathOverlay, PolygonFigure>
{

public static final int PRIORITY = EllipseAdapter.PRIORITY - 1;

static private GeneralPathOverlay downcastOverlay(final Overlay overlay) {
	assert overlay instanceof GeneralPathOverlay;
	return (GeneralPathOverlay) overlay;
}

@Override
public boolean supports(final Overlay overlay, final Figure figure) {
	if ((figure != null) && !(figure instanceof PolygonFigure)) return false;
	return (overlay instanceof GeneralPathOverlay);
}

@Override
public Overlay createNewOverlay() {
	final GeneralPathOverlay o = new GeneralPathOverlay(getContext());
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
	final GeneralPathOverlay poverlay = downcastOverlay(overlay.getData());
	final GeneralPathRegionOfInterest roi = poverlay.getRegionOfInterest();
	roi.reset();
	final double[] coords = new double[6];
	for (final PathIterator iterator = figure.getBezierPath().getPathIterator(null); !iterator.isDone(); iterator.next()) {
		int type = iterator.currentSegment(coords);
		switch (type) {
			case PathIterator.SEG_MOVETO:
				roi.moveTo(coords[0], coords[1]);
System.err.println("move to " + coords[0] + ", " + coords[1]);
				break;
			case PathIterator.SEG_LINETO:
				roi.lineTo(coords[0], coords[1]);
System.err.println("line to " + coords[0] + ", " + coords[1]);
				break;
			case PathIterator.SEG_QUADTO:
				roi.quadTo(coords[0], coords[1], coords[2], coords[3]);
System.err.println("quad to " + coords[0] + ", " + coords[1] + "; " + coords[2] + ", " + coords[3]);
				break;
			case PathIterator.SEG_CUBICTO:
				roi.cubicTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
System.err.println("cubic to " + coords[0] + ", " + coords[1] + "; " + coords[2] + ", " + coords[3] + "; " + coords[4] + ", " + coords[5]);
				break;
			case PathIterator.SEG_CLOSE:
				roi.close();
System.err.println("close");
				break;
			default:
				throw new RuntimeException("Unsupported segment type: " + type);
		}
	}
System.err.println("shape roi done");
}

@Override
public void updateFigure(final OverlayView overlay, final PolygonFigure figure) {
	super.updateFigure(overlay, figure);
	final GeneralPathOverlay pOverlay = downcastOverlay(overlay.getData());
	final GeneralPathRegionOfInterest roi = pOverlay.getRegionOfInterest();
	final BezierPath bezierPath = new BezierPath();
System.err.println("Hello");
	final Stack<double[]> closePath = new Stack<double[]>();
	roi.iteratePath(new GeneralPathSegmentHandler() {
		@Override
		public void moveTo(double x, double y) {
			if (closePath.empty())
				bezierPath.moveTo(x, y);
			else
				bezierPath.lineTo(x, y);
			closePath.push(new double[] { x, y });
System.err.println("Move to " + x + ", " + y);
		}
		
		@Override
		public void lineTo(double x, double y) {
			bezierPath.lineTo(x, y);
		}
		
		@Override
		public void quadTo(double x1, double y1, double x, double y) {
			bezierPath.quadTo(x1, y1, x, y);
		}
		
		@Override
		public void cubicTo(double x1, double y1, double x2, double y2, double x,
			double y)
		{
			bezierPath.curveTo(x1, y1, x2, y2, x, y);
		}
		
		@Override
		public void close() {
			double[] coords = closePath.peek();
			bezierPath.lineTo(coords[0], coords[1]);
		}
	});
	while (!closePath.empty()) {
		double[] coords = closePath.pop();
		bezierPath.lineTo(coords[0], coords[1]);
	}
	figure.setBezierPath(bezierPath);
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
