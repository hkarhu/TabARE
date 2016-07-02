package fi.conf.tabare;

import java.io.Serializable;

public class Params implements Serializable {
	
	public enum MorphOps { none, dilate, erode }
	public enum KernelShape { rect, ellipse }
	public enum KernelSize { size_3x3, size_5x5, size_7x7, size_9x9 }
	public enum Blur { none, normal, gaussian, median }
	
	//Calibration
	public int[][][] calibrationData;
	
	//Input image params
	public float brightness = 0f;
	public float contrast = 1.0f;
	public Blur blur = Blur.none;
	public KernelSize blurKernelSize = null;
	public float blurAmount = 0;
	
	//Distortion
	public boolean enableDistortion = false;
	public int k1=0, k2=0, p1=0, p2=0, k3=0, z=1;
	
	//BlobDetection params
	public boolean blobTracking = false;
	public float blobThreshold = 127;
	public boolean blobThInverted = false;
	public MorphOps blobMorphOps = MorphOps.none;
	public KernelSize blobMorpthKernelSize = KernelSize.size_3x3;
	public KernelShape blobMorphKernelShape = KernelShape.ellipse;
	
	//TripcodeDetection params
	public boolean tripTracking = false;
	public float tripBlurAmount = 0;
	public boolean tripEnableThresholding = false;
	public float tripThreshold = 127;
	public MorphOps tripMorphOps = MorphOps.none;
	public int tripCenterDist = 1;
	public int tripCannyThresh = 50;
	public int tripAccumThresh = 30;
	public boolean tripThInverted = false;
	public boolean tripAdaptThreshold = false;
	public double tripDP = 1;
	public int tripRadMin = 10;
	public int tripRadMax = 32;

}
