package x.mvmn.photometastats;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.io.IOUtils;

import x.mvmn.photometastats.gui.ChartHelper;
import x.mvmn.photometastats.gui.ScanProgressWindow;
import x.mvmn.photometastats.model.MetaKey;
import x.mvmn.photometastats.service.Callback;
import x.mvmn.photometastats.service.Function;
import x.mvmn.photometastats.service.PhotoStatsService;
import x.mvmn.photometastats.service.PhotoStatsService.PhotoScanControl;
import x.mvmn.photometastats.service.ScanCallback;
import x.mvmn.photometastats.util.StacktraceUtil;
import x.mvmn.photometastats.util.SwingUtil;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class PhotoMetaStats {

	public static void main(String[] args) throws Exception {
		new PhotoMetaStats();
	}

	protected final JFrame mainWindow = new JFrame("Photos Metadata Statistics");
	protected final JTextArea txaLog = new JTextArea();
	protected final JCheckBox enableScanLog = new JCheckBox("Enable scan log");
	protected final JCheckBox includeAll = new JCheckBox("Extract all available metadata");
	protected final JButton btnScan = new JButton("Scan folder...");
	protected final JTabbedPane tabPane = new JTabbedPane();
	protected final StringBuffer logBuffer = new StringBuffer();
	protected final DefaultTableModel tblFilterTagsModel = new DefaultTableModel();
	protected final JTable tblFilterTags = new JTable(tblFilterTagsModel);

	protected final JButton btnMetaAdd = new JButton("+");
	protected final JButton btnMetaDel = new JButton("-");

	protected final JButton btnMetaChoose = new JButton("Add from file scan...");

	public PhotoMetaStats() throws Exception {
		final String defaultTagNames = IOUtils.toString(PhotoMetaStats.class.getResourceAsStream("/default_tags_list"));
		tblFilterTagsModel.addColumn("Metadata directory");
		tblFilterTagsModel.addColumn("Metadata tag");
		for (final String dtn : defaultTagNames.split("\n")) {
			if (!dtn.trim().isEmpty() && dtn.contains("/")) {
				tblFilterTagsModel.addRow(new Object[] { dtn.split("/")[0], dtn.split("/")[1] });
			}
		}

		mainWindow.getContentPane().setLayout(new BorderLayout());
		JPanel topPanel = new JPanel(new GridLayout(3, 1));
		JLabel topLabel = new JLabel("Written by Mykola Makhin. Uses Metadata Extractor library by Drew Noakes.");
		topPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
		topPanel.add(topLabel);
		topPanel.add(enableScanLog);
		topPanel.add(includeAll);
		mainWindow.getContentPane().add(topPanel, BorderLayout.NORTH);
		JPanel metadataPanel = new JPanel(new BorderLayout());
		metadataPanel.add(new JScrollPane(tblFilterTags), BorderLayout.CENTER);
		JPanel metadataBtnPanel = new JPanel(new GridLayout(2, 1));
		metadataBtnPanel.add(btnMetaAdd);
		metadataBtnPanel.add(btnMetaDel);
		metadataPanel.add(btnMetaChoose, BorderLayout.NORTH);
		metadataPanel.add(metadataBtnPanel, BorderLayout.SOUTH);
		tabPane.add("Metadata to extract", metadataPanel);
		tabPane.add("Log", new JScrollPane(txaLog));
		mainWindow.getContentPane().add(tabPane, BorderLayout.CENTER);
		mainWindow.getContentPane().add(btnScan, BorderLayout.SOUTH);
		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		btnMetaDel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = tblFilterTags.getSelectedRows();
				Arrays.sort(selectedRows);
				for (int i = selectedRows.length - 1; i >= 0; i--) {
					tblFilterTagsModel.removeRow(selectedRows[i]);
				}
			}
		});

		btnMetaAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tblFilterTagsModel.addRow(new Object[] { "Metadata directory", "Metadata tag" });
			}
		});

		btnMetaChoose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fileChooser = new JFileChooser();
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

				if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
					try {
						Metadata metadata = ImageMetadataReader.readMetadata(fileChooser.getSelectedFile());
						for (final Directory dir : metadata.getDirectories()) {
							for (final Tag tag : dir.getTags()) {
								tblFilterTagsModel.addRow(new Object[] { tag.getDirectoryName(), tag.getTagName() });
							}
						}
					} catch (Exception e1) {
						log("Error occurred while scanning file for metadata", e1);
						JOptionPane.showMessageDialog(null, "Error occurred " + e1.getMessage());
					}
				}
			}
		});

		btnScan.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actEvent) {
				final JFileChooser fileChooser = new JFileChooser();
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
					final Map<String, Set<String>> filtersLowercase = new HashMap<String, Set<String>>();
					for (int i = 0; i < tblFilterTagsModel.getRowCount(); i++) {
						final String dir = tblFilterTagsModel.getValueAt(i, 0).toString().trim().toLowerCase();
						final String tag = tblFilterTagsModel.getValueAt(i, 1).toString().trim().toLowerCase();
						if (!dir.isEmpty() && !tag.isEmpty()) {
							Set<String> tagsSet = filtersLowercase.get(dir);
							if (tagsSet == null) {
								tagsSet = new HashSet<String>();
								filtersLowercase.put(dir, tagsSet);
							}
							tagsSet.add(tag);
						}
					}
					doScan(fileChooser.getSelectedFile(), filtersLowercase, enableScanLog.isSelected());
				}
			}
		});

		mainWindow.setPreferredSize(new Dimension(800, 600));
		mainWindow.pack();
		SwingUtil.center(mainWindow);
		mainWindow.setVisible(true);

		log("Started at " + System.currentTimeMillis());
		log("System properties:");
		for (Map.Entry<?, ?> e : System.getProperties().entrySet()) {
			log(" - " + e.getKey().toString() + " = " + e.getValue());
		}
		logFlush();
	}

	protected class GuiScanCallback implements ScanCallback, Callback<ConcurrentHashMap<MetaKey, ConcurrentHashMap<String, AtomicInteger>>>, Runnable {

		protected volatile long scanCount = 0;
		protected volatile ScanProgressWindow progressWindow;
		protected final AtomicBoolean hideProgress = new AtomicBoolean(false);

		protected final JLabel progressLabel = new JLabel("Preparing to scan.", JLabel.CENTER);
		protected final String folderPath;
		protected final boolean enableLog;

		public GuiScanCallback(final String folderPath, final boolean enableLog) {
			this.folderPath = folderPath;
			this.enableLog = enableLog;
		}

		public JLabel getProgressLabel() {
			return progressLabel;
		}

		@Override
		public void call(ConcurrentHashMap<MetaKey, ConcurrentHashMap<String, AtomicInteger>> result) {
			if (enableLog) {
				log("Scan finished - " + System.currentTimeMillis());
				logFlush();
			}

			updateLabel();
			if (result != null) {
				final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
				Map<String, JTabbedPane> directoryTabs = new HashMap<String, JTabbedPane>();
				FontMetrics fontMetrics = progressLabel.getFontMetrics(progressLabel.getFont());
				for (final MetaKey key : new TreeSet<MetaKey>(result.keySet())) {
					JTabbedPane dirTabs = directoryTabs.get(key.getDirectory());
					if (dirTabs == null) {
						dirTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
						directoryTabs.put(key.getDirectory(), dirTabs);
						tabbedPane.add(key.getDirectory(), dirTabs);
					}
					dirTabs.add(key.getTag(), new JScrollPane(ChartHelper.createChartPanel(key.getTag(), result.get(key), fontMetrics)));
				}
				final JFrame resultsFrame = new JFrame("Scan results for " + folderPath);
				resultsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				resultsFrame.getContentPane().setLayout(new BorderLayout());
				resultsFrame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						resultsFrame.pack();
						SwingUtil.center(resultsFrame);
						resultsFrame.setVisible(true);
					}
				});
			}
			synchronized (hideProgress) {
				hideProgress.set(true);
				if (this.progressWindow != null) {
					this.progressWindow.setVisible(false);
					this.progressWindow.dispose();
				}
			}
		}

		public void setProgressWindow(ScanProgressWindow progressWindow) {
			synchronized (hideProgress) {
				this.progressWindow = progressWindow;
				if (hideProgress.get()) {
					progressWindow.setVisible(false);
					progressWindow.dispose();
				}
			}
		}

		public void updateLabel() {
			SwingUtilities.invokeLater(this);
		}

		@Override
		public void run() {
			progressLabel.setText("Scanned files: " + scanCount);
		}

		@Override
		public void call(long val, String info, Throwable error) {
			if (val >= 0) {
				scanCount = val;
				if (val % 100 == 0) {
					updateLabel();
					if (enableLog) {
						logFlush();
					}
				}
			}
			if (enableLog) {
				log(info, error);
			}
		}
	}

	protected Function<Directory, Boolean> getDirectoryFilter(final Map<String, Set<String>> filterAllowedDirectoriesTagsLowercase) {
		return includeAll.isSelected() ? null : new Function<Directory, Boolean>() {
			@Override
			public Boolean get(Directory directory) {
				final String dirName = directory.getName().toLowerCase();
				return filterAllowedDirectoriesTagsLowercase.containsKey(dirName);
			}
		};
	}

	protected Function<Tag, Boolean> getTagFilter(final Map<String, Set<String>> filterAllowedDirectoriesTagsLowercase) {
		return includeAll.isSelected() ? null : new Function<Tag, Boolean>() {
			@Override
			public Boolean get(Tag tag) {
				final Set<String> allowedTags = filterAllowedDirectoriesTagsLowercase.get(tag.getDirectoryName().toLowerCase());
				return allowedTags != null && allowedTags.contains(tag.getTagName().toLowerCase());
			}
		};
	}

	protected void doScan(final File folder, final Map<String, Set<String>> filterAllowedDirectoriesTagsLowercase, boolean enableLog) {
		if (enableLog) {
			log("Scan started - " + System.currentTimeMillis());
			logFlush();
		}

		final GuiScanCallback callback = new GuiScanCallback(folder.getAbsolutePath(), enableLog);
		final PhotoScanControl scanControl = new PhotoStatsService().scan(folder, null, getDirectoryFilter(filterAllowedDirectoriesTagsLowercase),
				getTagFilter(filterAllowedDirectoriesTagsLowercase), callback, callback);
		callback.setProgressWindow(new ScanProgressWindow(folder.getAbsolutePath(), scanControl, callback.getProgressLabel()));
	}

	// protected void doScan(final File folder, final Set<String> filterAllowedTagsLowercase, boolean enableLog) {
	// if (enableLog) {
	// log("Scan started - " + System.currentTimeMillis());
	// logFlush();
	// }
	//
	// final GuiScanCallback callback = new GuiScanCallback(folder.getAbsolutePath(), enableLog);
	//
	// final PhotoScanControl scanControl = new PhotoStatsService().scan(folder, null, new Function<Directory, Boolean>() {
	// @Override
	// public Boolean get(Directory directory) {
	// final String dirName = directory.getName().toLowerCase();
	// return dirName.contains("exif") && !dirName.contains("thumbnail");
	// }
	// }, new Function<Tag, Boolean>() {
	// @Override
	// public Boolean get(Tag tag) {
	// return filterAllowedTagsLowercase.contains(tag.getTagName().toLowerCase());
	// }
	// }, callback, callback);
	// callback.setProgressWindow(new ScanProgressWindow(folder.getAbsolutePath(), scanControl, callback.getProgressLabel()));
	// }

	protected void log(final String message) {
		log(message, null);
	}

	protected void log(final String message, final Throwable err) {
		logBuffer.append(message).append("\n");
		if (err != null) {
			logBuffer.append(StacktraceUtil.getStacktrace(err)).append("\n");
		}
	}

	protected void logFlush() {
		final String log;
		synchronized (logBuffer) {
			log = logBuffer.toString();
			logBuffer.setLength(0);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				txaLog.append(log);
			}
		});
	}
}
