package fi.conf.tabare;

import java.util.TreeMap;

public class Reality {

	private TreeMap<Integer, TrackableObject> trackedObjects;
	
	private static volatile int lastGivenID = 0;
	
	public Reality() {
		trackedObjects = new TreeMap<>();
	}
	
	public static int getNewID(){
		lastGivenID++;
		return lastGivenID;
	}
	
	
}
