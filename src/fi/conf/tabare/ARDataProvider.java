/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.conf.tabare;

import java.awt.Color;
import java.awt.Graphics2D;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorMOG2;

import fi.conf.tabare.TrackableObject.ItemType;

/**
 *
 * @author irah
 */
public class ARDataProvider extends JFrame {
	
	private static final String PARAMS_FILE = "params.ser";
	private static final int MAX_VIDEO_DEVICE_INDEX = 5;
	
	static {
		//System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
		nu.pattern.OpenCV.loadShared();
		//nu.pattern.OpenCV.loadLocally();
		//System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
	}
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox blurComboBox;
    private javax.swing.JButton buttonRemoveBackg;
    private javax.swing.JButton buttonSave;
    private fi.conf.tabare.PreviewPanel camPreviewPanel;
    private javax.swing.JCheckBox checkAdaptiveThreshTrip;
    private javax.swing.JCheckBox checkEnableBlobs;
    private javax.swing.JCheckBox checkEnableBlobs1;
    private javax.swing.JCheckBox checkEnableFiducials;
    private javax.swing.JCheckBox checkInvertThreshBlob;
    private javax.swing.JCheckBox checkInvertThreshTrip;
    private javax.swing.JCheckBox checkTripPreThreshold;
    private javax.swing.JCheckBox jCheckBox1;
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
    private javax.swing.JSlider sliderContrast;
    private javax.swing.JSlider sliderContrast1;
    private javax.swing.JSlider sliderThresholdBlob;
    private javax.swing.JSlider sliderThresholdTrip;
    private javax.swing.JSpinner spinnerCannyThresh;
    private javax.swing.JSpinner spinnerTripCenterDist;
    private javax.swing.JSpinner spinnerTripCenterThresh;
    private javax.swing.JSpinner spinnerTripInvResRat;
    private javax.swing.JSpinner spinnerTripMaxRad;
    private javax.swing.JSpinner spinnerTripMinRad;
    private javax.swing.JPanel tabBGRemoval;
    private javax.swing.JPanel tabBlobDetect;
    private javax.swing.JPanel tabCalibration;
    private javax.swing.JPanel tabImgInput;
    private javax.swing.JPanel tabTripFiducials;
    // End of variables declaration//GEN-END:variables
	
    private Params params;
    
	private VideoCapture inputVideo;
	private int videoWidth = 0;
	private int videoHeight = 0;
	
	private BackgroundSubtractor backgSubstractor = new BackgroundSubtractorMOG2(100, 0.2f, false);
	private boolean dynamicBGRemoval = false;
	private float dynamicBGAmount = 1.0f;
	private Mat background;
	private boolean recaptureBg = true;
	
	private boolean blobTracking;
	private boolean tripTracking;
	
	private boolean running = true;
	private final Thread detectionThread;
	private final Thread trackingThread;
	
	public enum View {raw, bgrm, blob, trip, calib} //The tabs should be in the same order.
	public enum MorphOps {none, erode, dilate}
	public enum Blur {none, normal_3x3, normal_5x5, normal_7x7, gaussian, median}
	
	private long lastFrameDetectTime = 0;
	private long lastFrameTrackTime = 0;
	private float detectTime = 0;
	private float trackTime = 0;
	
	private FeatureDetector blobFeatureDetector;
	private int lastBlobIndex = 0;
	private ConcurrentLinkedQueue<MatOfKeyPoint> blobData;
	private ConcurrentLinkedQueue<TrackableBlob> trackedBlobs;
	
	private ConcurrentLinkedQueue<TrackableTripcode> unprocessedTripcodes;
	private ConcurrentHashMap<Integer,TrackableTripcode> trackedTripcodes;
	
	private View selectedView = View.raw;
	
	private Calibrator calibrator;
	
