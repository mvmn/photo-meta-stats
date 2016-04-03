package x.mvmn.photometastats.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StacktraceUtil {

	public static String getStacktrace(Throwable t) {
		final StringWriter strw = new StringWriter();
		t.printStackTrace(new PrintWriter(strw));
		return strw.toString();
	}
}
