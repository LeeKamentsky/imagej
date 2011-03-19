//
// NavigableImageDisplay.java
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

package imagej.gui.swing.display;

import imagej.display.Display;
import imagej.display.DisplayView;
import imagej.display.ImageCanvas;
import imagej.display.ImageDisplayWindow;
import imagej.display.LayeredDisplay;
import imagej.display.NavigableImageCanvas;
import imagej.model.Dataset;
import imagej.plugin.Plugin;

/**
 * TODO
 *
 * @author Curtis Rueden
 * @author Grant Harris
 */
// @Plugin(type = Display.class)
public class NavigableImageDisplay extends AbstractAWTDisplay
	implements Display
{

	//private NavigableImageFrame imageFrame;
	private ImageDisplayWindow imageFrame;
	private NavigableImageCanvas imagecanvas;
	@Override
	public boolean canDisplay(Dataset dataset) {
		return true;
	}

	@Override
	public void display(Dataset dataset) {
		// NavigableImagePanel imagePanel = new NavigableImagePanel();
		//imageFrame = new NavigableImageFrame(controller,imagePanel);
		//DisplayController controller = new DisplayController(dataset, imageFrame);



		// listen for user input
//		imageFrame.getPanel().addKeyListener(this);
//		imageFrame.getPanel().addMouseListener(this);
//		imageFrame.getPanel().addMouseMotionListener(this);
//		imageFrame.getPanel().addMouseWheelListener(this);
//		imageFrame.addWindowListener(this);
//
//		// TODO - use DisplayView instead of Dataset directly
//		//imageFrame.setDataset(dataset);
//		imageFrame.pack();
//		imageFrame.setVisible(true);
	}

	@Override
	public void pan(float x, float y) {
		//imageFrame.getPanel().pan((int) x, (int) y);
	}

	// TODO
	@Override
	public float getPanX() {
		return 0f;
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public float getPanY() {
		return 0f;
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setZoom(float factor, float centerX, float centerY) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void zoomIn(float centerX, float centerY) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void zoomOut(float centerX, float centerY) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void zoomToFit(int w, int h) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public float getZoom() {
		return 1f;
		//throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ImageDisplayWindow getImageDisplayWindow() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public NavigableImageCanvas getImageCanvas() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Dataset getDataset() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getCurrentPlane() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
