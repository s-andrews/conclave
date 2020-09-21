/**
 * Copyright Copyright 2010-19 Simon Andrews
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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.conclave.ConclaveApplication;
import uk.ac.babraham.conclave.ConclaveException;
import uk.ac.babraham.conclave.DataTypes.Genome.Genome;
import uk.ac.babraham.conclave.DataTypes.Probes.ProbeList;
import uk.ac.babraham.conclave.DataTypes.Probes.ProbeSet;

/**
 * The Class DataCollection is the main data storage object through
 * which all of the data in a project can be accessed.
 */
public class DataCollection {

	/** The probe set. */
	private ProbeSet probeSet = null;
	
	/** The data sets. */
	private Vector<DataSet> dataSets = new Vector<DataSet>();
		
	/** The replicate sets */
	private Vector<ReplicateSet> replicateSets = new Vector<ReplicateSet>();
	
	/** The active data store. */
	private DataStore activeDataStore = null;
	
	/** The data change listeners. */
	private Vector<DataChangeListener> listeners = new Vector<DataChangeListener>();
		
	/** The genome. */
	private Genome genome;
	
	/**
	 * Instantiates a new data collection.
	 * 
	 * @param g the g
	 */
	public DataCollection (Genome g) {
		if (g == null) {
			throw new NullPointerException("Genome can't be null when creating a data collection");
		}
		this.genome = g;
	}
	
	/**
	 * Genome.
	 * 
	 * @return the genome
	 */
	public Genome genome () {
		return genome;
	}
	
	/**
	 * Gets the data set.
	 * 
	 * @param position the position
	 * @return the data set
	 */
	public DataSet getDataSet (int position) {

		if (position>=0 && position <dataSets.size()) {
			return dataSets.elementAt(position);
		}
		return null;
	}
	

	/**
	 * Gets a replicate set.
	 * 
	 * @param position the position
	 * @return the replicate set
	 */
	public ReplicateSet getReplicateSet (int position) {
		if (position>=0 && position <replicateSets.size()) {
			return replicateSets.elementAt(position);
		}
		return null;
	}
	
	
	/**
	 * Checks if is quantitated.
	 * 
	 * @return true, if is quantitated
	 */
	public boolean isQuantitated () {
		
		if (probeSet == null || probeSet.getAllProbes().length == 0) return false;
		
		DataStore [] stores = getAllDataStores();
		
		for (int i=0;i<stores.length;i++) {
			if (stores[i].isQuantitated()) return true;
		}
		
		return false;
	}
	
	/**
	 * Sets the probe set.
	 * 
	 * @param newProbeSet the new probe set
	 */
	public void setProbeSet (ProbeSet newProbeSet) {
		if (probeSet != null) {
			probeSet.delete();
		}
		probeSet = newProbeSet;
		probeSet.setCollection(this);
		Enumeration<DataChangeListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().probeSetReplaced(probeSet);
		}
		
