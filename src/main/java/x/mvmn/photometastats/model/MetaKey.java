package x.mvmn.photometastats.model;

public class MetaKey implements Comparable<MetaKey> {

	protected final String directory;
	protected final String tag;

	public MetaKey(String directory, String tag) {
		this.directory = directory;
		this.tag = tag;
	}

	public String getDirectory() {
		return directory;
	}

	public String getTag() {
		return tag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((directory == null) ? 0 : directory.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetaKey other = (MetaKey) obj;
		if (directory == null) {
			if (other.directory != null)
				return false;
		} else if (!directory.equals(other.directory))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MetaKey [directory=").append(directory).append(", tag=").append(tag).append("]");
		return builder.toString();
	}

	@Override
	public int compareTo(MetaKey o) {
		int result = this.getDirectory().compareTo(o.getDirectory()) * 1000;
		if (result == 0) {
			result = this.getTag().compareTo(o.getTag());
		}
		return result;
	}

}
