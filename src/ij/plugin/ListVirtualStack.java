package ij.plugin;
import ijx.IjxImagePlus;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ijx.IjxImageStack;
import java.awt.*;
import java.io.*;
import java.util.*;

/** This plugin opens images specified by list of file paths as a virtual stack.
	It implements the File/Import/Stack From List command. */
public class ListVirtualStack extends VirtualStack implements PlugIn {
	static boolean virtual;
	String[] list;
	int nImages;
	int imageWidth, imageHeight;

	public void run(String arg) {
		OpenDialog  od = new OpenDialog("Open Image List", arg);
		String name = od.getFileName();
		if (name==null) return;
		String  dir = od.getDirectory();
		list = open(dir+name);
		nImages = list.length;
		//for (int i=0; i<list.length; i++)
		//	IJ.log(i+"  "+list[i]);
		if (list.length==0) {
			IJ.error("Stack From List", "The file path list is empty");
			return;
		}
		File f = new File(list[0]);
		if (!f.exists()) {
			IJ.error("Stack From List", "The first file on the list does not exist:\n \n"+list[0]);
			return;
		}
		IjxImagePlus imp = IJ.openImage(list[0]);
		if (imp==null) return;
		imageWidth = imp.getWidth();
		imageHeight = imp.getHeight();
		IjxImageStack stack = this;
		if (!showDialog(imp)) return;
		if (!virtual)
			stack = convertToRealStack(imp);
		IjxImagePlus imp2 = IJ.getFactory().newImagePlus(name, stack);
		imp2.setCalibration(imp.getCalibration());
		imp2.show();
	}
	
	boolean showDialog(IjxImagePlus imp) {
		double bytesPerPixel = 1;
		switch (imp.getType()) {
			case IjxImagePlus.GRAY16:
				bytesPerPixel=2; break;
			case IjxImagePlus.COLOR_RGB:
			case IjxImagePlus.GRAY32:
				bytesPerPixel=4; break;
		}
		double size = (imageWidth*imageHeight*bytesPerPixel)/(1024.0*1024.0);
		int digits = size*getSize()<10.0?1:0;
		String size1 = IJ.d2s(size*getSize(), digits)+" MB";
		String size2 = IJ.d2s(size,1)+" MB";
		GenericDialog gd = new GenericDialog("Open Stack From List");
		gd.addCheckbox("Use Virtual Stack", virtual);
		gd.addMessage("This "+imageWidth+"x"+imageHeight+"x"+getSize()+" stack will require "+size1+",\n or "+size2+" if opened as a virtual stack.");
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		virtual = gd.getNextBoolean();
		return true;
	}
	
	IjxImageStack convertToRealStack(IjxImagePlus imp) {
		IjxImageStack stack2 = IJ.getFactory().newImageStack(imageWidth, imageHeight, imp.getProcessor().getColorModel());
		int n = this.getSize();
		for (int i=1; i<=this.getSize(); i++) {
			IJ.showProgress(i, n);
			IJ.showStatus("Opening: "+i+"/"+n);
			ImageProcessor ip2 = this.getProcessor(i);
			stack2.addSlice(null, ip2);
		}
		return stack2;
	}
	
	String[] open(String path) {
		Vector v = new Vector();
		File file = new File(path);
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			while (true) {
				String s=r.readLine();
				if (s==null)
					break;
				else
					v.addElement(s);
			}
			r.close();
    		String[] list = new String[v.size()];
			v.copyInto((String[])list);
    		return list;
		}
		catch (Exception e) {
			IJ.error("Open List Error \n\""+e.getMessage()+"\"\n");
		}
		return null;
	}



	/** Deletes the specified image, were 1<=n<=nslices. */
	public void deleteSlice(int n) {
		if (n<1 || n>nImages)
			throw new IllegalArgumentException("Argument out of range: "+n);
		if (nImages<1) return;
		for (int i=n; i<nImages; i++)
			list[i-1] = list[i];
		list[nImages-1] = null;
		nImages--;
	}
	
	/** Returns an ImageProcessor for the specified slice,
		were 1<=n<=nslices. Returns null if the stack is empty.
	*/
	public ImageProcessor getProcessor(int n) {
		if (n<1 || n>nImages)
			throw new IllegalArgumentException("Argument out of range: "+n);
		IjxImagePlus imp = IJ.openImage(list[n-1]);
		if (imp.getWidth()!=imageWidth || imp.getHeight()!=imageHeight) {
			IJ.error("List Virtual Stack", "Image dimensions do not match:\n \n"+list[n-1]);
			return null;
		}
		if (imp!=null)
			return imp.getProcessor();
		else
			return null;
	 }
 
	 /** Returns the number of images in this stack. */
	public int getSize() {
		return nImages;
	}

	/** Returns the name of the specified image. */
	public String getSliceLabel(int n) {
		if (n<1 || n>nImages)
			throw new IllegalArgumentException("Argument out of range: "+n);
		return (new File(list[n-1])).getName();
	}
	
	public int getWidth() {
		return imageWidth;
	}

	public int getHeight() {
		return imageHeight;
	}


}
