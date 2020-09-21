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
package uk.ac.babraham.conclave.DataParsers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.conclave.ConclaveApplication;
import uk.ac.babraham.conclave.ConclaveException;
import uk.ac.babraham.conclave.DataTypes.DataSet;
import uk.ac.babraham.conclave.DataTypes.DataStore;
import uk.ac.babraham.conclave.DataTypes.Genome.Chromosome;
import uk.ac.babraham.conclave.DataTypes.Genome.Feature;
import uk.ac.babraham.conclave.DataTypes.Genome.Location;
import uk.ac.babraham.conclave.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.conclave.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.conclave.Utilities.NumberKeyListener;

/**
 * Reimports data from an existing set of DataStore.  This allows you to promote
 * a DataGroup to being a DataSet and would allow you to extend reads in
 * an existing set, or to remove duplicates from an existing set.
 */
public class VisibleStoresParser extends DataParser {

	private VisibleStoresOptionsPanel prefs;
	private DataStore [] visibleStores;
	private boolean filterByFeature = false;
	private boolean excludeFeature = false;
	private boolean filterByLength = false;
	private boolean extractCentres = false;
	private boolean filterByStrand = false;
	private Integer minLength = null;
	private Integer maxLength = null;
	private Integer centreExtractContext = null;
	private String featureType;
	private boolean downsample;
	private double downsampleProbabilty = 0;

	private boolean keepForward = true;
	private boolean keepReverse = true;
	private boolean keepUnknown = true;

	private int forwardOffset = 0;
	private int reverseOffset = 0;


