package fi.conf.tabare;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class Calibrator implements MouseListener, MouseMotionListener {

	private int[][][] cMult; //Calibration multipliers
	
	private int imageWidth = 640;
	private int imageHeight = 480;
	private int xPoints = 2;
	private int yPoints = 2;
	private int numPoints = 4;
	private boolean flipX = false;
	private boolean flipY = false;
	
	private int draggedPointIndex = 0;
	
	public Calibrator(int xPoints, int yPoints, int imageWidth, int imageHeight){
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		setGridSize(xPoints, yPoints);
		cMult = new int[2][xPoints][yPoints];
	}
	
	public void setGridSize(int xPoints, int yPoints){
		this.xPoints = xPoints;
		this.yPoints = yPoints;
		this.numPoints = xPoints*yPoints;
	}
	
	public int[][][] getRawCalibrationData(){
		return cMult;
	}
	
	public void setRawCalibrationData(int [][][] data){
		if(data == null) return;
		this.cMult = data;
		this.yPoints = data[0][0].length;
		this.xPoints = data[0].length;
		this.numPoints = xPoints*yPoints;
	}
	
	public void setCalibrationPoint(int xPoint, int yPoint, int calibX, int calibY){
		cMult[0][xPoint][yPoint] = calibX;
		cMult[1][xPoint][yPoint] = calibY;
	}
	
	public void applyCalibration(TrackableObject o){
		double[] cc = getCalibratedPoint(o.getRawX(), o.getRawY());
		o.fixLocation(cc);
	}
	
	private double[] getCalibratedPoint(double x, double y){

		double ymo = (y-(cMult[1][0][0]+cMult[1][0][1])/2.0f)/(cMult[1][0][1]-cMult[1][0][0]);
		double ymi = 1-ymo;

		double xmo = (x-(cMult[0][0][0]+cMult[0][1][0])/2.0f)/(cMult[0][1][0]-cMult[0][0][0]);
		double xmi = 1-xmo;

		return new double[] {(((x-cMult[0][0][0])/(cMult[0][1][0]-cMult[0][0][0]))*ymi + 
				((x-cMult[0][0][1])/(cMult[0][1][1]-cMult[0][0][1]))*ymo
				),
				(((y-cMult[1][0][0])/(cMult[1][0][1]-cMult[1][0][0]))*xmi + 
						((y-cMult[1][1][0])/(cMult[1][1][1]-cMult[1][1][0]))*xmo
						)};

		//		return new double[]{x/imageWidth, y/imageHeight};
	}

	public void drawGrid(Graphics2D g) {
		g.setColor(Color.cyan);
		g.drawLine(cMult[0][0][0],cMult[1][0][0], cMult[0][1][0], cMult[1][1][0]);
		g.drawLine(cMult[0][1][0],cMult[1][1][0], cMult[0][1][1], cMult[1][1][1]);
		g.drawLine(cMult[0][1][1],cMult[1][1][1], cMult[0][0][1], cMult[1][0][1]);
		g.drawLine(cMult[0][0][1],cMult[1][0][1], cMult[0][0][0], cMult[1][0][0]);
		
		if(draggedPointIndex >= 0){
			int x = (int)((draggedPointIndex/xPoints));
			int y = (int)(draggedPointIndex%xPoints);
			g.drawOval(cMult[0][x][y]-2,cMult[1][x][y]-2, 4, 4);
			g.drawString(""+draggedPointIndex, cMult[0][x][y]-12, cMult[1][x][y]-12);
		}
	}
	
	private int getIndexOfClosestPoint(int x, int y){
		
		float proximity = Float.MAX_VALUE;
		int index = -1;
		
		for(int i=0; i < xPoints*yPoints; i++){
			int tx = (int)((i/xPoints));
			int ty = (int)(i%xPoints);
	
			float nProx = (float) Math.sqrt(Math.pow(cMult[0][tx][ty]-x, 2)+Math.pow(cMult[1][tx][ty]-y, 2));
			if(nProx < proximity){ 
				proximity = nProx;
				index = i;
			}
		}
		
		return index;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		draggedPointIndex = getIndexOfClosestPoint(e.getX(), e.getY());
		mouseDragged(e);
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		//System.out.println(draggedPointIndex);
		if(draggedPointIndex >= 0){
			int x = (int)((draggedPointIndex/xPoints));
			int y = (int)(draggedPointIndex%xPoints);
			cMult[0][x][y] = e.getX();
			cMult[1][x][y] = e.getY();
			
			//System.out.println(e.getX());
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		draggedPointIndex = -1;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
