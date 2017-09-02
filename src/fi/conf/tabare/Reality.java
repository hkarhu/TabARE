package fi.conf.tabare;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Reality {
	
	public ConcurrentLinkedQueue<TrackableBlob> trackedBlobs;
	public ConcurrentHashMap<Integer,TrackableTripcode> trackedTripcodes;

	private static volatile int lastGivenID = 0;
	
	public Reality() {
		trackedTripcodes = new ConcurrentHashMap<>();
		trackedBlobs = new ConcurrentLinkedQueue<>();
	}
	
	public static int getNewID(){
		lastGivenID++;
		return lastGivenID;
	}
	
	public ConcurrentLinkedQueue<TrackableBlob> getTrackedBlobs() {
		return trackedBlobs;
	}

	public ConcurrentHashMap<Integer, TrackableTripcode> getTrackedTripcodes() {
		return trackedTripcodes;
	}
	
}
