package x.mvmn.photometastats.service;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import x.mvmn.photometastats.service.FolderScanService.FolderScanControl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class PhotoStatsService {

	protected final FolderScanService scanService = new FolderScanService();

	public class PhotoScanControl {

		protected final FolderScanControl folderScanControl;
		protected final AtomicLong scannedFilesCount;

		public PhotoScanControl(final FolderScanControl folderScanControl, final AtomicLong scannedFilesCount) {
			this.folderScanControl = folderScanControl;
			this.scannedFilesCount = scannedFilesCount;
		}

		public void interrupt() {
			folderScanControl.interrupt();
		}

		public boolean isCompleted() {
			return folderScanControl.isCompleted();
		}

		public void awaitCompletion() {
			folderScanControl.awaitCompletion();
		}

		public long getScannedFilesCount() {
			return scannedFilesCount.get();
		}
	}

	public PhotoScanControl scan(final File folder, final Function<String, Boolean> filesFilter, final Function<Directory, Boolean> metadataDirectoryFilter,
			final Function<Tag, Boolean> metadataTagFilter, final CallbackLong progressCallback,
			final Callback<ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>> finishCallback) {
		final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> result = new ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>();
		final AtomicLong scannedFilesCount = new AtomicLong();
		final FolderScanControl folderScanControl = scanService.scan(folder, filesFilter, new Callback<File>() {
			@Override
			public void call(File file) {
				try {
					Metadata metadata = ImageMetadataReader.readMetadata(file);
					populateMap(result, metadata, metadataDirectoryFilter, metadataTagFilter);
					progressCallback.call(scannedFilesCount.incrementAndGet());
				} catch (Exception e) {
					if (!e.getMessage().contains("format is not supported")) {
						System.err.println("Metadata read failed - " + e.getMessage() + " - for " + file.getAbsolutePath());
					}
				}
			}
		});

		new Thread(new Runnable() {
			public void run() {
				folderScanControl.awaitCompletion();
				finishCallback.call(folderScanControl.isInterrupted() ? null : result);
			}
		}).start();

		return new PhotoScanControl(folderScanControl, scannedFilesCount);
	}

	protected void populateMap(ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> map, Metadata metadata,
			Function<Directory, Boolean> metadataDirectoryFilter, Function<Tag, Boolean> metadataTagFilter) {
		for (final Directory directory : metadata.getDirectories()) {
			if (metadataDirectoryFilter == null || metadataDirectoryFilter.get(directory)) {
				for (final Tag tag : directory.getTags()) {
					if (metadataTagFilter == null || metadataTagFilter.get(tag)) {
						final String key = directory.getName() + "/" + tag.getTagName();
						ConcurrentHashMap<String, AtomicInteger> vals = map.get(key);
						if (vals == null) {
							vals = new ConcurrentHashMap<String, AtomicInteger>();
							ConcurrentHashMap<String, AtomicInteger> existingVals = map.putIfAbsent(key, vals);
							if (existingVals != null) {
								vals = existingVals;
							}
						}
						final String valKey = directory.getString(tag.getTagType());
						AtomicInteger count = vals.get(valKey);
						if (count == null) {
							count = new AtomicInteger();
							AtomicInteger existingCount = vals.putIfAbsent(valKey, count);
							if (existingCount != null) {
								count = existingCount;
							}
						}
						count.incrementAndGet();
					}
				}
			}
		}
	}
}
