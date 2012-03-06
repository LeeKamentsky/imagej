//
// PickerTool.java
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

package imagej.core.tools;

import imagej.data.ChannelCollection;
import imagej.event.EventService;
import imagej.ext.display.event.input.KyPressedEvent;
import imagej.ext.display.event.input.KyReleasedEvent;
import imagej.ext.display.event.input.MsButtonEvent;
import imagej.ext.display.event.input.MsClickedEvent;
import imagej.ext.plugin.Plugin;
import imagej.ext.plugin.PluginService;
import imagej.ext.tool.AbstractTool;
import imagej.ext.tool.Tool;
import imagej.options.OptionsService;
import imagej.options.plugins.OptionsChannels;

/**
 * Sets foreground and background values when tool is active and mouse clicked
 * over an image.
 * 
 * @author Barry DeZonia
 */
@Plugin(
	type = Tool.class,
	name = "Picker",
	description = "Picker Tool (sets foreground/background values)",
	iconPath = "/icons/tools/picker.png", priority = PickerTool.PRIORITY)
public class PickerTool extends AbstractTool {

	// -- constants --

	public static final int PRIORITY = -299;

	// -- instance variables --

	private final PixelHelper helper = new PixelHelper(true);
	private boolean altKeyDown = false;

	// -- Tool methods --

	@Override
	public void onMouseClick(final MsClickedEvent evt) {

		if (evt.getButton() != MsButtonEvent.LEFT_BUTTON) return;
		
		if (!helper.recordEvent(evt)) {
			evt.consume();
			return;
		}

		final OptionsChannels options = getOptions();

		// FIXME Hack that allows options to publish events about the changing
		// values. This is how IJ1 is informed about current FG/BG colors.
		final EventService es = getContext().getService(EventService.class);
		options.setEventService(es);

		final ChannelCollection values = helper.getValues();

		String name;
		ChannelCollection target;
		
		// background case?
		if (altKeyDown) {
			name = "BG";
			target = options.getBgValues();
			options.setLastBgColor(helper.getColor());
		}
		else { // foreground case
			name = "FG";
			target = options.getFgValues();
			options.setLastFgColor(helper.getColor());
		}
		
		// set the values of the FG or BG
		target.resetChannels(values);
		
		// make sure future options reflect those new values
		options.save();

		// let user know the FG or BG values changed
		statusMessage(name, values);

		evt.consume();
	}

	@Override
	public void onKeyDown(final KyPressedEvent evt) {
		altKeyDown =
			evt.getModifiers().isAltDown() || evt.getModifiers().isAltGrDown();
	}

	@Override
	public void onKeyUp(final KyReleasedEvent evt) {
		altKeyDown =
			evt.getModifiers().isAltDown() || evt.getModifiers().isAltGrDown();
	}

	@Override
	public String getDescription() {
		OptionsChannels opts = getOptions();
		StringBuilder sb = new StringBuilder();
		sb.append("Picker FG: ");
		ChannelCollection fgVals = opts.getFgValues();
		ChannelCollection bgVals = opts.getBgValues();
		sb.append(valuesString(fgVals));
		sb.append("  BG: ");
		sb.append(valuesString(bgVals));
		return sb.toString();
	}
	
	@Override
	public void configure() {
		final PluginService pluginService =
				getContext().getService(PluginService.class);
		
		pluginService.run(OptionsChannels.class, new Object[0]);
	}
	
	// -- private interface --

	private String valuesString(ChannelCollection chans) {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		for (int i = 0; i < chans.getChannelCount(); i++) {
			if (i != 0) builder.append(",");
			String valString;
			if (chans.areInteger())
				valString = String.format("%d", (long)chans.getChannelValue(i));
			else
				valString = String.format("%f", chans.getChannelValue(i));
			builder.append(valString);
		}
		builder.append(")");
		return builder.toString();
	}
	
	private void statusMessage(final String label, ChannelCollection values) {
		StringBuilder builder = new StringBuilder();
		builder.append(label);
		builder.append(" = ");
		builder.append(valuesString(values));
		helper.updateStatus(builder.toString());
	}

	private OptionsChannels getOptions() {
		final OptionsService service =
			getContext().getService(OptionsService.class);

		return service.getOptions(OptionsChannels.class);
	}
}
