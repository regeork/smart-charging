/***
 *  Smart Charging Algorithms for Electric Vehicles considering Voltage and Thermal Constraints
    Copyright (C) 2019  David Kröger <david.kroeger@tu-dortmund.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package application;

public class VRFB {
	
	//Power charge and discharge
	private double aufladeLeistung = 0;
	private double entladeLeistung = 0;
	
	private double soc = 0;
	
	private double capacity = 0;
	
	private double power = 0;
	
	private int currentStep;
	

	public int getCurrentStep() {
		return currentStep;
	}

	private int deltaCurrentFlexibilityBidStation;

	private int currentMinimumStepBidStation;
	
	
	private int participationFactor = 0;
	

	private int upperLimitParticipationFactor;
	

	private int CorrectionFactor;
	
	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	private double deltaVDeltaI = 0.35;
	
	private double maxPowerVoltControl;
	

	private final double MAXCAPACITY = 150000;
	private final double MAXCHARGEPOWER = 25000;
	private final double MAXDISCHARGEPOWER = -50000;
	
	
	private double voltageAvg;
	private double minVoltage;
	private double maxVoltage;
	
	private double voltageA;
	private double voltageB;
	private double voltageC;
		
	public VRFB(double soc) {
		this.soc = soc;
		this.capacity = MAXCAPACITY * (soc/100);
	}
	
	public void calculateUpperLimitParticipationFactor() {
		if (deltaCurrentFlexibilityBidStation != 0) {
		this.upperLimitParticipationFactor = deltaCurrentFlexibilityBidStation/currentMinimumStepBidStation;	
		}
		if (deltaCurrentFlexibilityBidStation == 0) {
		this.upperLimitParticipationFactor = 0;	
		}
	}
	
	public void incrementParticipationFactor() {
		this.participationFactor++;
	}
	
	public void incrementCorrectionFactor() {
		this.CorrectionFactor++;
	}
	
	
	public void setCurrentStep(int currentStep) {
		this.currentStep = currentStep;
	}
	
	public void powerFlow (double power) {
		this.power = power;
		capacity = capacity + this.power/60;
		soc = 100*(capacity/MAXCAPACITY);
	}
	
	public void aufLaden(double aufladeLeistung) {
		this.setAufladeLeistung(aufladeLeistung);
		//insert code to transmit power to real VRFB
		capacity = capacity + this.aufladeLeistung/60;
		soc = 100*(capacity/MAXCAPACITY);
	}
	
	public void entLaden(double entladeLeistung) {
		this.setEntladeLeistung(entladeLeistung);
		//insert code to transmit power to real VRFB
		capacity = capacity - this.entladeLeistung/60;
		soc = 100*(capacity/MAXCAPACITY);
	}

	public double getAufladeLeistung() {
		return aufladeLeistung;
	}

	public void setAufladeLeistung(double aufladeLeistung) {
		this.entladeLeistung = 0;
		this.aufladeLeistung = aufladeLeistung;
	}

	public double getEntladeLeistung() {
		return entladeLeistung;
	}

	public void setEntladeLeistung(double entladeLeistung) {
		this.aufladeLeistung = 0;
		this.entladeLeistung = entladeLeistung;
	}


	public double getVoltageA() {
		return voltageA;
	}

	public void setVoltageA(double voltageA) {
		this.voltageA = voltageA;
	}

	public double getVoltageB() {
		return voltageB;
	}

	public void setVoltageB(double voltageB) {
		this.voltageB = voltageB;
	}

	public double getVoltageC() {
		return voltageC;
	}

	public void setVoltageC(double voltageC) {
		this.voltageC = voltageC;
	}

	public double getMinVoltage() {
		minVoltage = Math.min(voltageA, Math.min(voltageB, voltageC));
		return minVoltage;
	}

	public void setMinVoltage(double minVoltage) {
		this.minVoltage = minVoltage;
	}

	public double getMaxVoltage() {
		maxVoltage = Math.max(voltageA, Math.max(voltageB, voltageC));
		return maxVoltage;
	}

	public void setMaxVoltage(double maxVoltage) {
		this.maxVoltage = maxVoltage;
	}

	public double getDeltaVDeltaI() {
		return deltaVDeltaI;
	}

	public void setDeltaVDeltaI(double deltaVDeltaI) {
		this.deltaVDeltaI = deltaVDeltaI;
	}

	public double getMaxPowerVoltControl() {
		return maxPowerVoltControl;
	}

	public void setMaxPowerVoltControl(double maxPowerVoltControl) {
		this.maxPowerVoltControl = maxPowerVoltControl;
	}

	public double getVoltageAvg() {
//		voltageAvg = (voltageA + voltageB + voltageC)/3;
		return voltageAvg;
	}

	public void setVoltageAvg(double voltageAvg) {
		this.voltageAvg = voltageAvg;
	}

	public double getMAXDISCHARGEPOWER() {
		return MAXDISCHARGEPOWER;
	}

	public double getMAXCHARGEPOWER() {
		return MAXCHARGEPOWER;
	}

	public int getCurrentMinimumStepBidStation() {
		return currentMinimumStepBidStation;
	}

	public void setCurrentMinimumStepBidStation(int currentMinimumStepBidStation) {
		this.currentMinimumStepBidStation = currentMinimumStepBidStation;
	}

	public int getDeltaCurrentFlexibilityBidStation() {
		return deltaCurrentFlexibilityBidStation;
	}

	public void setDeltaCurrentFlexibilityBidStation(int deltaCurrentFlexibilityBidStation) {
		this.deltaCurrentFlexibilityBidStation = deltaCurrentFlexibilityBidStation;
	}

	public int getParticipationFactor() {
		return participationFactor;
	}

	public void setParticipationFactor(int participationFactor) {
		this.participationFactor = participationFactor;
	}

	public int getUpperLimitParticipationFactor() {
		return upperLimitParticipationFactor;
	}

	public void setUpperLimitParticipationFactor(int upperLimitParticipationFactor) {
		this.upperLimitParticipationFactor = upperLimitParticipationFactor;
	}

	public int getCorrectionFactor() {
		return CorrectionFactor;
	}

	public void setCorrectionFactor(int correctionFactor) {
		CorrectionFactor = correctionFactor;
	}

}