    /**
     * Creates new form CamImageSettings
     */
    public ARDataProvider() {
    	
    	initComponents();
    	
    	blobData = new ConcurrentLinkedQueue<>();
    	unprocessedTripcodes = new ConcurrentLinkedQueue<>();
    	trackedTripcodes = new ConcurrentHashMap<>();
    	trackedBlobs = new ConcurrentLinkedQueue<>();

		int videoInID = MAX_VIDEO_DEVICE_INDEX/2;
		
		//Init
		while(inputVideo == null || videoHeight <= 0 || videoWidth <= 0){
			
			if(inputVideo != null) inputVideo.release();
			inputVideo = null;
			videoInID--;
			if(videoInID < 0) videoInID = MAX_VIDEO_DEVICE_INDEX;
			
			try {
				inputVideo = new VideoCapture();
				System.out.println("Trying input " + videoInID);
				inputVideo.open(videoInID);
				Thread.sleep(1000);
				//inputVideo.set(OpenCVUtils.CAP_PROP_FRAME_WIDTH, 640);
				//inputVideo.set(OpenCVUtils.CAP_PROP_FRAME_HEIGHT, 480);
				
				videoHeight = (int)inputVideo.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT);
				videoWidth = (int)inputVideo.get(Highgui.CV_CAP_PROP_FRAME_WIDTH);
				
				System.out.println("Video size: " + videoWidth + "x" + videoHeight);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			} catch (Error e) {
				e.printStackTrace();
				continue;
			}
		}

		camPreviewPanel.initialize(videoWidth, videoHeight);
		calibrator = new Calibrator(2, 2, videoWidth, videoHeight);
		//TODO: Load calibration values from storage!
		
		camPreviewPanel.addMouseListener(calibrator);
		camPreviewPanel.addMouseMotionListener(calibrator);
		
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
			e.printStackTrace();
			params = new Params();
		}
		
		//Calibration
		calibrator.setRawCalibrationData(params.calibrationData);
		
		//Input filters
		sliderContrast.setValue((int)(params.contrast/0.03f));
		sliderBrightness.setValue((int)((params.brightness+255)/5.12f ));
		blurComboBox.setSelectedIndex(params.blur.ordinal());
	 
		//Blob
		sliderThresholdBlob.setValue((int)((params.blobThreshold-1)/2.54f));
		checkInvertThreshTrip.setSelected(params.tripThInverted);
		
		//Trip
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
		
		Mat input_image = new Mat();
    	Mat circles = new Mat();
    	MatOfKeyPoint mokp = new MatOfKeyPoint();
    	
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

//				Imgproc.equalizeHist(input_image, input_image);
				
				input_image.convertTo(input_image, -1, params.contrast, params.brightness); //image*contrast[1.0-3.0] + brightness[0-255]
				
				doBlur(input_image, input_image, params.blur, params.blurAmount);
				
				if(selectedView == View.raw) preview_image = input_image.clone();
				
