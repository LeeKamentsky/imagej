
package imagej.updater.core;

import imagej.updater.util.Downloader;
import imagej.updater.util.Progress;
import imagej.updater.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Installer extends Downloader {

	protected PluginCollection plugins;

	public Installer(final PluginCollection plugins, final Progress progress) {
		this.plugins = plugins;
		addProgress(progress);
		addProgress(new VerifyFiles());
	}

	class Download implements FileDownload {

		PluginObject plugin;
		String url, destination;

		Download(final PluginObject plugin, final String url,
			final String destination)
		{
			this.plugin = plugin;
			this.url = url;
			this.destination = destination;
		}

		@Override
		public String toString() {
			return plugin.getFilename();
		}

		@Override
		public String getDestination() {
			return destination;
		}

		@Override
		public String getURL() {
			return url;
		}

		@Override
		public long getFilesize() {
			return plugin.filesize;
		}
	}

	public synchronized void start() throws IOException {
		// mark for removal
		for (final PluginObject plugin : plugins.toUninstall())
			try {
				plugin.stageForUninstall();
			}
			catch (final IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not mark '" + plugin +
					"' for removal");
			}

		final List<FileDownload> list = new ArrayList<FileDownload>();
		for (final PluginObject plugin : plugins.toInstallOrUpdate()) {
			final String name = plugin.filename;
			String saveTo = Util.prefixUpdate(name);
			if (Util.isLauncher(name)) {
				saveTo = Util.prefix(name);
				final File orig = new File(saveTo);
				final File old = new File(saveTo + ".old");
				if (old.exists()) old.delete();
				orig.renameTo(old);
			}

			final String url = plugins.getURL(plugin);
			final Download file = new Download(plugin, url, saveTo);
			list.add(file);
		}

		start(list);
	}

	class VerifyFiles implements Progress {

		@Override
		public void itemDone(final Object item) {
			verify((Download) item);
		}

		@Override
		public void setTitle(final String title) {}

		@Override
		public void setCount(final int count, final int total) {}

		@Override
		public void addItem(final Object item) {}

		@Override
		public void setItemCount(final int count, final int total) {}

		@Override
		public void done() {}
	}

	public void verify(final Download download) {
		final String fileName = download.getDestination();
		final long size = download.getFilesize();
		final long actualSize = Util.getFilesize(fileName);
		if (size != actualSize) throw new RuntimeException(
			"Incorrect file size for " + fileName + ": " + actualSize +
				" (expected " + size + ")");

		final PluginObject plugin = download.plugin;
		final String digest = download.plugin.getChecksum();
		String actualDigest;
		try {
			actualDigest = Util.getDigest(plugin.getFilename(), fileName);
		}
		catch (final Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not verify checksum " + "for " +
				fileName);
		}

		if (!digest.equals(actualDigest)) throw new RuntimeException(
			"Incorrect checksum " + "for " + fileName + ":\n" + actualDigest +
				"\n(expected " + digest + ")");

		plugin.setLocalVersion(digest, plugin.getTimestamp());
		plugin.setStatus(PluginObject.Status.INSTALLED);

		if (Util.isLauncher(fileName) && !Util.platform.startsWith("win")) try {
			Runtime.getRuntime().exec(
				new String[] { "chmod", "0755", download.destination });
		}
		catch (final Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not mark " + fileName +
				" as executable");
		}
	}
}
