package fi.conf.tabare;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TrackableTripcode extends TrackableObject {

		private static final int BUFFER_SIZE = 5;
		private static final int PROXIMITY = 24;
		private static final int TIMEOUT = 250;
		private static final int THRESHOLD = 127;
		private static final int DATA_LENGTH = 6; //Only data, this will be used in code parsing so +2 will be added for starting sectors
		private static final int ROUGH_SCAN_DIVISIONS = 15;
		private static final int FINE_SCAN_DIVS = ROUGH_SCAN_DIVISIONS;
		private static final int CODE_SEGMENT_SAMPLES = 3;
		
		private static final double RING1RAD = 0.48256625;
		private static final double RING2RAD = 0.70292887;
		private static final double RINGWIDTH = 0.071129707 + 0;
		
		private static final Font INFO_FONT = new Font("Monospace", 0, 8);
		private static final Color TARGET_COLOR = new Color(255, 0, 0, 255);
		private static final Color RING_COLOR = new Color(0, 255, 0, 255);
		private static final Color INFO_COLOR = new Color(127, 0, 0, 255);
		private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
		
		private TripcodeCandidateSample sample;
		
		private static final int MAX_BUFFERED_CODES = 4;
		private int codeIndex = 0;
		private int[] assignedCodes = new int[MAX_BUFFERED_CODES];;
		
		private double angle = 0;

		//Corrections to the coordinates calculated from the fiducial
		private double xShift = 0;
		private double yShift = 0;
		
		private double bufferedX = 0;
		private double bufferedY = 0;
		
		BufferedImage debugImg;
		//BufferedImage debugImg2;
		
		private boolean valid = false;
		
		public TrackableTripcode(TripcodeCandidateSample t) {
		    super(ItemType.tripFiducial, t.x, t.y, -1);
		    this.sample = t;
			
		    this.radius = t.r;
			
		    this.bufferedX = t.x;
		    this.bufferedY = t.y;
		    
			//Debug
		    debugImg = new BufferedImage(sample.frameRadius*2, sample.frameRadius*2, BufferedImage.TYPE_4BYTE_ABGR);
		}

		public void debugDraw(Graphics2D g, boolean complex){
			
			if(complex){
				g.drawImage(OpenCVUtils.matToBufferedImage(sample.rawFrame), (int)(getRawX()-sample.frameRadius), (int)(getRawY()-sample.frameRadius), null);
			}
			
			//g.drawImage(debugImg2, (int)(getFixedRawX()-sample.frameRadius), (int)(getFixedRawY()-sample.frameRadius), null);
			g.setColor(TARGET_COLOR);
			g.fillOval((int)getFixedRawX()-2, (int)getFixedRawY()-2, 4, 4);
			g.drawOval((int)(getFixedRawX()-getRadius()), (int)(getFixedRawY()-getRadius()), (int)(getRadius()*2), (int)(getRadius()*2));
			g.setColor(INFO_COLOR);
			g.setFont(INFO_FONT);
			g.drawString(String.format("[%+.2f, %+.2f]", xShift, yShift), (int)(getFixedRawX() - getRadius()), (int)(getFixedRawY() - getRadius() - 2));
			g.drawString(String.format(" %.2f  %.2f", radius, angle), (int)(getFixedRawX() - getRadius()), (int)(getFixedRawY() - getRadius() - 10));
			
			g.drawString("#"+this.id, (int)(getFixedRawX() - getRadius()), (int)(12 + getFixedRawY() + getRadius()));
			
		}

		double lastAngle;
		private static final double ROUGH_STEP = Math.PI*0.25f;
		private static final double FINE_STEP = ROUGH_STEP*0.25f;
		
		public void crunch(){

			Imgproc.adaptiveThreshold(sample.rawFrame, sample.rawFrame, 254, Imgproc.THRESH_BINARY, Imgproc.ADAPTIVE_THRESH_MEAN_C, 21, 11);
			
			Mat contourFrame = new Mat();
			Imgproc.Canny(sample.rawFrame, contourFrame, THRESHOLD, 250);
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>(2);
			Mat hierarchy = new Mat();
			Imgproc.findContours(contourFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_NONE);
			hierarchy.release();
			contourFrame.release();
			
			MatOfPoint2f edgels = new MatOfPoint2f();
			
			double largestArea = 0;
			
			for(int i=0; i < contours.size(); i++){
				
				MatOfPoint contour = contours.get(i);
				
				double contourArea = Imgproc.contourArea(contour);
				
				if(largestArea < contourArea){
					largestArea = contourArea;
					//Core.drawContours(frame, contours, i, new Scalar(255));
					contours.get(i).convertTo(edgels, CvType.CV_32FC2);
				}
				
				contour.release();
			}

			if(edgels.empty() || edgels.rows() < 100){
				edgels.release();
				return;
			}
			
			RotatedRect ellipse = Imgproc.fitEllipse(edgels);

			edgels.release();
			
			//Correct shift
			xShift = sample.frameRadius - ellipse.center.x;
			yShift = sample.frameRadius - ellipse.center.y;

			//Transform image 
			//TODO: optimize transformation matrices into one before applying the transformation
			
			double theta = Math.toRadians(ellipse.angle);
			double yshear = 0*Math.tan(-theta);
			double xshear = (1-(ellipse.size.height/ellipse.size.width))*0;
			
			double xscale = 1 + 0*(ellipse.size.height/ellipse.size.width);
			double yscale = 8;
			
			double a = Math.cos(theta);
			double b = Math.sin(theta);
			
			Mat M = Mat.zeros(2, 3, CvType.CV_32F);
//			M.put(0, 0, new double[] {a,b,(1-a)*(sample.frameRadius-xShift*0.5f)-b*(sample.frameRadius-yShift*0.5f),
//									 -b,a,b*(sample.frameRadius-xShift*0.5f) + (1-a)*(sample.frameRadius-yShift*0.5f)});
//			M.put(0, 0, new double[] {1,0,xShift,
//					  				  0,1,yShift});
//			Imgproc.warpAffine(sample.rawFrame, sample.rawFrame, M, new Size(sample.frameRadius*2, sample.frameRadius*2));
//			M.release();
			
			Imgproc.threshold(sample.rawFrame, sample.rawFrame, THRESHOLD, 255, Imgproc.THRESH_BINARY);
			
			this.radius = (float) ((2*this.radius + ellipse.size.height*0.5f)/3f);
			
			double roughAngle = 0;
			double refinedAngle = 0;
			
			//roughAngle = findAngleByMaximum(0, 2*Math.PI, ROUGH_STEP, sample.rawFrame);
			//refinedAngle = findAngleBySweep(roughAngle, FINE_STEP, sample.rawFrame);
			
			roughAngle = findAngleByMaximum(sample.rawFrame, sample.frameRadius - xShift, sample.frameRadius - yShift, this.radius);
			refinedAngle = refineAngleDNC(sample.rawFrame, roughAngle, sample.frameRadius - xShift, sample.frameRadius - yShift, this.radius);
			//System.out.println(roughAngle);

			
			updateAngle(refinedAngle);
			
			//float size = (2*SAMPLE_FRAME_SIZE-outerBottom-outerTop-outerLeft-outerRight)/(float)SAMPLE_FRAME_SIZE;
			
			//Take size with average of W and H		
			
			
			float size = (float) (ellipse.size.height)/(float)(sample.frameRadius*2);

			//Rotate sample
//			M = Imgproc.getRotationMatrix2D(new Point(sample.frameRadius - xShift, sample.frameRadius - yShift), Math.toDegrees(-refinedAngle), 1); //1+(ellipse.size.height/SAMPLE_FRAME_SIZE)*0.5f);
//			Imgproc.warpAffine(sample.rawFrame, sample.rawFrame, M, sample.rawFrame.size());
//			M.release();
			
			//Accumulate data
//			if(accumFrame == null) accumFrame = Mat.zeros(SAMPLE_FRAME_SIZE, SAMPLE_FRAME_SIZE, CvType.CV_32FC1);
//			Imgproc.accumulateWeighted(frame, accumFrame, 0.2f);
//			accumFrame.convertTo(frame, CvType.CV_8UC1);
			
			//int newCode = parseCodeBySweep(frame);
		
			Imgproc.erode(sample.rawFrame, sample.rawFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
			
			if(debugImg == null) return;
			
			//int newCode = parseCodeByRays(sample.rawFrame, (Graphics2D)debugImg2.getGraphics());
			
			//Here we add to the angle (2 * 1/8) + (0.5 * 1/8) to center it with the markers
			setCode(parseCode(sample.rawFrame, this.angle + (Math.PI*0.3125f), sample.frameRadius - xShift, sample.frameRadius-yShift));
			
			
			//if(newCode > 0) code_confirms[last_index] = newCode;
		}
		
		private int parseCodeByRays(Mat frame, Graphics2D g){

			char rawData[] = new char[DATA_LENGTH]; 
			char di = 0;
			
//			g.setBackground(TRANSPARENT);
//			g.clearRect(0, 0, sample.frameRadius*2, sample.frameRadius*2);
			
			for(float i=(float) (Math.PI*0.36f); i < Math.PI*1.75f; i += (Math.PI*2)/(DATA_LENGTH+2)){

				float xi = (float) (Math.sin(i));
				float yi = (float) (Math.cos(i));

				int x = (int)(xi*25 + sample.frameRadius);				
				int y = (int)(yi*25 + sample.frameRadius);
				if(x < 0) x = 0; else if(x >= sample.frameRadius*2) x = sample.frameRadius*2-1;
				if(y < 0) y = 0; else if(y >= sample.frameRadius*2) y = sample.frameRadius*2-1;

				g.setColor(new Color(255, 0, 0, 255));
				//g.drawLine(SAMPLE_FRAME_SIZE/2, SAMPLE_FRAME_SIZE/2, x, y);

				float value1 = 0;
				float value2 = 0;
				
				for(int r=13; r < 17; r++){
					x = (int)(xi*r + sample.frameRadius);				
					y = (int)(yi*r + sample.frameRadius);
					if(x < 0) x = 0; else if(x >= sample.frameRadius*2) x = sample.frameRadius*2-1;
					if(y < 0) y = 0; else if(y >= sample.frameRadius*2) y = sample.frameRadius*2-1;

					value1 += (float) (frame.get(y, x)[0]);
					//g.drawLine(x, y, x, y);
				}
				
				value1 /= 4;
				
				for(int r=19; r < 23; r++){
					x = (int)(xi*r + sample.frameRadius);				
					y = (int)(yi*r + sample.frameRadius);
					if(x < 0) x = 0; else if(x >= sample.frameRadius*2) x = sample.frameRadius*2-1;
					if(y < 0) y = 0; else if(y >= sample.frameRadius*2) y = sample.frameRadius*2-1;

					value2 += (float) (frame.get(y, x)[0]);
					//g.drawLine(x, y, x, y);
				}
				
				value2 /= 4;
				
				if(value1 > THRESHOLD && value2 < THRESHOLD){
					rawData[di] = 1;
				} else if(value1 < THRESHOLD && value2 > THRESHOLD){
					rawData[di] = 2;
				} else if(value1 > THRESHOLD && value2 > THRESHOLD){
					rawData[di] = 3;
				} else {
					rawData[di] = 0;
				}
				
				di++;
				
			}
			
			if(checksumOK(rawData)){
				int newCode = rawData[0] + rawData[1]*3 + rawData[2]*9 + rawData[3]*27 + rawData[4]*81 + rawData[5]*243;
				return newCode;
			}

			return -1;
		}
		
		private void updateAngle(double b){
			lastAngle = (lastAngle*2 + b)/3;
			this.angle = lastAngle;
		}
		
		private double findAngleByMaximum(double from, double to, double step, Mat m){
			int lim = 0;
			float target = Integer.MAX_VALUE;
			double angle = from;
			
			//Rough find the starting segment
			for(double i=from; i < to; i += step){

				double xi = Math.sin(i);
				double yi = Math.cos(i);
				lim = 0;

				for(int d = 5; d <= 30; d++){
					int x = (int)(xi*d + sample.frameRadius - xShift);				
					int y = (int)(yi*d + sample.frameRadius - yShift);
					if(x >= sample.frameRadius*2 || y >= sample.frameRadius) continue;
					lim += m.get(y, x)[0];
					//g.drawLine(x, y, x, y);
				}
				
				if(lim < target){
					target = lim;
					angle = i;
				}
				
			}
			return angle;
		}
		
		private double findAngleBySweep(double from, double step, Mat m){
			
			double angle = from;
			float value;
			int interrupt = 0;

			do {
				float xi = (float) (Math.sin(angle));
				float yi = (float) (Math.cos(angle));
				
				value = 0;
				
				//g.setColor(Color.red);
				
				int rad = (int)getRadius();
				
				for(int r = rad - 10; r < rad - 5; r++){
					int x = (int)(xi*r + sample.frameRadius + xShift);				
					int y = (int)(yi*r + sample.frameRadius + yShift);
					
					if(x >= sample.frameRadius*2 || y >= sample.frameRadius) return lastAngle;
					
					value += (float) m.get(y, x)[0];
					debugImg.getGraphics().setColor(Color.red);
					debugImg.getGraphics().drawLine(x, y, x, y);
				}
				
				value /= 5;
			
				if(value < THRESHOLD){
					angle += step;
					step *= 0.5f;
				}
			
				angle -= step;
				
				interrupt ++;
			
			} while (interrupt < 12);
			
			return angle;
		}
		
		
		public void crunc2() {
			
			if(sample == null) return;
			
//			Imgproc.adaptiveThreshold(sample.rawFrame, sample.rawFrame, 254, Imgproc.THRESH_BINARY, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 11, 11);
			
			quickTrimCentre();
//			Imgproc.dilate(sample.rawFrame, sample.rawFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5)));
			
			double roughAngle = findAngleByMaximum(sample.rawFrame, sample.frameRadius+xShift, sample.frameRadius+yShift, this.radius);
			this.angle = refineAngleDNC(sample.rawFrame, roughAngle, sample.frameRadius+xShift, sample.frameRadius+yShift, this.radius);
			
//			fitEllipse(sample.rawFrame);
//			
//			this.id = parseCodeSimple(sample.frameRadius+xShift, sample.frameRadius+yShift);
		
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
//			xShift = ((innerRight-innerLeft)+(outerRight-outerLeft))/4.0f;
//			yShift = ((innerBottom-innerTop)+(outerBottom-outerTop))/4.0f;
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
			//Imgproc.dilate(rawFrame, rawFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));

//			//float size = (2*SAMPLE_FRAME_SIZE-outerBottom-outerTop-outerLeft-outerRight)/(float)SAMPLE_FRAME_SIZE;
//			
//			//Take size with average of W and H
//			this.radius = (float) ((2*radius + ellipse.size.height*0.5f)/3f);
//			float size = (float) (ellipse.size.height)/(float)(frameRadius*2);
//			
//			M = Imgproc.getRotationMatrix2D(new Point(frameRadius, frameRadius), Math.toDegrees(-angle), 1/size); //1+(ellipse.size.height/SAMPLE_FRAME_SIZE)*0.5f);
//			Imgproc.warpAffine(rawFrame, rawFrame, M, rawFrame.size());
//			M.release();
//			
//			//Accumulate data
//			if(accumFrame == null) accumFrame = Mat.zeros(SAMPLE_FRAME_SIZE, SAMPLE_FRAME_SIZE, CvType.CV_32FC1);
//			Imgproc.accumulateWeighted(frame, accumFrame, 0.2f);
//			accumFrame.convertTo(frame, CvType.CV_8UC1);
//			
//			//int newCode = parseCodeBySweep(frame);
//		
//			int newCode = parseCodeByRays(rawFrame, g);
//			
//			if(newCode > 0) code_confirms[last_index] = newCode;
			
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
			for(int i=1; i < sample.frameRadius*0.30f; i++){
				if(findXMax && sample.frameRadius + i < sample.rawFrame.width() && sample.rawFrame.get(sample.frameRadius, sample.frameRadius + i)[0] > THRESHOLD) innerLeft = i; else findXMax = false;
				if(findXMin && sample.frameRadius - i > 0 && sample.rawFrame.get(sample.frameRadius, sample.frameRadius - i)[0] > THRESHOLD) innerRight = i; else findXMin = false;
				if(findYMax && sample.frameRadius + i < sample.rawFrame.height() && sample.rawFrame.get(sample.frameRadius + i, sample.frameRadius)[0] > THRESHOLD) innerTop = i; else findYMax = false;
				if(findYMin && sample.frameRadius - i > 0 && sample.rawFrame.get(sample.frameRadius - i, sample.frameRadius)[0] > THRESHOLD) innerBottom = i; else findYMin = false;
			}
			
			findXMin = true;
			findXMax = true;
			findYMin = true;
			findYMax = true;
			
			//Test the outsides. Outside in.
			for(int i=1; i < sample.frameRadius*0.15f; i++){
				if(findXMax && sample.rawFrame.get(sample.frameRadius, i)[0] > THRESHOLD) outerLeft = i; else findXMax = false;
				if(findXMin && sample.rawFrame.get(sample.frameRadius, sample.frameRadius - 1 - i)[0] > THRESHOLD) outerRight = i; else findXMin = false;
				if(findYMax && sample.rawFrame.get(i, sample.frameRadius)[0] > THRESHOLD) outerTop = i; else findYMax = false;
				if(findYMin && sample.rawFrame.get(sample.frameRadius - 1 - i, sample.frameRadius)[0] > THRESHOLD) outerBottom = i; else findYMin = false;
			}
			
			//this.xShift = this.xShift*0.5f + 0.5f*((innerLeft - innerRight + outerLeft - outerRight)/2.0);
			//this.yShift = this.yShift*0.5f + 0.5f*((innerTop - innerBottom + outerTop - outerBottom)/2.0);
			
			//this.radius = (this.radius*2 + Math.abs(sample.frameRadius*2 - outerLeft - outerRight) + Math.abs(sample.frameRadius*2-outerTop-outerBottom))/6f;
			this.radius = (Math.abs(sample.frameRadius*2 - outerLeft - outerRight) + Math.abs(sample.frameRadius*2 - outerTop - outerBottom))/4f;
		}
		
		private boolean fitEllipse(Mat frame){
			
			Mat contourFrame = new Mat();
			Imgproc.Canny(frame, contourFrame, 96, 192);
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
			//this.xShift = sample.frameRadius - ellipse.center.x;
			//this.yShift = sample.frameRadius - ellipse.center.y;
			
			this.radius = (ellipse.boundingRect().height + ellipse.boundingRect().width)/4.0;
			
			//Correct the rotation and scale
			double theta = Math.toRadians(ellipse.angle);
			double a = Math.cos(theta);
			double b = Math.sin(theta);
			
			Mat M = Mat.zeros(2, 3, CvType.CV_32F);
			
			M.put(0, 0, new double[] {a,b,(1-a)*(ellipse.center.x) -     b*(ellipse.center.y),
									 -b,a,    b*(ellipse.center.x) + (1-a)*(ellipse.center.y)});
			
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
			int raysum = 0;
			float maximum = 0;
			double angle = 0;

			Graphics2D g = (Graphics2D) debugImg.getGraphics(); 
			g.setBackground(TRANSPARENT);
			g.clearRect(0, 0, sample.frameRadius*2, sample.frameRadius*2);

			//Go through angles
			for(double i=0; i < 2*Math.PI; i += (2*Math.PI)/ROUGH_SCAN_DIVISIONS){

				double xi = Math.sin(i);
				double yi = Math.cos(i);
				raysum = 0;
				
				for(int d = (int) (ellipseRadius*(RING1RAD - 2*RINGWIDTH)); d <= (ellipseRadius*(RING2RAD+2*RINGWIDTH)); d+=2){
					int x = (int)(xi*d + xCenter);				
					int y = (int)(yi*d + yCenter);
					
					if(x < 0 || y < 0 || x >= m.width() || y >= m.height()){
						System.out.println("Off limits? " + x + " " + y + " " + m.width() + " " + m.height());
					} else {
						if(m.get(y, x)[0] < THRESHOLD) raysum++;
					}
				    
					g.setColor(Color.green);
					g.drawLine(x,y,x,y);
				    
				}
				
				if(raysum > maximum){
					maximum = raysum;
					angle = i;
				}
				
			}
			return angle;
		}
		
		private double refineAngleDNC(Mat m, double roughAngle, double xCenter, double yCenter, double ellipseRadius){
			
			Graphics2D g = (Graphics2D) debugImg.getGraphics(); 
			g.setBackground(TRANSPARENT);
			//g.clearRect(0, 0, sample.frameRadius*2, sample.frameRadius*2);
			
			double step = (Math.PI*2)/9;
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
					
					g.setColor(TARGET_COLOR);
					g.drawLine((int)xCenter, (int)yCenter, x, y);
				}
			
				if(value < THRESHOLD){
					refinedAngle -= step;
					step *= 0.5f;
				} 
				
				refinedAngle += step;
				
				interrupt++;
			
			} while (interrupt < FINE_SCAN_DIVS);
			
			return refinedAngle;
		}
		
		private int parseCodeSimple(double xCenter, double yCenter){
			
			final int SAMPLES = 3;
			final double SEGMENT_WIDTH = (Math.PI*2)/(DATA_LENGTH+2);
			char rawData[] = new char[DATA_LENGTH]; 
			char di = 0;
			
			//For appropriate area of the circle.
			for(double i=this.angle; i < 2*Math.PI+this.angle-2*SEGMENT_WIDTH; i += SEGMENT_WIDTH){

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
						
						if(x < 0) x = 0; else if(x >= sample.rawFrame.width()) x = sample.rawFrame.width()-1;
						if(y < 0) y = 0; else if(y >= sample.rawFrame.height()) y = sample.rawFrame.height()-1;
	
						value1 += (value1 + sample.rawFrame.get(y, x)[0])/2.0;
						
						Graphics2D g2 = (Graphics2D)debugImg.getGraphics();
						g2.setColor(TARGET_COLOR);
						g2.drawLine(x, y, x, y);
						it++;
					}
				}
				value1 /= it; 
			}

			if(checksumOK(rawData)){
				int newCode = rawData[0] + rawData[1]*3 + rawData[2]*9 + rawData[3]*27 + rawData[4]*81 + rawData[5]*243;
				return newCode;
			} else return -1;
		}
		
		private int parseCode(Mat frame, double startingAngle, double xCenter, double yCenter){

			Graphics2D g = (Graphics2D) debugImg.getGraphics(); 
			g.setBackground(TRANSPARENT);
			g.clearRect(0, 0, sample.frameRadius*2, sample.frameRadius*2);
			g.setColor(Color.red);
			
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

				double value1 = 0;
				it = 0;
				for (double d = i-SEGMENT_WIDTH/4; d < i+SEGMENT_WIDTH/4; d += SEGMENT_WIDTH/(CODE_SEGMENT_SAMPLES*2)){
					xi = Math.sin(d);
					yi = Math.cos(d);
					for(int r=(int) ((RING1RAD-RINGWIDTH)*this.radius); r < ((RING1RAD+RINGWIDTH)*this.radius); r++){
						x = (int)(xi*r + xCenter);
						y = (int)(yi*r + yCenter);
						
						if(x < 0) x = 0; else if(x >= frame.width()) x = frame.width()-1;
						if(y < 0) y = 0; else if(y >= frame.height()) y = frame.height()-1;
	
						value1 += frame.get(y, x)[0];

						g.drawLine(x, y, x, y);
						
						it++;
					}
				}
				value1 /= it; 
				
				double value2 = 0;
				it = 0;
				for (double d = i-SEGMENT_WIDTH/4; d < i+SEGMENT_WIDTH/4; d += SEGMENT_WIDTH/(CODE_SEGMENT_SAMPLES*2)){
					xi = Math.sin(d);
					yi = Math.cos(d);
					for(int r=(int) ((RING2RAD-RINGWIDTH)*this.radius); r < ((RING2RAD+RINGWIDTH)*this.radius); r++){
						x = (int)(xi*r + xCenter);
						y = (int)(yi*r + yCenter);
						
						if(x < 0) x = 0; else if(x >= frame.width()) x = frame.width()-1;
						if(y < 0) y = 0; else if(y >= frame.height()) y = frame.height()-1;
	
						value2 += frame.get(y, x)[0];
						
						g.drawLine(x, y, x, y);
						
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
			
			//System.out.print("Code: ");
			//for(double c : rawData){
			//	System.out.print((int)c);
			//}
			//System.out.print("\n");
			
			
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
		
		private void setCode(int code){
			this.assignedCodes[codeIndex] = code;
			codeIndex++;
			if(codeIndex >= MAX_BUFFERED_CODES) codeIndex = 0;
			
			this.id = codemode(assignedCodes)[0];
			
		}

		public static int[] codemode(int a[]) {
			
			Arrays.sort(a);
			
			int modeNum = a[0];
			int testNum = a[0];
			int curCount = 0;
			int maxCount = 0;
			
			for(int i=1; i < a.length; i++){
				
				if(a[i] == 0) break;
				
				if(a[i] == testNum){
					curCount++;
				} else {
					if(curCount > maxCount){
						modeNum = testNum;
						maxCount = curCount;
					}
					testNum = a[i];
					curCount = 0;
				}
				
				if(maxCount >= a.length-i) break;
			}
			
			return new int[] {modeNum, maxCount};
			
		}

		public double getFixedRawX(){
			return (rawX - xShift + bufferedX)/2.0f;
		}
		public double getFixedRawY(){
			return (rawY - yShift + bufferedY)/2.0f;
		}
		
		public double getAngle(){
			return angle;
		}
		
		public float getDecay(){
			return (System.currentTimeMillis()-timeLastSeen)/(float)120;
		}

		public void update(TripcodeCandidateSample t) {
			this.sample.release(); //Release old sample
			this.sample = t;
			
			//Priorize larger radii
			if(t.r > this.radius){
				this.radius = this.radius*0.5f + t.r*0.5f;
			} else {
				this.radius = this.radius*0.9f + t.r*0.1f;
			}
			
			this.bufferedX = getFixedRawX()*0.75f + t.x*0.25f;
			this.bufferedY = getFixedRawY()*0.75f + t.y*0.25f;
			
			super.update(t.x, t.y, -1, 1);
		}

		public void release(){
			this.sample.release();
		}
	
}
