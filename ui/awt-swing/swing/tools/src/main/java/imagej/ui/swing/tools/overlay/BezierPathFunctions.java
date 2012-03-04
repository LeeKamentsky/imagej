package imagej.ui.swing.tools.overlay;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jhotdraw.geom.BezierPath;

public class BezierPathFunctions {
	public static BezierPath toBezierPath(final PathIterator iterator) {
		final BezierPath path = new BezierPath();
		double[] segment = new double[6];
		double x0 = 0, y0 = 0; // the most recent moved-to coordinate
		for (; !iterator.isDone(); iterator.next()) {
			int type = iterator.currentSegment(segment);
			switch (type) {
			case PathIterator.SEG_MOVETO:
				path.moveTo(segment[0], segment[1]);
				x0 = segment[0];
				y0 = segment[1];
				break;
			case PathIterator.SEG_LINETO:
				path.lineTo(segment[0], segment[1]);
				break;
			case PathIterator.SEG_QUADTO:
				path.quadTo(segment[0], segment[1], segment[2], segment[3]);
				break;
			case PathIterator.SEG_CUBICTO:
				path.curveTo(segment[0], segment[1], segment[2], segment[3], segment[4], segment[5]);
				break;
			case PathIterator.SEG_CLOSE:
				path.lineTo(x0, y0);
				break;
			}
		}
		return path;
	}

	public enum OP { ADD, XOR, INTERSECT, SUBTRACT };

	public static BezierPath add(final BezierPath path1, final BezierPath path2) {
		return op(path1, path2, OP.ADD);
	}

	public static BezierPath exclusiveOr(final BezierPath path1, final BezierPath path2) {
		return op(path1, path2, OP.XOR);
	}

	public static BezierPath intersect(final BezierPath path1, final BezierPath path2) {
		return op(path1, path2, OP.INTERSECT);
	}

	public static BezierPath subtract(final BezierPath path1, final BezierPath path2) {
		return op(path1, path2, OP.SUBTRACT);
	}

	public static BezierPath op(final BezierPath path1, final BezierPath path2, final OP op) {
		final Area area1 = new Area(path1.toGeneralPath());
		final Area area2 = new Area(path2.toGeneralPath());
		switch (op) {
		case ADD: area1.add(area2); break;
		case XOR: area1.exclusiveOr(area2); break;
		case INTERSECT: area1.intersect(area2); break;
		case SUBTRACT: area1.subtract(area2); break;
		}
		return toBezierPath(area1.getPathIterator(new AffineTransform()));
	}

	@SuppressWarnings("unused")
	private static void show(final PathIterator iterator) {
		show(toBezierPath(iterator).toGeneralPath());
	}

	@SuppressWarnings("serial")
	private static void show(final Shape shape) {
		JFrame frame = new JFrame("debug");
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics graphics) {
				Graphics2D graphics2D = (Graphics2D)graphics;
				graphics2D.draw(shape);
			}
		};
		frame.getContentPane().add(panel);
		Rectangle bounds = shape.getBounds();
		frame.setSize(bounds.x + bounds.width, bounds.y + bounds.height);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		BezierPath path1 = new BezierPath();
		path1.moveTo(0, 0);
		path1.lineTo(100, 0);
		path1.lineTo(100, 100);
		path1.lineTo(0, 0);

		GeneralPath path2 = new GeneralPath();
		path2.moveTo(0, 0);
		path2.lineTo(100, 0);
		path2.lineTo(100, 100);
		path2.closePath();

		//path1 = toBezierPath(path2.getPathIterator(new AffineTransform()));

		BezierPath path3 = new BezierPath();
		path3.moveTo(0, 100);
		path3.lineTo(100, 0);
		path3.lineTo(100, 100);
		path3.lineTo(0, 100);

		path1 = subtract(path1, path3);

		show(path1.toGeneralPath());
	}
}
