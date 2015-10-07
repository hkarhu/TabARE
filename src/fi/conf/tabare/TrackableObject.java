package fi.conf.tabare;

import javax.sound.midi.Track;

public abstract class TrackableObject {

	protected final int DECAY_MS = 100;
	
	public enum ItemType { blob, tripFiducial }
	
	protected ItemType itemType;
	protected int id = -1;
	
	protected long timeLastSeen;
	protected double x, y, z;
	protected double rawX, rawY, rawZ;
	protected double quality = 0;
		
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
	
	public abstract boolean getProximity(double nx, double ny, double nz);
	
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
		this.z = cc[2];
		//System.out.println(x + " " + y);
	}

}
