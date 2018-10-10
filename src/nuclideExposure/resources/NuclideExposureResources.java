package nuclideExposure.resources;

import java.util.ListResourceBundle;

/**
 * The resource bundle for NuclideExposure class. <br>
 * 
 * @author Dan Fulea, 20 Jun. 2011
 * 
 */
public class NuclideExposureResources extends ListResourceBundle {
	/**
	 * Returns the array of strings in the resource bundle.
	 * 
	 * @return the resources.
	 */
	public Object[][] getContents() {
		// TODO Auto-generated method stub
		return CONTENTS;
	}

	/** The resources to be localised. */
	private static final Object[][] CONTENTS = {

			// displayed images..
			{ "form.icon.url", "/danfulea/resources/personal.png" },//jdf/resources/duke.png" },
			{ "icon.url", "/danfulea/resources/personal.png" },///jdf/resources/globe.gif" },

			{ "img.chart.bar", "/danfulea/resources/chart_bar.png" },
			{ "img.chart.curve", "/danfulea/resources/chart_curve.png" },
			
			{ "img.zoom.in", "/danfulea/resources/zoom_in.png" },
			{ "img.zoom.out", "/danfulea/resources/zoom_out.png" },
			{ "img.pan.left", "/danfulea/resources/arrow_left.png" },
			{ "img.pan.up", "/danfulea/resources/arrow_up.png" },
			{ "img.pan.down", "/danfulea/resources/arrow_down.png" },
			{ "img.pan.right", "/danfulea/resources/arrow_right.png" },
			{ "img.pan.refresh", "/danfulea/resources/arrow_refresh.png" },

			{ "img.accept", "/danfulea/resources/accept.png" },
			{ "img.insert", "/danfulea/resources/add.png" },
			{ "img.delete", "/danfulea/resources/delete.png" },
			{ "img.delete.all", "/danfulea/resources/bin_empty.png" },
			{ "img.view", "/danfulea/resources/eye.png" },
			{ "img.set", "/danfulea/resources/cog.png" },
			{ "img.report", "/danfulea/resources/document_prepare.png" },
			{ "img.today", "/danfulea/resources/time.png" },
			{ "img.open.file", "/danfulea/resources/open_folder.png" },
			{ "img.open.database", "/danfulea/resources/database_connect.png" },
			{ "img.save.database", "/danfulea/resources/database_save.png" },
			{ "img.substract.bkg", "/danfulea/resources/database_go.png" },
			{ "img.close", "/danfulea/resources/cross.png" },
			{ "img.about", "/danfulea/resources/information.png" },
			{ "img.printer", "/danfulea/resources/printer.png" },

			{ "Application.NAME", "NEDS - Nuclide exposures, dosimetry and shielding" },
			{ "About.NAME", "About" },
			{ "ViewGraph.NAME", "Decay plot" },

			{ "Author", "Author:" },
			{ "Author.name", "Dan Fulea , fulea.dan@gmail.com" },

			{ "Version", "Version:" },
			{ "Version.name", "NEDS 1.0" },

			{"License",	
				//"This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License (version 2) as published by the Free Software Foundation. \n\nThis program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. \n" },
			"Copyright (c) 2014, Dan Fulea \nAll rights reserved.\n\nRedistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:\n\n1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.\n\n2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.\n\n3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.\n\nTHIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 'AS IS' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n" },

			{ "menu.file", "File" },
			{ "menu.file.mnemonic", new Character('F') },

			{ "menu.file.exit", "Close" },
			{ "menu.file.exit.mnemonic", new Character('C') },
			{ "menu.file.exit.toolTip", "Close the application" },

			{ "menu.help", "Help" },
			{ "menu.help.mnemonic", new Character('H') },

			{ "menu.help.about", "About..." },
			{ "menu.help.about.mnemonic", new Character('A') },
			{ "menu.help.about.toolTip",
					"Several informations about this application" },

			{ "menu.help.howTo", "How to..." },
			{ "menu.help.howTo.mnemonic", new Character('H') },
			{ "menu.help.howTo.toolTip",
					"Several hints and tips about this application" },

			{ "menu.help.LF", "Look and feel..." },
			{ "menu.help.LF.mnemonic", new Character('L') },
			{ "menu.help.LF.toolTip", "Change application look and feel" },

			{ "library.master.dbCb", new String[] { "ICRP38", "JAERI03" } },
			{ "library.master.db", "ICRP38" },
			{ "library.master.db.indexTable", "ICRP38Index" },
			{ "library.master.db.radTable", "icrp38Rad" },

			{ "library.master.jaeri.db", "JAERI03" },
			{ "library.master.jaeri.db.indexTable", "JAERI03index" },
			{ "library.master.jaeri.db.radTable", "jaeri03Rad" },

			{ "internal.db", "InternalExposure" },
			{ "internal.db.public.inhalationAdultTable", "InhalationAdult" },
			
			{ "internal.db.public.inhalation1Table", "Inhalation1" },
			{ "internal.db.public.inhalation10Table", "Inhalation10" },
			
			{ "internal.db.public.ingestionAdultTable", "IngestionAdult" },
			
			{ "internal.db.public.ingestion1Table", "Ingestion1" },
			{ "internal.db.public.ingestion10Table", "Ingestion10" },
			
			{ "external.db", "ExternalExposure" },
			{ "external.db.waterTable", "WaterSubmersion" },
			{ "external.db.airTable", "AirSubmersion" },
			{ "external.db.groundSurfaceTable", "GroundSurface" },
			{ "external.db.infiniteSoilTable", "InfiniteSoil" },
			
			{ "dataTab", "Data" },
			{ "resultsTab", "Results" },
			
			{ "library.master.dbLabel", "Database: " },
			{ "library.inuse.hluCb",
				new String[] { "y", "d", "h", "m", "s", "ms", "us" } },
				
			{ "parentNuclideLb", "Parent nuclide: " },
			{ "parentCh", "Use all daughters" },
			{ "excelCh", "Save all results in .xls files (check the application FOLDER)!" },
			{ "activityTf", "Parent activity: " },
			{ "activityTf.Lb", "[Bq/m^3] for external exposure or [Bq] for internal exposure." },
			
			{ "library.sampleTimeLb", "Exposure time: " },
			{ "library.sampleTimeUnitLb", "units: " },
			{ "library.a_secRb", "Parent-daughter SECULAR equilibrum. Activities are constant during exposure time." },
			{ "library.a_afterRb", "No daughter activities at the begining of exposure. TIME-INTEGRATED activities are used in calculations." },
			{ "library.a_eqRb", "Parent-daughter EQUILIBRUM computed at: " },
			{ "library.a_eqRb.Lb", " x parent half life. Activities are constant during exposure time." },
			{ "library.a_secRb.toolTip", "Parent-daughter secular equilibrum. Activities are constant during exposure time." },
			{ "library.a_afterRb.toolTip", "No daughter activities at the begining of exposure. Activities follows natural decay law. Time-integrated activities are used in calculations." },
			{ "library.a_eqRb.toolTip",	"Parent-daughter equilibrum computed at 10(default) x parent half life. Activities are constant during exposure time." },
			{ "decay.border", "Decay options" },
			
			{ "threshold.border", "Thresholds" },
			{ "library.threshold.all", "All radiations" },
			{ "library.threshold.cut1", "Cutoff rad. yield <1%" },
			{ "library.threshold.cut2", "Cutoff rad. yield <2%" },
			{ "library.threshold.cut5", "Cutoff rad. yield <5%" },
			{ "library.threshold.cut10", "Cutoff rad. yield <10%" },
			{ "library.threshold.cut20", "Cutoff rad. yield <20%" },
			
			{ "e_airRb", "External air submersion" },
			{ "e_waterRb", "External water submersion" },
			{ "e_gsRb", "External ground surface" },
			{ "e_isRb", "External infinite soil" },
			{ "i_ingRb", "Internal ingestion" },
			{ "i_inhRb", "Internal inhalation" },
			
			{ "y1Rb", "1 yr" },
			{ "y10Rb", "10 yr" },
			{ "adultRb", "adult" },
			{ "age.border", "Age" },
			
			{ "rad_allRb", "Display ALL radiations" },
			{ "rad_gammaRb", "Display gamma photons" },
			{ "rad_xrayRb", "Display X-ray photons" },
			{ "rad_annihRb", "Display annihilation quanta" },
			{ "rad_betapRb", "Display beta plus electrons" },
			{ "rad_betanRb", "Display beta minus electrons" },
			{ "rad_iceRb", "Display internal conversion electrons" },
			{ "rad_augerRb", "Display Auger electrons" },
			{ "rad_alphaRb", "Display alpha particles" },
			{ "rad_photonsRb", "Display all photons. Dosimetry and shielding" },
			{ "rad_electronsRb", "Display all electrons" },
			
			{ "data.load", "Data" },
			{ "data.shielding", "Shielding" },
			{ "data.shielding.buildUpIndex", "buildup_material_index.xls" },
			{ "data.shielding.coeff", "coeff.xls" },
			{ "data.shielding.buildup", "buildup.xls" },
			
			{ "distanceTf", "Distance [cm]: " },
			{ "thicknessTf", "Thickness [cm]: " },
			{ "shieldCb", "Shield: " },
			{ "hvlCh", " HVL and TVL evaluation " },
			{ "radiationType.border", "Radiation Type. Dosimetry and shielding" },
			
			{ "energyTf", "Main energy [MeV]: " },
			{ "measuredDoseRateTf", "Measured dose rate [uSv/h]: " },
			{ "desiredDoseRateTf", "Desired dose rate [uSv/h]: " },
			{ "quickEvaluation.border", "Quick evaluation" },
			{ "auxData.border", "Auxiliar data" },
			
			{ "graph.button", "Plot" },
			{ "graph.button.mnemonic", new Character('P') },
			{ "graph.button.toolTip", "View decay chart." },
			
			{ "stop.button", "Stop computations" },
			{ "stop.button.mnemonic", new Character('S') },
			{ "stop.button.toolTip", "Stops all calculations!" },
					
			{"computeTime.button", "Compute time" },
			{ "computeTime.button.mnemonic", new Character('C') },
			{ "computeTime.button.toolTip", "Compute time based on time-steps" },
			{"chart.x", "TimeSteps [t/delta_t]; delta_t[sec]= " },
			{"chart.y", "Activity [Bq]" },
			{"dot.series", "2nd nuclide lead points" },
			{"chart.name", "Chain decay" },
			{"time.steps.label", "Time steps: " },
			{"results.label", "Results: " },
			
			{"decay.idealEq.", "Theoretical time elapsed for ideal equilibrum between parent and 1st daughter [sec]= " },
			{"decay.actualEq.", "Time elapsed for secular equilibrum between parent and 1st daughter is computed when differences between activities falls below 1%." },
			{"decay.activity", "Activity[Bq]= " },
			{"decay.after", "; after: " },
			{"decay.sec", " sec.; " },
			{"decay.h", " h.; " },
			{"decay.days", " days; " },
			{"decay.timeElapsed", "Time elapsed = " },
			{"decay.parent.activity", "Parent activity [Bq]= " },
			{"decay.1stdaughter.activity", " ; 1st daughter activity [Bq]= " },
				
			{ "calculate.button", "Calculate" },
			{ "calculate.button.mnemonic", new Character('C') },
			{ "calculate.button.toolTip", "Perform parent-daughter nuclide calculations" },
			{ "exposure.border", "Exposure options" },
			
			{ "display.button", "Display" },
			{ "display.button.mnemonic", new Character('D') },
			{ "display.button.toolTip", "Display radiations related to parent-daughter nuclides chain" },
			
			{ "evaluate.button", "Evaluate" },
			{ "evaluate.button.mnemonic", new Character('E') },
			{ "evaluate.button.toolTip", "Quick evaluation of shield thickness required for certain dose rate reduction!" },

			{ "status.wait", "Waiting your action!" },
			{ "status.computing", "Computing..." },
			{ "status.done", "Done! " },
			
			{ "number.error", "Insert valid positive numbers! " },
			{ "number.error.title", "Unexpected error" },
			{ "number.error2", "Insert valid positive numbers! Date values must be integers!" },
			{ "number.error.title2", "Unexpected error" },
			
			{ "dialog.exit.title", "Confirm..." },
			{ "dialog.exit.message", "Are you sure?" },
			{ "dialog.exit.buttons", new Object[] { "Yes", "No" } },			

			{ "radiation.x", "X" },
			{ "radiation.gamma", "Gamma" },
			{ "radiation.annihilationQuanta", "Annih." },
			{ "radiation.betap", "Beta+ particle" },
			{ "radiation.betan", "Beta- particle" },
			{ "radiation.ice", "Internal conversion electron" },
			{ "radiation.auger", "Auger electron" },
			{ "radiation.alpha", "Alpha particle" },

			{ "text.nuclide", "Nuclide: " },
			{ "text.halfLife", "Half life: " },
			{ "text.atomicMass", "Atomic mass: " },
			{ "text.gammaConstant", "Gamma constant [Gy X m^2/(Bq X s)]: " },
			{ "text.gammaConstant1", "Gamma constant: " },
			{ "text.gammaConstant2", "Gy X m^2/(Bq X s): " },
			{ "text.mainDecay", "Main decay: " },
			{ "text.chainDecayInfo", "Chain decay informations: " },
			{ "text.branchingRatio", "Branching ratio: " },
			{ "text.halfLifeUnits", "Half life units: " },
			{ "text.activity", "A [Bq]: " },
			{ "text.activity.atTime", "at time [s]: " },
			{ "text.activityEquilibrum", "A_equilibrum [Bq]: " },
			{ "text.integral", "Integral [Bq x s]: " },
			{ "text.activitySecular", "A_secular [Bq]: " },
			{ "text.activitySample", "A_afterTime [Bq]: " },
			{ "text.integral2", "Time-integrated A [Bq x s]: " },
			
			{ "text.desiredDoseRate", "Desired dose rate verification [arbitrary units]: " },
			{ "text.desiredDoseRate2"," at distance: " },
			{ "text.desiredDoseRate3"," cm" },
			{ "text.requiredShield","; Required shield thickness [cm]: " },
			
			{ "text.dose.total", "Total absorbed dose (sum over all organ doses) [Gy]: " },
			{ "text.dose", "; Dose [Gy or Sv]: " },
			{ "text.dose.eff", "Effective dose (Total) [Sv]= " },
			{ "text.dose.thyroid", "Equivalent dose in thyroid (Total) [Sv]= " },
			//{ "text.fatal.cases", "; Cases of fatal cancers/1 million population = " },
			{ "text.fatal.cases", "Cases of fatal cancers/1 million population = " },
			
			{ "xls.chains.br", "Chains and nuclide branching ratios (BR)" },
			{ "xls.chains.nr", "Chain #" },
			{ "xls.br", "BR" },
			{ "xls.activChainsAtTime", "Activity of radioactive chains computed at EXPOSURE TIME " },
			{ "xls.secularInfo", "Overall forced activity at secular equilibrum is computed as parent activity [1Bq] times relative yield. THIS CAN BE NEVER REACHED!!" },
			{ "xls.noDaughtersInfo", "Overall activity is resulted from decay chains at given EXPOSURE TIME, assuming the parent activity is 1 Bq at t=0 and has NO daughters at t=0." },
			{ "xls.integralInfo", "Overall time integrated activity is computed during exposure time, assuming the parent activity is 1 Bq at t=0 and has NO daughters at t=0." },
			{ "xls.eqInfo", "Overall activity at equilibrum is normalised to parent activity and the calculations were performed at a fixed elapsed time of 10 (default) x PARENT_HALF_LIFE. At this time the equilibrum (if any) is considered to be reached!" },
			{ "xls.overallFinal", "Overall activities (final results): " },
			{ "xls.organ", "Organ" },
			{ "xls.estimation.case", "Estimation performed in case of: " },
			
			{ "BladderWall", "Bladder Wall" },
			{ "StomachWall", "Stomach Wall" },
			{ "SmallIntestineWall", "Small Intestine Wall" },
			{ "UpperLargeIntestineWall", "Upper Large Intestine Wall" },
			{ "LowerLargeIntestineWall", "Lower Large Intestine Wall" },
			
			{ "Adrenals", "Adrenals" },
			{ "UrinaryBladder", "Urinary Bladder" },
			{ "BoneSurface", "Bone Surface" },
			{ "Brain", "Brain" },
			{ "Breast", "Breast" },
			{ "Esophagus", "Esophagus" },
			{ "Stomach", "Stomach" },
			{ "SmallIntestine", "Small Intestine" },
			{ "UpperLargeIntestine", "Upper Large Intestine" },
			{ "LowerLargeIntestine", "Lower Large Intestine" },
			{ "Colon", "Colon" },
			{ "Kidneys", "Kidneys" },
			{ "Liver", "Liver" },
			{ "Muscle", "Muscle" },
			{ "Ovaries", "Ovaries" },
			{ "Pancreas", "Pancreas" },
			{ "RedMarrow", "Red Marrow" },
			{ "ExtratrachialAirways", "Extratrachial Airways" },
			{ "Lungs", "Lungs" },
			{ "Skin", "Skin" },
			{ "Spleen", "Spleen" },
			{ "Testes", "Testes" },
			{ "Thymus", "Thymus" },
			{ "Thyroid", "Thyroid" },
			{ "Uterus", "Uterus" },
			{ "Remainder", "Remainder" },
			{ "Effectivedose", "Effective dose" },
			
			{ "Dosimetry", "Dosimetric calculations based on parent (input) nuclide activity:" },
			
			{ "calculation.required", "Perform nuclide calculations first!" },
			{ "text.radiation.type", "Radiation type: " },	
			
			{ "text.radiation.yield", "Yield: " },
			{ "text.radiation.energy", "Energy: " },
			{ "text.radiation.energy1", "Energy[MeV]: " },
			{ "text.radiation.energy2", "[MeV]" },
			{ "text.radiation.from", "Radiation from: " },	
			
			{ "text.dataFor", "Data for: " },
			{ "text.tissueDose", " Tissue dose [uSv]: " },
			{ "text.tissueDose.total", " Total tissue dose [uSv]: " },
			
			{ "text.tissueDoseRate", " Tissue dose rate [uSv/h]: " },
			{ "text.tissueDoseRate.total", " Total tissue dose rate [uSv.h]: " },
			
			{ "text.for", " for " },
			{ "text.of", " of " },
			{ "text.BqAtDistance", " Bq, at distance " },
			{ "text.cm", " cm " },
			{ "text.shieldedWith", " shielded with " },
			{ "text.ofThickness", " of thickness " },
			{ "text.hvlEstimation", "HVL dose estimation: " },
			{ "text.hvlEstimation1", " uSv/hr; HVL: " },
			{ "text.tvlEstimation", "TVL dose estimation: " },
			{ "text.tvlEstimation1", " uSv/hr; TVL: " },
			
			{ "text.simulation.stop", "Interrupred by user!" },
			
			{ "HowTo.title", "Hints and tips" },
			{ "HowTo",				
					"This software was developped (and tested) on LINUX (Linux Mint 11 Katya) and WINDOWS \n"
					+ "(Windows 7). Due to the Java portability, NO major glitches for running under MAC OS are expected!" },		

	};
}
