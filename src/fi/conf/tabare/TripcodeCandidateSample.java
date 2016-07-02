package fi.conf.tabare;

import org.opencv.core.Mat;

public class TripcodeCandidateSample {
	
	private static final int FRAME_PADDING = 8; //Stuff happens nicer when this is an even number.
	private static final int MINIMUM_VALID_RADIUS = 8;
	
	public double x = 0, y = 0, r = 0;
	public int frameRadius = 0; //0.5*FrameWidth, (frames are always rectangles)
	public Mat rawFrame;
	public boolean valid = false;
	
	public TripcodeCandidateSample(Mat frame, double[] coordinates) {
		
		if (frame.channels() != 1){
			System.err.println("Wrong format matrix given! Only one channel supported.");
			return;
		}
		
		this.x = coordinates[0];
		this.y = coordinates[1];
		this.r = coordinates[2];

		frameRadius = (int)(r+FRAME_PADDING);
		
		int yLow = (int) (y - frameRadius);
		int yHigh = (int) (y + frameRadius);
		int xLow = (int) (x - frameRadius);
		int xHigh = (int) (x + frameRadius);

		if(yLow < 0) {
			yLow = 0;
		}
		if(xLow < 0){
			xLow = 0;
		}
		if(yHigh >= frame.height()) {
			yHigh = frame.height() - 1;
		}
		if(xHigh >= frame.width()) {
			xHigh = frame.width() - 1;
		}
		
		if(r >= MINIMUM_VALID_RADIUS){
			valid = true;
		} else {
			return;
		}
		
		rawFrame = frame.submat(yLow, yHigh, xLow, xHigh).clone();
		
	}

	public boolean isValid(){
		return valid;
	}

	public void release() {
		rawFrame.release();
	}
	
}
