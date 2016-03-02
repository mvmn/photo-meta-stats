package x.mvmn.photometastats.util;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

public class SwingUtil {
	public static void center(JFrame mainWindow) {
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ((dimension.getWidth() - mainWindow.getWidth()) / 2);
		int y = (int) ((dimension.getHeight() - mainWindow.getHeight()) / 2);
		mainWindow.setLocation(x, y);
	}
}
