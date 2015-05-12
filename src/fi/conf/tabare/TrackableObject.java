package fi.conf.tabare;

public abstract class TrackableObject {

	protected final double MERGE_PROXIMITY_SPREAD = 10f;
	protected final int DECAY_MS = 80;
	
	public enum ItemType { blob, tripFiducial }
	
	protected ItemType itemType;
	protected int id = -1;
	
	protected long timeLastSeen;
	protected double x, y;
	protected double rawX, rawY;
	protected double radius = 1;
	
	protected double quality = 0;
	protected boolean active = true;
		
	public TrackableObject(double x, double y, int id, ItemType itemType) {
		this.id = id;
		this.itemType = itemType;
		update(x, y, radius, quality);
	}
	
	public void update(double x, double y, double radius, double quality){
		this.radius = radius; 
		this.quality = quality;
		this.timeLastSeen = System.currentTimeMillis();
		this.rawX = x; this.rawY = y;
	}
	
	public boolean isCloseTo(double nx, double ny){
		double d = Math.sqrt(Math.pow(nx-rawX, 2) + Math.pow(ny-rawY, 2));
		if(d > radius + MERGE_PROXIMITY_SPREAD) return false;
		return true;
	}
	
	public double getQuality(){
		return quality;
	}
	
	public boolean isDead(){
		if(System.currentTimeMillis() < timeLastSeen+DECAY_MS) return false;
		return true;
	}
	
	public int getID(){
		return id;
	}
	
	public double getRadius(){
		return radius;
	}

	public double getRawX() {
		return rawX;
	}
	
	public double getRawY() {
		return rawY;
	}
	
	public double getX(){
		return x;
	}
	
	public double getY(){
		return y;
	}

	public void setCalibration(double[] cc) {
		this.x = cc[0];
		this.y = cc[1];
		
		//System.out.println(x + " " + y);
	}

}
