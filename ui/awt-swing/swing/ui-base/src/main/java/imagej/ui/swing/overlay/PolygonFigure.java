package imagej.ui.swing.overlay;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.util.Collection;
import java.util.LinkedList;

import org.jhotdraw.draw.BezierFigure;
import org.jhotdraw.draw.Figure;
import org.jhotdraw.draw.handle.BezierNodeHandle;
import org.jhotdraw.draw.handle.BezierOutlineHandle;
import org.jhotdraw.draw.handle.Handle;

public class PolygonFigure extends BezierFigure {

	public PolygonFigure() {
		// The constructor makes the BezierFigure a closed figure.
		super(true);
	}

	@Override
	public Collection<Handle> createHandles(final int detailLevel) {
		final LinkedList<Handle> handles = new LinkedList<Handle>();
		if (detailLevel != 0) {
			return super.createHandles(detailLevel);
		}
		handles.add(new BezierOutlineHandle(this));
		for (int i = 0, n = path.size(); i < n; i++) {
			handles.add(new PolygonNodeHandle(this, i));
		}
		return handles;
	}

	private static final long serialVersionUID = 1L;

	/*
	 * The BezierFigure uses a BezierNodeHandle which can change the curve
	 * connecting vertices from a line to a Bezier curve. We subclass both 
	 * the figure and the node handle to defeat this.
	 */
	public static class PolygonNodeHandle extends BezierNodeHandle {

		public PolygonNodeHandle(final BezierFigure owner, final int index,
			final Figure transformOwner)
		{
			super(owner, index, transformOwner);
		}

		public PolygonNodeHandle(final BezierFigure owner, final int index) {
			super(owner, index);
		}

		@Override
		public void trackEnd(final Point anchor, final Point lead,
			final int modifiersEx)
		{
			// Remove the behavior associated with the shift keys
			super.trackEnd(anchor, lead, modifiersEx &
				~(InputEvent.META_DOWN_MASK | InputEvent.CTRL_DOWN_MASK |
					InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		}

	}

}