		// We need to tell all of the DataSets and Groups
		// about the new probe set
		DataStore [] stores = getAllDataStores();
		for (int i=0;i<stores.length;i++) {
			stores[i].probeSetReplaced(newProbeSet);
		}
	}
		
	/**
	 * Probe set.
	 * 
	 * @return the probe set
	 */
	public ProbeSet probeSet () {
		return probeSet;
	}
	
	/**
	 * Adds the data set.
	 * 
	 * @param data the data
	 */
	public void addDataSet (DataSet data) {
		dataSets.add(data);
		data.setCollection(this);

		// We need to let this dataset know about the
		// current probset.
		data.probeSetReplaced(probeSet());
		
		Enumeration<DataChangeListener> e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().dataSetAdded(data);
		}
	}
		
	/**
	 * Adds the data group.
	 * 
	 * @param group the group
	 */
	public void addReplicateSet (ReplicateSet set) {
		replicateSets.add(set);
		set.setCollection(this);

		Enumeration<DataChangeListener> e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().replicateSetAdded(set);
		}
	}
	
	
	/**
	 * Removes a replicate set.
	 * 
	 * @param group the group
	 */
	public void removeReplicateSets (ReplicateSet [] sets) {
		
		// We inform the listeners first to give 
		// a chance for the tree to update.
		Enumeration<DataChangeListener> e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().replicateSetsRemoved(sets);
		}

		for (int s=0;s<sets.length;s++) {
			replicateSets.remove(sets[s]);
			sets[s].setCollection(null);
		}
	}
	
	/**
	 * Removes the data set.
	 * 
	 * @param data the data
	 */
	public void removeDataSets (DataSet [] data) {
		
		for (int d=0;d<data.length;d++) {		
			Enumeration<ReplicateSet> e2 = replicateSets.elements();
			while (e2.hasMoreElements()) {
				ReplicateSet r = e2.nextElement();
			
				if (r.containsDataStore(data[d])) {
					r.removeDataStore(data[d]);
				}
			}
		}
		
		// Notify listeners before actually removing to allow the
		// tree to pick up the changes correctly
		Enumeration<DataChangeListener> e3 = listeners.elements();
		while (e3.hasMoreElements()) {
			((DataChangeListener)e3.nextElement()).dataSetsRemoved(data);
		}
		
		for (int d=0;d<data.length;d++) {
			dataSets.remove(data[d]);
			data[d].setCollection(null);
		}
		
	}
	
	
	/**
	 * Gets all replicate sets.
	 * 
	 * @return all replicate sets
	 */
	public ReplicateSet [] getAllReplicateSets () {
		ReplicateSet [] sets = replicateSets.toArray(new ReplicateSet[0]);
		Arrays.sort(sets);
		return sets;
	}
	
	/**
	 * Gets the all data sets.
	 * 
	 * @return the all data sets
	 */
	public DataSet [] getAllDataSets () {
		DataSet [] sets = dataSets.toArray(new DataSet[0]);
		Arrays.sort(sets);
		return sets;
	}
	
	/**
	 * Gets the all data stores.
	 * 
	 * @return the all data stores
	 */
	public DataStore [] getAllDataStores () {
		DataSet [] sets = getAllDataSets();
		ReplicateSet [] replicates = getAllReplicateSets();
		
		DataStore [] stores = new DataStore [sets.length+replicates.length];
		
		for (int i=0;i<replicates.length;i++) {
			stores[i] = replicates[i];
		}
		for (int i=0;i<sets.length;i++) {
			stores[replicates.length+i] = sets[i];
		}
		return stores;
	}
	
	public void activeProbeListChanged (ProbeList list) {
		Enumeration<DataChangeListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().activeProbeListChanged(list);
		}
	}
	
	/**
	 * Sets the active data store.
	 * 
	 * @param d the new active data store
	 * @throws ConclaveException the seq monk exception
	 */
	public void setActiveDataStore (DataStore d) throws ConclaveException {
		if (d == null || dataSets.contains(d) || replicateSets.contains(d)) {
			activeDataStore = d;
			
			Enumeration<DataChangeListener>e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().activeDataStoreChanged(d);
			}
			
		}
		else {
			throw new ConclaveException("Data store "+d.name()+" could not be found in the data collection");
		}
	}
	
	/**
	 * Gets the active data store.
	 * 
	 * @return the active data store
	 */
	public DataStore getActiveDataStore () {
		if (activeDataStore != null) {
			return activeDataStore;
		}
		else if (getAllDataStores().length == 1) {
			return getAllDataStores()[0];
		}
		else if (ConclaveApplication.getInstance().drawnDataStores().length == 1) {
			return ConclaveApplication.getInstance().drawnDataStores()[0];
		}
		else {
			return null;
		}
	}
	
	/**
	 * Adds the data change listener.
	 * 
	 * @param l the l
	 */
	public void addDataChangeListener (DataChangeListener l) {
		if (l != null && ! listeners.contains(l)) {
			listeners.add(l);
		}
	}
	
	/**
	 * Removes the data change listener.
	 * 
	 * @param l the l
	 */
	public void removeDataChangeListener (DataChangeListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}
	
		
	/**
	 * Data set renamed.
	 * 
	 * @param s the s
	 */
	public void dataSetRenamed (DataSet s, String oldName) {
		Enumeration<DataChangeListener> e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().dataSetRenamed(s,oldName);
		}
	}
	
		
	public void replicateSetRenamed (ReplicateSet r, String oldName) {
		Enumeration<DataChangeListener> e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().replicateSetRenamed(r,oldName);
		}
	}
	
	public void replicateSetStoresChanged (ReplicateSet r) {
		Enumeration<DataChangeListener> e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().replicateSetStoresChanged(r);
		}
	}
}
