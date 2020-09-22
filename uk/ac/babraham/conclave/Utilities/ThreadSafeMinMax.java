/**
 * Copyright 2011-19 Simon Andrews
 *
 *    This file is part of Conclave.
 *
 *    Conclave is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Conclave; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.conclave.Utilities;
/**
 * This class provides a thread safe implementation of a record
 * of the max and min values ever seen
 * 
 * @author andrewss
 *
 */
public class ThreadSafeMinMax {

	private float min = 0;
	private float max = 0;
	private boolean anyData = false;
	
	public synchronized void addValue (float value) {
		if (anyData) {
			if (value < min) min = value;
			if (value > max) max = value;
		}
		else {
			min = value;
			max = value;
		}
	
	}
	
	public float min () {
		return min;
	}
	
	public float max () {
		return max;
	}
	
	
}
