package x.mvmn.photometastats.service;

public interface ScanCallback {

	public void call(long val, final String info, final Throwable error);
}
