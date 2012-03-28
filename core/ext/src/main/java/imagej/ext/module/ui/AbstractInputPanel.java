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

package imagej.ext.module.ui;

import imagej.ImageJ;
import imagej.ext.module.ModuleException;
import imagej.object.ObjectService;
import imagej.util.ColorRGB;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract superclass of UI-specific {@link InputPanel} implementations.
 * 
 * @author Curtis Rueden
 */
public abstract class AbstractInputPanel implements InputPanel {

	/** Number of messages in the panel. */
	protected int messageCount = 0;

	/** Widget table for numbers. */
	protected Map<String, NumberWidget> numberWidgets =
		new HashMap<String, NumberWidget>();

	/** Widget table for toggles. */
	protected Map<String, ToggleWidget> toggleWidgets =
		new HashMap<String, ToggleWidget>();

	/** Widget table for text fields. */
	protected Map<String, TextFieldWidget> textFieldWidgets =
		new HashMap<String, TextFieldWidget>();

	/** Widget table for choices. */
	protected Map<String, ChoiceWidget> choiceWidgets =
		new HashMap<String, ChoiceWidget>();

	/** Widget table for files. */
	protected Map<String, FileWidget> fileWidgets =
		new HashMap<String, FileWidget>();

	/** Widget table for colors. */
	protected Map<String, ColorWidget> colorWidgets =
		new HashMap<String, ColorWidget>();

	/** Widget table for objects. */
	protected Map<String, ObjectWidget> objectWidgets =
		new HashMap<String, ObjectWidget>();

	@Override
	public Number getNumber(final String name) {
		return numberWidgets.get(name).getValue();
	}

	@Override
	public boolean getToggle(final String name) {
		return toggleWidgets.get(name).getValue();
	}

	@Override
	public String getTextField(final String name) {
		return textFieldWidgets.get(name).getValue();
	}

	@Override
	public String getChoice(final String name) {
		return choiceWidgets.get(name).getValue();
	}

	@Override
	public int getChoiceIndex(final String name) {
		return choiceWidgets.get(name).getIndex();
	}

	@Override
	public File getFile(final String name) {
		return fileWidgets.get(name).getValue();
	}

	@Override
	public ColorRGB getColor(final String name) {
		return colorWidgets.get(name).getValue();
	}

	@Override
	public Object getObject(final String name) {
		return objectWidgets.get(name).getValue();
	}

	@Override
	public boolean hasWidgets() {
		return getWidgetCount() > 0;
	}

	@Override
	public boolean isMessageOnly() {
		return messageCount == getWidgetCount();
	}

	@Override
	public void refresh() {
		for (final InputWidget w : numberWidgets.values())
			w.refreshWidget();
		for (final InputWidget w : toggleWidgets.values())
			w.refreshWidget();
		for (final InputWidget w : textFieldWidgets.values())
			w.refreshWidget();
		for (final InputWidget w : choiceWidgets.values())
			w.refreshWidget();
		for (final InputWidget w : fileWidgets.values())
			w.refreshWidget();
		for (final InputWidget w : colorWidgets.values())
			w.refreshWidget();
		for (final InputWidget w : objectWidgets.values())
			w.refreshWidget();
	}

	// -- Internal methods --

	protected Object[] getObjects(final WidgetModel model)
		throws ModuleException
	{
		final Class<?> type = model.getItem().getType();
		final ObjectService objectService = ImageJ.get(ObjectService.class);
		final Object[] items = objectService.getObjects(type).toArray();
		if (items.length == 0) {
			// no valid objects of the given type
			throw new ModuleException("No objects of type " + type.getName());
		}
		return items;
	}

}
