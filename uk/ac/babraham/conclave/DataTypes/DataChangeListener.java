/**
 * Copyright 2006-19 Simon Andrews
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
package uk.ac.babraham.conclave.DataTypes;

import uk.ac.babraham.conclave.DataTypes.Probes.ProbeList;
import uk.ac.babraham.conclave.DataTypes.Probes.ProbeSet;

/**
 * The listener interface for receiving dataChange events.
 * The class that is interested in processing a dataChange
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addDataChangeListener<code> method. When
 * the dataChange event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see DataChangeEvent
 */
public interface DataChangeListener {

	/**
	 * Data set added.
	 * 
	 * @param d the d
	 */
	public void dataSetAdded (DataSet d);
	
	/**
	 * Data set removed.
	 * 
	 * @param d the d
	 */
	public void dataSetsRemoved (DataSet [] d);
	
	public void dataSetRenamed (DataSet d, String oldName);
	
	public void probeSetReplaced (ProbeSet p);
	
	public void replicateSetAdded (ReplicateSet r);
	
	public void replicateSetsRemoved (ReplicateSet [] r);
	
	public void replicateSetRenamed (ReplicateSet r, String oldName);
	
	public void replicateSetStoresChanged (ReplicateSet r);
	
	public void activeDataStoreChanged (DataStore s);
	
	public void activeProbeListChanged (ProbeList l);
	
}
