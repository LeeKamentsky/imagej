/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2012 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.ext.ui.swing;

import imagej.ext.module.ui.ColorWidget;
import imagej.ext.module.ui.WidgetModel;
import imagej.util.ColorRGB;
import imagej.util.awt.AWTColors;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;

/**
 * Swing implementation of color chooser widget.
 * 
 * @author Curtis Rueden
 */
public class SwingColorWidget extends SwingInputWidget implements
	ActionListener, ColorWidget
{

	private static final int SWATCH_WIDTH = 64, SWATCH_HEIGHT = 16;

	private final JButton choose;
	private Color color;

	public SwingColorWidget(final WidgetModel model) {
		super(model);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		choose = new JButton() {

			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			}
		};
		setToolTip(choose);
		add(choose);
		choose.addActionListener(this);

		refreshWidget();
	}

	// -- ActionListener methods --

	@Override
	public void actionPerformed(final ActionEvent e) {
		final Color choice =
			JColorChooser.showDialog(choose, "Select a color", color);
		if (choice == null) return;
		color = choice;
		updateModel();
		refreshWidget();
	}

	// -- ColorWidget methods --

	@Override
	public ColorRGB getValue() {
		return AWTColors.getColorRGB(color);
	}

	// -- InputWidget methods --

	@Override
	public void refreshWidget() {
		final ColorRGB value = (ColorRGB) getModel().getValue();
		color = AWTColors.getColor(value);

		final BufferedImage image =
			new BufferedImage(SWATCH_WIDTH, SWATCH_HEIGHT,
				BufferedImage.TYPE_INT_RGB);
		final Graphics g = image.getGraphics();
		g.setColor(color);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.dispose();
		final ImageIcon icon = new ImageIcon(image);
		choose.setIcon(icon);
	}

}
