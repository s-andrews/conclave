/**
 * Copyright 2014-19 Laura Biggins
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
package uk.ac.babraham.conclave.Filters.GeneSetFilter;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.JPanel;

import uk.ac.babraham.conclave.ConclaveException;
import uk.ac.babraham.conclave.DataTypes.DataStore;
import uk.ac.babraham.conclave.DataTypes.Genome.Location;
import uk.ac.babraham.conclave.DataTypes.Probes.Probe;
import uk.ac.babraham.conclave.DataTypes.Probes.ProbeList;
import uk.ac.babraham.conclave.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.conclave.Gradients.ColourGradient;
import uk.ac.babraham.conclave.Gradients.ColourIndexSet;
import uk.ac.babraham.conclave.Gradients.HotColdColourGradient;
import uk.ac.babraham.conclave.Preferences.DisplayPreferences;
import uk.ac.babraham.conclave.Utilities.AxisScale;

public class ZScoreScatterPlotPanel extends JPanel implements Runnable, MouseMotionListener, MouseListener {

	/** The x store. */
	private DataStore xStore;

	/** The y store. */
	private DataStore yStore;

	/** The probe list. */
	//private ProbeList probeList;

	/** The set of sublists to highlight */
	private ProbeList [] subLists;

	/** The difference value at which we started the current selection */
	private double diffStart = 0;

	/** The difference value at which we ended the current selection */
	private double diffEnd = 0;

	/** Whether we're making a selection. */
	private boolean makingSelection = false;

	/** Whether there is a selection made which we need to render (as opposed to an active one) */
	private boolean madeSelection = false;

	/** The min value x. */
	private double minValueX = 0;

	/** The max value x. */
	private double maxValueX = 1;

	/** The min value y. */
	private double minValueY = 0;

	/** The max value y. */
	private double maxValueY = 1;

	/** The ready to draw. */
	private boolean readyToDraw = false;

	/** The dot size. */
	private int dotSize = 1;

	/** The df. */
	private final DecimalFormat df = new DecimalFormat("#.###");

	/**  Some points which say where the cursor is currently located */
	private int cursorX = 0;
	private int cursorY = 0;

	/** The last calculated non-redundant set of probe values **/
	private ProbePairValue [] nonRedundantValues = null;

	/** The last image size for which we calculated a nonredundant set **/
	private int lastNonredWidth = 0;
	private int lastNonredHeight = 0;

	/** The probes and their z-scores */
	private Hashtable<Probe, Double>probeZScoreLookupTable;
	
	/**
	 * The last ProbePairValue we were near
	 */
	private ProbePairValue closestPoint = null;

	private Probe[] probes;

	private static final int X_AXIS_SPACE = 50;
	private static final int Y_AXIS_SPACE = 30;


	/**
	 * Instantiates a new scatter plot panel.
	 * 
	 * @param xStore the x store
	 * @param yStore the y store
	 * @param probeList the probe list
	 * @param commonScale the common scale
	 * @param dotSize the dot size
	 */
	public ZScoreScatterPlotPanel (DataStore xStore, DataStore yStore, Probe[] probes, ProbeList [] subLists, int dotSize, Hashtable<Probe, Double>probeZScoreLookupTable) {

		this.xStore = xStore;
		this.yStore = yStore;
		this.probes = probes;
		this.subLists = subLists;
		this.dotSize = dotSize;
		this.probeZScoreLookupTable = probeZScoreLookupTable; 
		
		
		addMouseListener(this);
		addMouseMotionListener(this);

		Thread t = new Thread(this);
		t.start();
	}

	/**
	 * Sets the dot size.
	 * 
	 * @param dotSize the new dot size
	 */
	public void setDotSize (int dotSize) {
		this.dotSize = dotSize;
		repaint();
	}

	public void setSubLists(ProbeList [] subLists){
		this.subLists = subLists;
		calculateNonredundantSet();
		repaint();
	}
	
	
	/**
	 * This collapses individual points which are over the same
	 * pixel when redrawing the plot at a different scale
	 */
	private synchronized void calculateNonredundantSet () {

		closestPoint = null;

		ProbePairValue [][] grid = new ProbePairValue [getWidth()][getHeight()];

		try {
			for (int p=0;p<probes.length;p++) {
				
				/* x value is the mean intensity */
				float xValue = (xStore.getValueForProbe(probes[p]) + yStore.getValueForProbe(probes[p]))/2;
				
				/* y value is the z-score */
				float yValue = probeZScoreLookupTable.get(probes[p]).floatValue();
				
				if (Float.isNaN(xValue)  || Float.isInfinite(xValue)  || Float.isNaN(yValue) || Float.isInfinite(yValue)) {
					continue;
				}
				
				int x = getX(xValue);
				int y = getY(yValue);
				if (grid[x][y] == null) {
					grid[x][y] = new ProbePairValue(xValue, yValue,x,y);
					grid[x][y].setProbe(probes[p]);
				}
				else {
					// We don't increment the count if we're plotting sublists
					// as we use the count field to indicate which sublist we
					// belong to
					if (subLists == null) grid[x][y].count++;
					
					// As we have multiple probes at this point we remove the 
					// specific probe annotation.
					grid[x][y].setProbe(null);
				}
			}


			// If we have subLists then we need to re-use the count values
			// in the grid to assign a value to indicate which sublist it
			// came from.

			//  We could improve this by doing something clever where values
			// from two lists share the same pixel, but for now we just let
			// the last one we see win.

			if (subLists != null) {

				for (int s=0;s<subLists.length;s++) {
					Probe [] subListProbes = subLists[s].getAllProbes();
					
					//System.err.println("no of probes in sublist = " + subListProbes.length);
					
					for (int p=0;p<subListProbes.length;p++) {
						
						// TODO: this needs to be sorted out properly - it's to do with the filtering out of NA probes
						if(probeZScoreLookupTable.containsKey(subListProbes[p])){
						
							//System.err.println("p = " + p + ", name = " + subListProbes[p].name());
							
							/* x value is the mean intensity */
							float xValue = (xStore.getValueForProbe(subListProbes[p]) + yStore.getValueForProbe(subListProbes[p]))/2;
							
							/* y value is the z-score */
							float yValue = probeZScoreLookupTable.get(subListProbes[p]).floatValue();
							
							//System.err.println("for " + probes[p].name() + ", xValue = " + xValue+ ", yValue = " + yValue);
							
							int x = getX(xValue);
							int y = getY(yValue);
							if (grid[x][y] == null) {
								// This messes up where we catch it in the middle of a redraw
								continue;
	//							throw new IllegalArgumentException("Found subList position not in main list");
							}
	
							grid[x][y].count = s+2; // 1 = no list so 2 is the lowest sublist index
						}
					}
				}

			}
		}

		catch (ConclaveException e) {
			throw new IllegalStateException(e);
		}

		// Now we need to put all of the ProbePairValues into
		// a single array;

		int count = 0;

		for (int x=0;x<grid.length;x++) {
			for (int y=0;y<grid[x].length;y++) {
				if (grid[x][y] != null) count++;
			}
		}

		ProbePairValue [] nonred = new ProbePairValue[count];
		count--;

		for (int x=0;x<grid.length;x++) {
			for (int y=0;y<grid[x].length;y++) {
				if (grid[x][y] != null) {
					nonred[count] = grid[x][y];
					count--;
				}
			}
		}

		Arrays.sort(nonred);

		// Work out the 95% percentile count
		int minCount = 1;
		int maxCount = 2;
		if (nonred.length > 0) {
			minCount = nonred[0].count;
			maxCount = nonred[((nonred.length-1) *95) / 100].count;
		}


		// Go through every nonred assigning a suitable colour
		ColourGradient gradient = new HotColdColourGradient();
		for (int i=0;i<nonred.length;i++) {
			
			if (subLists == null) {
				nonred[i].color = gradient.getColor(nonred[i].count, minCount, maxCount);
			}
			else {
				if (nonred[i].count > subLists.length+1) {
					throw new IllegalArgumentException("Count above threshold when showing sublists");
				}
				if (nonred[i].count == 1) {
					nonred[i].color = Color.LIGHT_GRAY;
				}
				else {
					nonred[i].color = ColourIndexSet.getColour(nonred[i].count-2);
				}
			}

		}

		nonRedundantValues = nonred;
		lastNonredWidth = getWidth();
		lastNonredHeight = getHeight();

	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint (Graphics g) {
		super.paint(g);
		g.setColor(Color.WHITE);

		g.fillRect(0, 0, getWidth(), getHeight());

		FontMetrics metrics = getFontMetrics(g.getFont());

		if (! readyToDraw) {
			g.setColor(Color.GRAY);
			String message = "Calculating Plot";
			g.drawString(message, (getWidth()/2)-(metrics.stringWidth(message)/2), (getHeight()/2-2));
			return;
		}

		// Check to see if we need to work out the counts for this size
		if (nonRedundantValues == null || lastNonredWidth != getWidth() || lastNonredHeight != getHeight()) {
			calculateNonredundantSet();
		}


		// If we're here then we can actually draw the graphs
		g.setColor(Color.BLACK);

		// X axis
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth()-10, getHeight()-Y_AXIS_SPACE);

		AxisScale xAxisScale = new AxisScale(minValueX, maxValueX);
		double currentXValue = xAxisScale.getStartingValue();
		while (currentXValue < maxValueX) {
			g.drawString(xAxisScale.format(currentXValue), getX(currentXValue), getHeight()-(Y_AXIS_SPACE-(3+g.getFontMetrics().getHeight())));
			g.drawLine(getX(currentXValue),getHeight()-Y_AXIS_SPACE,getX(currentXValue),getHeight()-(Y_AXIS_SPACE-3));
			currentXValue += xAxisScale.getInterval();
		}

		// Y axis
		g.drawLine(X_AXIS_SPACE, 10, X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE);

		// 		
		AxisScale yAxisScale = new AxisScale(minValueY, maxValueY);
		double currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < maxValueY) {
			g.drawString(yAxisScale.format(currentYValue), 5, getY(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(X_AXIS_SPACE,getY(currentYValue),X_AXIS_SPACE-3,getY(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}

		// X label
		g.drawString("mean probe intensity",(getWidth()/2)-(metrics.stringWidth("mean probe intensity")/2),getHeight()-3);		

		// Y label
		g.drawString("z-score",X_AXIS_SPACE-metrics.stringWidth("z-score")-3,15);
		
		g.setColor(Color.BLUE);

		for (int p=0;p<nonRedundantValues.length;p++) {

			if ((madeSelection||makingSelection) && nonRedundantValues[p].yValue >= Math.min(diffStart, diffEnd) && nonRedundantValues[p].yValue <= Math.max(diffStart, diffEnd)) {
				g.setColor(Color.BLACK);
			}
			else {

				g.setColor(nonRedundantValues[p].color);
			}

			g.fillRect(nonRedundantValues[p].x - (dotSize/2), nonRedundantValues[p].y-(dotSize/2), dotSize, dotSize);
		}

		// If we have a highlighted list, add the name
		if (subLists != null) {
			for (int s=0;s<subLists.length;s++) {
				g.setColor(ColourIndexSet.getColour(s));
				g.drawString(subLists[s].name(),getWidth()-metrics.stringWidth(subLists[s].name())-5,30+(g.getFontMetrics().getHeight()*(s+1)));
			}
			g.setColor(Color.BLACK);
		}
		
		
		// 0 line
		g.setColor(Color.GRAY);
		g.drawLine(X_AXIS_SPACE,getY(0),getWidth()-10,getY(0));
		
		// label the datasets
		// the negative z-scores
		g.drawString(xStore.name(),X_AXIS_SPACE+10,getHeight()-50);		

		// one with the positive z-scores
		g.drawString(yStore.name(),X_AXIS_SPACE+10,30);
		
		// Finally we draw the current measures if the mouse is inside the plot
		if (cursorX > 0) {
			g.setColor(Color.BLACK);
			//			System.out.println("Drawing label at x="+cursorX+" y="+cursorY+" x*="+getValueFromX(cursorX)+" y*="+getValueFromY(cursorY));

			String label = "x="+df.format(getValueFromX(cursorX))+" y="+df.format(getValueFromY(cursorY));
			int labelXPos = X_AXIS_SPACE+((getWidth()-(X_AXIS_SPACE+10))/2)-(g.getFontMetrics().stringWidth(label)/2);
			g.drawString(label, labelXPos, getHeight()-(Y_AXIS_SPACE+3));

			// We also draw the names on the closest point if there is one
			if (closestPoint != null && closestPoint.probe() != null) {
				g.drawString(closestPoint.probe().name(),closestPoint.x+1,closestPoint.y-1);
			}
		}

	}

	/**
	 * Gets the value from y.
	 * 
	 * @param y the y
	 * @return the value from y
	 */
	public double getValueFromY (int y) {
		double value = minValueY;
		value += (maxValueY-minValueY) * (((getHeight()-(10+Y_AXIS_SPACE)-(y-10d)) / (getHeight()-(10+Y_AXIS_SPACE))));
		return value;
	}

	/**
	 * Gets the value from x.
	 * 
	 * @param x the x
	 * @return the value from x
	 */
	public double getValueFromX (int x) {
		double value = minValueX;
		value += (maxValueX-minValueX) * ((x-X_AXIS_SPACE) / (double)(getWidth()-(10+X_AXIS_SPACE)));
		return value;
	}


	/**
	 * Gets the y.
	 * 
	 * @param value the value
	 * @return the y
	 */
	public int getY (double value) {
		double proportion = (value-minValueY)/(maxValueY-minValueY);

		int y = getHeight()-Y_AXIS_SPACE;

		y -= (int)((getHeight()-(10+Y_AXIS_SPACE))*proportion);

		return y;
	}

	/**
	 * Gets the x.
	 * 
	 * @param value the value
	 * @return the x
	 */
	public int getX (double value) {
		double proportion = (value-minValueX)/(maxValueX-minValueX);

		int x = X_AXIS_SPACE;

		x += (int)((getWidth()-(10+X_AXIS_SPACE))*proportion);

		return x;
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {


		// We need to find the mix/max counts, and get a non-overlapping
		// set of probes

		// If there aren't any probes there's no point going any further
		if (probes.length == 0) return;

		boolean someXValueSet = false;
		boolean someYValueSet = false;
		
		try {

			// We extract the data to allow the calculation of an r-value
			float [] xData = new float[probes.length];
			float [] yData = new float[probes.length];


			for (int p=0;p<probes.length;p++) {
								
				xData[p]= (xStore.getValueForProbe(probes[p]) + yStore.getValueForProbe(probes[p]))/2;
				
				/* y value is the z-score */
				yData[p]= probeZScoreLookupTable.get(probes[p]).floatValue();
				
				
				if (!someXValueSet && !(Float.isNaN(xData[p]) || Float.isInfinite(xData[p]))) {
					minValueX = xData[p];
					maxValueX = xData[p];
					someXValueSet = true;
				}
				if (!someYValueSet && !(Float.isNaN(yData[p]) || Float.isInfinite(yData[p]))) {
					minValueY = yData[p];
					maxValueY = yData[p];
					someYValueSet = true;
				}
				
				
				if (!(Float.isNaN(xData[p]) || Float.isInfinite(xData[p]))) {
					if (xData[p]<minValueX) minValueX = xData[p];
					if (xData[p]>maxValueX) maxValueX = xData[p];
				}
				
				if (!(Float.isNaN(yData[p]) || Float.isInfinite(yData[p]))) {
					if (yData[p]<minValueY) minValueY = yData[p];
					if (yData[p]>maxValueY) maxValueY = yData[p];
				}
			}

			//we want the y axis, the z-scores to be centred around 0
				
			if(maxValueY > Math.abs(minValueY)){
				
				minValueY = - maxValueY;
			}
			else if(Math.abs(minValueY) > maxValueY){
				
				maxValueY = Math.abs(minValueY);
			}	

			// Calculate the R-value
//			rValue = "R = "+df.format(PearsonCorrelation.calculateCorrelation(xData, yData));
			// Clear these arrays since we don't need them any more.
			xData = null;
			yData = null;

		}
		catch (ConclaveException e) {
			e.printStackTrace();
		}

		readyToDraw = true;
		repaint();

	}


	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {

		if (makingSelection) {
			// Set the end points
			int x = e.getX();
			int y = e.getY();

			if (x<10) x = 10;
			if (x>getWidth()-10) x = getWidth()-10;

			if (y<10) y=10;
			if (y>getHeight()-20) y = getHeight()-20;

			
			diffEnd = getValueFromY(y);
		}
		else {
			// Set the start points
			makingSelection = true;
			madeSelection = false;

			// Set the end points
			int x = e.getX();
			int y = e.getY();

			if (x<10) x = 10;
			if (x>getWidth()-10) x = getWidth()-10;

			if (y<10) y=10;
			if (y>getHeight()-20) y = getHeight()-20;

			makingSelection = true;
			
			diffStart = getValueFromY(y);
			
			diffEnd = diffStart;
		}

		repaint();

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {

		int x = e.getX();
		int y = e.getY();

		// We only care if we're inside the plot area
		if (x<10 || x>getWidth()-10 || y<10 || y>getHeight()-20) {
			cursorX = 0;
			closestPoint = null;
		}
		else {

			// Find out if we're near a ProbePairValue
			double closestDistance = 5;
			if (nonRedundantValues != null) {
				for (int i=0;i<nonRedundantValues.length;i++) {
					double distance = nonRedundantValues[i].getDistance(x, y);
					if (distance < closestDistance) {
						closestPoint = nonRedundantValues[i];
						closestDistance = distance;
					}
				}
				if (closestDistance == 5) {
					closestPoint = null;
				}
			}

			cursorX = x;
			cursorY = y;
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			if (closestPoint != null && closestPoint.probe() != null) {
				DisplayPreferences.getInstance().setLocation(closestPoint.probe().chromosome(),SequenceRead.packPosition(closestPoint.probe().start(),closestPoint.probe().end(),Location.UNKNOWN));
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
		if (madeSelection) {
			makingSelection = false;
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
		if (madeSelection) {
			madeSelection = false;
		}

		if (makingSelection) {
			makingSelection = false;

			if (diffStart != diffEnd) {
				madeSelection = true;
				//				System.out.println("New selection is "+selectionStartX+"-"+selectionEndX+" "+selectionStartY+"-"+selectionEndY);
			}
		}

		repaint();
	}

	/**
	 * The Class ProbePairValue.
	 */
	private class ProbePairValue implements Comparable<ProbePairValue> {

		/** The y value */
		public double yValue;

		/** The count. */
		public int count;

		/** The color. */
		public Color color = null;

		public Probe probe = null;

		/** The x,y positions */
		public int x = 0;
		public int y = 0;

		/**
		 * Instantiates a new probe pair value.
		 * 
		 * @param xValue the x value
		 * @param yValue the y value
		 * @param x the X position for this spot
		 * @param y the Y position for this spot
		 */
		public ProbePairValue (double xValue, double yValue, int x, int y) {
			this.yValue = yValue;
			this.x = x;
			this.y = y;
			this.count = 1;
		}

		public void setProbe (Probe probe) {
			this.probe = probe;
		}

		public Probe probe () {
			return probe;
		}

		public double getDistance (int x, int y) {
			return Math.sqrt(Math.pow(x-this.x, 2)+Math.pow(y-this.y,2));
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(ProbePairValue o) {
			return count - o.count;
		}

	}
}

