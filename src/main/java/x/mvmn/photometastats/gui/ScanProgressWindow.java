package x.mvmn.photometastats.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import x.mvmn.photometastats.service.PhotoStatsService.PhotoScanControl;
import x.mvmn.photometastats.util.SwingUtil;

public class ScanProgressWindow extends JFrame {
	private static final long serialVersionUID = 6523614448231608912L;
	protected final PhotoScanControl scanControl;
	protected final JButton btnInterrupt = new JButton("Stop scan.");

	public ScanProgressWindow(final String folderPath, final PhotoScanControl scanControl, final JLabel countLabel) {
		super("Scanning " + folderPath);
		this.scanControl = scanControl;
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(countLabel, BorderLayout.CENTER);
		this.getContentPane().add(btnInterrupt, BorderLayout.SOUTH);

		btnInterrupt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actEvent) {
				scanControl.interrupt();
			}
		});

		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		this.setPreferredSize(new Dimension(400, 80));
		this.pack();
		SwingUtil.center(this);
		this.setVisible(true);
	}
}
