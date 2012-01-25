//
// LegacyPlugin.java
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

package imagej.legacy.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import imagej.data.Dataset;
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.ext.module.ItemIO;
import imagej.ext.plugin.ImageJPlugin;
import imagej.ext.plugin.Parameter;
import imagej.legacy.LegacyImageMap;
import imagej.legacy.LegacyOutputTracker;
import imagej.legacy.LegacyService;
import imagej.legacy.translate.DefaultImageTranslator;
import imagej.legacy.translate.Harmonizer;
import imagej.legacy.translate.ImageTranslator;
import imagej.legacy.translate.LegacyUtils;
import imagej.ui.DialogPrompt;
import imagej.ui.UIService;
import imagej.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Executes an IJ1 plugin.
 * 
 * @author Curtis Rueden
 * @author Barry DeZonia
 */
public class LegacyPlugin implements ImageJPlugin {

	@Parameter
	private String className;

	@Parameter
	private String arg;

	@Parameter(type = ItemIO.OUTPUT)
	private List<ImageDisplay> outputs;

	@Parameter(required = true, persist = false)
	private ImageDisplayService imageDisplayService;

	@Parameter(required = true, persist = false)
	private LegacyService legacyService;

	@Parameter(required = true, persist = false)
	private UIService uiService;

	// -- LegacyPlugin methods --

	/** Gets the list of output {@link ImageDisplay}s. */
	public List<ImageDisplay> getOutputs() {
		return Collections.unmodifiableList(outputs);
	}

	// -- Runnable methods --

	@Override
	public void run() {

		final ImageDisplay activeDisplay =
			imageDisplayService.getActiveImageDisplay();

		if (!isLegacyCompatible(activeDisplay)) {
			final String err =
				"The active dataset is too large to be represented inside IJ1.";
			Log.error(err);
			notifyUser(err);
			outputs = new ArrayList<ImageDisplay>();
			return;
		}

		final LegacyImageMap map = legacyService.getImageMap();

		// sync legacy images to match existing modern displays
		final ImageTranslator imageTranslator = new DefaultImageTranslator();
		final Harmonizer harmonizer = new Harmonizer(imageTranslator);

		final Set<ImagePlus> outputSet = LegacyOutputTracker.getOutputImps();
		final Set<ImagePlus> closedSet = LegacyOutputTracker.getClosedImps();

		harmonizer.resetTypeTracking();

		updateImagePlusesFromDisplays(map, harmonizer);

		// must happen after updateImagePlusesFromDisplays()
		outputSet.clear();
		closedSet.clear();

		// set ImageJ1's active image
		legacyService.syncActiveImage();

		try {

			final Set<Thread> originalThreads = getCurrentThreads();

			// execute the legacy plugin
			IJ.runPlugIn(className, arg);

			// we always sleep at least once to make sure plugin has time to hatch
			// it's first thread if its going to create any.
			try {
				Thread.sleep(50);
			}
			catch (final InterruptedException e) {/**/}

			// wait for any threads hatched by plugin to terminate
			waitForPluginThreads(originalThreads);

			// sync modern displays to match existing legacy images
			outputs = updateDisplaysFromImagePluses(map, harmonizer);
		}
		catch (final Exception e) {
			final String msg = "ImageJ 1.x plugin threw exception";
			Log.error(msg, e);
			notifyUser(msg);
			// make sure our ImagePluses are in sync with original Datasets
			updateImagePlusesFromDisplays(map, harmonizer);
			// return no outputs
			outputs = new ArrayList<ImageDisplay>();
		}

		// close any displays that IJ1 wants closed
		for (final ImagePlus imp : closedSet) {
			final ImageDisplay disp = map.lookupDisplay(imp);
			if (disp != null) {
				// REMOVED next line to fix #803. May leave extra windows open.
				// outputs.remove(display);
				// Now only close displays that have not been changed
				if (!outputs.contains(disp)) disp.close();
			}
		}

		// clean up
		harmonizer.resetTypeTracking();
		outputSet.clear();
		closedSet.clear();

		// reflect any changes to globals in IJ2 options/prefs
		legacyService.updateIJ2Settings();
	}

	// -- Helper methods --

	private Set<Thread> getCurrentThreads() {
		final ThreadGroup group = Thread.currentThread().getThreadGroup();
		Thread[] threads;
		int numThreads;
		int size = 25;
		do {
			threads = new Thread[size];
			numThreads = group.enumerate(threads);
			size *= 2;
		}
		while (numThreads > threads.length);
		final Set<Thread> threadSet = new HashSet<Thread>();
		for (int i = 0; i < numThreads; i++)
			threadSet.add(threads[i]);
		return threadSet;
	}

