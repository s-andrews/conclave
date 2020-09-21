/**
 * Copyright 2012-19 Simon Andrews
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
package uk.ac.babraham.conclave.Menu;

import javax.swing.JToolBar;

import uk.ac.babraham.conclave.DataTypes.DataChangeListener;
import uk.ac.babraham.conclave.DataTypes.DataCollection;

public abstract class ConclaveToolbar extends JToolBar implements DataChangeListener {

	private DataCollection collection = null;
	private ConclaveMenu menu;
	private boolean shown = false;
	
	
	public ConclaveToolbar (ConclaveMenu menu) {
		this.menu = menu;
		setFocusable(false);
		setShown(showByDefault());
	}
	
	public void setShown (boolean shown) {
		this.shown = shown;
	}
	
	public boolean shown () {
		return shown;
	}

	abstract public void reset();
	
	abstract public void genomeLoaded();
	
	abstract public boolean showByDefault();
	
	protected ConclaveMenu menu () {
		return menu;
	}
	
	
	public void setDataCollection (DataCollection collection) {
		this.collection = collection;
		collection.addDataChangeListener(this);
	}
	
	protected DataCollection collection () {
		return collection;
	}
	
	abstract public String name();
		
	
}
