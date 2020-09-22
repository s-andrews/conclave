/**
 * Copyright Copyright 2017-19 Simon Andrews
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


package uk.ac.babraham.conclave.DataTypes.Sequence;

import java.io.Serializable;
import java.util.Arrays;

import uk.ac.babraham.conclave.Utilities.IntVector;
import uk.ac.babraham.conclave.Utilities.LongVector;

/**
 * This class is just a simple way to pass around paired read and count
 * arrays in a single object.
 * @author andrewss
 *
 */
public class ReadsWithCounts implements Serializable {

	private static final long serialVersionUID = 137753900273L;
	public long [] reads;
	public float [] counts;

	/**
	 * We assume that he reads and counts have already been
	 * collapsed and that sorted.
	 * @param reads
	 * @param counts
	 */
	public ReadsWithCounts (long [] reads, float [] counts) {
		if (reads == null || counts == null) {
			throw new IllegalStateException("Unexpected null: Reads ="+reads+" counts="+counts);
		}
		this.reads = reads;
		this.counts = counts;
	}

	
	/**
	 * This will be a constructor which will merge multiple readswithcounts
	 * We need them to have the same read positions and we take the mean of
	 * the associated values
	 * 
	 * @return
	 */
	public ReadsWithCounts (ReadsWithCounts [] readsToMerge) {
		for (int i=0;i<readsToMerge.length; i++) {
			if (i==0) {
				reads = readsToMerge[i].reads;
				counts = Arrays.copyOf(readsToMerge[i].counts,readsToMerge[i].counts.length);
			}
			else {
				if (readsToMerge[i].reads.length != reads.length) {
					throw new IllegalStateException("DataSets in a Replicate Set must have the same reads");
				}
				for (int j=0;j<counts.length;j++) {
					counts[j] += readsToMerge[i].counts[j];
				}
			}
		}
		
		for (int j=0;j<counts.length;j++) {
			counts[j] /= readsToMerge.length;
		}
		
	}
	
	

	public float totalCount () {
		int count = 0;
		for (int i=0;i<counts.length;i++) {
			count += counts[i];
		}

		return count;
	}
}