	/**
	 * Instantiates a new active store parser.
	 * 
	 * @param data The dataCollection to which new data will be added and from which the active set will be taken
	 */
	public VisibleStoresParser (ConclaveApplication application) {
		super(application.dataCollection());
		visibleStores = application.drawnDataSets();
		prefs = new VisibleStoresOptionsPanel();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getFileFilter()
	 */
	public FileFilter getFileFilter () {
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		filterByStrand = prefs.filterStrandCheckbox.isSelected();
		if (filterByStrand) {
			if (prefs.strandFilterDirectionBox.getSelectedItem().equals("Forward")) {
				keepForward = true;
				keepReverse = false;
				keepUnknown = false;
			}
			if (prefs.strandFilterDirectionBox.getSelectedItem().equals("Reverse")) {
				keepForward = false;
				keepReverse = true;
				keepUnknown = false;
			}

			if (prefs.strandFilterDirectionBox.getSelectedItem().equals("Forward or Reverse")) {
				keepForward = true;
				keepReverse = true;
				keepUnknown = false;
			}

			if (prefs.strandFilterDirectionBox.getSelectedItem().equals("Unknown")) {
				keepForward = false;
				keepReverse = false;
				keepUnknown = true;
			}

		}

		forwardOffset = prefs.forwardOffset();
		reverseOffset = prefs.reverseOffset();


		filterByFeature = prefs.filterFeatureCheckbox.isSelected();
		if (filterByFeature) {
			excludeFeature = prefs.featureFilterTypeBox.getSelectedItem().equals("Excluding");
			featureType = (String)prefs.filterFeatureBox.getSelectedItem();
		}

		filterByLength = prefs.filterByLengthBox.isSelected();
		if (filterByLength) {
			if (prefs.minLengthField.getText().length() > 0) {
				minLength = Integer.parseInt(prefs.minLengthField.getText());
			}
			if (prefs.maxLengthField.getText().length() > 0) {
				maxLength = Integer.parseInt(prefs.maxLengthField.getText());
			}
		}

		extractCentres = prefs.extractCentres();
		centreExtractContext = prefs.centreExtractContext();

		int downsampleTarget = 0;
		if (prefs.downsampleBox.isSelected() && prefs.downsampleTargetField.getText().length() > 0) {

			downsample = true;
			downsampleTarget = Integer.parseInt(prefs.downsampleTargetField.getText());
		}

		DataSet [] newDataStores = new DataSet[visibleStores.length];

		for (int s=0;s<visibleStores.length;s++) {

			if (downsample) {
				// Work out the probability for this sample
				long realReadCount = visibleStores[s].getTotalReadCount();
				downsampleProbabilty = ((double)downsampleTarget) / realReadCount;
			}

			DataSet newData = null;
			try {
				newData = processNormalDataStore(visibleStores[s]);
			}

			catch (Exception ex) {
				progressExceptionReceived(ex);
			}

			if (newData == null) {
				// They cancelled
				return;
			}

			// Cache the data in the new dataset
			progressUpdated("Caching data", 1, 1);
			newData.finalise();

			newDataStores[s] = newData;

		}

		processingFinished(newDataStores);
	}	




	private DataSet processNormalDataStore (DataStore store) {

		int extendBy = prefs.extendReads();
		boolean reverse = prefs.reverseReads();
		boolean removeStrand = prefs.removeStrandInfo();

		DataSet newData = new DataSet(store.name()+"_reimport","Reimported from "+store.name(),prefs.removeDuplicates(),prefs.getImportOptionsDescription());

		// Now process the data
		Chromosome [] chrs = dataCollection().genome().getAllChromosomes();

		for (int c=0;c<chrs.length;c++) {

			progressUpdated("Processing "+store.name()+" chr "+chrs[c].name(),c,chrs.length);

			ReadsWithCounts reads = store.getReadsForChromosome(chrs[c]);

			Feature [] features = null;
			if (filterByFeature) {
				features = collection.genome().annotationCollection().getFeaturesForType(chrs[c], featureType);
				Arrays.sort(features);
			}

			int currentFeaturePostion = 0;

			for (int r=0;r<reads.reads.length;r++) {

				for (int ct=0;ct<reads.counts[r];ct++) {

					long thisRead = reads.reads[r];
					if (cancel) {
						progressCancelled();
						return null;
					}

					if (downsample && downsampleProbabilty < 1) {
						if (Math.random() > downsampleProbabilty) {
							continue;
						}
					}

					long read;

					int start = SequenceRead.start(thisRead);
					int end = SequenceRead.end(thisRead);

					int strand = SequenceRead.strand(thisRead);

					if (filterByStrand) {
						if (strand == Location.FORWARD && !keepForward) continue;
						if (strand == Location.REVERSE && !keepReverse) continue;
						if (strand == Location.UNKNOWN && !keepUnknown) continue;
					}

					if (filterByLength) {
						int length = SequenceRead.length(thisRead);
						if (minLength != null && length < minLength) continue;
						if (maxLength != null && length > maxLength) continue;
					}

					if (strand == Location.FORWARD) {
						start += forwardOffset;
						end += forwardOffset;
					}

					if (strand == Location.REVERSE) {
						start -= reverseOffset;
						end -= reverseOffset;
					}

					if (filterByFeature && features.length == 0 && !excludeFeature) continue;

					if (filterByFeature  && features.length > 0) {
						// Check if we pass the filter for the current feature set

						// See if we're comparing against the right feature
						while (SequenceRead.start(thisRead) > features[currentFeaturePostion].location().end() && currentFeaturePostion < (features.length-1)) {
							currentFeaturePostion++;
						}

						// Test to see if we overlap
						if (SequenceRead.overlaps(thisRead, features[currentFeaturePostion].location().packedPosition())) {
							if (excludeFeature) continue;
						}
						else {
							if (!excludeFeature) continue;
						}

					}


					if (reverse) {
						if (strand == Location.FORWARD) {
							strand = Location.REVERSE;
						}
						else if (strand == Location.REVERSE) {
							strand = Location.FORWARD;
						}

					}

					if (removeStrand) {
						strand = Location.UNKNOWN;
					}

					if (extractCentres) {
						int centre = start + ((end-start)/2);
						start = centre-centreExtractContext;
						end = centre+centreExtractContext;

					}


					if (extendBy != 0) {

						// We now allow negative extensions to shorten reads
						if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
							end += extendBy;
							if (end < start) end = start;


						}
						else if (strand == Location.REVERSE) {
							start -= extendBy;

							if (start > end) start = end;
						}
					}

					// We don't allow reads before the start of the chromosome
					if (start < 1) {
						int overrun = (0 - start)+1;
						progressWarningReceived(new ConclaveException("Reading position "+start+" was "+overrun+"bp before the start of chr"+chrs[c].name()+" ("+chrs[c].length()+")"));
						continue;
					}

					// We also don't allow readings which are beyond the end of the chromosome
					if (end > chrs[c].length()) {
						int overrun = end - chrs[c].length();
						progressWarningReceived(new ConclaveException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+chrs[c].name()+" ("+chrs[c].length()+")"));
						continue;
					}

					// We can now make the new reading
					try {
						read = SequenceRead.packPosition(start,end,strand);
						if (! prefs.isHiC()) {
							// HiC additions are deferred until we know the other end is OK too.
							newData.addData(chrs[c],read);
						}
					}
					catch (ConclaveException e) {
						progressWarningReceived(e);
						continue;
					}

				}
			}
		}
		return newData;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#description()
	 */
	public String description() {
		return "Imports General Feature Format Files";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getOptionsPanel()
	 */
	public JPanel getOptionsPanel() {
		return prefs;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#hasOptionsPanel()
	 */
	public boolean hasOptionsPanel() {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#name()
	 */
	public String name() {
		return "Active Store Importer";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#readyToParse()
	 */
	public boolean readyToParse() {
		return true;
	}

	private class VisibleStoresOptionsPanel extends JPanel implements ActionListener {

		private JComboBox removeDuplicates;

		private JCheckBox isHiC;
		private JTextField hiCDistance;
		private JCheckBox hiCIgnoreTransBox;

		private JCheckBox reverseReads;

		private JCheckBox removeStrandInfo;

		private JTextField extendReads;
		private JPanel singleEndOptions;

		private JComboBox featureFilterTypeBox;

		private JCheckBox filterFeatureCheckbox;
		private JComboBox filterFeatureBox;

		private JCheckBox filterStrandCheckbox;
		private JComboBox strandFilterDirectionBox;

		private JCheckBox filterByLengthBox;
		private JTextField minLengthField;
		private JTextField maxLengthField;

		private JCheckBox shiftReadsBox;
		private JTextField shiftOffsetForward;
		private JTextField shiftOffsetReverse;


		private JCheckBox downsampleBox;
		private JTextField downsampleTargetField;

		private JCheckBox extractCentresBox;
		private JTextField centreExtractContextField;

		public VisibleStoresOptionsPanel () {
			setLayout(new BorderLayout());

			JPanel commonOptions = new JPanel();

			commonOptions.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx = 0.5;
			gbc.weighty = 0.5;
			gbc.fill=GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(5,5,5,5);

			commonOptions.add(new JLabel("Remove duplicate reads"),gbc);
			JPanel dupPanel = new JPanel();
			removeDuplicates = new JComboBox(new String [] {"No","Yes, based on start", "Yes, based on end", "Yes, start and end"});
			dupPanel.add(removeDuplicates);
			gbc.gridx=3;
			commonOptions.add(dupPanel,gbc);

			gbc.gridx=1;
			gbc.gridy++;

			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Reverse all reads"),gbc);
			gbc.gridx=2;
			reverseReads = new JCheckBox();
			commonOptions.add(reverseReads,gbc);

			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Remove strand information"),gbc);
			gbc.gridx=2;
			removeStrandInfo = new JCheckBox();
			commonOptions.add(removeStrandInfo,gbc);

			reverseReads.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					removeStrandInfo.setEnabled(!reverseReads.isSelected());
				}
			});

			removeStrandInfo.addActionListener(new ActionListener() {		
				public void actionPerformed(ActionEvent e) {
					reverseReads.setEnabled(!removeStrandInfo.isSelected());
				}
			});


			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Filter by strand"),gbc);
			gbc.gridx=2;

			filterStrandCheckbox = new JCheckBox();
			commonOptions.add(filterStrandCheckbox,gbc);

			JPanel directionPanel = new JPanel();

			strandFilterDirectionBox = new JComboBox(new String [] {"Forward","Reverse","Forward or Reverse","Unknown"});
			strandFilterDirectionBox.setEnabled(false);
			strandFilterDirectionBox.setPrototypeDisplayValue("No longer than this please");

			filterStrandCheckbox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					if (filterStrandCheckbox.isSelected()) {
						strandFilterDirectionBox.setEnabled(true);
					}
					else {
						strandFilterDirectionBox.setEnabled(false);
					}
				}
			});
			directionPanel.add(new JLabel("Keep only "));
			directionPanel.add(strandFilterDirectionBox);
			gbc.gridx=3;
			commonOptions.add(directionPanel,gbc);


			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Filter by feature"),gbc);
			gbc.gridx=2;

