package fi.conf.tabare;


public class TrackableBlob extends TrackableObject {

	private double radius = -1;

	public TrackableBlob(double x, double y, double z, double r) {
		super(ItemType.blob, x, y, z);
		this.radius = r;
		this.quality = 1;
	}
	
}
