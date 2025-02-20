/*
 * Copyright (C) 2011 McMaster University PI: Dr. Michael Schutz
 * <schutz@mcmaster.ca>
 * 
 * Distributed under the terms of the GNU Lesser General Public License (LGPL).
 * See LICENSE.TXT that came with this file.
 */
package edu.mcmaster.maplelab.av.media.animation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AnimationFrame represents one frame in the animation.
 * 
 * @author Catherine Elder <cje@datamininglab.com>
 */
public class AnimationFrame {
	private List<AnimationPoint> _pointList;
	private final long _nanoTime;
	private final Double _luminance;

	public AnimationFrame(double timeInSeconds, List<AnimationPoint> pointList, Double luminance) {
		_nanoTime = TimeUnit.NANOSECONDS.convert(((long) (1000d*timeInSeconds)), TimeUnit.MILLISECONDS);
		_pointList = pointList;
		_luminance = luminance;
	}

	/** Provides an unmodifiable ordered list of joint locations. */
	public List<AnimationPoint> getJointLocations() {	
		return Collections.unmodifiableList(_pointList);
	}

	/**
	 * Get the time frame should be rendered after start of animation.
	 */
	public long getTimeInNanos() {
		return _nanoTime;
	}
	
	/**
	 * Get the luminance value.
	 */
	public Double getLuminance () {
		return _luminance;
	}
}
