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

package imagej.core.plugins.app;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.imglib2.img.ImgPlus;
import net.imglib2.io.ImgOpener;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import imagej.ImageJ;
import imagej.data.ChannelCollection;
import imagej.data.Dataset;
import imagej.data.DatasetService;
import imagej.data.DrawingTool;
import imagej.data.DrawingTool.TextJustification;
import imagej.ext.display.DisplayService;
import imagej.ext.menu.MenuConstants;
import imagej.ext.plugin.ImageJPlugin;
import imagej.ext.plugin.Menu;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;
import imagej.options.OptionsService;
import imagej.options.plugins.OptionsMemoryAndThreads;
import imagej.util.ColorRGB;
import imagej.util.Colors;
import imagej.util.Log;

// TODO
//   Have imageX.tif files and ImageX.metadata.txt files
//     Metadata file lists some useful info
//     - image attribution
//     - best color to render text in
//     - recommended font size???
//   This plugin should load a random image and display info including
//     attribution text

/**
 * Display information and credits about the ImageJ2 software. Note that some
 * of this code was adapted from code written by Wayne Rasband for ImageJ.
 * 
 * @author Barry DeZonia
 */
@Plugin(iconPath = "/icons/plugins/information.png", menu = {
	@Menu(label = MenuConstants.HELP_LABEL, weight = MenuConstants.HELP_WEIGHT,
		mnemonic = MenuConstants.HELP_MNEMONIC),
	@Menu(label = "About ImageJ...", weight = 43) }, headless = true)
