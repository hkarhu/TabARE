package fi.conf.tabare;


public class TrackableBlob extends TrackableItem {

	public TrackableBlob(double x, double y, int id) {
		super(x, y, id, ItemType.blob);
		this.quality = 1;
	}
	
}
