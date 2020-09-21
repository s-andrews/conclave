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
package uk.ac.babraham.conclave.Utilities.ImageSaver;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;


/**
 * Implements the Tranferable interface for a BufferedImage so
 * we can copy it to the clipboard from the ImageSaver.
 * 
 * @author Andrewss
 *
 */

public class TransferrableImage implements Transferable {

	private BufferedImage b;
	
	public TransferrableImage (BufferedImage b) {
		this.b = b;
	}
	
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return DataFlavor.imageFlavor.equals(flavor);
	}
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor [] {DataFlavor.imageFlavor};
	}
	
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (isDataFlavorSupported(flavor)) {
			return b;
		}
		throw new UnsupportedFlavorException(flavor);
	}

}