//				if(background == null) background = input_image.clone();			
//				if(recaptureBg){
//					backgSubstractor.apply(background, background);
//					System.out.println(background.channels() + " " + background.size() );
//					System.out.println(input_image.channels() + " " + input_image.size() );
//					recaptureBg = false;
//				}
//				if(dynamicBGRemoval){
//					//Imgproc.accumulateWeighted(input_image, background, dynamicBGAmount);
//					//Imgproc.accumulateWeighted(input_image, background, 1.0f);
//					//Core.subtract(input_image, background, input_image);
//					//Core.bitwise_xor(input_image, background, input_image);
//					
//					doBlur(input_image, background, Blur.normal_7x7, 0); //Blur a little, to get nicer result when substracting
//					backgSubstractor.apply(background, background, dynamicBGAmount);
//				}
//				if(background != null) Core.add(input_image, background, input_image);
				
				if(blobTracking){
					Mat blobs_image = input_image.clone();
					
					Imgproc.threshold(blobs_image, blobs_image, params.blobThreshold, 254, (params.blobThInverted ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY));
					
					switch(params.blobMorphOps){
						case dilate:
							Imgproc.dilate(blobs_image, blobs_image, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5)));
							break;
						case erode:
							Imgproc.erode(blobs_image, blobs_image, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5)));
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
				
				if(tripTracking){
					Mat trips_image = input_image.clone();
		
//					if(params.tripAdaptThreshold){
//						Imgproc.adaptiveThreshold(input_image, trips_image, 254, (params.tripThInverted ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY), Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 5, params.tripThreshold);
//					} else {
//						Imgproc.threshold(input_image, trips_image, params.tripThreshold, 254, (params.tripThInverted ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY));	
//					}
					
					switch(params.tripMorphOps){
					case dilate:
						Imgproc.dilate(trips_image, trips_image, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5)));
						break;
					case erode:
						Imgproc.erode(trips_image, trips_image, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5)));
						break;
					default:
						break;
					}
					
					Imgproc.HoughCircles(trips_image, circles, Imgproc.CV_HOUGH_GRADIENT, params.tripDP, params.tripCenterDist, params.tripCannyThresh, params.tripAccumThresh, params.tripRadMin, params.tripRadMax);
					
					for (int i = 0; i < circles.cols(); i++){
						double[] coords = circles.get(0,i);
						if(coords == null || coords[0] <= 1 && coords[1] <= 1) continue; //TODO: Wat? Why? I don't remember. :D
						unprocessedTripcodes.add(new TrackableTripcode(input_image, coords));
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
		input_image.release();
		inputVideo.release();
		
	}
	
	private void track(){
		while(running){
		
			//Process blobs
			while(blobTracking && !blobData.isEmpty()){

				MatOfKeyPoint r = blobData.poll();
				for(int i=0; i < r.rows(); i++){
					double[] c = r.get(i, 0);
					TrackableBlob blob = null;
					for(TrackableBlob cb : trackedBlobs){
						if(cb.isCloseTo(c[0], c[1])){
							blob = cb;
							cb.update(c[0], c[1], c[2], 1);
							break;
						}
					}
					if(blob == null){
						blob = new TrackableBlob(c[0], c[1], ++lastBlobIndex);
						trackedBlobs.add(blob);
					}
					calibrator.applyCalibration(blob);
				}
				r.release();
				
			}
			
			//Process tripcodes
			while(tripTracking && !unprocessedTripcodes.isEmpty()){
				TrackableTripcode t = unprocessedTripcodes.poll();
				t.crunch();
				
				int id = t.getID();
				
				if(id < 0){
					for(TrackableTripcode tb : trackedTripcodes.values()){
						if(tb.isCloseTo(t.getFixedRawX(), t.getFixedRawY())){
							tb.update(t);
							break;
						}
					}
					continue;
				}
				
				if(trackedTripcodes.contains(id)){
					trackedTripcodes.get(id).update(t);
				} else {
					trackedTripcodes.put((int)t.getID(), t);
				}
				
			}
			
			//Visualization and cleanup
			Graphics2D g = camPreviewPanel.getOverlayGraphics();
			if(g == null) continue;
			
			calibrator.drawGrid(g);
			
			for(TrackableBlob blob : trackedBlobs){
				if(blob.isDead()){
					trackedBlobs.remove(blob);
					continue;
				}
				g.setColor(Color.MAGENTA);
				g.drawString("" + blob.getID(), (int)blob.getRawX(), (int)blob.getRawY());
				g.fillOval((int)blob.getRawX()-2, (int)blob.getRawY()-2, 4, 4);
			}
			
			for(TrackableTripcode trip : trackedTripcodes.values()){
				
				if(trip.isDead()){
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
		int ksize = ((int)(p*7))*2+1;
		switch (b) {
		case normal_3x3:
			Imgproc.blur(matIn, matOut, new Size(3,3));
			break;
		case normal_5x5:
			Imgproc.blur(matIn, matOut, new Size(5,5));
			break;			
		case normal_7x7:
			Imgproc.blur(matIn, matOut, new Size(7,7));
			break;
		case gaussian:
			Imgproc.GaussianBlur(matIn, matOut, new Size(ksize, ksize), 1+(p*14)/3f);
			break;
		case median:
			Imgproc.medianBlur(matIn, matOut, ksize);
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
	
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        pipelineTabs = new javax.swing.JTabbedPane();
        tabImgInput = new javax.swing.JPanel();
        sliderContrast = new javax.swing.JSlider();
        labelContrast = new javax.swing.JLabel();
        sliderBrightness = new javax.swing.JSlider();
        labelBrightness = new javax.swing.JLabel();
        labelBlur = new javax.swing.JLabel();
        blurComboBox = new javax.swing.JComboBox();
        sliderBlur = new javax.swing.JSlider();
        tabBGRemoval = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jSpinner1 = new javax.swing.JSpinner();
        sliderContrast1 = new javax.swing.JSlider();
        buttonRemoveBackg = new javax.swing.JButton();
        checkEnableBlobs1 = new javax.swing.JCheckBox();
        tabBlobDetect = new javax.swing.JPanel();
        sliderThresholdBlob = new javax.swing.JSlider();
        labelThresholdBlob = new javax.swing.JLabel();
        checkInvertThreshBlob = new javax.swing.JCheckBox();
        checkEnableBlobs = new javax.swing.JCheckBox();
        morphOpsComboBoxBlob = new javax.swing.JComboBox();
        morphOpsLabelBlob = new javax.swing.JLabel();
        tabTripFiducials = new javax.swing.JPanel();
        labelCertaintyTrip = new javax.swing.JLabel();
        checkEnableFiducials = new javax.swing.JCheckBox();
        checkInvertThreshTrip = new javax.swing.JCheckBox();
        checkAdaptiveThreshTrip = new javax.swing.JCheckBox();
        sliderThresholdTrip = new javax.swing.JSlider();
        morphOpsLabelTrip = new javax.swing.JLabel();
        morphOpsComboBoxTrip = new javax.swing.JComboBox();
        spinnerTripCenterThresh = new javax.swing.JSpinner();
        spinnerTripInvResRat = new javax.swing.JSpinner();
        spinnerTripMaxRad = new javax.swing.JSpinner();
        labelTripCannyThresh = new javax.swing.JLabel();
        labelTripCenterDetectThresh = new javax.swing.JLabel();
        spinnerTripCenterDist = new javax.swing.JSpinner();
        spinnerTripMinRad = new javax.swing.JSpinner();
        labelTripMinRad = new javax.swing.JLabel();
        labelTripMaxRad = new javax.swing.JLabel();
        labelTripMinCenterDist = new javax.swing.JLabel();
        labelTripInvResRat = new javax.swing.JLabel();
        spinnerCannyThresh = new javax.swing.JSpinner();
        checkTripPreThreshold = new javax.swing.JCheckBox();
        tabCalibration = new javax.swing.JPanel();
        jSpinner2 = new javax.swing.JSpinner();
        labelThresholdTrip1 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jSpinner3 = new javax.swing.JSpinner();
        camPreviewPanel = new fi.conf.tabare.PreviewPanel();
        buttonSave = new javax.swing.JButton();

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

        sliderContrast.setFont(new java.awt.Font("Sans Serif", 0, 8)); // NOI18N
        sliderContrast.setMajorTickSpacing(5);
        sliderContrast.setMinorTickSpacing(1);
        sliderContrast.setPaintLabels(true);
        sliderContrast.setPaintTicks(true);
        sliderContrast.setToolTipText("");
        sliderContrast.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderContrastStateChanged(evt);
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
        sliderBlur.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderBlurStateChanged(evt);
            }
        });

        javax.swing.GroupLayout tabImgInputLayout = new javax.swing.GroupLayout(tabImgInput);
        tabImgInput.setLayout(tabImgInputLayout);
        tabImgInputLayout.setHorizontalGroup(
            tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabImgInputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tabImgInputLayout.createSequentialGroup()
                        .addGroup(tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(labelContrast)
                            .addComponent(labelBrightness))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sliderBrightness, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sliderContrast, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabImgInputLayout.createSequentialGroup()
                        .addGroup(tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelBlur)
                            .addComponent(blurComboBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliderBlur, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        tabImgInputLayout.setVerticalGroup(
            tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabImgInputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sliderBrightness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelBrightness))
                .addGap(18, 18, 18)
                .addGroup(tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sliderContrast, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelContrast))
                .addGap(18, 18, 18)
                .addGroup(tabImgInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sliderBlur, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(tabImgInputLayout.createSequentialGroup()
                        .addComponent(labelBlur)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(blurComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pipelineTabs.addTab("Raw input", tabImgInput);

        jCheckBox1.setText("Accumulative");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        sliderContrast1.setFont(new java.awt.Font("Sans Serif", 0, 8)); // NOI18N
        sliderContrast1.setMajorTickSpacing(5);
        sliderContrast1.setMinorTickSpacing(1);
        sliderContrast1.setPaintLabels(true);
        sliderContrast1.setPaintTicks(true);
        sliderContrast1.setToolTipText("");
        sliderContrast1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderContrast1StateChanged(evt);
            }
        });

        buttonRemoveBackg.setText("Resample");
        buttonRemoveBackg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveBackgActionPerformed(evt);
            }
        });

        checkEnableBlobs1.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
        checkEnableBlobs1.setText("Enable background removal");
        checkEnableBlobs1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkEnableBlobs1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout tabBGRemovalLayout = new javax.swing.GroupLayout(tabBGRemoval);
        tabBGRemoval.setLayout(tabBGRemovalLayout);
        tabBGRemovalLayout.setHorizontalGroup(
            tabBGRemovalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabBGRemovalLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tabBGRemovalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tabBGRemovalLayout.createSequentialGroup()
                        .addComponent(checkEnableBlobs1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemoveBackg))
                    .addGroup(tabBGRemovalLayout.createSequentialGroup()
                        .addGroup(tabBGRemovalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jCheckBox1)
                            .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliderContrast1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        tabBGRemovalLayout.setVerticalGroup(
            tabBGRemovalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabBGRemovalLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tabBGRemovalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonRemoveBackg)
                    .addGroup(tabBGRemovalLayout.createSequentialGroup()
                        .addComponent(checkEnableBlobs1, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(tabBGRemovalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(tabBGRemovalLayout.createSequentialGroup()
                                .addComponent(jCheckBox1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(sliderContrast1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        //TODO: functionality
        pipelineTabs.addTab("BG Removal", tabBGRemoval);

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

        morphOpsLabelBlob.setText("Morphological Ops.");

        javax.swing.GroupLayout tabBlobDetectLayout = new javax.swing.GroupLayout(tabBlobDetect);
        tabBlobDetect.setLayout(tabBlobDetectLayout);
        tabBlobDetectLayout.setHorizontalGroup(
            tabBlobDetectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabBlobDetectLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tabBlobDetectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkEnableBlobs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabBlobDetectLayout.createSequentialGroup()
                        .addGroup(tabBlobDetectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelThresholdBlob)
                            .addComponent(checkInvertThreshBlob, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliderThresholdBlob, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(tabBlobDetectLayout.createSequentialGroup()
                        .addComponent(morphOpsLabelBlob, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(morphOpsComboBoxBlob, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        tabBlobDetectLayout.setVerticalGroup(
            tabBlobDetectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabBlobDetectLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkEnableBlobs, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(tabBlobDetectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sliderThresholdBlob, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(tabBlobDetectLayout.createSequentialGroup()
                        .addComponent(labelThresholdBlob)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkInvertThreshBlob)))
                .addGap(18, 18, 18)
                .addGroup(tabBlobDetectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(morphOpsLabelBlob)
                    .addComponent(morphOpsComboBoxBlob, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pipelineTabs.addTab("Blob detect", tabBlobDetect);

        labelCertaintyTrip.setFont(new java.awt.Font("Sans Serif", 1, 12)); // NOI18N
        labelCertaintyTrip.setText("Hough circle detection parameters");

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

        spinnerTripCenterThresh.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(80), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinnerTripCenterThresh.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTripCenterThreshStateChanged(evt);
            }
        });

        spinnerTripInvResRat.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.1d, 256.0d, 0.1d));
        spinnerTripInvResRat.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTripInvResRatStateChanged(evt);
            }
        });

        spinnerTripMaxRad.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(60), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinnerTripMaxRad.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTripMaxRadStateChanged(evt);
            }
        });

        labelTripCannyThresh.setText("Canny threshold");
        labelTripCannyThresh.setToolTipText("Higher threshold of the two passed to the Canny() edge detector (the lower one is twice smaller).");

        labelTripCenterDetectThresh.setText("Center detection threshold");
        labelTripCenterDetectThresh.setToolTipText("Accumulator threshold for the circle centers at the detection stage. The smaller it is, the more false circles may be detected. Circles, corresponding to the larger accumulator values, will be returned first.");

        spinnerTripCenterDist.setModel(new javax.swing.SpinnerNumberModel(20, 1, 1024, 1));
        spinnerTripCenterDist.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTripCenterDistStateChanged(evt);
            }
        });

        spinnerTripMinRad.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinnerTripMinRad.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTripMinRadStateChanged(evt);
            }
        });

        labelTripMinRad.setText("Min Radius");

        labelTripMaxRad.setText("Max Radius");

        labelTripMinCenterDist.setText("Min center distance");
        labelTripMinCenterDist.setToolTipText("Minimum distance between the center (x, y) coordinates of detected circles. If the minDist is too small, multiple circles in the same neighborhood as the original may be (falsely) detected. If the minDist is too large, then some circles may not be detected at all.");

        labelTripInvResRat.setText("Inverse resolution ratio");
        labelTripInvResRat.setToolTipText("This parameter is the inverse ratio of the accumulator resolution to the image resolution (see Yuen et al. for more details). Essentially, the larger the dp gets, the smaller the accumulator array gets.");

        spinnerCannyThresh.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(200), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinnerCannyThresh.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerCannyThreshStateChanged(evt);
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

        javax.swing.GroupLayout tabTripFiducialsLayout = new javax.swing.GroupLayout(tabTripFiducials);
        tabTripFiducials.setLayout(tabTripFiducialsLayout);
        tabTripFiducialsLayout.setHorizontalGroup(
            tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                        .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(labelTripCannyThresh)
                            .addComponent(labelTripInvResRat)
                            .addComponent(labelTripMinRad))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                                .addComponent(spinnerTripInvResRat, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(labelTripCenterDetectThresh)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerTripCenterThresh, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                                .addComponent(spinnerTripMinRad, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(labelTripMaxRad)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerTripMaxRad, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                                .addComponent(spinnerCannyThresh, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(labelTripMinCenterDist)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerTripCenterDist, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                        .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkTripPreThreshold)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabTripFiducialsLayout.createSequentialGroup()
                                .addComponent(checkAdaptiveThreshTrip)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(checkInvertThreshTrip)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliderThresholdTrip, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                        .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkEnableFiducials)
                            .addComponent(morphOpsLabelTrip, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelCertaintyTrip)
                            .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                                .addGap(159, 159, 159)
                                .addComponent(morphOpsComboBoxTrip, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        tabTripFiducialsLayout.setVerticalGroup(
            tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkEnableFiducials, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tabTripFiducialsLayout.createSequentialGroup()
                        .addComponent(checkTripPreThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkAdaptiveThreshTrip)
                            .addComponent(checkInvertThreshTrip)))
                    .addComponent(sliderThresholdTrip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(morphOpsLabelTrip)
                    .addComponent(morphOpsComboBoxTrip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(labelCertaintyTrip)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelTripInvResRat)
                    .addComponent(spinnerTripInvResRat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerTripCenterThresh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelTripCenterDetectThresh))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelTripCannyThresh)
                    .addComponent(spinnerCannyThresh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerTripCenterDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelTripMinCenterDist))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(tabTripFiducialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelTripMinRad)
                    .addComponent(spinnerTripMinRad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerTripMaxRad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelTripMaxRad))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pipelineTabs.addTab("Fiducials", tabTripFiducials);

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

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void pipelineTabsStateChanged(ChangeEvent evt) {//GEN-FIRST:event_pipelineTabsStateChanged
        selectedView = View.values()[((JTabbedPane)evt.getSource()).getSelectedIndex()];
    }//GEN-LAST:event_pipelineTabsStateChanged

    private void checkEnableBlobsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkEnableBlobsActionPerformed
        blobTracking = ((JCheckBox)evt.getSource()).isSelected();
    }//GEN-LAST:event_checkEnableBlobsActionPerformed

    private void checkInvertThreshBlobActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkInvertThreshBlobActionPerformed
        params.blobThInverted = ((JCheckBox)evt.getSource()).isSelected();
    }//GEN-LAST:event_checkInvertThreshBlobActionPerformed

    private void sliderThresholdBlobStateChanged(ChangeEvent evt) {//GEN-FIRST:event_sliderThresholdBlobStateChanged
        params.blobThreshold = 1+((JSlider)(evt.getSource())).getValue()*2.54f;
    }//GEN-LAST:event_sliderThresholdBlobStateChanged

    private void morphOpsComboBoxBlobActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_morphOpsComboBoxBlobActionPerformed
       params.blobMorphOps = MorphOps.values()[((JComboBox<MorphOps>)evt.getSource()).getSelectedIndex()];
    }//GEN-LAST:event_morphOpsComboBoxBlobActionPerformed

    private void buttonRemoveBackgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveBackgActionPerformed
        recaptureBg = true;
    }//GEN-LAST:event_buttonRemoveBackgActionPerformed

    private void sliderBrightnessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderBrightnessStateChanged
        params.brightness = ((JSlider)(evt.getSource())).getValue()*5.12f - 255;
    }//GEN-LAST:event_sliderBrightnessStateChanged

    private void sliderContrastStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderContrastStateChanged
        params.contrast = ((JSlider)(evt.getSource())).getValue()*0.03f;
    }//GEN-LAST:event_sliderContrastStateChanged

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        dynamicBGRemoval = jCheckBox1.isSelected();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void sliderContrast1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderContrast1StateChanged
        dynamicBGAmount = sliderContrast1.getValue()*0.01f + 0.001f;
    }//GEN-LAST:event_sliderContrast1StateChanged

    private void sliderBlurStateChanged(ChangeEvent evt) {//GEN-FIRST:event_sliderBlurBlobStateChanged
    	params.blurAmount = ((JSlider)(evt.getSource())).getValue()*0.01f;
    }//GEN-LAST:event_sliderBlurBlobStateChanged

    private void blurComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blurComboBoxBlobActionPerformed
    	int i=blurComboBox.getSelectedIndex();
    	params.blur = Blur.values()[i];
    	if(i < 4){
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
        tripTracking = ((JCheckBox)evt.getSource()).isSelected();
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
        // TODO add your handling code here:
    }//GEN-LAST:event_checkTripPreThresholdActionPerformed

    private void checkEnableBlobs1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkEnableBlobs1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_checkEnableBlobs1ActionPerformed

}
