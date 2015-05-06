package fi.conf.tabare;

import org.lwjgl.util.vector.Vector3f;

public class TrackableItem {

	protected final double MERGE_PROXIMITY_SPREAD = 3.5f;
	protected final int DECAY_MS = 80;
	
	public enum ItemType { blob, tripFiducial }
	
	protected ItemType itemType;
	protected int id = -1;
	
	protected long timeLastSeen;
	protected double rawX, rawY;
	protected double radius = 1;
	
	protected Vector3f glposition;
	
	protected double quality = 0;
	protected boolean active = true;
		
	public TrackableItem(double x, double y, int id, ItemType itemType) {
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
	
	public Vector3f getGLPosition(){
		return glposition;
	}

	public double getRawX() {
		return rawX;
	}
	
	public double getRawY() {
		return rawY;
	}
	
	public double getX(){
		return 0.5f;
	}
	
	public double getY(){
		return 0.5f;
	}
	
}
