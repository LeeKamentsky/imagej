//
// Tool.java
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

package imagej.ext.tool;

import imagej.ext.MouseCursor;
import imagej.ext.display.event.input.KyPressedEvent;
import imagej.ext.display.event.input.KyReleasedEvent;
import imagej.ext.display.event.input.MsClickedEvent;
import imagej.ext.display.event.input.MsDraggedEvent;
import imagej.ext.display.event.input.MsMovedEvent;
import imagej.ext.display.event.input.MsPressedEvent;
import imagej.ext.display.event.input.MsReleasedEvent;
import imagej.ext.display.event.input.MsWheelEvent;
import imagej.ext.plugin.IPlugin;
import imagej.ext.plugin.Plugin;
import imagej.ext.plugin.PluginInfo;

/**
 * Interface for ImageJ tools. Tools discoverable at runtime must implement this
 * interface and be annotated with @{@link Plugin} with {@link Plugin#type()} =
 * {@link Tool}.class. While it possible to create a tool merely by
 * implementing this interface, it is encouraged to instead extend
 * {@link AbstractTool}, for convenience.
 * 
 * @author Rick Lentz
 * @author Grant Harris
 * @author Curtis Rueden
 * @see ToolService
 */
public interface Tool extends IPlugin {

	/** Gets the info describing the tool. */
	PluginInfo<Tool> getInfo();

	/** Sets the info describing the tool. */
	void setInfo(final PluginInfo<Tool> info);

	/** The tool's mouse pointer. */
	MouseCursor getCursor();

	/** Informs the tool that it is now active. */
	void activate();

	/** Informs the tool that it is no longer active. */
	void deactivate();

	/** Occurs when a key on the keyboard is pressed while the tool is active. */
	void onKeyDown(KyPressedEvent event);

	/** Occurs when a key on the keyboard is released while the tool is active. */
	void onKeyUp(KyReleasedEvent event);

	/** Occurs when a mouse button is pressed while the tool is active. */
	void onMouseDown(MsPressedEvent event);

	/** Occurs when a mouse button is released while the tool is active. */
	void onMouseUp(MsReleasedEvent event);

	/** Occurs when a mouse button is double clicked while the tool is active. */
	void onMouseClick(MsClickedEvent event);

	/** Occurs when the mouse is moved while the tool is active. */
	void onMouseMove(MsMovedEvent event);

	/** Occurs when the mouse is dragged while the tool is active. */
	void onMouseDrag(MsDraggedEvent event);

	/** Occurs when the mouse wheel is moved while the tool is active. */
	void onMouseWheel(MsWheelEvent event);

	/** Occurs when the user right clicks this tool's icon. */ 
	void configure();
}
