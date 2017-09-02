package fi.conf.tabare;

public abstract class TrackableObject {

	protected final double MERGE_PROXIMITY_SPREAD = 10f;
	protected final int DECAY_MS = 80;
	
	public enum ItemType { blob, tripFiducial }
	
	protected ItemType itemType;

	protected int id = -1;
	
	protected long timeLastSeen;
	protected double x, y, z;
	protected double rawX, rawY, rawZ;
	protected double radius = 1;
	protected double angle = 0;
	protected double quality = 0;
	protected boolean active = true;
		
	public TrackableObject(ItemType itemType, double rawX, double rawY, double rawZ) {
		this.id = Reality.getNewID();
		this.itemType = itemType;
		update(rawX, rawY, rawZ, quality);
	}
	
	public void update(double rawX, double rawY, double rawZ, double quality){
		this.quality = quality;
		this.timeLastSeen = System.currentTimeMillis();
		this.rawX = rawX; this.rawY = rawY; this.rawZ = rawZ;
	}
	
	public double getProximity(double nx, double ny){
		return Math.sqrt(Math.pow(this.rawX-nx, 2) + Math.pow(this.rawY-ny, 2));
	}
	
	public double getProximity(double nx, double ny, double nz){
		return Math.sqrt(Math.pow(this.rawX-nx, 2) + Math.pow(this.rawY-ny, 2) + Math.pow(this.rawZ-nz, 2));
	}
	
	public ItemType getItemType() {
		return itemType;
	}
	
	public double getQuality(){
		double q = quality*(1-getDecay());
		if(quality > 0) return q; else return 0;
	}
	
	/**
	 * If this is larger than 1.0f then the object has decayed and shouldn't be used in interaction.
	 * @return
	 */
	public float getDecay(){
		return (System.currentTimeMillis()-timeLastSeen)/(float)DECAY_MS;
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

	public double getRawZ() {
		return rawZ;
	}
	
	public double getX(){
		return x;
	}
	
	public double getY(){
		return y;
	}
	
	public double getZ(){
		return z;
	}

	/**
	 * Calibrator uses this to set the calibrated coordinates.
	 * @param cc
	 */
	public void fixLocation(double[] cc) {
		this.x = cc[0];
		this.y = cc[1];
		if(cc.length > 2) this.z = cc[2];
		//System.out.println(x + " " + y);
	}

	public double getAngle() {
		return angle;
	}

}
