package imagej.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class CheckSezpoz {
	private final static String[] annotationClasses = {
		"imagej.ext.plugin.Plugin",
		"imagej.service.Service"
	};

	public static boolean check() throws IOException {
		boolean upToDate = true;
		for (final String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
			if (!check(new File(path))) upToDate = false;
		}
		return upToDate;
	}
	
	public static boolean check(final File file) throws IOException {
		if (!file.exists()) return true;
		if (file.isDirectory()) return checkDirectory(file);
		else if (file.isFile() && file.getName().endsWith(".jar")) checkJar(file);
		else Log.warn("Skipping sezpoz check of " + file);
		return true;
	}
	
	public static boolean checkDirectory(final File directory) throws IOException {
		// We know a few projects not needing any annotations
		final String path = directory.getPath();
		if (path.endsWith("ui/app/target/classes") ||
				path.endsWith("ui/awt-swing/util/target/classes") ||
				path.endsWith("ui/awt-swing/common/target/classes") ||
				path.endsWith("ImageJA/target/classes")) {
			return true;
		}

		final File[] annotations = new File(directory, "META-INF/annotations/").listFiles();
		if (annotations == null) {
			Log.warn("Class path " + directory + " does not contain any annotations");
			fix(directory);
			return false;
		}

		long mtime = -1;
		for (final File annotationFile : annotations) {
			if (annotationFile.exists() && mtime < annotationFile.lastModified()) mtime = annotationFile.lastModified();
		}
		if (!checkDirectory(directory, mtime)) {
			fix(directory);
			return false;
		}
		return true;
	}
	
	public static boolean checkDirectory(final File directory, final long olderThan) throws IOException {
		if (directory.getName().equals("META-INF")) return true;

		final File[] list = directory.listFiles();
		if (list == null) return true;
		for (final File file : list) {
			if (file.isDirectory()) {
				if (!checkDirectory(file, olderThan)) return false;
			}
			else if (file.isFile() && file.lastModified() > olderThan) {
System.err.println(file.getPath() + " last modified: " + new java.util.Date(file.lastModified()));
System.err.println("older than: " + new java.util.Date(olderThan));
//throw new IOException("Annotations for " + file + " are out-of-date!");
				return false;
			}
		}
		return true;
	}
	
	public static void checkJar(final File file) throws IOException {
		final JarFile jar = new JarFile(file);
		long mtime = -1;
		for (final String name : annotationClasses) {
			final JarEntry annotations = jar.getJarEntry("META-INF/annotations/" + name);
			if (annotations != null && mtime < annotations.getTime()) mtime = annotations.getTime();
		}
		if (mtime < 0) {
			// Eclipse cannot generate .jar files (except in manual mode). Assume everything is alright
			return;
		}
		for (final JarEntry entry : iterate(jar.entries())) {
			if (entry.getTime() > mtime) {
				throw new IOException("Annotations for " + entry + " in " + file + " are out-of-date!");
			}
		}
	}

	public static boolean fix(final File directory) {
		Log.info("Running sezpoz annotation on " + directory);
		final Method aptProcess;
		try {
			Class<?> aptClass = CheckSezpoz.class.getClassLoader().loadClass("com.sun.tools.apt.Main");
			aptProcess = aptClass.getMethod("process", new Class[] { String[].class });
		} catch (Exception e) {
			Log.error("Could not fix " + directory + ": apt not found", e);
			return false;
		}
		if (!directory.getPath().endsWith("target/classes")) return false; // TODO: tell the user why
		final File baseDirectory = directory.getParentFile().getParentFile();
		if (baseDirectory == null) return false;
		final File srcDirectory = new File(baseDirectory, "src/main/java");
		if (!srcDirectory.exists()) return false;

		// before running, remove possibly outdated annotations
		final File[] obsoleteAnnotations = new File(directory, "META-INF/annotations").listFiles();
		if (obsoleteAnnotations != null) {
			for (final File annotation : obsoleteAnnotations) annotation.delete();
		}

		List<String> aptArgs = new ArrayList<String>();
		//aptArgs.add("-nocompile");
		aptArgs.add("-factory");
		aptArgs.add("net.java.sezpoz.impl.IndexerFactory");
		aptArgs.add("-d");
		aptArgs.add(directory.getPath());
		addJavaPathsRecursively(aptArgs, srcDirectory);
		final String[] args = aptArgs.toArray(new String[aptArgs.size()]);
		try {
			aptProcess.invoke(null, new Object[] { args });
		} catch (Exception e) {
			Log.error("Could not fix " + directory + ": apt failed", e);
			return false;
		}
		return true;
	}

	protected static void addJavaPathsRecursively(final List<String> list,
		final File directory)
	{
		final File[] files = directory.listFiles();
		if (files == null) return;
		for (final File file : files) {
			if (file.isDirectory()) addJavaPathsRecursively(list, file);
			else if (file.isFile() && file.getName().endsWith(".java")) list.add(file.getPath());
		}
	}

	public static <T> Iterable<T> iterate(final Enumeration<T> en) {
    final Iterator<T> iterator = new Iterator<T>() {
          public boolean hasNext() {  
              return en.hasMoreElements();  
            }
          
            public T next() {
              return en.nextElement();  
            }
            
            public void remove() {
              throw new UnsupportedOperationException();  
            }
    };
    
    return new Iterable<T>() {
        public Iterator<T> iterator() {
            return iterator;
        }
    };
	}

	public static void main(String[] args) throws IOException {
		check();
	}
}
