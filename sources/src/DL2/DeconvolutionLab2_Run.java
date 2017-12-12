package DL2;/*
 * DeconvolutionLab2
 * 
 * Conditions of use: You are free to use this software for research or
 * educational purposes. In addition, we expect you to include adequate
 * citations and acknowledgments whenever you present or publish results that
 * are based on it.
 * 
 * Reference: DeconvolutionLab2: An Open-Source Software for Deconvolution
 * Microscopy D. Sage, L. Donati, F. Soulez, D. Fortun, G. Schmit, A. Seitz,
 * R. Guiet, C. Vonesch, M Unser, Methods of Elsevier, 2017.
 */

/*
 * Copyright 2010-2017 Biomedical Imaging Group at the EPFL.
 * 
 * This file is part of DeconvolutionLab2 (DL2).
 * 
 * DL2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * DL2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * DL2. If not, see <http://www.gnu.org/licenses/>.
 */

import DL2.deconvolution.Deconvolution;
import DL2.deconvolutionlab.Imager;
import DL2.deconvolutionlab.Lab;
import DL2.deconvolutionlab.LabDialog;
import ij.IJ;
import ij.Macro;
import ij.plugin.PlugIn;

import java.io.File;

public class DeconvolutionLab2_Run implements PlugIn {

	@Override
	public void run(String arg) {
		Lab.init(Imager.Platform.IMAGEJ, IJ.getDirectory("plugins") + File.separator + "DeconvolutionLab2.config");
		if (Macro.getOptions() == null) {
			LabDialog dlg = new LabDialog();
			Lab.setVisible(dlg, false);
		}
		else
			new Deconvolution("Run", Macro.getOptions()).deconvolve();
	}
}
