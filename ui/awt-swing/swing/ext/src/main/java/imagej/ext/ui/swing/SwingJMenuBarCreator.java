//
// SwingJMenuBarCreator.java
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

package imagej.ext.ui.swing;

import imagej.event.EventHandler;
import imagej.ext.menu.ShadowMenu;
import imagej.ext.menu.event.MenuEvent;
import imagej.util.Log;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * Populates a {@link JMenuBar} with menu items from a {@link ShadowMenu}.
 * 
 * @author Curtis Rueden
 */
public class SwingJMenuBarCreator extends AbstractSwingMenuCreator<JMenuBar> {

	@Override
	protected void addLeafToTop(final ShadowMenu shadow, final JMenuBar target) {
		final JMenuItem menuItem = createLeaf(shadow);
		target.add(menuItem);
	}

	@Override
	protected JMenu addNonLeafToTop(final ShadowMenu shadow,
		final JMenuBar target)
	{
		final JMenu menu = createNonLeaf(shadow);
		target.add(menu);
		return menu;
	}

	@Override
	protected void addSeparatorToTop(final JMenuBar target) {
		Log.debug("SwingJMenuBarCreator: Ignoring top-level separator");
	}

	@Override
	public void createMenus(final ShadowMenu root, final JMenuBar target) {
		super.createMenus(root, target);
		final Object listener = new Object() {
			// -- Event handlers --

			@SuppressWarnings("unused")
			@EventHandler
			protected void onEvent(final MenuEvent event)
			{
				// TODO - This rebuilds the entire menu structure whenever the
				// menus change at all. Better would be to listen to MenusAddedEvent,
				// MenusRemovedEvent and MenusUpdatedEvent separately and surgically
				// adjust the menus accordingly. But this would require updates to
				// the MenuCreator API to be more powerful.
				boolean sameRoot = false;
				for (final ShadowMenu menu : event.getItems()) {
					if (menu.getRoot() == root) {
						sameRoot = true;
						break;
					}
				}
				if (sameRoot) {
					// throw away everything, at this point we cannot tell whether some item was removed
					target.removeAll();
					SwingJMenuBarCreator.super.createMenus(root, target);
				}
			}
		};
		root.getMenuService().getEventService().subscribe(listener);
		// the event service has only a weak reference, so bind the listener to the JMenuBar
		target.putClientProperty("remember this", listener);
	}

}
