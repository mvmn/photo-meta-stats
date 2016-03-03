package x.mvmn.photometastats.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FolderScanService {

	protected int scanBreakLimit = 100;

	protected static class FilesFoldersComparator implements Comparator<File> {
		public int compare(File f1, File f2) {
			return (f1.isDirectory() ? 1 : 0) - (f2.isDirectory() ? 1 : 0);
		}

		public static final FilesFoldersComparator INSTANCE = new FilesFoldersComparator();
	}

	protected class ScanAction extends RecursiveAction {
		private static final long serialVersionUID = -1013804045503042034L;

		protected final ForkJoinPool pool;
		protected final List<File> files;
		protected final Function<String, Boolean> filter;
		protected final Callback<File> callback;
		protected final AtomicBoolean interrupt;

		public ScanAction(ForkJoinPool pool, List<File> files, Function<String, Boolean> filter, Callback<File> callback, AtomicBoolean interrupt) {
			this.pool = pool;
			this.files = new ArrayList<File>(files);
			this.filter = filter;
			this.callback = callback;
			this.interrupt = interrupt;
		}

		@Override
		protected void compute() {
			List<File> processingFiles = this.files;
			while (processingFiles.size() > scanBreakLimit && !interrupt.get()) {
				pool.submit(new ScanAction(pool, processingFiles.subList(0, scanBreakLimit), filter, callback, interrupt));
				processingFiles = processingFiles.subList(scanBreakLimit, processingFiles.size());
			}
			Collections.sort(processingFiles, FilesFoldersComparator.INSTANCE);

			List<File> subFiles = new ArrayList<File>();
			for (File file : processingFiles) {
				if (interrupt.get()) {
					break;
				}
				if (filter == null || filter.get(file.getName())) {
					if (file.isDirectory()) {
						subFiles.addAll(Arrays.asList(file.listFiles()));
					} else {
						try {
							callback.call(file);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
				while (subFiles.size() > scanBreakLimit && !interrupt.get()) {
					pool.submit(new ScanAction(pool, subFiles.subList(0, scanBreakLimit), filter, callback, interrupt));
					subFiles = new ArrayList<File>(subFiles.subList(scanBreakLimit, subFiles.size()));
				}
			}
			if (!interrupt.get() && subFiles.size() > 0) {
				pool.submit(new ScanAction(pool, subFiles, filter, callback, interrupt));
			}
		}
	}

	public FolderScanControl scan(File folder, Function<String, Boolean> filter, Callback<File> callback) {
		final ForkJoinPool pool = new ForkJoinPool();
		AtomicBoolean interrupt = new AtomicBoolean(false);
		if (folder.exists() && folder.isDirectory()) {
			pool.submit(new ScanAction(pool, Arrays.asList(folder.listFiles()), filter, callback, interrupt));
		}

		return new FolderScanControl(interrupt, pool);
	}

	public class FolderScanControl {
		protected final AtomicBoolean interrupt;
		protected final ForkJoinPool pool;

		public FolderScanControl(AtomicBoolean interrupt, ForkJoinPool pool) {
			this.interrupt = interrupt;
			this.pool = pool;
		}

		public void interrupt() {
			this.interrupt.set(true);
		}

		public boolean isCompleted() {
			return pool.isQuiescent();
		}

		public boolean isInterrupted() {
			return interrupt.get();
		}

		public void awaitCompletion() {
			boolean finished = false;
			while (!finished && !interrupt.get()) {
				try {
					finished = pool.isQuiescent();
					if (!finished) {
						pool.awaitTermination(1, TimeUnit.SECONDS);
					} else {
						pool.shutdown();
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public int getScanBreakLimit() {
		return scanBreakLimit;
	}

	public void setScanBreakLimit(int scanBreakLimit) {
		this.scanBreakLimit = scanBreakLimit;
	}
}
