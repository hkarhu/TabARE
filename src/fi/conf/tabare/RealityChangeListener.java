package fi.conf.tabare;

public interface RealityChangeListener {

	void objectAppeared(TrackableObject t);
	void objectDisappeared(TrackableObject t);
	void objectChanged(TrackableObject t);
	
}
