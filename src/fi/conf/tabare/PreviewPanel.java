package fi.conf.tabare;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JPanel;
import javax.swing.RepaintManager;

import org.opencv.core.Mat;

public class PreviewPanel extends JPanel {

	private int previewImageChannels;
	private BufferedImage previewImage;
	private byte[] imageData;
	//private Object previewLock = new Object();
	
	private BufferedImage[] dataOverlayImage;
	private Graphics2D[] dataOverlayGraphics;
	private volatile boolean outputOverlayIndex = false;
	
	private volatile boolean showAlignmentGrid = false;
	
	private float trackTime = 0;
	private float detectTime = 0;
	
	private int videoHeight = -1;
	private int videoWidth = -1;
	
	public PreviewPanel() {
		this.setPreferredSize(new Dimension(800, 600));
	}

	public void initialize(int videoWidth, int videoHeight){
		this.videoWidth = videoWidth;
		this.videoHeight = videoHeight;
		this.setMinimumSize(new Dimension(videoWidth, videoHeight));
		if(dataOverlayImage == null){
			dataOverlayImage = new BufferedImage[2];
			dataOverlayImage[0] = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_4BYTE_ABGR);
			dataOverlayImage[1] = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_4BYTE_ABGR);
			dataOverlayGraphics = new Graphics2D[2];
			dataOverlayGraphics[0] = (Graphics2D)dataOverlayImage[0].getGraphics();
			dataOverlayGraphics[1] = (Graphics2D)dataOverlayImage[1].getGraphics();
			dataOverlayGraphics[0].setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
			dataOverlayGraphics[1].setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
		}
	}

	public void updatePreviewImage(Mat mat){
		//synchronized (previewLock) {
			if(previewImage == null || mat.channels() != previewImageChannels){
				previewImage = OpenCVUtils.matToBufferedImage(mat);
				if(previewImage == null) return;
				previewImageChannels = mat.channels();
				imageData = ((DataBufferByte) previewImage.getRaster().getDataBuffer()).getData();
			} else {
				mat.get(0, 0, imageData);
			}
		//}
		RepaintManager.currentManager(this).markCompletelyDirty(this);
	}
	
	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.DARK_GRAY);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		//synchronized (previewLock) {
		if(previewImage != null){
			g2.drawImage(previewImage, (getWidth()/2)-(videoWidth/2), (getHeight()/2)-(videoHeight/2), null);	
		}
		//}
		
		if(dataOverlayImage != null){
			g2.drawImage(dataOverlayImage[(outputOverlayIndex?0:1)], (getWidth()/2)-(videoWidth/2), (getHeight()/2)-(videoHeight/2), null);
		}
			
		g2.setColor(Color.CYAN);
		g2.drawString("Detect: " + String.format("%.1fms (%d FPS)", detectTime, (int)(1000/(1+detectTime))), 8, 18);
		g2.drawString("Track: " + String.format("%.1fms (%d FPS)", trackTime, (int)(1000/(1+trackTime))), 8, 30);
		
	}

	public void updateDetectTime(float detectTime) {
		this.detectTime = 0.8f*this.detectTime + 0.2f*detectTime;
	}

	public void updateTrackTime(float trackTime) {
		this.trackTime = 0.8f*this.trackTime + 0.2f*trackTime;
	}

	public Graphics2D getOverlayGraphics() {
		
		Graphics2D g2 = dataOverlayGraphics[(outputOverlayIndex?1:0)]; 
		g2.setBackground(new Color(0,0,0,0));
		g2.clearRect(0, 0, videoWidth, videoHeight);
		
		return g2;
	}

	public void swapOverlay() {
		outputOverlayIndex = !outputOverlayIndex;
	}
	
	public void showAlignmentGrid(boolean visible){
		this.showAlignmentGrid = visible;
	}
	
	@Override
	protected void processMouseEvent(MouseEvent e) {
		//Intercept and traslate MouseEvents point to correspond the video frame.
		e.translatePoint(-(getWidth()/2)+(videoWidth/2), -(getHeight()/2)+(videoHeight/2));
		super.processMouseEvent(e);
	}
	
	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		//Intercept and traslate MouseEvents point to correspond the video frame.
		e.translatePoint(-(getWidth()/2)+(videoWidth/2), -(getHeight()/2)+(videoHeight/2));
		super.processMouseMotionEvent(e);
	}
	
}
