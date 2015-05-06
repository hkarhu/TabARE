package fi.conf.tabare;

public interface TrackingEventListener {

	void itemAppeared(int id);
	void itemDataUpdated(int id);
	void itemRemoved(int id);
	void pointAppeared(int id);
	void pointDataUpdated(int id);
	void pointRemoved(int id);
	
}
