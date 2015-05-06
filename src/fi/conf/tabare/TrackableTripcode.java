package fi.conf.tabare;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.dnd.DnDConstants;
import java.awt.image.BufferedImage;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TrackableTripcode extends TrackableItem {

		private static final int BUFFER_SIZE = 5;
		private static final int PROXIMITY = 24;
		private static final int TIMEOUT = 250;
		
		private static final int THRESHOLD = 127;
		
		private static final int ROUGH_SCAN_DIVISIONS = 14;
		private static final int FINE_SCAN_DIVS = 10;
		
		private static final int DATA_LENGTH = 6;
		
		private static final int FRAME_PADDING = 6; //Stuff happens nicer when this is an even number.
		
		private static final double RING1RAD = 0.48256625 + 0.1;
		private static final double RING2RAD = 0.70292887 + 0.1;
		private static final double RINGWIDTH = 0.071129707 + 0;
		
		private static final Color TARGET_COLOR = new Color(255, 0, 0, 127);
		
		private int last_index;

		private double angle = 0;
		
		private int frameRadius = 0; //0.5*FrameWidth, (frames are always rectangles)

		//Corrections to the coordinates calculated from the fiducial
		private double xShift = 0;
		private double yShift = 0;
		
		private Mat rawFrame;
		
		BufferedImage debugImg;
		BufferedImage debugImg2;
		
		public TrackableTripcode(Mat m, double[] coords) {
			
			super(coords[0], coords[1], -1, ItemType.tripFiducial);
			
			if (m.channels() != 1){
				System.err.println("Wrong format matrix given! Only one channel supported.");
				return;
			}

			this.radius = coords[2];
			
			frameRadius = (int)(radius+FRAME_PADDING);
			
			int yLow = (int) (getRawY() - frameRadius);
			int yHigh = (int) (getRawY() + frameRadius);
			int xLow = (int) (getRawX() - frameRadius);
			int xHigh = (int) (getRawX() + frameRadius);

			if(yLow < 0) {
				yLow = 0;
			}
			if(xLow < 0){
				xLow = 0;
			}
			if(yHigh >= m.height()) {
				yHigh = m.height() - 1;
			}
			if(xHigh >= m.width()) {
				xHigh = m.width() - 1;
			}
			
			rawFrame = m.submat(yLow, yHigh, xLow, xHigh).clone();
			
			//Debug
			debugImg2 = new BufferedImage(rawFrame.width(), rawFrame.height(), BufferedImage.TYPE_4BYTE_ABGR);
			
		}
		
		public void debugDraw(Graphics2D g, boolean complex){
			
			if(complex){
				if(debugImg == null) debugImg = new BufferedImage(frameRadius*2, frameRadius*2, BufferedImage.TYPE_4BYTE_ABGR);
				g.drawImage(OpenCVUtils.matToBufferedImage(rawFrame), (int)(getRawX()-frameRadius), (int)(getRawY()-frameRadius), null);
			}
			
			g.setColor(TARGET_COLOR);
			g.fillOval((int)getFixedRawX()-2, (int)getFixedRawY()-2, 4, 4);
			g.drawOval((int)(getFixedRawX()-getRadius()), (int)(getFixedRawY()-getRadius()), (int)(getRadius()*2), (int)(getRadius()*2));
			g.drawString(xShift + " " + yShift, (int)getRawX(), (int)(24 + getRawY() + getRadius()));
			
			//g.drawImage(debugImg2, (int)(getFixedRawX()-debugImg2.getWidth()/2), (int)(getFixedRawY()-debugImg2.getHeight()/2), null);
			
			g.drawString("#"+this.id, (int)(getRawX() - getRadius()), (int)(12 + getRawY() + getRadius()));
			
		}

		public void crunch() {
			
			if(rawFrame == null) return;
			
			Imgproc.adaptiveThreshold(rawFrame, rawFrame, 254, Imgproc.THRESH_BINARY, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 21, 32);
			
//			Imgproc.threshold(rawFrame, rawFrame, THRESHOLD, 254, Imgproc.THRESH_BINARY);
//			//Imgproc.erode(rawFrame, rawFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
//			
//			fitEllipse(rawFrame);
//			
//			double roughAngle = findAngleByMaximum(rawFrame);
//			
//			Imgproc.dilate(rawFrame, rawFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
//			
//			this.angle = refineAngleDNC(rawFrame, roughAngle, frameRadius+xShift, frameRadius+yShift, this.radius);
//			
//			
//			
//			this.id = parseCode(rawFrame, this.angle + Math.PI/5);		
//			//double refinedAngle = findAngleBySweep(roughAngle, FINE_STEP, rawFrame);
//			double refinedAngle = roughAngle;
//			refinedAngle = roughAngle;
//			updateAngle(refinedAngle);
//			Mat M = Imgproc.getRotationMatrix2D(new Point(frameRadius, frameRadius), Math.toDegrees(-refinedAngle), 1); //1+(ellipse.size.height/SAMPLE_FRAME_SIZE)*0.5f);
//			Imgproc.warpAffine(rawFrame, rawFrame, M, rawFrame.size());
//			M.release();
			
			//System.out.println(parseCodeByRays(rawFrame));
			
			
//			//OLD STUFF!!!
//			Mat contourFrame = new Mat();
//			Imgproc.Canny(rawFrame, contourFrame, THRESHOLD, 250);
//			List<MatOfPoint> contours = new ArrayList<MatOfPoint>(2);
//			Mat hierarchy = new Mat();
//			Imgproc.findContours(contourFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_NONE);
//			hierarchy.release();
//			contourFrame.release();
//			MatOfPoint2f edgels = new MatOfPoint2f();
//			double largestArea = 0;
//			for(int i=0; i < contours.size(); i++){
//				MatOfPoint contour = contours.get(i);
//				double contourArea = Imgproc.contourArea(contour);
//				if(largestArea < contourArea){
//					largestArea = contourArea;
//					//Core.drawContours(frame, contours, i, new Scalar(255));
//					contours.get(i).convertTo(edgels, CvType.CV_32FC2);
//				}
//				contour.release();
//			}
//			if(edgels.empty() || edgels.rows() < 100){
//				edgels.release();
//				return;
//			}
//			RotatedRect ellipse = Imgproc.fitEllipse(edgels);
//			edgels.release();
//			//Correct shift
////			xShift = ((innerRight-innerLeft)+(outerRight-outerLeft))/4.0f;
////			yShift = ((innerBottom-innerTop)+(outerBottom-outerTop))/4.0f;
//			this.xShift = (frameRadius - ellipse.center.x);
//			this.yShift = (frameRadius - ellipse.center.y);
//			
//			//Transform image 
//			//TODO: optimize transformation matrices into one before applying the transformation
//			double theta = Math.toRadians(ellipse.angle);
//			double yshear = 0*Math.tan(-theta);
//			double xshear = (1-(ellipse.size.height/ellipse.size.width))*0;
//			double xscale = 1 + 0*(ellipse.size.height/ellipse.size.width);
//			double yscale = 8;
//			
//			double a = Math.cos(theta);
//			double b = Math.sin(theta);
//			
//			Mat M = Mat.zeros(2, 3, CvType.CV_32F);
//			
//			M.put(0, 0, new double[] {a,b,(1-a)*(frameRadius+xShift)-b*(frameRadius+yShift),
//									 -b,a,b*(frameRadius+xShift) + (1-a)*(frameRadius+yShift)});
//
//			M.put(0, 0, new double[] {1,0,xShift,
//					  				  0,1,yShift});
//			
//			Imgproc.warpAffine(rawFrame, rawFrame, M, rawFrame.size());
//			
//			M.release();
//			
//			Imgproc.threshold(rawFrame, rawFrame, THRESHOLD, 255, Imgproc.THRESH_BINARY);
//			
			double roughAngle = findAngleByMaximum(rawFrame, frameRadius+xShift, frameRadius+yShift, this.radius);
			//Imgproc.dilate(rawFrame, rawFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
			this.angle = refineAngleDNC(rawFrame, roughAngle, frameRadius+xShift, frameRadius+yShift, this.radius);
//			
//			//float size = (2*SAMPLE_FRAME_SIZE-outerBottom-outerTop-outerLeft-outerRight)/(float)SAMPLE_FRAME_SIZE;
//			
//			//Take size with average of W and H		
//			this.radius = (float) ((2*radius + ellipse.size.height*0.5f)/3f);
//			
//			float size = (float) (ellipse.size.height)/(float)(frameRadius*2);
//			
//			M = Imgproc.getRotationMatrix2D(new Point(frameRadius, frameRadius), Math.toDegrees(-angle), 1/size); //1+(ellipse.size.height/SAMPLE_FRAME_SIZE)*0.5f);
//			Imgproc.warpAffine(rawFrame, rawFrame, M, rawFrame.size());
//			M.release();
//			
//			//Accumulate data
////			if(accumFrame == null) accumFrame = Mat.zeros(SAMPLE_FRAME_SIZE, SAMPLE_FRAME_SIZE, CvType.CV_32FC1);
////			Imgproc.accumulateWeighted(frame, accumFrame, 0.2f);
////			accumFrame.convertTo(frame, CvType.CV_8UC1);
//			
//			//int newCode = parseCodeBySweep(frame);
//		
////			int newCode = parseCodeByRays(rawFrame, g);
////			
////			if(newCode > 0) code_confirms[last_index] = newCode;
			
			this.id = parseCode(rawFrame, this.angle, frameRadius+xShift, frameRadius+yShift);
			
		}
		
		private void quickTrimCentre(){
			//Center trimming
			boolean findXMin = true, findXMax = true, findYMin = true, findYMax = true;
			int outerTop = 0;
			int outerBottom = 0;
			int outerLeft = 0;
			int outerRight = 0;
			int innerTop = 0;
			int innerBottom = 0;
			int innerLeft = 0;
			int innerRight = 0;
			
			//Test where the center is. Inside out.
			for(int i=1; i < frameRadius*0.30f; i++){
				if(findXMax && frameRadius + i < rawFrame.width() && rawFrame.get(frameRadius, frameRadius + i)[0] < THRESHOLD) innerLeft = i; else findXMax = false;
				if(findXMin && frameRadius - i > 0 && rawFrame.get(frameRadius, frameRadius - i)[0] < THRESHOLD) innerRight = i; else findXMin = false;
				if(findYMax && frameRadius + i < rawFrame.height() && rawFrame.get(frameRadius + i, frameRadius)[0] < THRESHOLD) innerTop = i; else findYMax = false;
				if(findYMin && frameRadius - i > 0 && rawFrame.get(frameRadius - i, frameRadius)[0] < THRESHOLD) innerBottom = i; else findYMin = false;
			}
			
			findXMin = true;
			findXMax = true;
			findYMin = true;
			findYMax = true;
			
			//Test the outsides. Outside in.
			for(int i=1; i < frameRadius*0.15f; i++){
				if(findXMax && rawFrame.get(frameRadius, i)[0] < THRESHOLD) outerLeft = i; else findXMax = false;
				if(findXMin && rawFrame.get(frameRadius, frameRadius - 1 - i)[0] < THRESHOLD) outerRight = i; else findXMin = false;
				if(findYMax && rawFrame.get(i, frameRadius)[0] < THRESHOLD) outerTop = i; else findYMax = false;
				if(findYMin && rawFrame.get(frameRadius - 1 - i, frameRadius)[0] < THRESHOLD) outerBottom = i; else findYMin = false;
			}
			xShift = (innerLeft - innerRight + outerLeft - outerRight)/4.0;
			yShift = (innerTop - innerBottom + outerTop - outerBottom)/4.0;	
		}
		
		private boolean fitEllipse(Mat frame){
			
			Mat contourFrame = new Mat();
			Imgproc.Canny(frame, contourFrame, THRESHOLD, 250);
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>(2);
			Mat hierarchy = new Mat();
			Imgproc.findContours(contourFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
			hierarchy.release();
			contourFrame.release();
			MatOfPoint2f edgels = new MatOfPoint2f();	
			
			//Find largest contour
			double largestArea = 0;
			for(int i=0; i < contours.size(); i++){
				MatOfPoint contour = contours.get(i);
				double contourArea = Imgproc.contourArea(contour);
				if(largestArea < contourArea){
					largestArea = contourArea;
					contours.get(i).convertTo(edgels, CvType.CV_32FC2);
				}
				contour.release();
			}

			//Return false if the edge was too short
			if(edgels.empty() || edgels.rows() < 60){
				edgels.release();
				return false;
			}
			
			RotatedRect ellipse = Imgproc.fitEllipse(edgels);
			edgels.release();
			
			//Update values
			this.xShift = 0;
			this.yShift = 0;
			//this.radius = (ellipse.boundingRect().height*0.5 + ellipse.boundingRect().width*0.5)/4.0;
			
			//Correct the rotation and scale
			double theta = Math.toRadians(ellipse.angle);
			double a = Math.cos(theta);
			double b = Math.sin(theta);
			
			Mat M = Mat.zeros(2, 3, CvType.CV_32F);
			
			M.put(0, 0, new double[] {a,b,(1-a)*(ellipse.center.x)-b*(ellipse.center.y),
									 -b,a,b*(ellipse.center.x) + (1-a)*(ellipse.center.y)});
			
			Imgproc.warpAffine(frame, frame, M, frame.size());
						
			M.release();
			
			return true;
		}

		//	private void transformNewFrame(Mat frame){
		//		if(lastProcessedFrame == null){
		//			lastProcessedFrame = frame;
		//		} else {
		//			try {
		//				Mat transform = Video.estimateRigidTransform(lastProcessedFrame,frame,false);
		//				if(transform.empty()){
		//					lastProcessedFrame = frame;
		//				} else {
		//					Imgproc.warpAffine(frame,lastProcessedFrame,transform,lastProcessedFrame.size(),Imgproc.INTER_NEAREST|Imgproc.WARP_INVERSE_MAP);
		//				}
		//			} catch (Exception e){
		//				S.eprintf(e.getMessage());
		//			}
		//		}
		//	}

		private double findAngleByMaximum(Mat m, double xCenter, double yCenter, double ellipseRadius){
			int lim = 0;
			float target = 0;
			double angle = 0;
			
			//Take some angle
			for(double i=0; i < 2*Math.PI; i += (2*Math.PI)/ROUGH_SCAN_DIVISIONS){

				double xi = Math.sin(i);
				double yi = Math.cos(i);
				lim = 0;
				
				for(int d = (int) (ellipseRadius*(RING1RAD - 2*RINGWIDTH)); d <= (ellipseRadius*(RING2RAD+2*RINGWIDTH)); d+=2){
					int x = (int)(xi*d + xCenter);				
					int y = (int)(yi*d + yCenter);
					
					if(x < 0 || y < 0 || x >= m.width() || y >= m.height()){
						System.out.println("Off limits? " + x + " " + y + " " + m.width() + " " + m.height());
					} else {
						if(m.get(y, x)[0] > THRESHOLD) lim++;
					}
					//if(debugImg2 != null) debugImg2.getGraphics().drawLine(x,y,x,y);
				}
				
				if(lim > target){
					target = lim;
					angle = i;
				}
				
			}
			return angle;
		}
		
		private double refineAngleDNC(Mat m, double roughAngle, double xCenter, double yCenter, double ellipseRadius){
			 
			double step = (Math.PI*2)/8;
			double refinedAngle = roughAngle;
			double value;
			int interrupt = 0;

			do {
				float xi = (float) (Math.sin(refinedAngle));
				float yi = (float) (Math.cos(refinedAngle));
				
				value = 0;
				
				//Take some samples to establish the value for this segment.
				for(int r = (int) (ellipseRadius*RING1RAD); r <= ellipseRadius*RING2RAD; r++){
					int x = (int)(xi*r + xCenter);				
					int y = (int)(yi*r + yCenter);
					
					if(x >= m.width() || y >= m.height()) return refinedAngle;
					
					value = (value + m.get(y, x)[0])/2;
					
//					debugImg2.getGraphics().setColor(TARGET_COLOR);
//					debugImg2.getGraphics().drawLine(x, y, x, y);
				}
			
				if(value < THRESHOLD){
					refinedAngle += step;
					step *= 0.5f;
				} 
				
				refinedAngle -= step;
				
				interrupt++;
			
			} while (interrupt < FINE_SCAN_DIVS);
			
			return refinedAngle;
		}
		
		private int parseCode(Mat frame, double startingAngle, double xCenter, double yCenter){

			final int SAMPLES = 3;
			final double SEGMENT_WIDTH = (Math.PI*2)/8;
			char rawData[] = new char[DATA_LENGTH]; 
			char di = 0;
			
			//For appropriate area of the circle.
			for(double i=startingAngle; i < 2*Math.PI+startingAngle-2*SEGMENT_WIDTH; i += SEGMENT_WIDTH){

				//Remember the x and y multipliers
				double xi,yi;
				int x,y;
				int it;
				//g.setColor(Color.lightGray);
				//g.drawLine(SAMPLE_FRAME_SIZE/2, SAMPLE_FRAME_SIZE/2, x, y);

				double value1 = 0;
				it = 0;
				for (double d = i; d < i+SEGMENT_WIDTH; d += SEGMENT_WIDTH/SAMPLES){
					xi = Math.sin(d);
					yi = Math.cos(d);
					for(int r=(int) ((RING1RAD-RINGWIDTH)*this.radius); r < ((RING1RAD+RINGWIDTH)*this.radius); r++){
						x = (int)(xi*r + xCenter);
						y = (int)(yi*r + yCenter);
						
						if(x < 0) x = 0; else if(x >= frame.width()) x = frame.width()-1;
						if(y < 0) y = 0; else if(y >= frame.height()) y = frame.height()-1;
	
						value1 += (value1 + frame.get(y, x)[0])/2.0;
						
						Graphics2D g2 = (Graphics2D)debugImg2.getGraphics();
						g2.setColor(TARGET_COLOR);
						g2.drawLine(x, y, x, y);
						it++;
					}
				}
				value1 /= it; 
				
				double value2 = 0;
				it = 0;
				for (double d = i; d < i+SEGMENT_WIDTH; d += SEGMENT_WIDTH/SAMPLES){
					xi = Math.sin(d);
					yi = Math.cos(d);
					for(int r=(int) ((RING2RAD-RINGWIDTH)*this.radius); r < ((RING2RAD+RINGWIDTH)*this.radius); r++){
						x = (int)(xi*r + xCenter);
						y = (int)(yi*r + yCenter);
						
						if(x < 0) x = 0; else if(x >= frame.width()) x = frame.width()-1;
						if(y < 0) y = 0; else if(y >= frame.height()) y = frame.height()-1;
	
						value2 += (value2 + frame.get(y, x)[0])/2.0;
						
						Graphics2D g2 = (Graphics2D)debugImg2.getGraphics();
						g2.setColor(TARGET_COLOR);
						g2.drawLine(x, y, x, y);
						it++;
					}
				}
				value2 /= it;
				
				if(di < DATA_LENGTH){
					if(value1 < THRESHOLD && value2 > THRESHOLD){
						rawData[di] = 1;
					} else if(value1 > THRESHOLD && value2 < THRESHOLD){
						rawData[di] = 2;
					} else if(value1 < THRESHOLD && value2 < THRESHOLD){
						rawData[di] = 3;
					} else {
						rawData[di] = 0;
					}
				}
				
				di++;
				
			}
			
			System.out.print("Code: ");
			for(double c : rawData){
				System.out.print((int)c);
			}
			System.out.print("\n");
			
			
			if(checksumOK(rawData)){
				int newCode = rawData[0] + rawData[1]*3 + rawData[2]*9 + rawData[3]*27 + rawData[4]*81 + rawData[5]*243;
				return newCode;
			} else return -1;

		}
		
		private boolean checksumOK(char[] data){
			boolean dataCorrect = true;
			//Correctness check
			int ld = -1;
			for(int d : data){
				if(d == ld){
					dataCorrect = false;
					break;
				}
				ld = d;
			}
			return dataCorrect;
		}

		public double getFixedRawX(){
			return rawX + xShift;
		}
		public double getFixedRawY(){
			return rawY + yShift;
		}
		
		public double getAngle(){
			return angle;
		}

		public void update(TrackableTripcode t) {
			super.update(t.getFixedRawX(), t.getFixedRawY(), t.getRadius(), t.getQuality());
			this.quality *= 0.9;
			this.radius = (t.getRadius() + this.radius)*0.5;
			this.angle = t.getAngle();
			
			t.rawFrame.release(); //Release bad frame from new instance
		}
	
}
