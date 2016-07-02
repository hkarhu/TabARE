/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.conf.tabare;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorMOG2;

import fi.conf.tabare.Params.Blur;
import fi.conf.tabare.Params.MorphOps;
import fi.conf.tabare.TrackableObject.ItemType;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 *
 * @author irah
 */
public class ARDataProvider extends JFrame {

	private static final String PARAMS_FILE = "saved_params.ser";
	private static final int MAX_VIDEO_DEVICE_INDEX = 5;

	static {
		//System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
		nu.pattern.OpenCV.loadShared();
		//nu.pattern.OpenCV.loadLocally();
		//System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) {
		(new ARDataProvider()).setVisible(true);
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JComboBox blurComboBox;
	//private javax.swing.JButton buttonRemoveBackg;
	private javax.swing.JButton buttonSave;
	private fi.conf.tabare.PreviewPanel camPreviewPanel;
	private javax.swing.JCheckBox checkAdaptiveThreshTrip;
	private javax.swing.JCheckBox checkEnableBlobs;
	private javax.swing.JCheckBox checkEnableBlobs1;
	private javax.swing.JCheckBox checkEnableFiducials;
	private javax.swing.JCheckBox checkInvertThreshBlob;
	private javax.swing.JCheckBox checkInvertThreshTrip;
	private javax.swing.JCheckBox checkTripPreThreshold;
	private JCheckBox chckbxEnableDistort; 
	//private javax.swing.JCheckBox checkBGRemovalAccumulative;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JMenuItem jMenuItem1;
	private javax.swing.JSpinner jSpinner1;
	private javax.swing.JSpinner jSpinner2;
	private javax.swing.JSpinner jSpinner3;
	private javax.swing.JLabel labelBlur;
	private javax.swing.JLabel labelBrightness;
	private javax.swing.JLabel labelCertaintyTrip;
	private javax.swing.JLabel labelContrast;
	private javax.swing.JLabel labelThresholdBlob;
	private javax.swing.JLabel labelThresholdTrip1;
	private javax.swing.JLabel labelTripCannyThresh;
	private javax.swing.JLabel labelTripCenterDetectThresh;
	private javax.swing.JLabel labelTripInvResRat;
	private javax.swing.JLabel labelTripMaxRad;
	private javax.swing.JLabel labelTripMinCenterDist;
	private javax.swing.JLabel labelTripMinRad;
	private javax.swing.JMenuBar menuBar;
	private javax.swing.JMenu menuFile;
	private javax.swing.JComboBox morphOpsComboBoxBlob;
	private javax.swing.JComboBox morphOpsComboBoxTrip;
	private javax.swing.JLabel morphOpsLabelBlob;
	private javax.swing.JLabel morphOpsLabelTrip;
	private javax.swing.JTabbedPane pipelineTabs;
	private javax.swing.JSlider sliderBlur;
	private javax.swing.JSlider sliderBrightness;
	private javax.swing.JSlider sliderThresholdBlob;
	private javax.swing.JSlider sliderThresholdTrip;
	private javax.swing.JSpinner spinnerCannyThresh;
	private javax.swing.JSpinner spinnerTripCenterDist;
	private javax.swing.JSpinner spinnerTripCenterThresh;
	private javax.swing.JSpinner spinnerTripInvResRat;
	private javax.swing.JSpinner spinnerTripMaxRad;
	private javax.swing.JSpinner spinnerTripMinRad;
	//private javax.swing.JPanel tabBGRemoval;
	private javax.swing.JPanel tabBlobDetect;
	private javax.swing.JPanel tabCalibration;
	private javax.swing.JPanel tabImgInput;
	private javax.swing.JPanel tabTripFiducials;
	// End of variables declaration//GEN-END:variables

	private final float OBJECT_MERGE_PROXIMITY = 10;

	private Params params;

	private Reality reality;

	private VideoCapture inputVideo;
	//	private int videoWidth = 0;
	//	private int videoHeight = 0;
	private Size frameSize;

	private Mat shiftMat = Mat.zeros( 2, 3, CvType.CV_32F );
	private Mat distCoeffs = new Mat( 5, 1, CvType.CV_32F );

	//	private BackgroundSubtractor backgSubstractor = new BackgroundSubtractorMOG2(100, 0.2f, false);
	//	private boolean dynamicBGRemoval = false;
	//	private float dynamicBGAmount = 1.0f;
	//	private Mat background;
	//	private boolean recaptureBg = true;

	private boolean running = true;
	private final Thread detectionThread;
	private final Thread trackingThread;

	public enum View {raw, dist, bgrm, blob, trip, calib} //The tabs should be in the same order.

	private long lastFrameDetectTime = 0;
	private long lastFrameTrackTime = 0;
	private float detectTime = 0;
	private float trackTime = 0;

	private FeatureDetector blobFeatureDetector;
	private int lastBlobIndex = 0;
	private ConcurrentLinkedQueue<MatOfKeyPoint> blobData;
	private ConcurrentLinkedQueue<TrackableBlob> trackedBlobs;

	private ConcurrentLinkedQueue<TripcodeCandidateSample> tripcodeData;
	private ConcurrentHashMap<Integer,TrackableTripcode> trackedTripcodes;

	private View selectedView = View.raw;

	private JLabel lblFound;
	private Calibrator calibrator;
	private JPanel tabDistortion;
	private JSlider slider_k1;
	private JSlider slider_k2;
	private JSlider slider_p1;
	private JSlider slider_p2;
	private JSlider slider_k3;
	private JSlider slider_z;
	private JLabel lblK1;
	private JLabel lblK2;
	private JLabel lblK3;
	private JLabel lblP1;
	private JLabel lblP2;
	private JLabel lblZ;
	private JSlider sliderContrast;
	private JPanel tabBGRemoval;

	/**
	 * Creates new form CamImageSettings
	 */
	public ARDataProvider() {

		blobData = new ConcurrentLinkedQueue<>();
		tripcodeData = new ConcurrentLinkedQueue<>();
		trackedTripcodes = new ConcurrentHashMap<>();
		trackedBlobs = new ConcurrentLinkedQueue<>();

		reality = new Reality();

		int videoInID = MAX_VIDEO_DEVICE_INDEX/2;

		//Init
		while(inputVideo == null || frameSize == null || (int)frameSize.width <= 0 || (int)frameSize.height <= 0){

			if(inputVideo != null) inputVideo.release();
			inputVideo = null;
			videoInID--;
			if(videoInID < 0) videoInID = MAX_VIDEO_DEVICE_INDEX;

			try {
				inputVideo = new VideoCapture();

				System.out.println("Trying input " + videoInID);
				inputVideo.open(videoInID);

				Thread.sleep(1000);

//				inputVideo.set(OpenCVUtils.CAP_PROP_FRAME_WIDTH, 640);
//				inputVideo.set(OpenCVUtils.CAP_PROP_FRAME_HEIGHT, 480);
//				inputVideo.set(OpenCVUtils.CAP_PROP_FPS, 60);
				
				frameSize = new Size((int)inputVideo.get(Highgui.CV_CAP_PROP_FRAME_WIDTH),(int)inputVideo.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT));

				if(inputVideo.isOpened()){
					System.out.println("Video size: " + frameSize.width + "x" + frameSize.height);
					System.out.println("Camera initialised with " + inputVideo.get(OpenCVUtils.CAP_PROP_FPS) + "fps");
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			} catch (Error e) {
				e.printStackTrace();
				continue;
			}

		}

		camPreviewPanel = new PreviewPanel();
		camPreviewPanel.initialize((int)frameSize.width, (int)frameSize.height);
		calibrator = new Calibrator(2, 2, (int)frameSize.width, (int)frameSize.height);
		//TODO: Load calibration values from storage!

		camPreviewPanel.addMouseListener(calibrator);
		camPreviewPanel.addMouseMotionListener(calibrator);

		initComponents();
		loadParams();

		detectionThread = new Thread(){
			@Override
			public void run() {
				detect();
			}
		};

		trackingThread = new Thread(){
			@Override
			public void run() {
				track();
			}
		};

		trackingThread.start();
		detectionThread.start();

		this.pack();

	}

	public void requestClose() {
		stop();
		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private void loadParams(){
		try {
			FileInputStream fileIn = new FileInputStream(PARAMS_FILE);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			params = (Params) in.readObject();
			in.close();
			fileIn.close();
		} catch(Exception e) {
			System.out.println("No params file.");
			params = new Params();
		}

		//Distortion
		chckbxEnableDistort.setSelected(params.enableDistortion);
		slider_k1.setValue(params.k1);
		slider_k2.setValue(params.k2);
		slider_k3.setValue(params.k3);
		slider_p1.setValue(params.p1);
		slider_p2.setValue(params.p2);
		slider_z.setValue(params.z);

		//Calibration
		calibrator.setRawCalibrationData(params.calibrationData);
		sliderBrightness.setValue((int)((params.brightness+255)/5.12f));
		sliderContrast.setValue((int)((params.contrast-0.5f)/0.04f));
		blurComboBox.setSelectedIndex(params.blur.ordinal());

		//Blob
		sliderThresholdBlob.setValue((int)((params.blobThreshold-1)/2.54f));
		checkInvertThreshTrip.setSelected(params.tripThInverted);

		//Trip
		checkEnableFiducials.setSelected(params.tripTracking);
		checkTripPreThreshold.setSelected(params.tripEnableThresholding);
		morphOpsComboBoxTrip.setSelectedIndex(params.tripMorphOps.ordinal());
		spinnerCannyThresh.setValue(params.tripCannyThresh);
		spinnerTripCenterDist.setValue(params.tripCenterDist);
		spinnerTripCenterThresh.setValue(params.tripAccumThresh);
		spinnerTripInvResRat.setValue(params.tripDP);
		spinnerTripMaxRad.setValue(params.tripRadMax);
		spinnerTripMinRad.setValue(params.tripRadMin);
		sliderThresholdTrip.setValue((int)params.tripThreshold);
		checkInvertThreshTrip.setSelected(params.tripThInverted);
		checkAdaptiveThreshTrip.setSelected(params.tripAdaptThreshold);

	}

	private void saveParams(){

		params.calibrationData = calibrator.getRawCalibrationData();

		try {
			FileOutputStream fileOut = new FileOutputStream(PARAMS_FILE);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(params);
			out.close();
			fileOut.close();
			System.out.printf("Serialized calibration.");
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	private void detect() {

		//Mat composite_image;
		Mat input_image = new Mat();
		Mat undistorted_image = new Mat();
		Mat circles = new Mat();
		MatOfKeyPoint mokp = new MatOfKeyPoint();
		Mat cameraMatrix = null;

		//List<Mat> channels = new LinkedList<>();

		//Loop
		while(running){
			try {
				if (inputVideo.read(input_image)){
					Mat preview_image = null;

					if(selectedView == View.calib) preview_image = input_image.clone();

					//Imgproc.cvtColor(input_image, input_image, Imgproc.COLOR_RGB2HSV);
					//Core.split(input_image, channels);

					Imgproc.cvtColor(input_image, input_image, Imgproc.COLOR_BGR2GRAY);

					//Imgproc.equalizeHist(input_image, input_image);

					input_image.convertTo(input_image, -1, params.contrast, params.brightness); //image*contrast[1.0-3.0] + brightness[0-255]

					doBlur(input_image, input_image, params.blur, params.blurAmount);

					if(selectedView == View.raw) preview_image = input_image.clone();

					if(params.enableDistortion){

						if(cameraMatrix == null) cameraMatrix = Imgproc.getDefaultNewCameraMatrix(Mat.eye(3, 3, CvType.CV_64F), new Size(input_image.width(), input_image.height()), true);

						Imgproc.warpAffine(input_image, input_image, shiftMat, frameSize);

						if(undistorted_image == null) undistorted_image = new Mat((int)frameSize.width*2, (int)frameSize.height*2, CvType.CV_64F);

						Imgproc.undistort(input_image, undistorted_image, cameraMatrix, distCoeffs);

						input_image = undistorted_image.clone();

						if(selectedView == View.dist) preview_image = input_image.clone();

					}

					//					if(background == null) background = input_image.clone();			
					//					if(recaptureBg){
					//						backgSubstractor.apply(background, background);
					//						System.out.println(background.channels() + " " + background.size() );
					//						System.out.println(input_image.channels() + " " + input_image.size() );
					//						recaptureBg = false;
					//					}
					//					if(dynamicBGRemoval){
					//						//Imgproc.accumulateWeighted(input_image, background, dynamicBGAmount);
					//						//Imgproc.accumulateWeighted(input_image, background, 1.0f);
					//						//Core.subtract(input_image, background, input_image);
					//						//Core.bitwise_xor(input_image, background, input_image);
					//
					//						doBlur(input_image, background, Blur.normal_7x7, 0); //Blur a little, to get nicer result when substracting
					//						backgSubstractor.apply(background, background, dynamicBGAmount);
					//					}
					//					if(background != null) Core.add(input_image, background, input_image);

					if(params.blobTracking){
						Mat blobs_image = input_image.clone();

						Imgproc.threshold(blobs_image, blobs_image, params.blobThreshold, 254, (params.blobThInverted ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY));

						Size kernelSize = null;

						switch(params.blobMorpthKernelSize){
						case size_3x3:
							kernelSize = new Size(3, 3); 
							break;
						case size_5x5:
							kernelSize = new Size(5, 5); 
							break;
						case size_7x7:
							kernelSize = new Size(7, 7);
							break;
						case size_9x9:
							kernelSize = new Size(9, 9); 
							break;
						}

						int kernelType = -1;

						switch(params.blobMorphKernelShape){
						case ellipse:
							kernelType = Imgproc.MORPH_ELLIPSE;
							break;
						case rect:
							kernelType = Imgproc.MORPH_RECT;
							break;
						default:
							break;
						}

						switch(params.blobMorphOps){
						case dilate:
							Imgproc.dilate(blobs_image, blobs_image, Imgproc.getStructuringElement(kernelType, kernelSize));
							break;
						case erode:
							Imgproc.erode(blobs_image, blobs_image, Imgproc.getStructuringElement(kernelType, kernelSize));
							break;
						default:
							break;
						}

						if(blobFeatureDetector == null) blobFeatureDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);

						blobFeatureDetector.detect(blobs_image, mokp);
						blobData.add(mokp);

						if(selectedView == View.blob) preview_image = blobs_image.clone();

						blobs_image.release();
					}

					if(params.tripTracking){
						
						Mat trips_image = undistorted_image.clone();

						if(params.tripEnableThresholding)
						if(params.tripAdaptThreshold){
							Imgproc.adaptiveThreshold(trips_image, trips_image, 255, (params.tripThInverted ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY), Imgproc.ADAPTIVE_THRESH_MEAN_C, 5, params.tripThreshold*0.256f);
						} else {
							Imgproc.threshold(trips_image, trips_image, params.tripThreshold, 255, (params.tripThInverted ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY));	
						}

						switch(params.tripMorphOps){
						case dilate:
							Imgproc.dilate(trips_image, trips_image, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
							break;
						case erode:
							Imgproc.erode(trips_image, trips_image, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
							break;
						default:
							break;
						}

						//Imgproc.HoughCircles(tres, circ, Imgproc.CV_HOUGH_GRADIENT, 1, tres.height()/8, 80, 1+p.par4, p.par5, p.par6);
						Imgproc.HoughCircles(trips_image, circles, Imgproc.CV_HOUGH_GRADIENT, params.tripDP, params.tripCenterDist, params.tripCannyThresh, params.tripAccumThresh, params.tripRadMin, params.tripRadMax);
						
						for (int i = 0; i < circles.cols(); i++){
							
							double[] coords = circles.get(0,i);
							
							if(coords == null || coords[0] <= 1 || coords[1] <= 1) continue; //If the circle is off the limits, or too small, don't process it.

							TripcodeCandidateSample tc = new TripcodeCandidateSample(undistorted_image, coords);
							
							if(tc.isValid()) tripcodeData.add(tc);
							
						}
						
						if(selectedView == View.trip) preview_image = trips_image.clone();
						trips_image.release();

					}

					if(preview_image != null){
						camPreviewPanel.updatePreviewImage(preview_image);
						preview_image.release();
					}

				} else {
					System.out.println("frame/cam failiure!");
				}

			} catch(Exception e){
				e.printStackTrace();
				running = false;
			}

			//FPS calculations
			if(camPreviewPanel != null){
				long t = System.currentTimeMillis();
				detectTime = (t-lastFrameDetectTime);
				lastFrameDetectTime = t;
				camPreviewPanel.updateDetectTime(detectTime);
			}

		}

		//De-init
		circles.release();
		undistorted_image.release();
		input_image.release();
		inputVideo.release();
		shiftMat.release();
	}

	private void track(){
		while(running){

			//Process blobs
			while(params.blobTracking && !blobData.isEmpty()){

				MatOfKeyPoint r = blobData.poll();
				for(int i=0; i < r.rows(); i++){
					double[] c = r.get(i, 0);
					TrackableBlob blob = null;
					for(TrackableBlob cb : trackedBlobs){
						if(cb.getProximity(c[0], c[1], 0) <= OBJECT_MERGE_PROXIMITY){
							blob = cb;
							cb.update(c[0], c[1], 0, c[2]);
							break;
						}
					}
					if(blob == null){
						blob = new TrackableBlob(c[0], c[1], 0, c[2]);
						trackedBlobs.add(blob);
					}
					calibrator.applyCalibration(blob);
				}
				r.release();

			}

			//Process tripcodes
			if(params.tripTracking){ lblFound.setText(tripcodeData.size() + "/" + trackedTripcodes.size() + " fiducials found"); }
			while(params.tripTracking && !tripcodeData.isEmpty()){
				TripcodeCandidateSample t = tripcodeData.poll();

				TrackableTripcode trip = null;
				
				//Search if fiducial is close/overlapping with previously detected fiducials
				for(TrackableTripcode tb : trackedTripcodes.values()){
					//System.out.println("old: " + tb.getRawX() + " " + tb.getRawY() + ", new: " + t.x + " " + t.y + ", d: " + tb.getProximity(t.x, t.y));
					if(tb.getProximity(t.x, t.y) <= params.tripCenterDist){
						trip = tb;
						trip.update(t);
						break;
					}
				}

				if(trip == null){
					trip = new TrackableTripcode(t);
				}
				
				trip.crunch();
				
				int id = trip.getID();

				if(trackedTripcodes.contains(id)){
					trackedTripcodes.get(id).release();
				}
				
				trackedTripcodes.put((int)trip.getID(), trip);						

			}

			//Visualization and cleanup
			Graphics2D g = camPreviewPanel.getOverlayGraphics();
			if(g == null) continue;

			calibrator.drawGrid(g);

			for(TrackableBlob blob : trackedBlobs){
				if(blob.getDecay() > 1){
					trackedBlobs.remove(blob);
					continue;
				}
				g.setColor(Color.MAGENTA);
				g.drawString("" + blob.getID(), (int)blob.getRawX(), (int)blob.getRawY());
				g.fillOval((int)blob.getRawX()-2, (int)blob.getRawY()-2, 4, 4);
			}

			for(TrackableTripcode trip : trackedTripcodes.values()){

				if(trip.getDecay() > 1){
					continue;
				}

				trip.debugDraw(g, true);
			}

			camPreviewPanel.swapOverlay();

			//FPS calculations
			if(camPreviewPanel != null){
				long t = System.currentTimeMillis();
				trackTime = (t-lastFrameTrackTime);
				lastFrameTrackTime = t;
				camPreviewPanel.updateTrackTime(trackTime);
			}

			//Slowdown if not enough work, so we don't consume time for nothing.
			if(trackTime < 100){
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	private static void doBlur(Mat matIn, Mat matOut, Blur b, float p){
		int s = ((int)(p*7))*2+1;
		Size ksz = new Size(s,s);

		switch (b) {
		case normal:
			Imgproc.blur(matIn, matOut, ksz);
			break;
		case gaussian:
			Imgproc.GaussianBlur(matIn, matOut, ksz, 1+(p*14)/3f);
			break;
		case median:
			Imgproc.medianBlur(matIn, matOut, s);
			break;
		default:
			break;
		}
	}

	public Iterator<? extends TrackableObject> getTrackedItemIterator(ItemType type){
		switch (type) {
		case blob:
			return trackedBlobs.iterator();
		case tripFiducial:
			return trackedTripcodes.values().iterator();
		default:
			return null;
		}
	}

	public void stop(){
		running = false;
		System.out.println("Stopping OpenCV threads...");
		try {
			detectionThread.join();
			trackingThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/* Note! 
	 * When initializing sliders etc. check that the change listener 
	 * is added only after the whole element has been initialized/modified.
	 * This will trigger change event and will cause null pointer exception
	 * because 'params' hasn't been initialized yet.
	 */
	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		menuBar = new JMenuBar();
		menuFile = new JMenu();
		jMenuItem1 = new JMenuItem();
		pipelineTabs = new JTabbedPane();
		tabImgInput = new JPanel();
		labelContrast = new JLabel();
		sliderBrightness = new JSlider();
		labelBrightness = new JLabel();
		labelBlur = new JLabel();
		blurComboBox = new JComboBox();
		sliderBlur = new JSlider();
		sliderBlur.setValue(0);
		//tabBGRemoval = new JPanel();
		//checkBGRemovalAccumulative = new JCheckBox();
		jSpinner1 = new JSpinner();
		//buttonRemoveBackg = new JButton();
		checkEnableBlobs1 = new JCheckBox();
		buttonSave = new JButton();

		menuFile.setText("File");

		jMenuItem1.setText("jMenuItem1");
		menuFile.add(jMenuItem1);

		menuBar.add(menuFile);

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		pipelineTabs.setTabPlacement(javax.swing.JTabbedPane.LEFT);
		pipelineTabs.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				pipelineTabsStateChanged(evt);
			}
		});

		labelContrast.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		labelContrast.setText("Contrast");

		sliderBrightness.setFont(new java.awt.Font("Sans Serif", 0, 8)); // NOI18N
		sliderBrightness.setMajorTickSpacing(5);
		sliderBrightness.setMinorTickSpacing(1);
		sliderBrightness.setPaintLabels(true);
		sliderBrightness.setPaintTicks(true);
		sliderBrightness.setToolTipText("");
		sliderBrightness.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				sliderBrightnessStateChanged(evt);
			}
		});

		labelBrightness.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		labelBrightness.setText("Brightness");

		labelBlur.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		labelBlur.setText("Blur / Noise removal");

		blurComboBox.setModel(new DefaultComboBoxModel<>(Blur.values()));
		blurComboBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				blurComboBoxActionPerformed(evt);
			}
		});

		sliderBlur.setFont(new java.awt.Font("Sans Serif", 0, 8)); // NOI18N
		sliderBlur.setMajorTickSpacing(5);
		sliderBlur.setMinorTickSpacing(1);
		sliderBlur.setToolTipText("");
		sliderBlur.setEnabled(false);
		sliderBlur.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				sliderBlurStateChanged(evt);
			}
		});

		sliderContrast = new JSlider();
		sliderContrast.setToolTipText("");
		sliderContrast.setPaintTicks(true);
		sliderContrast.setPaintLabels(true);
		sliderContrast.setMinorTickSpacing(1);
		sliderContrast.setMajorTickSpacing(5);
		sliderContrast.setFont(new Font("Dialog", Font.PLAIN, 8));
		sliderContrast.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				sliderContrastStateChanged(evt);
			}
		});

		GroupLayout tabImgInputLayout = new javax.swing.GroupLayout(tabImgInput);
		tabImgInputLayout.setHorizontalGroup(
				tabImgInputLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(tabImgInputLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(tabImgInputLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(tabImgInputLayout.createSequentialGroup()
										.addGroup(tabImgInputLayout.createParallelGroup(Alignment.TRAILING)
												.addComponent(labelContrast)
												.addComponent(labelBrightness))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(tabImgInputLayout.createParallelGroup(Alignment.TRAILING)
												.addComponent(sliderContrast, GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
												.addComponent(sliderBrightness, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)))
								.addComponent(labelBlur)
								.addGroup(tabImgInputLayout.createSequentialGroup()
										.addComponent(blurComboBox, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(sliderBlur, GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)))
						.addContainerGap())
				);
		tabImgInputLayout.setVerticalGroup(
				tabImgInputLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(tabImgInputLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(tabImgInputLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(sliderBrightness, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(labelBrightness))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(tabImgInputLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(labelContrast)
								.addComponent(sliderContrast, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(labelBlur)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(tabImgInputLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(sliderBlur, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(blurComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addContainerGap(134, Short.MAX_VALUE))
				);
		tabImgInput.setLayout(tabImgInputLayout);

		pipelineTabs.addTab("Raw input", tabImgInput);

		//		checkBGRemovalAccumulative.setText("Accumulative");
		//		checkBGRemovalAccumulative.addActionListener(new java.awt.event.ActionListener() {
		//			public void actionPerformed(java.awt.event.ActionEvent evt) {
		//				jCheckBox1ActionPerformed(evt);
		//			}
		//		});
		//
		//		buttonRemoveBackg.setText("Resample background");
		//		buttonRemoveBackg.addActionListener(new java.awt.event.ActionListener() {
		//			public void actionPerformed(java.awt.event.ActionEvent evt) {
		//				buttonRemoveBackgActionPerformed(evt);
		//			}
		//		});

		checkEnableBlobs1.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		checkEnableBlobs1.setText("Enable background removal");
		checkEnableBlobs1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkEnableBlobs1ActionPerformed(evt);
			}
		});

		tabDistortion = new JPanel();
		pipelineTabs.addTab("Distortion", null, tabDistortion, null);

		chckbxEnableDistort = new JCheckBox();
		chckbxEnableDistort.setText("Enable distortion");
		chckbxEnableDistort.setFont(new Font("Dialog", Font.BOLD, 12));
		chckbxEnableDistort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chckbxEnableUndistortActionPerformed(e);
			}
		});

		slider_k1 = new JSlider();
		slider_k1.setValue(0);
		slider_k1.setMinimum(-1000);
		slider_k1.setMaximum(1000);
		slider_k1.setToolTipText("");
		slider_k1.setFont(new Font("Dialog", Font.PLAIN, 8));
		slider_k1.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				slider_k1StateChanged(e);
			}
		});

		slider_k2 = new JSlider();
		slider_k2.setValue(0);
		slider_k2.setToolTipText("");
		slider_k2.setMinimum(-1000);
		slider_k2.setMaximum(1000);
		slider_k2.setFont(new Font("Dialog", Font.PLAIN, 8));
		slider_k2.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				slider_k2StateChanged(e);
			}
		});

		slider_p1 = new JSlider();
		slider_p1.setValue(0);
		slider_p1.setToolTipText("");
		slider_p1.setMinimum(-1000);
		slider_p1.setMaximum(1000);
		slider_p1.setFont(new Font("Dialog", Font.PLAIN, 8));
		slider_p1.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				slider_p1StateChanged(e);
			}
		});

		slider_p2 = new JSlider();
		slider_p2.setValue(0);
		slider_p2.setToolTipText("");
		slider_p2.setMinimum(-1000);
		slider_p2.setMaximum(1000);
		slider_p2.setFont(new Font("Dialog", Font.PLAIN, 8));
		slider_p2.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				slider_p2StateChanged(e);
			}
		});

		slider_k3 = new JSlider();
		slider_k3.setValue(0);
		slider_k3.setToolTipText("");
		slider_k3.setMinimum(-1000);
		slider_k3.setMaximum(1000);
		slider_k3.setFont(new Font("Dialog", Font.PLAIN, 8));
		slider_k3.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				slider_k3StateChanged(e);
			}
		});

		slider_z = new JSlider();
		slider_z.setValue(0);
		slider_z.setToolTipText("");
		slider_z.setMinimum(0);
		slider_z.setMaximum(1000);
		slider_z.setFont(new Font("Dialog", Font.PLAIN, 8));
		slider_z.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				slider_zStateChanged(e);
			}
		});

		lblK1 = new JLabel("k1");
		lblK2 = new JLabel("k2");
		lblP1 = new JLabel("p1");
		lblP2 = new JLabel("p2");
		lblK3 = new JLabel("k3");
		lblZ = new JLabel("z");

		GroupLayout gl_tabDistortion = new GroupLayout(tabDistortion);
		gl_tabDistortion.setHorizontalGroup(
				gl_tabDistortion.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_tabDistortion.createSequentialGroup()
						.addContainerGap()
						.addGroup(gl_tabDistortion.createParallelGroup(Alignment.LEADING)
								.addGroup(Alignment.TRAILING, gl_tabDistortion.createSequentialGroup()
										.addComponent(lblK2, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(slider_k2, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE))
								.addGroup(Alignment.TRAILING, gl_tabDistortion.createSequentialGroup()
										.addComponent(lblK1, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(slider_k1, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE))
								.addGroup(Alignment.TRAILING, gl_tabDistortion.createSequentialGroup()
										.addComponent(lblK3, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(slider_k3, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE))
								.addGroup(Alignment.TRAILING, gl_tabDistortion.createSequentialGroup()
										.addComponent(lblP1, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(slider_p1, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE))
								.addGroup(Alignment.TRAILING, gl_tabDistortion.createSequentialGroup()
										.addComponent(lblP2, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(slider_p2, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE))
								.addComponent(chckbxEnableDistort)
								.addGroup(Alignment.TRAILING, gl_tabDistortion.createSequentialGroup()
										.addComponent(lblZ, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(slider_z, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE)))
						.addContainerGap())
				);
		gl_tabDistortion.setVerticalGroup(
				gl_tabDistortion.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_tabDistortion.createSequentialGroup()
						.addContainerGap()
						.addComponent(chckbxEnableDistort)
						.addGap(8)
						.addGroup(gl_tabDistortion.createParallelGroup(Alignment.TRAILING)
								.addComponent(slider_k1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblK1))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_tabDistortion.createParallelGroup(Alignment.TRAILING)
								.addComponent(slider_k2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblK2))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_tabDistortion.createParallelGroup(Alignment.TRAILING)
								.addComponent(slider_k3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblK3))
						.addGap(18)
						.addGroup(gl_tabDistortion.createParallelGroup(Alignment.TRAILING)
								.addComponent(slider_p1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblP1))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_tabDistortion.createParallelGroup(Alignment.TRAILING)
								.addComponent(slider_p2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblP2))
						.addGap(18)
						.addGroup(gl_tabDistortion.createParallelGroup(Alignment.TRAILING)
								.addComponent(slider_z, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblZ))
						.addGap(100))
				);
		tabDistortion.setLayout(gl_tabDistortion);

		//javax.swing.GroupLayout tabBGRemovalLayout = new javax.swing.GroupLayout(tabBGRemoval);
		//tabBGRemoval.setLayout(tabBGRemovalLayout);

		//TODO: functionality
		//pipelineTabs.addTab("BG Removal", tabBGRemoval);
		tabBlobDetect = new javax.swing.JPanel();
		sliderThresholdBlob = new javax.swing.JSlider();
		sliderThresholdBlob.setPaintLabels(true);
		labelThresholdBlob = new javax.swing.JLabel();
		checkInvertThreshBlob = new javax.swing.JCheckBox();
		checkEnableBlobs = new javax.swing.JCheckBox();
		morphOpsComboBoxBlob = new javax.swing.JComboBox();
		morphOpsLabelBlob = new javax.swing.JLabel();

		sliderThresholdBlob.setFont(new java.awt.Font("Sans Serif", 0, 8)); // NOI18N
		sliderThresholdBlob.setMajorTickSpacing(5);
		sliderThresholdBlob.setMinorTickSpacing(1);
		sliderThresholdBlob.setPaintTicks(true);
		sliderThresholdBlob.setToolTipText("");
		sliderThresholdBlob.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				sliderThresholdBlobStateChanged(evt);
			}
		});

		labelThresholdBlob.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		labelThresholdBlob.setText("Threshold");

		checkInvertThreshBlob.setText("Invert");
		checkInvertThreshBlob.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		checkInvertThreshBlob.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		checkInvertThreshBlob.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkInvertThreshBlobActionPerformed(evt);
			}
		});

		checkEnableBlobs.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		checkEnableBlobs.setText("Enable Blob Tracking");
		checkEnableBlobs.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkEnableBlobsActionPerformed(evt);
			}
		});

		morphOpsComboBoxBlob.setModel(new DefaultComboBoxModel<>(MorphOps.values()));
		morphOpsComboBoxBlob.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				morphOpsComboBoxBlobActionPerformed(evt);
			}
		});

		tabBGRemoval = new JPanel();
		pipelineTabs.addTab("BG Removal", null, tabBGRemoval, null);

		morphOpsLabelBlob.setText("Morphological Ops.");

		javax.swing.GroupLayout tabBlobDetectLayout = new javax.swing.GroupLayout(tabBlobDetect);
		tabBlobDetectLayout.setHorizontalGroup(
				tabBlobDetectLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(tabBlobDetectLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(tabBlobDetectLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(tabBlobDetectLayout.createSequentialGroup()
										.addGroup(tabBlobDetectLayout.createParallelGroup(Alignment.LEADING)
												.addGroup(tabBlobDetectLayout.createSequentialGroup()
														.addGroup(tabBlobDetectLayout.createParallelGroup(Alignment.LEADING)
																.addComponent(labelThresholdBlob)
																.addComponent(checkInvertThreshBlob))
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(sliderThresholdBlob, GroupLayout.DEFAULT_SIZE, 509, Short.MAX_VALUE)
														.addPreferredGap(ComponentPlacement.RELATED))
												.addComponent(checkEnableBlobs, GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE))
										.addGap(12))
								.addGroup(tabBlobDetectLayout.createSequentialGroup()
										.addComponent(morphOpsLabelBlob, GroupLayout.PREFERRED_SIZE, 158, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(morphOpsComboBoxBlob, GroupLayout.PREFERRED_SIZE, 154, GroupLayout.PREFERRED_SIZE)
										.addContainerGap(280, Short.MAX_VALUE))))
				);
		tabBlobDetectLayout.setVerticalGroup(
				tabBlobDetectLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(tabBlobDetectLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(checkEnableBlobs)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(tabBlobDetectLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(tabBlobDetectLayout.createSequentialGroup()
										.addComponent(labelThresholdBlob)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(checkInvertThreshBlob)
										.addGap(12))
								.addGroup(tabBlobDetectLayout.createSequentialGroup()
										.addComponent(sliderThresholdBlob, GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED)))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(tabBlobDetectLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(morphOpsLabelBlob)
								.addComponent(morphOpsComboBoxBlob, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGap(172))
				);
		tabBlobDetect.setLayout(tabBlobDetectLayout);

		pipelineTabs.addTab("Blob detect", tabBlobDetect);

		camPreviewPanel.setPreferredSize(new java.awt.Dimension(600, 480));

		javax.swing.GroupLayout camPreviewPanelLayout = new javax.swing.GroupLayout(camPreviewPanel);
		camPreviewPanel.setLayout(camPreviewPanelLayout);
		camPreviewPanelLayout.setHorizontalGroup(
				camPreviewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGap(0, 0, Short.MAX_VALUE)
				);
		camPreviewPanelLayout.setVerticalGroup(
				camPreviewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGap(0, 0, Short.MAX_VALUE)
				);

		buttonSave.setText("Save configuration");
		buttonSave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonSaveActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(camPreviewPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 662, Short.MAX_VALUE)
								.addComponent(pipelineTabs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 662, Short.MAX_VALUE)
								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
										.addGap(0, 0, Short.MAX_VALUE)
										.addComponent(buttonSave)))
						.addContainerGap())
				);
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
						.addContainerGap()
						.addComponent(camPreviewPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
						.addComponent(pipelineTabs, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(buttonSave)
						.addContainerGap())
				);
		tabTripFiducials = new javax.swing.JPanel();
		checkEnableFiducials = new javax.swing.JCheckBox();
		checkInvertThreshTrip = new javax.swing.JCheckBox();
		checkAdaptiveThreshTrip = new javax.swing.JCheckBox();
		sliderThresholdTrip = new javax.swing.JSlider();
		morphOpsLabelTrip = new javax.swing.JLabel();
		morphOpsComboBoxTrip = new javax.swing.JComboBox();
		checkTripPreThreshold = new javax.swing.JCheckBox();

		checkEnableFiducials.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		checkEnableFiducials.setText("Enable Fiducial Tracking (Tripcode-variant)");
		checkEnableFiducials.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkEnableFiducialsActionPerformed(evt);
			}
		});

		checkInvertThreshTrip.setText("Invert");
		checkInvertThreshTrip.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		checkInvertThreshTrip.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		checkInvertThreshTrip.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkInvertThreshTripActionPerformed(evt);
			}
		});

		checkAdaptiveThreshTrip.setText("Adaptive");
		checkAdaptiveThreshTrip.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		checkAdaptiveThreshTrip.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		checkAdaptiveThreshTrip.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkAdaptiveThreshTripActionPerformed(evt);
			}
		});

		sliderThresholdTrip.setFont(new java.awt.Font("Sans Serif", 0, 8)); // NOI18N
		sliderThresholdTrip.setMajorTickSpacing(8);
		sliderThresholdTrip.setMaximum(254);
		sliderThresholdTrip.setMinimum(1);
		sliderThresholdTrip.setMinorTickSpacing(4);
		sliderThresholdTrip.setPaintTicks(true);
		sliderThresholdTrip.setToolTipText("");
		sliderThresholdTrip.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				sliderThresholdTripStateChanged(evt);
			}
		});

		morphOpsLabelTrip.setText("Morphological Ops.");

		morphOpsComboBoxTrip.setModel(new DefaultComboBoxModel<>(MorphOps.values()));
		morphOpsComboBoxTrip.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				morphOpsComboBoxTripActionPerformed(evt);
			}
		});

		checkTripPreThreshold.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		checkTripPreThreshold.setText("Pre Detection Thresholding");
		checkTripPreThreshold.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		checkTripPreThreshold.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		checkTripPreThreshold.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkTripPreThresholdActionPerformed(evt);
			}
		});

		JPanel panel = new JPanel();
		labelCertaintyTrip = new javax.swing.JLabel();

		labelCertaintyTrip.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		labelCertaintyTrip.setText("Hough circle detection parameters");
		
		lblFound = new JLabel();

		javax.swing.GroupLayout tabTripFiducialsLayout = new javax.swing.GroupLayout(tabTripFiducials);
		tabTripFiducialsLayout.setHorizontalGroup(
			tabTripFiducialsLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(tabTripFiducialsLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(tabTripFiducialsLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(labelCertaintyTrip, GroupLayout.PREFERRED_SIZE, 271, GroupLayout.PREFERRED_SIZE)
						.addGroup(tabTripFiducialsLayout.createSequentialGroup()
							.addComponent(checkEnableFiducials)
							.addPreferredGap(ComponentPlacement.RELATED, 140, Short.MAX_VALUE)
							.addComponent(lblFound))
						.addGroup(tabTripFiducialsLayout.createSequentialGroup()
							.addGroup(tabTripFiducialsLayout.createParallelGroup(Alignment.TRAILING)
								.addGroup(tabTripFiducialsLayout.createSequentialGroup()
									.addComponent(checkInvertThreshTrip)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(checkAdaptiveThreshTrip))
								.addComponent(checkTripPreThreshold))
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(sliderThresholdTrip, GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE))
						.addGroup(tabTripFiducialsLayout.createSequentialGroup()
							.addComponent(morphOpsLabelTrip)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(morphOpsComboBoxTrip, GroupLayout.PREFERRED_SIZE, 154, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		tabTripFiducialsLayout.setVerticalGroup(
			tabTripFiducialsLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(tabTripFiducialsLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(tabTripFiducialsLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(checkEnableFiducials)
						.addComponent(lblFound))
					.addGap(4)
					.addGroup(tabTripFiducialsLayout.createParallelGroup(Alignment.LEADING, false)
						.addGroup(tabTripFiducialsLayout.createSequentialGroup()
							.addComponent(checkTripPreThreshold, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(tabTripFiducialsLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(checkAdaptiveThreshTrip, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
								.addComponent(checkInvertThreshTrip, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)))
						.addComponent(sliderThresholdTrip, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addGap(18)
					.addGroup(tabTripFiducialsLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(morphOpsLabelTrip)
						.addComponent(morphOpsComboBoxTrip, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(labelCertaintyTrip, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(60, Short.MAX_VALUE))
		);

		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] {0, 80, 132, 80};
		gbl_panel.rowHeights = new int[] {0, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, 0.0, 1.0, 0.0};
		gbl_panel.rowWeights = new double[]{1.0, 0.0, 0.0};
		panel.setLayout(gbl_panel);
		labelTripInvResRat = new javax.swing.JLabel();
		labelTripInvResRat.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_labelTripInvResRat = new GridBagConstraints();
		gbc_labelTripInvResRat.anchor = GridBagConstraints.EAST;
		gbc_labelTripInvResRat.insets = new Insets(0, 0, 5, 5);
		gbc_labelTripInvResRat.fill = GridBagConstraints.VERTICAL;
		gbc_labelTripInvResRat.gridx = 0;
		gbc_labelTripInvResRat.gridy = 0;
		panel.add(labelTripInvResRat, gbc_labelTripInvResRat);

		labelTripInvResRat.setText("Inverse resolution ratio");
		labelTripInvResRat.setToolTipText("This parameter is the inverse ratio of the accumulator resolution to the image resolution (see Yuen et al. for more details). Essentially, the larger the dp gets, the smaller the accumulator array gets.");
		spinnerTripInvResRat = new javax.swing.JSpinner();
		GridBagConstraints gbc_spinnerTripInvResRat = new GridBagConstraints();
		gbc_spinnerTripInvResRat.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerTripInvResRat.fill = GridBagConstraints.BOTH;
		gbc_spinnerTripInvResRat.gridx = 1;
		gbc_spinnerTripInvResRat.gridy = 0;
		panel.add(spinnerTripInvResRat, gbc_spinnerTripInvResRat);

		spinnerTripInvResRat.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.1d, 256.0d, 0.1d));
		spinnerTripInvResRat.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				spinnerTripInvResRatStateChanged(evt);
			}
		});
		labelTripCenterDetectThresh = new javax.swing.JLabel();
		labelTripCenterDetectThresh.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_labelTripCenterDetectThresh = new GridBagConstraints();
		gbc_labelTripCenterDetectThresh.anchor = GridBagConstraints.EAST;
		gbc_labelTripCenterDetectThresh.insets = new Insets(0, 0, 5, 5);
		gbc_labelTripCenterDetectThresh.fill = GridBagConstraints.VERTICAL;
		gbc_labelTripCenterDetectThresh.gridx = 2;
		gbc_labelTripCenterDetectThresh.gridy = 0;
		panel.add(labelTripCenterDetectThresh, gbc_labelTripCenterDetectThresh);

		labelTripCenterDetectThresh.setText("Center detection threshold");
		labelTripCenterDetectThresh.setToolTipText("Accumulator threshold for the circle centers at the detection stage. The smaller it is, the more false circles may be detected. Circles, corresponding to the larger accumulator values, will be returned first.");
		spinnerTripCenterThresh = new javax.swing.JSpinner();
		GridBagConstraints gbc_spinnerTripCenterThresh = new GridBagConstraints();
		gbc_spinnerTripCenterThresh.insets = new Insets(0, 0, 5, 0);
		gbc_spinnerTripCenterThresh.fill = GridBagConstraints.BOTH;
		gbc_spinnerTripCenterThresh.gridx = 3;
		gbc_spinnerTripCenterThresh.gridy = 0;
		panel.add(spinnerTripCenterThresh, gbc_spinnerTripCenterThresh);

		spinnerTripCenterThresh.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(80), Integer.valueOf(1), null, Integer.valueOf(1)));
		spinnerTripCenterThresh.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				spinnerTripCenterThreshStateChanged(evt);
			}
		});
		labelTripMinCenterDist = new javax.swing.JLabel();
		labelTripMinCenterDist.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_labelTripMinCenterDist = new GridBagConstraints();
		gbc_labelTripMinCenterDist.anchor = GridBagConstraints.EAST;
		gbc_labelTripMinCenterDist.insets = new Insets(0, 0, 5, 5);
		gbc_labelTripMinCenterDist.fill = GridBagConstraints.VERTICAL;
		gbc_labelTripMinCenterDist.gridx = 0;
		gbc_labelTripMinCenterDist.gridy = 1;
		panel.add(labelTripMinCenterDist, gbc_labelTripMinCenterDist);

		labelTripMinCenterDist.setText("Min center distance");
		labelTripMinCenterDist.setToolTipText("Minimum distance between the center (x, y) coordinates of detected circles. If the minDist is too small, multiple circles in the same neighborhood as the original may be (falsely) detected. If the minDist is too large, then some circles may not be detected at all.");
		spinnerTripCenterDist = new javax.swing.JSpinner();
		GridBagConstraints gbc_spinnerTripCenterDist = new GridBagConstraints();
		gbc_spinnerTripCenterDist.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerTripCenterDist.fill = GridBagConstraints.BOTH;
		gbc_spinnerTripCenterDist.gridx = 1;
		gbc_spinnerTripCenterDist.gridy = 1;
		panel.add(spinnerTripCenterDist, gbc_spinnerTripCenterDist);

		spinnerTripCenterDist.setModel(new javax.swing.SpinnerNumberModel(20, 1, 1024, 1));
		spinnerTripCenterDist.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				spinnerTripCenterDistStateChanged(evt);
			}
		});
		labelTripCannyThresh = new javax.swing.JLabel();
		labelTripCannyThresh.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_labelTripCannyThresh = new GridBagConstraints();
		gbc_labelTripCannyThresh.anchor = GridBagConstraints.EAST;
		gbc_labelTripCannyThresh.insets = new Insets(0, 0, 5, 5);
		gbc_labelTripCannyThresh.fill = GridBagConstraints.VERTICAL;
		gbc_labelTripCannyThresh.gridx = 2;
		gbc_labelTripCannyThresh.gridy = 1;
		panel.add(labelTripCannyThresh, gbc_labelTripCannyThresh);

		labelTripCannyThresh.setText("Canny threshold");
		labelTripCannyThresh.setToolTipText("Higher threshold of the two passed to the Canny() edge detector (the lower one is twice smaller).");
		spinnerCannyThresh = new javax.swing.JSpinner();
		GridBagConstraints gbc_spinnerCannyThresh = new GridBagConstraints();
		gbc_spinnerCannyThresh.insets = new Insets(0, 0, 5, 0);
		gbc_spinnerCannyThresh.fill = GridBagConstraints.BOTH;
		gbc_spinnerCannyThresh.gridx = 3;
		gbc_spinnerCannyThresh.gridy = 1;
		panel.add(spinnerCannyThresh, gbc_spinnerCannyThresh);

		spinnerCannyThresh.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(200), Integer.valueOf(1), null, Integer.valueOf(1)));
		spinnerCannyThresh.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				spinnerCannyThreshStateChanged(evt);
			}
		});
		labelTripMinRad = new javax.swing.JLabel();
		labelTripMinRad.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_labelTripMinRad = new GridBagConstraints();
		gbc_labelTripMinRad.anchor = GridBagConstraints.EAST;
		gbc_labelTripMinRad.insets = new Insets(0, 0, 0, 5);
		gbc_labelTripMinRad.fill = GridBagConstraints.VERTICAL;
		gbc_labelTripMinRad.gridx = 0;
		gbc_labelTripMinRad.gridy = 2;
		panel.add(labelTripMinRad, gbc_labelTripMinRad);

		labelTripMinRad.setText("Min Radius");
		spinnerTripMinRad = new javax.swing.JSpinner();
		GridBagConstraints gbc_spinnerTripMinRad = new GridBagConstraints();
		gbc_spinnerTripMinRad.fill = GridBagConstraints.BOTH;
		gbc_spinnerTripMinRad.insets = new Insets(0, 0, 0, 5);
		gbc_spinnerTripMinRad.gridx = 1;
		gbc_spinnerTripMinRad.gridy = 2;
		panel.add(spinnerTripMinRad, gbc_spinnerTripMinRad);

		spinnerTripMinRad.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(0), null, Integer.valueOf(1)));
		spinnerTripMinRad.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				spinnerTripMinRadStateChanged(evt);
			}
		});
		labelTripMaxRad = new javax.swing.JLabel();
		labelTripMaxRad.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_labelTripMaxRad = new GridBagConstraints();
		gbc_labelTripMaxRad.anchor = GridBagConstraints.EAST;
		gbc_labelTripMaxRad.insets = new Insets(0, 0, 0, 5);
		gbc_labelTripMaxRad.fill = GridBagConstraints.VERTICAL;
		gbc_labelTripMaxRad.gridx = 2;
		gbc_labelTripMaxRad.gridy = 2;
		panel.add(labelTripMaxRad, gbc_labelTripMaxRad);

		labelTripMaxRad.setText("Max Radius");
		spinnerTripMaxRad = new javax.swing.JSpinner();
		GridBagConstraints gbc_spinnerTripMaxRad = new GridBagConstraints();
		gbc_spinnerTripMaxRad.fill = GridBagConstraints.BOTH;
		gbc_spinnerTripMaxRad.gridx = 3;
		gbc_spinnerTripMaxRad.gridy = 2;
		panel.add(spinnerTripMaxRad, gbc_spinnerTripMaxRad);

		spinnerTripMaxRad.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(60), Integer.valueOf(0), null, Integer.valueOf(1)));
		spinnerTripMaxRad.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				spinnerTripMaxRadStateChanged(evt);
			}
		});
		tabTripFiducials.setLayout(tabTripFiducialsLayout);

		pipelineTabs.addTab("Fiducials", tabTripFiducials);
		tabCalibration = new javax.swing.JPanel();
		jSpinner2 = new javax.swing.JSpinner();
		labelThresholdTrip1 = new javax.swing.JLabel();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jSpinner3 = new javax.swing.JSpinner();

		labelThresholdTrip1.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
		labelThresholdTrip1.setText("Amount of calibration points");

		jLabel1.setText("Horizontal");

		jLabel2.setText("Vertical");

		javax.swing.GroupLayout tabCalibrationLayout = new javax.swing.GroupLayout(tabCalibration);
		tabCalibration.setLayout(tabCalibrationLayout);
		tabCalibrationLayout.setHorizontalGroup(
				tabCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(tabCalibrationLayout.createSequentialGroup()
						.addGap(24, 24, 24)
						.addComponent(jLabel1)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jLabel2)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap(282, Short.MAX_VALUE))
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabCalibrationLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(labelThresholdTrip1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addContainerGap())
				);
		tabCalibrationLayout.setVerticalGroup(
				tabCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(tabCalibrationLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(labelThresholdTrip1)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(tabCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel1)
								.addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jLabel2))
						.addContainerGap(231, Short.MAX_VALUE))
				);

		pipelineTabs.addTab("Calibration", tabCalibration);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void pipelineTabsStateChanged(ChangeEvent evt) {//GEN-FIRST:event_pipelineTabsStateChanged
		selectedView = View.values()[((JTabbedPane)evt.getSource()).getSelectedIndex()];
	}//GEN-LAST:event_pipelineTabsStateChanged

	private void checkEnableBlobsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkEnableBlobsActionPerformed
		params.blobTracking = ((JCheckBox)evt.getSource()).isSelected();
	}//GEN-LAST:event_checkEnableBlobsActionPerformed

	private void checkInvertThreshBlobActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkInvertThreshBlobActionPerformed
		params.blobThInverted = ((JCheckBox)evt.getSource()).isSelected();
	}//GEN-LAST:event_checkInvertThreshBlobActionPerformed

	private void chckbxEnableUndistortActionPerformed(ActionEvent evt) {//GEN-FIRST:event_chckbxEnableUndistortActionPerformed
		params.enableDistortion = ((JCheckBox)evt.getSource()).isSelected();
	}//GEN-LAST:event_chckbxEnableUndistortActionPerformed

	private void sliderThresholdBlobStateChanged(ChangeEvent evt) {//GEN-FIRST:event_sliderThresholdBlobStateChanged
		params.blobThreshold = 1+((JSlider)(evt.getSource())).getValue()*2.54f;
	}//GEN-LAST:event_sliderThresholdBlobStateChanged

	private void morphOpsComboBoxBlobActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_morphOpsComboBoxBlobActionPerformed
		params.blobMorphOps = MorphOps.values()[((JComboBox<MorphOps>)evt.getSource()).getSelectedIndex()];
	}//GEN-LAST:event_morphOpsComboBoxBlobActionPerformed

	//	private void buttonRemoveBackgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveBackgActionPerformed
	//		recaptureBg = true;
	//	}//GEN-LAST:event_buttonRemoveBackgActionPerformed

	private void sliderBrightnessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderBrightnessStateChanged
		params.brightness = ((JSlider)(evt.getSource())).getValue()*5.12f - 255;
	}//GEN-LAST:event_sliderBrightnessStateChanged

	private void sliderContrastStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderContrastStateChanged
		params.contrast = ((JSlider)(evt.getSource())).getValue()*0.04f + 0.5f;
	}//GEN-LAST:event_sliderContrastStateChanged

	//	private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
	//		dynamicBGRemoval = checkBGRemovalAccumulative.isSelected();
	//	}//GEN-LAST:event_jCheckBox1ActionPerformed

	private void sliderBlurStateChanged(ChangeEvent evt) {//GEN-FIRST:event_sliderBlurBlobStateChanged
		params.blurAmount = ((JSlider)(evt.getSource())).getValue()*0.01f;
	}//GEN-LAST:event_sliderBlurBlobStateChanged

	private void blurComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blurComboBoxBlobActionPerformed
		int i=blurComboBox.getSelectedIndex();
		params.blur = Blur.values()[i];
		if(i < 1){
			sliderBlur.setEnabled(false);
		} else {
			sliderBlur.setEnabled(true);
		}
	}//GEN-LAST:event_blurComboBoxBlobActionPerformed

	private void morphOpsComboBoxTripActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_morphOpsComboBoxTripActionPerformed
		params.tripMorphOps = MorphOps.values()[((JComboBox<MorphOps>)evt.getSource()).getSelectedIndex()];
	}//GEN-LAST:event_morphOpsComboBoxTripActionPerformed

	private void sliderThresholdTripStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderThresholdTripStateChanged
		params.tripThreshold = sliderThresholdTrip.getValue();
	}//GEN-LAST:event_sliderThresholdTripStateChanged

	private void checkAdaptiveThreshTripActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkAdaptiveThreshTripActionPerformed
		params.tripAdaptThreshold = ((JCheckBox)evt.getSource()).isSelected();
	}//GEN-LAST:event_checkAdaptiveThreshTripActionPerformed

	private void checkInvertThreshTripActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkInvertThreshTripActionPerformed
		params.tripThInverted = ((JCheckBox)evt.getSource()).isSelected();
	}//GEN-LAST:event_checkInvertThreshTripActionPerformed

	private void checkEnableFiducialsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkEnableFiducialsActionPerformed
		params.tripTracking = ((JCheckBox)evt.getSource()).isSelected();
	}//GEN-LAST:event_checkEnableFiducialsActionPerformed

	private void spinnerTripCenterThreshStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTripCenterThreshStateChanged
		params.tripAccumThresh = (int)spinnerTripCenterThresh.getValue();
	}//GEN-LAST:event_spinnerTripCenterThreshStateChanged

	private void spinnerTripInvResRatStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTripInvResRatStateChanged
		params.tripDP = (double) spinnerTripInvResRat.getValue();
	}//GEN-LAST:event_spinnerTripInvResRatStateChanged

	private void spinnerTripMaxRadStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTripMaxRadStateChanged
		params.tripRadMax = (int) spinnerTripMaxRad.getValue();
	}//GEN-LAST:event_spinnerTripMaxRadStateChanged

	private void spinnerTripCenterDistStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTripCenterDistStateChanged
		params.tripCenterDist = (int) spinnerTripCenterDist.getValue();
	}//GEN-LAST:event_spinnerTripCenterDistStateChanged

	private void spinnerTripMinRadStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTripMinRadStateChanged
		params.tripRadMin = (int) spinnerTripMinRad.getValue();
	}//GEN-LAST:event_spinnerTripMinRadStateChanged

	private void spinnerCannyThreshStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerCannyThreshStateChanged
		params.tripCannyThresh = (int)spinnerCannyThresh.getValue();
	}//GEN-LAST:event_spinnerCannyThreshStateChanged

	private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
		saveParams();
	}//GEN-LAST:event_buttonSaveActionPerformed

	private void checkTripPreThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkTripPreThresholdActionPerformed
		params.tripEnableThresholding = ((JCheckBox)evt.getSource()).isSelected();
	}//GEN-LAST:event_checkTripPreThresholdActionPerformed

	private void checkEnableBlobs1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkEnableBlobs1ActionPerformed
		// TODO add your handling code here:
	}//GEN-LAST:event_checkEnableBlobs1ActionPerformed

	private void slider_k1StateChanged(ChangeEvent e) {//GEN-FIRST:event_slider_k1StateChanged
		params.k1 = ((JSlider)(e.getSource())).getValue();
		updateDistortionValues();
	}//GEN-LAST:event_slider_k1StateChanged	

	private void slider_k2StateChanged(ChangeEvent e) {//GEN-FIRST:event_slider_k2StateChanged
		params.k2 = ((JSlider)(e.getSource())).getValue();
		updateDistortionValues();
	}//GEN-LAST:event_slider_k2StateChanged

	private void slider_k3StateChanged(ChangeEvent e) {//GEN-FIRST:event_slider_k3StateChanged
		params.k3 = ((JSlider)(e.getSource())).getValue();
		updateDistortionValues();
	}//GEN-LAST:event_slider_k3StateChanged	

	private void slider_p1StateChanged(ChangeEvent e) {//GEN-FIRST:event_slider_p1StateChanged
		params.p1 = ((JSlider)(e.getSource())).getValue();
		updateDistortionValues();
	}//GEN-LAST:event_slider_p1StateChanged

	private void slider_p2StateChanged(ChangeEvent e) {//GEN-FIRST:event_slider_p2StateChanged
		params.p2 = ((JSlider)(e.getSource())).getValue();
		updateDistortionValues();
	}//GEN-LAST:event_slider_p2StateChanged	

	private void slider_zStateChanged(ChangeEvent e) {//GEN-FIRST:event_slider_zStateChanged
		params.z = ((JSlider)(e.getSource())).getValue();
		float z = 0.25f+params.z*0.001f;
		double a = Math.cos(0)*z;
		double b = Math.sin(0)*z;

		float xShift = 0;
		float yShift = 0;

		shiftMat.put(0, 0, new double[] {a,b, (1-a)*(frameSize.width*0.5f-xShift*0.5f)-b*(frameSize.height*0.5f-yShift*0.5f),
				-b,a, b*(frameSize.width*0.5f-xShift*0.5f) + (1-a)*(frameSize.height*0.5f-yShift*0.5f)});

	}//GEN-LAST:event_slider_zStateChanged

	private void updateDistortionValues(){
		distCoeffs.put(0,0, new float[]{params.k1*0.00000001f, params.k2*0.0000000000001f, params.p1*0.000001f, params.p2*0.000001f, params.k3*0.0000000000000000001f});
	}
}
