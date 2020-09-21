/**
 * Copyright Copyright 2018-19 Simon Andrews
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
package uk.ac.babraham.conclave.Displays.QuantitationTrendPlot;

import uk.ac.babraham.conclave.DataTypes.DataCollection;
import uk.ac.babraham.conclave.DataTypes.DataStore;
import uk.ac.babraham.conclave.DataTypes.Probes.ProbeList;

public class QuantitationTrendHeatmapPreferencesDialog extends QuantitationTrendPlotPreferencesDialog {

	public QuantitationTrendHeatmapPreferencesDialog(DataCollection collection, ProbeList[] probes,DataStore[] stores) {
		super(collection, probes, stores);
	}

}
