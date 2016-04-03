package x.mvmn.photometastats;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;

import x.mvmn.photometastats.gui.ChartHelper;
import x.mvmn.photometastats.gui.ScanProgressWindow;
import x.mvmn.photometastats.service.Callback;
import x.mvmn.photometastats.service.ScanCallback;
import x.mvmn.photometastats.service.Function;
import x.mvmn.photometastats.service.PhotoStatsService;
import x.mvmn.photometastats.service.PhotoStatsService.PhotoScanControl;
import x.mvmn.photometastats.util.StacktraceUtil;
import x.mvmn.photometastats.util.SwingUtil;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;

public class PhotoMetaStats {

	public static void main(String[] args) throws Exception {
		new PhotoMetaStats();
	}

	protected final JFrame mainWindow = new JFrame("Photos Metadata Statistics");
	protected final JTextArea txaTagNames = new JTextArea();
	protected final JTextArea txaLog = new JTextArea();
	protected final JCheckBox enableScanLog = new JCheckBox("Enable scan log");
	protected final JButton btnScan = new JButton("Scan folder...");
	protected final JTabbedPane tabPane = new JTabbedPane();
	protected final StringBuffer logBuffer = new StringBuffer();

	public PhotoMetaStats() throws Exception {
		final String defaultTagNames = IOUtils.toString(PhotoMetaStats.class.getResourceAsStream("/default_tags_list"));
		txaTagNames.setText(defaultTagNames);

		mainWindow.getContentPane().setLayout(new BorderLayout());
		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		JLabel topLabel = new JLabel("Written by Mykola Makhin. Uses Metadata Extractor library by Drew Noakes.");
		topPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
		topPanel.add(topLabel);
		topPanel.add(enableScanLog);
		mainWindow.getContentPane().add(topPanel, BorderLayout.NORTH);
		tabPane.add("Exif tags to use", new JScrollPane(txaTagNames));
		tabPane.add("Log", new JScrollPane(txaLog));
		mainWindow.getContentPane().add(tabPane, BorderLayout.CENTER);
		mainWindow.getContentPane().add(btnScan, BorderLayout.SOUTH);
		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		btnScan.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actEvent) {
				final JFileChooser fileChooser = new JFileChooser();
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
					final String tagNames = txaTagNames.getText();
					final Set<String> filterAllowedTagsLowercase = new HashSet<String>();
					for (final String tagName : tagNames.split("\n")) {
						if (!tagName.trim().isEmpty()) {
							filterAllowedTagsLowercase.add(tagName.trim().toLowerCase());
						}
					}
					doScan(fileChooser.getSelectedFile(), filterAllowedTagsLowercase, enableScanLog.isSelected());
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

	protected class GuiScanCallback implements ScanCallback, Callback<ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>>, Runnable {

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
		public void call(ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> result) {
			if (enableLog) {
				log("Scan finished - " + System.currentTimeMillis());
				logFlush();
			}

			updateLabel();
			if (result != null) {
				final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

				FontMetrics fontMetrics = progressLabel.getFontMetrics(progressLabel.getFont());
				for (final String key : new TreeSet<String>(result.keySet())) {
					final String label = key.substring(key.indexOf("/") + 1);
					tabbedPane.add(label, new JScrollPane(ChartHelper.createChartPanel(label, result.get(key), fontMetrics)));
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

	protected void doScan(final File folder, final Set<String> filterAllowedTagsLowercase, boolean enableLog) {
		if (enableLog) {
			log("Scan started - " + System.currentTimeMillis());
			logFlush();
		}

		final GuiScanCallback callback = new GuiScanCallback(folder.getAbsolutePath(), enableLog);

		final PhotoScanControl scanControl = new PhotoStatsService().scan(folder, null, new Function<Directory, Boolean>() {
			@Override
			public Boolean get(Directory directory) {
				final String dirName = directory.getName().toLowerCase();
				return dirName.contains("exif") && !dirName.contains("thumbnail");
			}
		}, new Function<Tag, Boolean>() {
			@Override
			public Boolean get(Tag tag) {
				return filterAllowedTagsLowercase.contains(tag.getTagName().toLowerCase());
			}
		}, callback, callback);
		callback.setProgressWindow(new ScanProgressWindow(folder.getAbsolutePath(), scanControl, callback.getProgressLabel()));
	}

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
