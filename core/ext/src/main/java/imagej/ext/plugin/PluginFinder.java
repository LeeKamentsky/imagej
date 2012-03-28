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

package imagej.ext.plugin;

import imagej.ext.MenuEntry;
import imagej.ext.MenuPath;
import imagej.util.Log;

import java.util.List;

import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;

/**
 * Discovers ImageJ plugins.
 * <p>
 * To accomplish this, SezPoz scans the classpath for {@link Plugin}
 * annotations.
 * </p>
 * 
 * @author Curtis Rueden
 */
public class PluginFinder {

	/** Class loader to use when querying SezPoz. */
	private final ClassLoader classLoader;

	// -- Constructors --

	public PluginFinder() {
		this(null);
	}

	public PluginFinder(final ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	// -- PluginFinder methods --

	public void findPlugins(final List<PluginInfo<?>> plugins) {
		final Index<Plugin, IPlugin> pluginIndex;
		if (classLoader == null) {
			pluginIndex = Index.load(Plugin.class, IPlugin.class);
		}
		else {
			pluginIndex = Index.load(Plugin.class, IPlugin.class, classLoader);
		}

		final int oldSize = plugins.size();
		for (final IndexItem<Plugin, IPlugin> item : pluginIndex) {
			final PluginInfo<?> info = createInfo(item);

			// CTR TEMP - add "IJ2" suffix to all modern plugins, for debugging
			final MenuPath menuPath = info.getMenuPath();
			if (menuPath != null && menuPath.size() > 0) {
				final MenuEntry menuLeaf = menuPath.getLeaf();
				menuLeaf.setName(menuLeaf.getName() + " [IJ2]");
			}

			plugins.add(info);
		}
		final int newSize = plugins.size();

		Log.info("Found " + (newSize - oldSize) + " plugins.");
		if (Log.isDebug()) {
			for (int i = oldSize; i < newSize; i++) {
				Log.debug("- " + plugins.get(i));
			}
		}
	}

	// -- Helper methods --

	private <P extends IPlugin> PluginInfo<P> createInfo(
		final IndexItem<Plugin, IPlugin> item)
	{
		final String className = item.className();
		final Plugin plugin = item.annotation();

		@SuppressWarnings("unchecked")
		final Class<P> pluginType = (Class<P>) plugin.type();

		if (RunnablePlugin.class.isAssignableFrom(pluginType)) {
			// TODO - Investigate a simpler way to handle this.
			final PluginModuleInfo<? extends RunnablePlugin> moduleInfo =
				createModuleInfo(className, plugin);
			@SuppressWarnings("unchecked")
			final PluginInfo<P> result = (PluginInfo<P>) moduleInfo;
			return result;
		}
		return new PluginInfo<P>(className, pluginType, plugin);
	}

	private <R extends RunnablePlugin> PluginModuleInfo<R> createModuleInfo(
		final String className, final Plugin plugin)
	{
		@SuppressWarnings("unchecked")
		final Class<R> pluginType = (Class<R>) plugin.type();

		return new PluginModuleInfo<R>(className, pluginType, plugin);
	}

}
