package x.mvmn.photometastats;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.io.IOUtils;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;

import x.mvmn.photometastats.service.Callback;
import x.mvmn.photometastats.service.CallbackLong;
import x.mvmn.photometastats.service.Function;
import x.mvmn.photometastats.service.PhotoStatsService;
import x.mvmn.photometastats.util.SwingUtil;

public class PhotoMetaStats {

	public static void main(String[] args) throws Exception {
		new PhotoMetaStats();
	}

	protected final JFrame mainWindow = new JFrame("Photos Metadata Statistics");
	protected final JTextArea txaTagNames = new JTextArea();
	protected final JButton btnScan = new JButton("Scan folder...");

	public PhotoMetaStats() throws Exception {
		final String defaultTagNames = IOUtils.toString(PhotoMetaStats.class.getResourceAsStream("/default_tags_list"));
		txaTagNames.setText(defaultTagNames);

		mainWindow.getContentPane().setLayout(new BorderLayout());
		mainWindow.getContentPane().add(new JLabel("Written by Mykola Makhin. Uses Metadata Extractor library by Drew Noakes."), BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(txaTagNames);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Exif tags to use"));
		mainWindow.getContentPane().add(scrollPane, BorderLayout.CENTER);
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
					doScan(fileChooser.getSelectedFile(), filterAllowedTagsLowercase);
				}
			}
		});

		mainWindow.setPreferredSize(new Dimension(800, 600));
		mainWindow.pack();
		SwingUtil.center(mainWindow);
		mainWindow.setVisible(true);
	}

	protected void doScan(final File folder, final Set<String> filterAllowedTagsLowercase) {
		new PhotoStatsService().scan(folder, null, new Function<Directory, Boolean>() {
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
		}, new CallbackLong() {
			@Override
			public void call(long val) {
				if (val % 100 == 0) {
					System.out.println("Scanned " + val);
				}
			}
		}, new Callback<ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>>() {
			@Override
			public void call(ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> arg) {
				for (Map.Entry<String, ConcurrentHashMap<String, AtomicInteger>> tags : arg.entrySet()) {
					System.out.println(tags.getKey() + ":");
					for (Map.Entry<String, AtomicInteger> vals : tags.getValue().entrySet()) {
						System.out.println("\t" + vals.getKey() + ": \t\t\t" + vals.getValue().get());
					}
				}
			}
		});
	}
}