			filterFeatureCheckbox = new JCheckBox();
			commonOptions.add(filterFeatureCheckbox,gbc);

			JPanel featurePanel = new JPanel();

			featureFilterTypeBox = new JComboBox(new String [] {"Overlapping","Excluding"});
			featureFilterTypeBox.setEnabled(false);
			filterFeatureBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
			filterFeatureBox.setPrototypeDisplayValue("No longer than this please");
			filterFeatureBox.setEnabled(false);


			filterFeatureCheckbox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					if (filterFeatureCheckbox.isSelected()) {
						filterFeatureBox.setEnabled(true);
						featureFilterTypeBox.setEnabled(true);
					}
					else {
						filterFeatureBox.setEnabled(false);
						featureFilterTypeBox.setEnabled(false);
					}
				}
			});
			featurePanel.add(featureFilterTypeBox);
			featurePanel.add(filterFeatureBox);
			gbc.gridx=3;
			commonOptions.add(featurePanel,gbc);

			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Filter by read length"),gbc);
			gbc.gridx=2;
			filterByLengthBox = new JCheckBox();
			commonOptions.add(filterByLengthBox,gbc);

			JPanel lengthFilterPanel = new JPanel();
			minLengthField = new JTextField(4);
			minLengthField.addKeyListener(new NumberKeyListener(false, false));
			minLengthField.setEnabled(false);
			maxLengthField = new JTextField(4);
			maxLengthField.addKeyListener(new NumberKeyListener(false, false));
			maxLengthField.setEnabled(false);

			filterByLengthBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent ae) {
					if (filterByLengthBox.isSelected()) {
						minLengthField.setEnabled(true);
						maxLengthField.setEnabled(true);
					}
					else {
						minLengthField.setEnabled(false);
						maxLengthField.setEnabled(false);
					}
				}
			});
			lengthFilterPanel.add(new JLabel("From"));
			lengthFilterPanel.add(minLengthField);
			lengthFilterPanel.add(new JLabel("bp to"));
			lengthFilterPanel.add(maxLengthField);			
			lengthFilterPanel.add(new JLabel("bp"));

			if (isHiC != null) {
				isHiC.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						if (isHiC()) {
							filterByLengthBox.setSelected(false);
							filterByLengthBox.setEnabled(false);
						}
						else {
							filterByLengthBox.setEnabled(true);
						}
					}
				});
			}

			gbc.gridx=3;
			commonOptions.add(lengthFilterPanel,gbc);


			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Shift Reads"),gbc);
			gbc.gridx=2;
			shiftReadsBox = new JCheckBox();
			commonOptions.add(shiftReadsBox,gbc);

			JPanel shiftReadsPanel = new JPanel();
			shiftOffsetForward = new JTextField("5",4);
			shiftOffsetForward.addKeyListener(new NumberKeyListener(false, true));
			shiftOffsetForward.setEnabled(false);
			shiftOffsetReverse = new JTextField("4",4);
			shiftOffsetReverse.addKeyListener(new NumberKeyListener(false, true));
			shiftOffsetReverse.setEnabled(false);

			shiftReadsBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent ae) {
					if (shiftReadsBox.isSelected()) {
						shiftOffsetForward.setEnabled(true);
						shiftOffsetReverse.setEnabled(true);
					}
					else {
						shiftOffsetForward.setEnabled(false);
						shiftOffsetReverse.setEnabled(false);
					}
				}
			});
			shiftReadsPanel.add(new JLabel("For offset"));
			shiftReadsPanel.add(shiftOffsetForward);
			shiftReadsPanel.add(new JLabel("bp. Rev offset"));
			shiftReadsPanel.add(shiftOffsetReverse);			
			shiftReadsPanel.add(new JLabel("bp."));

			gbc.gridx=3;
			commonOptions.add(shiftReadsPanel,gbc);

			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Extract Read Centres"),gbc);
			gbc.gridx=2;
			extractCentresBox = new JCheckBox();
			commonOptions.add(extractCentresBox,gbc);

			JPanel extractCentresPanel = new JPanel();
			centreExtractContextField = new JTextField(6);
			centreExtractContextField.setEnabled(false);
			centreExtractContextField.addKeyListener(new NumberKeyListener(false, false));

			extractCentresBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent ae) {
					if (extractCentresBox.isSelected()) {
						centreExtractContextField.setEnabled(true);
					}
					else {
						centreExtractContextField.setEnabled(false);
					}
				}
			});
			extractCentresPanel.add(new JLabel("Context around centre (bp) "));
			extractCentresPanel.add(centreExtractContextField);

			gbc.gridx=3;
			commonOptions.add(extractCentresPanel,gbc);


			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Downsample data"),gbc);
			gbc.gridx=2;
			downsampleBox = new JCheckBox();
			commonOptions.add(downsampleBox,gbc);

			JPanel downsamplePanel = new JPanel();
			downsampleTargetField = new JTextField(6);
			downsampleTargetField.setEnabled(false);
			downsampleTargetField.addKeyListener(new NumberKeyListener(false, false));

			downsampleBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent ae) {
					if (downsampleBox.isSelected()) {
						downsampleTargetField.setEnabled(true);
					}
					else {
						downsampleTargetField.setEnabled(false);
					}
				}
			});
			downsamplePanel.add(new JLabel("Target read count "));
			downsamplePanel.add(downsampleTargetField);

			gbc.gridx=3;
			commonOptions.add(downsamplePanel,gbc);




			add(commonOptions,BorderLayout.NORTH);

			singleEndOptions = new JPanel();
			singleEndOptions.setLayout(new GridBagLayout());

			gbc.gridx=1;
			gbc.gridy=1;

			singleEndOptions.add(new JLabel("Extend reads by (bp)"),gbc);
			gbc.gridx=2;
			extendReads = new JTextField(6);
			extendReads.addKeyListener(new NumberKeyListener(false, true));
			singleEndOptions.add(extendReads,gbc);

			add(singleEndOptions,BorderLayout.SOUTH);


		}

		
		public String getImportOptionsDescription () {
			StringBuffer sb = new StringBuffer();
			
			sb.append("Visible stores parser");

			if (filterByStrand) {
				sb.append(" Keep only strand ");
				sb.append(strandFilterDirectionBox.getSelectedItem());
			}
			
			if (filterByFeature) {
				sb.append(" Filter by feature ");
				sb.append(featureFilterTypeBox.getSelectedItem());
				sb.append(" ");
				sb.append(filterFeatureBox.getSelectedItem());
			}
			
			if (filterByLength) {
				sb.append(" Filter by read length from ");
				sb.append(minLengthField.getText());
				sb.append(" to ");
				sb.append(maxLengthField.getText());
			}
			
			sb.append(" Remove Duplicates");
			sb.append(removeDuplicates.getSelectedItem());
			
			if (shiftReadsBox.isSelected()) {
				sb.append(" Shift reads for=");
				sb.append(shiftOffsetForward.getText());
				sb.append(" rev=");
				sb.append(shiftOffsetReverse.getText());
			}
			
			
			if (extractCentres) {
				sb.append(" Extract centres +/- ");
				sb.append(centreExtractContext());				
			}
			
			if (downsample) {
				sb.append(" Downsample to approx ");
				sb.append(downsampleTargetField.getText());
			}
			
			if (extendReads() != 0) {
				sb.append(" Extend by ");
				sb.append(extendReads());
			}
			
			if (isHiC()) {
				sb.append("HiC distance ");
				sb.append(hiCDistance());
				
				if (hiCIgnoreTrans()) {
					sb.append(" Ignoring trans ditags");
				}
			}

			
			return sb.toString();
		}
		
		public boolean isHiC () {
			// isHiC only exists if this datastore is HiC to start
			// with, so it has to exist and be selected.
			return isHiC != null && isHiC.isSelected();
		}

		public int hiCDistance () {
			if (hiCDistance.getText().length()==0) return 0;
			else {
				return Integer.parseInt(hiCDistance.getText());
			}
		}

		public boolean hiCIgnoreTrans () {
			return hiCIgnoreTransBox.isSelected();
		}

		public int removeDuplicates () {
			if (removeDuplicates.getSelectedItem().equals("No")) {
				return DataSet.DUPLICATES_REMOVE_NO;
			}
			else if (removeDuplicates.getSelectedItem().equals("Yes, based on start")) {
				return DataSet.DUPLICATES_REMOVE_START;
			}
			else if (removeDuplicates.getSelectedItem().equals("Yes, based on end")) {
				return DataSet.DUPLICATES_REMOVE_END;
			}
			else if (removeDuplicates.getSelectedItem().equals("Yes, start and end")) {
				return DataSet.DUPLICATES_REMOVE_START_END;
			}

			throw new IllegalStateException("Didn't understand duplicate string "+removeDuplicates.getSelectedItem());
		}


		public boolean reverseReads () {
			return reverseReads.isSelected();
		}

		public boolean removeStrandInfo () {
			return removeStrandInfo.isSelected();
		}

		public int extendReads () {
			if (extendReads == null || extendReads.getText().length()==0) {
				return 0;
			}
			return Integer.parseInt(extendReads.getText());
		}

		public int forwardOffset () {
			if (shiftReadsBox.isSelected()) {
				if (shiftOffsetForward.getText().length() > 0) {
					return Integer.parseInt(shiftOffsetForward.getText());
				}
				return 0;
			}
			else return 0;
		}


		public int reverseOffset () {
			if (shiftReadsBox.isSelected()) {
				if (shiftOffsetReverse.getText().length() > 0) {
					return Integer.parseInt(shiftOffsetReverse.getText());
				}
				return 0;
			}
			else return 0;
		}


		public boolean extractCentres () {
			return extractCentresBox.isSelected();
		}

		public int centreExtractContext () {
			if (centreExtractContextField.getText().length() == 0) {
				return 0;
			}
			return Integer.parseInt(centreExtractContextField.getText());
		}


		public Dimension getPreferredSize () {
			return new Dimension(600,500);
		}

		public void actionPerformed(ActionEvent ae) {

			if (ae.getSource() == isHiC) {
				hiCDistance.setEnabled(isHiC());
				hiCIgnoreTransBox.setEnabled(isHiC());
			}
		}
	}
}
