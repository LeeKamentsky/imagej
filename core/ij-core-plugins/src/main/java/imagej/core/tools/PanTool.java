package imagej.core.tools;

import imagej.display.Display;
import imagej.display.event.key.KyPressedEvent;
import imagej.display.event.mouse.MsDraggedEvent;
import imagej.display.event.mouse.MsPressedEvent;
import imagej.tool.BaseTool;
import imagej.tool.Tool;

import java.awt.event.KeyEvent;

// TODO - rework tool framework to use event bus instead
// E.g., PanTool implements ImageSubscriber<DisplayMouseEvent>?

/**
 * TODO
 * 
 * @author Rick Lentz
 * @author Grant Harris
 * @author Curtis Rueden
 */
@Tool(
	name = "Pan",
	description = "Pans the display",
	iconPath = "/tools/pan.png"
)
public class PanTool extends BaseTool {

	private static final float PAN_AMOUNT = 10;

	// @todo: Add customization to set pan amount

	private int lastX, lastY;

	@Override
	public void onKeyDown(KyPressedEvent evt) {
		final Display display = evt.getDisplay();
		switch (evt.getCode()) {
			case KeyEvent.VK_UP:
				display.pan(0, -PAN_AMOUNT);
				break;
			case KeyEvent.VK_DOWN:
				display.pan(0, -PAN_AMOUNT);
				break;
			case KeyEvent.VK_LEFT:
				display.pan(-PAN_AMOUNT, 0);
				break;
			case KeyEvent.VK_RIGHT:
				display.pan(PAN_AMOUNT, 0);
				break;
		}
	}

	@Override
	public void onMouseDown(MsPressedEvent evt) {
		lastX = evt.getX();
		lastY = evt.getY();
	}

	@Override
	public void onMouseDrag(MsDraggedEvent evt)  {
		final Display display = evt.getDisplay();
		display.pan(evt.getX() - lastX, evt.getY() - lastY);
		lastX = evt.getX();
		lastY = evt.getY();
	}

}