	private void waitForPluginThreads(final Set<Thread> threadsToIgnore) {
		final Set<Thread> currentThreads = getCurrentThreads();
		for (final Thread thread : currentThreads) {
			if ((thread != Thread.currentThread()) &&
				(!threadsToIgnore.contains(thread)))
			{
				// Ignore some threads that IJ1 hatches that never terminate
				if (whitelisted(thread)) continue;

				// make other threads join
				try {
					thread.join();
				}
				catch (final InterruptedException e) {
					// do nothing
				}
			}
		}
	}

	private void updateImagePlusesFromDisplays(final LegacyImageMap map,
		final Harmonizer harmonizer)
	{
		// TODO - track events and keep a dirty bit, then only harmonize those
		// displays that have changed. See ticket #546.
		final List<ImageDisplay> imageDisplays =
			imageDisplayService.getImageDisplays();
		for (final ImageDisplay display : imageDisplays) {
			ImagePlus imp = map.lookupImagePlus(display);
			if (imp == null) {
				if (isLegacyCompatible(display)) {
					imp = map.registerDisplay(display);
					harmonizer.registerType(imp);
				}
			}
			else { // imp already exists : update it
				harmonizer.updateLegacyImage(display, imp);
				harmonizer.registerType(imp);
			}
		}
	}

	private List<ImageDisplay> updateDisplaysFromImagePluses(
		final LegacyImageMap map, final Harmonizer harmonizer)
	{
		// TODO - check the changes flag for each ImagePlus that already has a
		// ImageDisplay and only harmonize those that have changed. Maybe changes
		// flag does not track everything (such as metadata changes?) and thus
		// we might still have to do some minor harmonization. Investigate.

		final Set<ImagePlus> imps = LegacyOutputTracker.getOutputImps();
		final ImagePlus currImp = WindowManager.getCurrentImage();

		// see method below
		finishInProgressPastes(currImp, imps);

		// the IJ1 plugin may not have any outputs but just changes current
		// ImagePlus make sure we catch any changes via harmonization
		final List<ImageDisplay> displays = new ArrayList<ImageDisplay>();
		if (currImp != null) {
			ImageDisplay display = map.lookupDisplay(currImp);
			if (display != null) {
				harmonizer.updateDisplay(display, currImp);
			}
			else {
				display = map.registerLegacyImage(currImp);
				displays.add(display);
			}
		}

		// also harmonize any outputs

		for (final ImagePlus imp : imps) {
			if (imp.getStack().getSize() == 0) { // totally emptied by plugin
				// TODO - do we need to delete display or is it already done?
			}
			else { // image plus is not totally empty
				ImageDisplay display = map.lookupDisplay(imp);
				if (display == null) {
					if (imp.getWindow() != null) {
						display = map.registerLegacyImage(imp);
					}
					else {
						continue;
					}
				}
				else {
					if (imp == currImp) {
						// we harmonized this earlier
					}
					else harmonizer.updateDisplay(display, imp);
				}
				displays.add(display);
			}
		}

		return displays;
	}

	private boolean isLegacyCompatible(final ImageDisplay display) {
		if (display == null) return true;
		final Dataset ds = imageDisplayService.getActiveDataset(display);
		return LegacyUtils.dimensionsIJ1Compatible(ds);
	}

	private void notifyUser(final String message) {
		uiService.showDialog(message, "Error",
			DialogPrompt.MessageType.INFORMATION_MESSAGE,
			DialogPrompt.OptionType.DEFAULT_OPTION);
	}

	/**
	 * Identifies threads that IJ1 hatches that don't terminate in a timely way
	 */
	private boolean whitelisted(final Thread thread) {

		// StackWindow slider selector thread: thread does not go away until the
		// window closes.
		if (thread.getName().equals("zSelector")) return true;

		/*
			// select by class name
			System.out.println("---"+thread.getClass().getDeclaringClass());
			System.out.println("---"+thread.getClass().getEnclosingClass());
			System.out.println("---"+thread.getClass().getCanonicalName());
			System.out.println("---"+thread.getClass().getName());
			System.out.println("---"+thread.getClass().getSimpleName());
			System.out.println("---"+thread.getClass().getClass());
			System.out.println("---"+thread.getClass().getInterfaces());
			
			// select by name of runnable class it owns
			//System.out.println(thread.HOW???);
		*/

		return false;
	}

	// Finishes any in progress paste() operations. Done before harmonization.
	// In IJ1 the paste operations are usually handled by ImageCanvas::paint().
	// In IJ2 that method is never called. It would be nice to hook something
	// that calls paint() via the legacy injector but that may raise additional
	// problems. This is a simple fix.

	private void finishInProgressPastes(final ImagePlus currImp,
		final Set<ImagePlus> outputList)
	{
		endPaste(currImp);
		for (final ImagePlus imp : outputList) { // potentially empty list
			if (imp == currImp) continue;
			endPaste(imp);
		}
	}

	private void endPaste(final ImagePlus imp) {
		if (imp == null) return;
		final Roi roi = imp.getRoi();
		if (roi == null) return;
		if (roi.getPasteMode() == Roi.NOT_PASTING) return;
		roi.endPaste();
	}
}
