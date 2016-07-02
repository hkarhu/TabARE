package fi.conf.tabare;
/**
 * When using this interface, you probably also should keep a copy of the current reality in your application. You can get the reality from the ARDataProvider instance with getReality();
 * @author irah
 *
 */
public interface RealityChangeListener {

	void objectAppeared(TrackableObject t);
	void objectDisappeared(TrackableObject t);
	void objectChanged(TrackableObject t);
	
}
