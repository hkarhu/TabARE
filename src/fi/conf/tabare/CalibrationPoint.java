package fi.conf.tabare;

public class CalibrationPoint {

	double realX, realY;
	double virtualX, virtualY;
	
	public double distanceTo(double x, double y){
		return Math.sqrt(Math.pow(virtualX-x, 2) + Math.pow(virtualY - y, 2));
	}
	
}
