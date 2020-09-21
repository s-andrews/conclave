/**
 * Copyright 2019 Simon Andrews
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


package uk.ac.babraham.conclave.Displays.DesignEditor;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import uk.ac.babraham.conclave.ConclaveApplication;
import uk.ac.babraham.conclave.DataTypes.ReplicateSet;
import uk.ac.babraham.conclave.Dialogs.ReplicateSetSelector;

public class StatisticalDesignEditorDialog extends JDialog implements ActionListener {
	
	private StatisticalDesign design;
	
	public StatisticalDesignEditorDialog (StatisticalDesign design) {
		super(ConclaveApplication.getInstance(), "Statistical Design Editor");
		this.design = design;
		
		Container c = getContentPane();
		
		c.setLayout(new BorderLayout());
		
		JTable table = new JTable(design);
		
		c.add(new JScrollPane(table), BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton addButton = new JButton ("Add Cofactor");
		addButton.setActionCommand("add");
		addButton.addActionListener(this);
		buttonPanel.add(addButton);
		
		JButton removeButton = new JButton("Remove Cofactor");
		removeButton.setActionCommand("remove");
		removeButton.addActionListener(this);
		buttonPanel.add(removeButton);
		
		c.add(buttonPanel,BorderLayout.NORTH);
		
		
		JPanel finishedPanel = new JPanel();
		
		JButton finishedButton = new JButton("Finished");
		finishedButton.setActionCommand("finished");
		finishedButton.addActionListener(this);
		finishedPanel.add(finishedButton);
		
		c.add(finishedPanel,BorderLayout.SOUTH);
		
		setModal(true);
		setSize(300,400);
		setLocationRelativeTo(ConclaveApplication.getInstance());
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().contentEquals("finished")) {
			setVisible(true);
			dispose();
		}
		else if (ae.getActionCommand().contentEquals("remove")) {
			design.removeFactor();
		}
		else if (ae.getActionCommand().contentEquals("add")) {
			ReplicateSet [] factorSets = ReplicateSetSelector.selectReplicateSets();
			if (factorSets != null && factorSets.length > 1) {
				try {
					design.addFactor(factorSets);
				}
				catch(InvalidFactorException ife) {
					JOptionPane.showMessageDialog(this, "Couldn't add factor: "+ife.getLocalizedMessage(), "Invalid Factor", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}
	}

}
