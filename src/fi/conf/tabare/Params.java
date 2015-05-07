package fi.conf.tabare;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import fi.conf.tabare.ARDataProvider.Blur;
import fi.conf.tabare.ARDataProvider.MorphOps;

public class Params implements Serializable {
	
	//Input image params
	public float brightness = 0f;
	public float contrast = 1.0f;
	public Blur blur = Blur.none;
	public float blurAmount = 0;
	
	//BlobDetection params
	public float blobThreshold = 127;
	public boolean blobThInverted;
	public MorphOps blobMorphOps = MorphOps.none;
	
	//TripcodeDetection params
	public float tripBlurAmount = 0;	
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
