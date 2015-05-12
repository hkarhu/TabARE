package fi.conf.tabare;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Calibrator implements MouseListener {

	private int[][][] cMult; //Calibration multipliers
	
	private int imageWidth = 640;
	private int imageHeight = 480;
	private int xPoints = 2;
	private int yPoints = 2;
	private int numPoints = 4;
	private int calibrationIndex = -1;
	private boolean flipX = false;
	private boolean flipY = false;
	
	public Calibrator(int xPoints, int yPoints, int imageWidth, int imageHeight){
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		setGridSize(xPoints, yPoints);
		cMult = new int[xPoints][yPoints][2];
	}
	
	public void setGridSize(int xPoints, int yPoints){
		this.xPoints = xPoints;
		this.yPoints = yPoints;
		this.numPoints = xPoints*yPoints;
	}
	
	public void startCalibration(){
		calibrationIndex = 0;
	}
	
	public void setCalibrationPoint(int xPoint, int yPoint, int calibX, int calibY){
		cMult[xPoint][yPoint][0] = calibX;
		cMult[xPoint][yPoint][1] = calibY;
	}
	
	public void applyCalibration(TrackableObject o){
		double[] cc = getCalibrated(o.getRawX(), o.getRawY());
		o.setCalibration(cc);
	}
	
	private double[] getCalibrated(double x, double y){
			
			double ymo = (y-(cMult[0][0][1]+cMult[0][1][1])/2.0f)/(cMult[0][1][1]-cMult[0][0][1]);
			double ymi = 1-ymo;
		
			double xmo = (x-(cMult[0][0][0]+cMult[1][0][0])/2.0f)/(cMult[1][0][0]-cMult[0][0][0]);
			double xmi = 1-xmo;
			
			return new double[] {(((x-cMult[0][0][0])/(cMult[1][0][0]-cMult[0][0][0]))*ymi + 
					((x-cMult[0][1][0])/(cMult[1][1][0]-cMult[0][1][0]))*ymo
					),
					(((y-cMult[0][0][1])/(cMult[0][1][1]-cMult[0][0][1]))*xmi + 
					((y-cMult[1][0][1])/(cMult[1][1][1]-cMult[1][0][1]))*xmo
					)};
		
//		return new double[]{x/imageWidth, y/imageHeight};
	}

	public void drawGrid(Graphics2D g) {
		g.setColor(Color.cyan);
		g.drawLine(cMult[0][0][0],cMult[0][0][1], cMult[1][0][0], cMult[1][0][1]);
		g.drawLine(cMult[1][0][0],cMult[1][0][1], cMult[1][1][0], cMult[1][1][1]);
		g.drawLine(cMult[1][1][0],cMult[1][1][1], cMult[0][1][0], cMult[0][1][1]);
		g.drawLine(cMult[0][1][0],cMult[0][1][1], cMult[0][0][0], cMult[0][0][1]);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
		int x = (int)((calibrationIndex/xPoints));
		int y = (int)(calibrationIndex%xPoints);
		
		System.out.println("Calibrated " + x + ", " + y + " as " + e.getX() + ", " + e.getY());
		
		calibrationIndex++;
		if(calibrationIndex >= numPoints) calibrationIndex = 0;
		
		if(calibrationIndex >= 0){
			cMult[x][y][0] = e.getX();
			cMult[x][y][1] = e.getY();
		}
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
