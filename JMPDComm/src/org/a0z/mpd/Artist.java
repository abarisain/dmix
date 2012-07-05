package org.a0z.mpd;

public class Artist extends Item {
	public static String singleAlbumFormat="%1 Album";
	public static String multipleAlbumsFormat="%1 Albums";

	private final String name;
	private final String sort;
	//private final boolean isVa;
	private final int albumCount;

	public Artist(String name, int albumCount) {
		this.name=name;
		//this.isVa=null!=name && name.equals("Various Artists");
		if (null!=name && name.startsWith("The ")) {
			sort=name.substring(4);
		} else {
			sort=null;
		}
		this.albumCount=albumCount;
	}

	public String getName() {
		return name;
	}

	public String sort() {
		return null==sort ? name : sort;
	}

	@Override
	public String subText() {
		if (0==albumCount) {
			return null;
		}

		return String.format(1==albumCount ? singleAlbumFormat : multipleAlbumsFormat, albumCount);
	}

	@Override
    public int compareTo(Item o) {
		if (o instanceof Artist) {
			Artist oa=(Artist)o;
			/*
			if (isVa && !oa.isVa) {
				return -1;
			}
			if (!isVa && oa.isVa) {
				return 1;
			}
			*/
			return sort().compareTo(oa.sort());
		}

		return super.compareTo(o);
	}

    @Override
    public boolean equals(Object o) {
    	return (o instanceof Artist) && ((Artist)o).name.equals(name);
    }
}