public class AboutImageJ<T extends RealType<T> & NativeType<T>>
	implements ImageJPlugin
{
	// -- parameters --

	@Parameter
	private ImageJ context;
	
	@Parameter
	private DatasetService dataSrv;

	@Parameter
	private DisplayService dispSrv;
	
	// -- instance variables that are not parameters --
	private List<String> attributionStrings = new LinkedList<String>();
	private ColorRGB textColor = Colors.YELLOW;
	private ColorRGB outlineColor = Colors.BLACK;
	private int largestFontSize = 30;
	private ChannelCollection textChannels = null;
	private ChannelCollection outlineChannels = null;
	
	// -- public interface --
	
	@Override
	public void run() {
		final Dataset image = getData();
		drawTextOverImage(image);
		dispSrv.createDisplay("About ImageJ", image);
	}

	// -- private helpers --

	/**
	 * Returns a merged color Dataset as a backdrop.
	 */
	private Dataset getData() {
		
		final URL imageURL = getImageURL();
		
		final String filename;
		if (imageURL == null)
			filename = "";
		else
			filename = imageURL.toString().substring(5); // strip off "file:"
		
		final ImgPlus<T> img = getImage(filename);
		
		final String title = "About ImageJ " + ImageJ.VERSION;

		Dataset ds = null;
		
		// did we successfully load a background image?
		if (img != null) {
			// yes we did - inspect it
			ds = dataSrv.create(img);
			boolean validImage = true;
			validImage &= (ds.numDimensions() == 3);
			// Too restrictive? Ran into images where 3rd axis is mislabeled
			//validImage &= (ds.getAxisIndex(Axes.CHANNEL) == 2);
			validImage &= (ds.getImgPlus().firstElement().getBitsPerPixel() == 8);
			validImage &= (ds.isInteger());
			validImage &= (!ds.isSigned());
			if (validImage) {
				loadAttributes(filename);
			}
			else {
				ds = null;
			}
		}

		// Did we fail to load a valid dataset?
		if (ds == null) {
			Log.warn("Could not load a 3 channel unsigned 8 bit image as backdrop");
			// make a black 3 channel 8-bit unsigned background image.
			ds = dataSrv.create(
				new long[]{400,400,3} , title,
				new AxisType[]{Axes.X,Axes.Y,Axes.CHANNEL}, 8, false, false);
		}
		
		ds.setName(title);
		ds.setRGBMerged(true);
		
		return ds;
	}

	/**
	 * Loads an ImgPlus from a filename
	 */
	private ImgPlus<T> getImage(String filename) {
		if (filename == null) return null;
		try {
			final ImgOpener opener = new ImgOpener();
			// TODO - ImgOpener should be extended to handle URLs
			// Hack for now to get local file name
			return opener.openImg(filename);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns the URL of a backdrop image
	 */
	private URL getImageURL() {
		// TODO - cycle through one of many
		final String fname = "/images/image2.tif";  // NB - THIS PATH IS CORRECT 
		return getClass().getResource(fname);
	}

	/**
	 * Draws the textual information over a given merged color Dataset
	 */
	private void drawTextOverImage(Dataset ds) {
		textChannels = getChannels(textColor);
		outlineChannels = getChannels(outlineColor);
		final DrawingTool tool = new DrawingTool(ds);
		tool.setUAxis(0);
		tool.setVAxis(1);
		final long width = ds.dimension(0);
		final long x = width / 2;
		long y = 50;
		tool.setTextAntialiasing(true);
		//tool.setTextOutlineWidth(5);
		tool.setFontSize(largestFontSize);
		drawOutlinedText(tool, x, y, "ImageJ2 "+ImageJ.VERSION,
			TextJustification.CENTER, textChannels, outlineChannels);
		y += 5*tool.getFontSize()/4;
		tool.setFontSize((int)Math.round(0.6 * largestFontSize));
		for (final String line : getTextBlock()) {
			drawOutlinedText(tool, x, y, line, TextJustification.CENTER,
				textChannels, outlineChannels);
			y += 5*tool.getFontSize()/4;
		}
	}

	/**
	 * Returns a ChannelCollection containing the three RGB values of a given
	 * color.
	 */
	private ChannelCollection getChannels(ColorRGB color) {
		final List<Double> channels = new LinkedList<Double>();
		channels.add((double) color.getRed());
		channels.add((double) color.getGreen());
		channels.add((double) color.getBlue());
		return new ChannelCollection(channels);
	}

	/** Draws a text string and outline in two different set of fill values */
	private void drawOutlinedText(DrawingTool tool, long x, long y, String text,
		TextJustification just, ChannelCollection textValues,
		ChannelCollection outlineValues)
	{
		tool.setChannels(outlineValues);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) continue;
				tool.drawText(x+dx, y+dy, text, just);
			}
		}
		tool.setChannels(textValues);
		tool.drawText(x, y, text, just);
	}
	
	/**
	 * Returns the paragraph of textual information to display over the
	 * backdrop image
	 */
	private List<String> getTextBlock() {
		
		LinkedList<String> stringList = new LinkedList<String>();
		
		stringList.add("Open source image processing software");
		stringList.add("Copyright 2010, 2011, 2012");
		stringList.add("http://developer.imagej.net/");
		stringList.add(javaInfo());
		stringList.add(memoryInfo());
		stringList.addAll(attributionStrings);

		return stringList;
	}
	
	/**
	 * Returns a string showing java platform and version information
	 */
	private String javaInfo() {
		return "Java "+System.getProperty("java.version") +
				(is64Bit() ? " (64-bit)" : " (32-bit)");
	}
	
	/**
	 * Returns a string showing used and available memory information
	 */
	private String memoryInfo() {
		long inUse = currentMemory();
		String inUseStr = inUse<10000*1024?inUse/1024L+"K":inUse/1048576L+"MB";
		String maxStr="";
		long max = maxMemory();
		if (max>0L) {
			long percent = inUse * 100 / max;
			maxStr = " of "+max/1048576L+"MB ("+(percent<1 ? "<1" : percent) + "%)";
		}
		return inUseStr + maxStr;
	}
	
	/** Returns the amount of memory currently being used by ImageJ2. */
	private long currentMemory() {
		long freeMem = Runtime.getRuntime().freeMemory();
		long totMem = Runtime.getRuntime().totalMemory();
		return totMem-freeMem;
	}

	/** Returns the maximum amount of memory available to ImageJ2 or
		zero if ImageJ2 is unable to determine this limit. */
	private long maxMemory() {
		OptionsService srv = context.getService(OptionsService.class);
		OptionsMemoryAndThreads opts = srv.getOptions(OptionsMemoryAndThreads.class);
		long totMem = Runtime.getRuntime().totalMemory();
		long userMem = opts.getMaxMemory() * 1024L * 1024L;
		if (userMem > totMem)
			return totMem;
		return userMem;
	}

	/** Returns true if ImageJ2 is running a 64-bit version of Java. */
	private boolean is64Bit() {
		String osarch = System.getProperty("os.arch");
		return osarch!=null && osarch.indexOf("64")!=-1;
	}

	/**
	 * Loads attributes from an associated filename.ext.txt file if possible  
	 */
	private void loadAttributes(String baseFileName) {
		String fileName = baseFileName + ".txt";
		File file = new File(fileName);
		if (file.exists()) {
			Pattern attributionPattern = Pattern.compile("attribution\\s+\"(.*)\"");
			Pattern colorPattern = Pattern.compile("color\\s+([0-9]+)\\s+([0-9]+)\\s+([0-9]+)");
			Pattern fontsizePattern = Pattern.compile("fontsize\\s+([1-9][0-9]*)");
			try {
				FileInputStream fstream = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				//Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					Matcher attributionMatcher = attributionPattern.matcher(strLine);
					if (attributionMatcher.matches()) {
						attributionStrings.add(attributionMatcher.group(1));
					}
					Matcher colorMatcher = colorPattern.matcher(strLine);
					if (colorMatcher.matches()) {
						try {
							int r = Integer.parseInt(colorMatcher.group(1));
							int g = Integer.parseInt(colorMatcher.group(2));
							int b = Integer.parseInt(colorMatcher.group(3));
							textColor = new ColorRGB(r,g,b);
						} catch (Exception e) {
							// do nothing
						}
					}
					Matcher fontsizeMatcher = fontsizePattern.matcher(strLine);
					if (fontsizeMatcher.matches()) {
						try {
							largestFontSize = Integer.parseInt(fontsizeMatcher.group(1));
						} catch (Exception e) {
							// do nothing
						}
					}
				}
				//Close the input stream
				in.close();
			   
			}
			catch (Exception e) {
				// do nothing
			}
		}
	}
}
