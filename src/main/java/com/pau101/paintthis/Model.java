package com.pau101.paintthis;

import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;

public class Model extends SampleModel {
	public Model(int dataType, int w, int h, int numBands) {
		super(dataType, w, h, numBands);
	}

	@Override
	public int getNumDataElements() {
		return 0;
	}

	@Override
	public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
		return null;
	}

	@Override
	public void setDataElements(int x, int y, Object obj, DataBuffer data) {
		
	}

	@Override
	public int getSample(int x, int y, int b, DataBuffer data) {
		return 0;
	}

	@Override
	public void setSample(int x, int y, int b, int s, DataBuffer data) {

	}

	@Override
	public SampleModel createCompatibleSampleModel(int w, int h) {
		return null;
	}

	@Override
	public SampleModel createSubsetSampleModel(int[] bands) {
		return null;
	}

	@Override
	public DataBuffer createDataBuffer() {
		return null;
	}

	@Override
	public int[] getSampleSize() {
		return null;
	}

	@Override
	public int getSampleSize(int band) {
		return 0;
	}
}
