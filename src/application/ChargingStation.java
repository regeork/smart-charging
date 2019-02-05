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

import java.io.IOException;
import java.util.Random;

import de.re.easymodbus.exceptions.ModbusException;
import de.re.easymodbus.modbusclient.ModbusClient;

public class ChargingStation {

	//Classes
	private Schedule ladeplan = null;

	private ElectricVehicle currentEV = null;
	
	//Charging Infrastructure
	ModbusClient clientEVCC = new ModbusClient();
	
	
	//Variables
	//Name of charging station
	private String name;
	
	//Id of charging station
	private int id;
	
	//Parameter that describes wether the EV can be controlled
	private boolean controlTMBS = false;
	
	//Parameter that describes wether the EV participates in TOU scheduling
	private boolean touControl = false;
	
	//Sensitivity parameter estimation
	private boolean sensitivityParUpdate = false;
	
	private boolean sensitivityParEstimation = false;
	
	private int iterationSensitivityParameter = 0;
	
	//Virtual charging current [A] in each phase
	private int charCurr = 0;
	
	//Virtual charging current sum
	private int charTest;
	
	//Virtual charging currents for each phase
	private int charCurrA = 0;
	private int charCurrB = 0;
	private int charCurrC = 0;
	
	//Real charging currents for each phase
	private double charCurrA_0Real = 0;
	private double charCurrB_0Real = 0;
	private double charCurrC_0Real = 0;
	
	//Further real charging currents for sensitivity parameter estimation
	private double charCurrA_1Real = 0;
	private double charCurrB_1Real = 0;
	private double charCurrC_1Real = 0;
	
	private double charCurrA_2Real = 0;
	private double charCurrB_2Real = 0;
	private double charCurrC_2Real = 0;
	
	private double charCurrA_3Real = 0;
	private double charCurrB_3Real = 0;
	private double charCurrC_3Real = 0;
	
	private double charCurrA_4Real = 0;
	private double charCurrB_4Real = 0;
	private double charCurrC_4Real = 0;
	
	private double charCurrA_5Real = 0;
	private double charCurrB_5Real = 0;
	private double charCurrC_5Real = 0;
	
	//Real voltage values for each phase
	private double voltageCStationA_0 = 0;
	private double voltageCStationB_0 = 0;
	private double voltageCStationC_0 = 0;
	
	//Further real voltage values for sensitivity parameter estimation
	private double voltageCStationA_1 = 0;
	private double voltageCStationB_1 = 0;
	private double voltageCStationC_1 = 0;
	
	private double voltageCStationA_2 = 0;
	private double voltageCStationB_2 = 0;
	private double voltageCStationC_2 = 0;
	
	private double voltageCStationA_3 = 0;
	private double voltageCStationB_3 = 0;
	private double voltageCStationC_3 = 0;
	
	private double voltageCStationA_4 = 0;
	private double voltageCStationB_4 = 0;
	private double voltageCStationC_4 = 0;
	
	private double voltageCStationA_5 = 0;
	private double voltageCStationB_5 = 0;
	private double voltageCStationC_5 = 0;
	
	//Virtual voltage for each phase
	private double voltageCStationA = 0;
	private double voltageCStationB = 0;
	private double voltageCStationC = 0;
	

	
	//Sensitivity parameter estimations for each iteration
	private double deltaVDeltaIIteration1 = 0;
	
	private double deltaVDeltaIIteration2 = 0;
	
	private double deltaVDeltaIIteration3 = 0;
	
	private double deltaVDeltaIIteration4 = 0;
	
	private double deltaVDeltaIIteration5 = 0;
		
	//Variables VM and TM
	/***
	 *  * For more information regarding these variables, refer to: 
	 * "Real Time Voltage and Thermal Management of Low Voltage Distribution Networks through Plug-in Electric Vehicles"
	 * https://upcommons.upc.edu/handle/2117/109082
	 * García Veloso, César
	 */
	
	//Sensitivity parameter voltage control
	private double deltaVDeltaI = 0.30;
	
	/***
	 * Maximum charging current calculated by the voltage control algorithm
	 */
	private int maxCharCurrVoltControl = 16;
	
	/***
	 * Minimum voltage value of the three phases of the charging station
	 */
	private double minVoltageCStation;
	
	/***
	 * Most restrictive current limit from voltage and thermal control
	 */
	private int mostRestrictiveCurrLimitVoltThermal = 16;
	
	/***
	 * Maximum thermal control value from dynTable
	 */
	private int cStationSortmaxCharCurrThermalControl = 16;
		
	/***
	 * Voltage at charging station
	 */
	private double voltageCStation = 0;
		
	/***
	 * The cumulative charging time of the vehicle
	 */
	private int cumulativeChargingTime = 0;
	
	/***
	 * The current flexibility bid represents the maximum change in the charging
current offered by the station-vehicle tandem. 
	 */
	private int deltaCurrentFlexibilityBidStation;
	
	/***
	 * The minimum current step of the bid denotes the minimum unitary division
the current bid consists of. 
	 */
	private int currentMinimumStepBidStation;
	
	/***
	 * The variable Xk, defined as the
participation factor of the vehicle “k”, indicates the contribution selected from vehicle “k”
by the transformer agent to solve the thermal optimization, in terms of the total amount
of selected current steps. Obviously this is an integer value ranging from 0 to the
maximum bid issued by the vehicle divided by its current step (deltaI/StepI). As it was
already previously pointed out, the use of this participation factor system, allows to
merge stations with different current steps, as well as to consider charge
increase/decrease offers jointly with charge stop/resume bids. 
	 */
	private int participationFactor = 0;
	
	/***
	 * maximum bid issued by the vehicle divided by its current step (deltaI/StepI). 
	 */
	private int upperLimitParticipationFactor;
	
	/***
	 * This check, only executed under reloading,
verifies whether any of the utilization factors has changed its sign. If so, that means the
last participation factor increase by that vehicle is the responsible to exceed the control
limit in that asset. This is registered by increasing in one unit the value of the car
correction factor associated with that vehicle. 
	 */
	private int carCorrectionFactor;
	
	/***
	 * Maximum charging current from thermal management control algorithm
	 */
	private int maxCharCurrThermalControl;
	
	/***
	 * Finally the resulting connection of the station-car pair (abcsv) is used to fully
characterize the duo’s connection to the network. This parameter is of extreme
relevance since it represents how each vehicle-station tandem directly affects the
different network assets. For instance a single-phase car connected to phase “b”
cannot alleviate feeder congestion in phases “a” or “c”. Thus in order to complete
describe such connection, this parameter is modeled to take an integer numerical
value ranging from one to four accounting the four possible considered combinations.
Therefore the first three values are assigned to single-phase AC connections to phases
“a”, “b” and “c”, while number four corresponds to three-phase AC connections. 
	 */
	private int connectionParameter;
	
	private int currentStepA;
	private int currentStepB;
	private int currentStepC;
	private int currentStep3;
	
	//Variables for scheduling
	//Decision vector
	private int[] decisionVector = new int [288];
		
	//Amount of energy desired converted to time slots
	private int chargingRequirement = 0;
	
	//Amount of energy charged since beginning of charging process
	private double loadAmount = 0;
	
	private int chargingCurrentfromStation = 0;
    
	private int flexibility = 0;
	
	private int avgLoadAmountPerTimeslot = 0;
	
	private int powerPerTimeslot = 0;
	
	private int loadAmountInTimeslots = 0;
	
	private double chargingRequirementAbs = 0;
	
	//Variables interface charging infrastructure
	private boolean boolean_EVCC_2_ReadEnaChar;
	
	
	//Constructors
	public ChargingStation(int id, int connectionParameter) {
		this.setId(id);
		this.connectionParameter = connectionParameter;
	}
	
	/***
	 * 
	 * @param id
	 * @param connectionParameter
	 * @param clientEVCC
	 */
	public ChargingStation(int id, int connectionParameter, ModbusClient clientEVCC) {
		this.setId(id);
		this.connectionParameter = connectionParameter;
		this.clientEVCC = clientEVCC;
	}
	
	//Methods
	//Write virtual charging current to electric vehicle charging controller (EVCC)
	public void setCharCurrReal(int charCurr) {
		this.charCurr = charCurr;	
		try {
		// Read "Enable Charging"
		boolean[] enableChargingData;
		enableChargingData = clientEVCC.ReadCoils(400, 1);

//		System.out.println("Enable Charging: " + enableChargingData[0]);

		if (enableChargingData[0] == true) { // Boolean is true
			boolean_EVCC_2_ReadEnaChar = true;
		} else { // Boolean is false
			boolean_EVCC_2_ReadEnaChar = false;
		}

		if (charCurr < 6 && boolean_EVCC_2_ReadEnaChar == true) {
			// Write "Coil Enable Charging Process"
			// Set the value of coil
			clientEVCC.WriteSingleCoil(400, false);
		}

		if (charCurr >= 6 && boolean_EVCC_2_ReadEnaChar == false) {
			// Write "Coil Enable Charging Process"
			clientEVCC.WriteSingleCoil(400, true);
		}

		// EVCC_2 Write Charging Current
		clientEVCC.WriteSingleRegister(300, this.charCurr);
		
		} catch (ModbusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Fehler: Verbindung zu Kontroller verloren!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//Charge the EV virtually
	public void ladeAuto() {
		if (currentEV != null && charCurr > 0) {
			currentEV.laden((charCurrA + charCurrB + charCurrC), voltageCStation);
			cumulativeChargingTime++;
		}
	}
	
	//Print EV data in console
	public void printValues () {
		if (currentEV != null) {
			System.out.println("\nAusgabe Station: " + name);
			System.out.println();
			System.out.println("Aktuelles Auto: " + currentEV.getName() + "    Ladung seit Beginn: " + loadAmount + "Wh" + "        Aktueller SoC [%]: " + currentEV.getSoc());
			System.out.println("Spannung [V]: " + voltageCStation + "       Ladestrom [A]: " + (charCurrA + charCurrB + charCurrC));	
		}
		else {
			System.out.println("Kein Auto in Ladestation " + name);			
		}
		System.out.println("Ende aktueller Ausgabe \n \n");
	}
	
	//Set connection of charging station-electric vehicle tandem 
	//4 = three phase, 1 = phase a, 2 = phase b, 3 = phase c
	//if EV is 1-phase; choose random phase of charging station
	private void connectionParameterTandemRandom (boolean connectionCar) {
		if (connectionCar == true){
			connectionParameter = 4;
		}
		if (connectionCar == false) {
			Random rn = new Random ();
			connectionParameter = rn.nextInt(3-1 + 1) + 1;
		}		
	}
	
	//if EV is 1-phase; and the phase is known, use this method and comment out 2 of the three connection
	//posibilities
	private void connectionParameterTandemKnown (boolean connectionCar) {
		if (connectionCar == true){
			connectionParameter = 4;
		}
		if (connectionCar == false) {
//			Random rn = new Random ();
			connectionParameter = 1;
//			connectionParameter = 2;
//			connectionParameter = 3;
		}	
	}
	
	//Assign virtual EV to charging station.
	public void setCurrentEV (ElectricVehicle a) {
		currentEV = a;
	}
	
	//Assign a virtual EV with a schedule and the desired amount of energy to a charging station. 
	//The schedule only consists of the arrival and departure time.
	//The charging requirement is dependend on the desired sampling time: 
	//Use factor 6 for a sampling time of 30 seconds
	//Use factor 180 for a real time simulation
	public void assignEV (ElectricVehicle a, Schedule ladeplan, double loadAmount) {
		this.currentEV = a;
		this.ladeplan = ladeplan;
		this.chargingRequirementAbs = loadAmount;
		connectionParameterTandemRandom(a.isConnection());
		if (a.isConnection() == true) {
			this.chargingRequirement = (int) Math.ceil((loadAmount/(48*230/6)));
//			this.chargingRequirement = (int) (loadAmount/(48*230/180));
		}
		if (a.isConnection() == false) {
			this.chargingRequirement = (int) Math.ceil((loadAmount/(16*230/6)));
//			this.chargingRequirement = (int) (loadAmount/(16*230/180));
		}	
	}
	
	public void assignEVReal (ElectricVehicle a, Schedule ladeplan, double loadAmount) {
		this.currentEV = a;
		this.ladeplan = ladeplan;
		this.chargingRequirementAbs = loadAmount;
		connectionParameterTandemKnown(a.isConnection());
		if (a.isConnection() == true) {
			this.chargingRequirement = (int) (loadAmount/(48*230/6));
//			this.chargingRequirement = (int) (loadAmount/(48*230/180));
		}
		if (a.isConnection() == false) {
			this.chargingRequirement = (int) (loadAmount/(16*230/6));
//			this.chargingRequirement = (int) (loadAmount/(16*230/180));
		}	
	}
	
	public ElectricVehicle getCurrentEV () {
		return currentEV;
	}
	
	public int getCharCurr() {
		return charCurr;
	}
	
	//Return sum of virtual charging current
	public int getCharCurrSum() {
		charTest = charCurrA + charCurrB + charCurrC;
		return charTest;
	}
	
	public Schedule getLadeplan() {
		return ladeplan;
	}

	public void setLadeplan(Schedule ladeplan) {
		this.ladeplan = ladeplan;
	}
	
	public int getLadeIntervalle () {
		return getLadeplan().intervall;
	}
	
	public void setLadeIntervallEins () {
		this.ladeplan.intervall = 1;
	}
	
	public void setRestIntervallEins () {
		this.ladeplan.restIntervall = 1;
	}
	
	public int getRestIntervalle() {
		return ladeplan.restIntervall;
	}
		
	public void setCharCurr(int charCurr) {
		this.charCurr = charCurr;
	}

	public double getVoltageCStation() {
		voltageCStation = (voltageCStationA + voltageCStationB + voltageCStationC)/3;
		return voltageCStation;
	}
	
	public void setVoltageCStation(double voltageCStation) {
		this.voltageCStation = voltageCStation;
		this.voltageCStationA = voltageCStation;
		this.voltageCStationB = voltageCStation;
		this.voltageCStationC = voltageCStation;
	}
	

		
	public void restIntervalleRunter() {
		if (ladeplan != null) {
		this.ladeplan.restIntervalleRunter();
		}
	}
	
	public void increaseCumCharTime() {
		this.cumulativeChargingTime++;
	}

	public double getDeltaCurrentFlexibilityBidStation() {
		return deltaCurrentFlexibilityBidStation;
	}


	public void setDeltaCurrentFlexibilityBidStation(int currentFlexibilityBidStation) {
		this.deltaCurrentFlexibilityBidStation = currentFlexibilityBidStation;
	}


	public int getCurrentMinimumStepBidStation() {
		return currentMinimumStepBidStation;
	}


	public void setCurrentMinimumStepBidStation(int currentMinimumStepBidStation) {
		this.currentMinimumStepBidStation = currentMinimumStepBidStation;
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

	public void calculateUpperLimitParticipationFactor() {
		if (deltaCurrentFlexibilityBidStation != 0) {
		this.upperLimitParticipationFactor = deltaCurrentFlexibilityBidStation/currentMinimumStepBidStation;	
		}
		if (deltaCurrentFlexibilityBidStation == 0) {
		this.upperLimitParticipationFactor = 0;	
		}
	}

	public void setUpperLimitParticipationFactor(int upperLimitParticipationFactor) {
		this.upperLimitParticipationFactor = upperLimitParticipationFactor;
	}

	public void incrementParticipationFactor() {
		this.participationFactor++;
	}


	public int getCarCorrectionFactor() {
		return carCorrectionFactor;
	}


	public void setCarCorrectionFactor(int carCorrectionFactor) {
		this.carCorrectionFactor = carCorrectionFactor;
	}
	
	
	public void incrementCarCorrectionFactor() {
		this.carCorrectionFactor++;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public int getMaxCharCurrThermalControl() {
		return maxCharCurrThermalControl;
	}


	public void setMaxCharCurrThermalControl(int maxCharCurrThermalControl) {
		this.maxCharCurrThermalControl = maxCharCurrThermalControl;
	}

	public int getCumulativeChargingTime() {
		return cumulativeChargingTime;
	}

	public void setCumulativeChargingTime(int cumulativeChargingTime) {
		this.cumulativeChargingTime = cumulativeChargingTime;
	}



	public int getConnectionParameter() {
		return connectionParameter;
	}



	public void setConnectionParameter(int connectionParameter) {
		this.connectionParameter = connectionParameter;
	}



//	public int getCharCurrA() {
//		return charCurrA;
//	}
//
//
//
//	public void setCharCurrA(int charCurrA) {
//		this.charCurrA = charCurrA;
//	}
//
//
//
//	public int getCharCurrB() {
//		return charCurrB;
//	}
//
//
//
//	public void setCharCurrB(int charCurrB) {
//		this.charCurrB = charCurrB;
//	}
//
//
//
//	public int getCharCurrC() {
//		return charCurrC;
//	}
//
//
//
//	public void setCharCurrC(int charCurrC) {
//		this.charCurrC = charCurrC;
//	}



	public double getVoltageCStationA() {
		return voltageCStationA;
	}



	public double getVoltageCStationB() {
		return voltageCStationB;
	}



	public double getVoltageCStationC() {
		return voltageCStationC;
	}



	public int getCharCurrA() {
		return charCurrA;
	}



	public void setCharCurrA(int charCurrA) {
		this.charCurrA = charCurrA;
	}



	public int getCharCurrB() {
		return charCurrB;
	}



	public void setCharCurrB(int charCurrB) {
		this.charCurrB = charCurrB;
	}



	public int getCharCurrC() {
		return charCurrC;
	}



	public void setCharCurrC(int charCurrC) {
		this.charCurrC = charCurrC;
	}



	public int getCurrentStepA() {
		return currentStepA;
	}



	public void setCurrentStepA(int currentStepA) {
		this.currentStepA = currentStepA;
	}



	public int getCurrentStepB() {
		return currentStepB;
	}



	public void setCurrentStepB(int currentStepB) {
		this.currentStepB = currentStepB;
	}



	public int getCurrentStepC() {
		return currentStepC;
	}



	public void setCurrentStepC(int currentStepC) {
		this.currentStepC = currentStepC;
	}



	public int getCurrentStep3() {
		return currentStep3;
	}



	public void setCurrentStep3(int currentStep3) {
		this.currentStep3 = currentStep3;
	}



	public void setVoltageCStationA(double voltageCStationA) {
		this.voltageCStationA = voltageCStationA;
	}



	public void setVoltageCStationB(double voltageCStationB) {
		this.voltageCStationB = voltageCStationB;
	}



	public void setVoltageCStationC(double voltageCStationC) {
		this.voltageCStationC = voltageCStationC;
	}





	public Integer getFlexibility() {
		return flexibility;
	}





	public void setFlexibility(int flexibility) {
		this.flexibility = flexibility;
	}





	public int[] getDecisionVector() {
		return decisionVector;
	}





	public void setDecisionVector(int[] decisionVector) {
		this.decisionVector = decisionVector;
	}
	
	public int getDecisionVectorAtI(int i) {
		return decisionVector [i];
	} 
	
	/***
	 * 
	 * @param i timestep/position of value
	 * @param value
	 */
	public void setDecisionVectorAtI(int i, int value) {
		this.decisionVector[i] = value;
	}





	public int getChargingRequirement() {
		return chargingRequirement;
	}





	public void setChargingRequirement(int chargingRequirement) {
		this.chargingRequirement = chargingRequirement;
	}





	public Double getLoadAmount() {
		return loadAmount;
	}





	public void setLoadAmount(double loadAmount) {
		this.loadAmount = loadAmount;
	}

	public int getChargingCurrentfromStation() {
		return chargingCurrentfromStation;
	}

	public void setChargingCurrentfromStation(int chargingCurrentfromStation) {
		this.chargingCurrentfromStation = chargingCurrentfromStation;
	}

	public int getMaxCharCurrVoltControl() {
		return maxCharCurrVoltControl;
	}

	public void setMaxCharCurrVoltControl(int maxCharCurrVoltControl) {
		this.maxCharCurrVoltControl = maxCharCurrVoltControl;
	}
	
	public void incrementMaxCharCurrVoltControl() {
		this.maxCharCurrVoltControl++;
	}

	public double getMinVoltageCStation() {
		return minVoltageCStation;
	}

	public void setMinVoltageCStation(double minVoltageCStation) {
		this.minVoltageCStation = minVoltageCStation;
	}

	public int getMostRestrictiveCurrLimitVoltThermal() {
		return mostRestrictiveCurrLimitVoltThermal;
	}

	public void setMostRestrictiveCurrLimitVoltThermal(int mostRestrictiveCurrLimitVoltThermal) {
		this.mostRestrictiveCurrLimitVoltThermal = mostRestrictiveCurrLimitVoltThermal;
	}

	public int getcStationSortmaxCharCurrThermalControl() {
		return cStationSortmaxCharCurrThermalControl;
	}

	public void setcStationSortmaxCharCurrThermalControl(int cStationSortmaxCharCurrThermalControl) {
		this.cStationSortmaxCharCurrThermalControl = cStationSortmaxCharCurrThermalControl;
	}

	/**
	 * @return the avgLoadAmountPerTimeslot
	 */
	public int getAvgLoadAmountPerTimeslot() {
		return avgLoadAmountPerTimeslot;
	}

	/**
	 * @param avgLoadAmountPerTimeslot the avgLoadAmountPerTimeslot to set
	 */
	public void setAvgLoadAmountPerTimeslot(int avgLoadAmountPerTimeslot) {
		this.avgLoadAmountPerTimeslot = avgLoadAmountPerTimeslot;
	}

	/**
	 * @return the powerPerTimeslot
	 */
	public int getPowerPerTimeslot() {
		return powerPerTimeslot;
	}

	/**
	 * @param powerPerTimeslot the powerPerTimeslot to set
	 */
	public void setPowerPerTimeslot(int powerPerTimeslot) {
		this.powerPerTimeslot = powerPerTimeslot;
	}

	/**
	 * @return the loadAmountInTimeslots
	 */
	public int getLoadAmountInTimeslots() {
		return loadAmountInTimeslots;
	}

	/**
	 * @param loadAmountInTimeslots the loadAmountInTimeslots to set
	 */
	public void setLoadAmountInTimeslots(int loadAmountInTimeslots) {
		this.loadAmountInTimeslots = loadAmountInTimeslots;
	}

	/**
	 * @return the chargingRequirementAbs
	 */
	public double getChargingRequirementAbs() {
		return chargingRequirementAbs;
	}

	/**
	 * @param chargingRequirementAbs the chargingRequirementAbs to set
	 */
	public void setChargingRequirementAbs(double chargingRequirementAbs) {
		this.chargingRequirementAbs = chargingRequirementAbs;
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the sensitivityParUpdate
	 */
	public boolean isSensitivityParUpdate() {
		return sensitivityParUpdate;
	}

	/**
	 * @param sensitivityParUpdate the sensitivityParUpdate to set
	 */
	public void setSensitivityParUpdate(boolean sensitivityParUpdate) {
		this.sensitivityParUpdate = sensitivityParUpdate;
	}

	/**
	 * @return the deltaVdeltaI
	 */
	public double getDeltaVDeltaI() {
		return deltaVDeltaI;
	}

	/**
	 * @param deltaVdeltaI the deltaVdeltaI to set
	 */
	public void setDeltaVDeltaI(double deltaVdeltaI) {
		this.deltaVDeltaI = deltaVdeltaI;
	}





	/**
	 * @return the control
	 */
	public boolean isControlTMBS() {
		return controlTMBS;
	}

	/**
	 * @param control the control50 to set
	 */
	public void setControlTMBS(boolean control) {
		this.controlTMBS = control;
	}

	public boolean isTouControl() {
		return touControl;
	}

	public void setTouControl(boolean touControl) {
		this.touControl = touControl;
	}

    public boolean isSensitivityParEstimation() {
		return sensitivityParEstimation;
	}

	public void setSensitivityParEstimation(boolean sensitivityParEstimation) {
		this.sensitivityParEstimation = sensitivityParEstimation;
	}

	public int getIterationSensitivityParameter() {
		return iterationSensitivityParameter;
	}

	public void setIterationSensitivityParameter(int iterationSensitivityParameter) {
		this.iterationSensitivityParameter = iterationSensitivityParameter;
	}

	public double getCharCurrA_0Real() {
		return charCurrA_0Real;
	}

	public void setCharCurrA_0Real(double charCurrA_0Real) {
		this.charCurrA_0Real = charCurrA_0Real;
	}

	public double getCharCurrB_0Real() {
		return charCurrB_0Real;
	}

	public void setCharCurrB_0Real(double charCurrB_0Real) {
		this.charCurrB_0Real = charCurrB_0Real;
	}

	public double getCharCurrC_0Real() {
		return charCurrC_0Real;
	}

	public void setCharCurrC_0Real(double charCurrC_0Real) {
		this.charCurrC_0Real = charCurrC_0Real;
	}

	public double getCharCurrA_1Real() {
		return charCurrA_1Real;
	}

	public void setCharCurrA_1Real(double charCurrA_1Real) {
		this.charCurrA_1Real = charCurrA_1Real;
	}

	public double getCharCurrB_1Real() {
		return charCurrB_1Real;
	}

	public void setCharCurrB_1Real(double charCurrB_1Real) {
		this.charCurrB_1Real = charCurrB_1Real;
	}

	public double getCharCurrC_1Real() {
		return charCurrC_1Real;
	}

	public void setCharCurrC_1Real(double charCurrC_1Real) {
		this.charCurrC_1Real = charCurrC_1Real;
	}

	public double getCharCurrA_2Real() {
		return charCurrA_2Real;
	}

	public void setCharCurrA_2Real(double charCurrA_2Real) {
		this.charCurrA_2Real = charCurrA_2Real;
	}

	public double getCharCurrB_2Real() {
		return charCurrB_2Real;
	}

	public void setCharCurrB_2Real(double charCurrB_2Real) {
		this.charCurrB_2Real = charCurrB_2Real;
	}

	public double getCharCurrC_2Real() {
		return charCurrC_2Real;
	}

	public void setCharCurrC_2Real(double charCurrC_2Real) {
		this.charCurrC_2Real = charCurrC_2Real;
	}

	public double getCharCurrA_3Real() {
		return charCurrA_3Real;
	}

	public void setCharCurrA_3Real(double charCurrA_3Real) {
		this.charCurrA_3Real = charCurrA_3Real;
	}

	public double getCharCurrB_3Real() {
		return charCurrB_3Real;
	}

	public void setCharCurrB_3Real(double charCurrB_3Real) {
		this.charCurrB_3Real = charCurrB_3Real;
	}

	public double getCharCurrC_3Real() {
		return charCurrC_3Real;
	}

	public void setCharCurrC_3Real(double charCurrC_3Real) {
		this.charCurrC_3Real = charCurrC_3Real;
	}

	public double getCharCurrA_4Real() {
		return charCurrA_4Real;
	}

	public void setCharCurrA_4Real(double charCurrA_4Real) {
		this.charCurrA_4Real = charCurrA_4Real;
	}

	public double getCharCurrB_4Real() {
		return charCurrB_4Real;
	}

	public void setCharCurrB_4Real(double charCurrB_4Real) {
		this.charCurrB_4Real = charCurrB_4Real;
	}

	public double getCharCurrC_4Real() {
		return charCurrC_4Real;
	}

	public void setCharCurrC_4Real(double charCurrC_4Real) {
		this.charCurrC_4Real = charCurrC_4Real;
	}

	public double getCharCurrA_5Real() {
		return charCurrA_5Real;
	}

	public void setCharCurrA_5Real(double charCurrA_5Real) {
		this.charCurrA_5Real = charCurrA_5Real;
	}

	public double getCharCurrB_5Real() {
		return charCurrB_5Real;
	}

	public void setCharCurrB_5Real(double charCurrB_5Real) {
		this.charCurrB_5Real = charCurrB_5Real;
	}

	public double getCharCurrC_5Real() {
		return charCurrC_5Real;
	}

	public void setCharCurrC_5Real(double charCurrC_5Real) {
		this.charCurrC_5Real = charCurrC_5Real;
	}

	public double getVoltageCStationA_0() {
		return voltageCStationA_0;
	}

	public void setVoltageCStationA_0(double voltageCStationA_0) {
		this.voltageCStationA_0 = voltageCStationA_0;
	}

	public double getVoltageCStationB_0() {
		return voltageCStationB_0;
	}

	public void setVoltageCStationB_0(double voltageCStationB_0) {
		this.voltageCStationB_0 = voltageCStationB_0;
	}

	public double getVoltageCStationC_0() {
		return voltageCStationC_0;
	}

	public void setVoltageCStationC_0(double voltageCStationC_0) {
		this.voltageCStationC_0 = voltageCStationC_0;
	}

	public double getVoltageCStationA_1() {
		return voltageCStationA_1;
	}

	public void setVoltageCStationA_1(double voltageCStationA_1) {
		this.voltageCStationA_1 = voltageCStationA_1;
	}

	public double getVoltageCStationB_1() {
		return voltageCStationB_1;
	}

	public void setVoltageCStationB_1(double voltageCStationB_1) {
		this.voltageCStationB_1 = voltageCStationB_1;
	}

	public double getVoltageCStationC_1() {
		return voltageCStationC_1;
	}

	public void setVoltageCStationC_1(double voltageCStationC_1) {
		this.voltageCStationC_1 = voltageCStationC_1;
	}

	public double getVoltageCStationA_2() {
		return voltageCStationA_2;
	}

	public void setVoltageCStationA_2(double voltageCStationA_2) {
		this.voltageCStationA_2 = voltageCStationA_2;
	}

	public double getVoltageCStationB_2() {
		return voltageCStationB_2;
	}

	public void setVoltageCStationB_2(double voltageCStationB_2) {
		this.voltageCStationB_2 = voltageCStationB_2;
	}

	public double getVoltageCStationC_2() {
		return voltageCStationC_2;
	}

	public void setVoltageCStationC_2(double voltageCStationC_2) {
		this.voltageCStationC_2 = voltageCStationC_2;
	}

	public double getVoltageCStationA_3() {
		return voltageCStationA_3;
	}

	public void setVoltageCStationA_3(double voltageCStationA_3) {
		this.voltageCStationA_3 = voltageCStationA_3;
	}

	public double getVoltageCStationB_3() {
		return voltageCStationB_3;
	}

	public void setVoltageCStationB_3(double voltageCStationB_3) {
		this.voltageCStationB_3 = voltageCStationB_3;
	}

	public double getVoltageCStationC_3() {
		return voltageCStationC_3;
	}

	public void setVoltageCStationC_3(double voltageCStationC_3) {
		this.voltageCStationC_3 = voltageCStationC_3;
	}

	public double getVoltageCStationA_4() {
		return voltageCStationA_4;
	}

	public void setVoltageCStationA_4(double voltageCStationA_4) {
		this.voltageCStationA_4 = voltageCStationA_4;
	}

	public double getVoltageCStationB_4() {
		return voltageCStationB_4;
	}

	public void setVoltageCStationB_4(double voltageCStationB_4) {
		this.voltageCStationB_4 = voltageCStationB_4;
	}

	public double getVoltageCStationC_4() {
		return voltageCStationC_4;
	}

	public void setVoltageCStationC_4(double voltageCStationC_4) {
		this.voltageCStationC_4 = voltageCStationC_4;
	}

	public double getVoltageCStationA_5() {
		return voltageCStationA_5;
	}

	public void setVoltageCStationA_5(double voltageCStationA_5) {
		this.voltageCStationA_5 = voltageCStationA_5;
	}

	public double getVoltageCStationB_5() {
		return voltageCStationB_5;
	}

	public void setVoltageCStationB_5(double voltageCStationB_5) {
		this.voltageCStationB_5 = voltageCStationB_5;
	}

	public double getVoltageCStationC_5() {
		return voltageCStationC_5;
	}

	public void setVoltageCStationC_5(double voltageCStationC_5) {
		this.voltageCStationC_5 = voltageCStationC_5;
	}

	public double getDeltaVDeltaIIteration1() {
		return deltaVDeltaIIteration1;
	}

	public void setDeltaVDeltaIIteration1(double deltaVDeltaIIteration1) {
		this.deltaVDeltaIIteration1 = deltaVDeltaIIteration1;
	}

	public double getDeltaVDeltaIIteration2() {
		return deltaVDeltaIIteration2;
	}

	public void setDeltaVDeltaIIteration2(double deltaVDeltaIIteration2) {
		this.deltaVDeltaIIteration2 = deltaVDeltaIIteration2;
	}

	public double getDeltaVDeltaIIteration3() {
		return deltaVDeltaIIteration3;
	}

	public void setDeltaVDeltaIIteration3(double deltaVDeltaIIteration3) {
		this.deltaVDeltaIIteration3 = deltaVDeltaIIteration3;
	}

	public double getDeltaVDeltaIIteration4() {
		return deltaVDeltaIIteration4;
	}

	public void setDeltaVDeltaIIteration4(double deltaVDeltaIIteration4) {
		this.deltaVDeltaIIteration4 = deltaVDeltaIIteration4;
	}

	public double getDeltaVDeltaIIteration5() {
		return deltaVDeltaIIteration5;
	}

	public void setDeltaVDeltaIIteration5(double deltaVDeltaIIteration5) {
		this.deltaVDeltaIIteration5 = deltaVDeltaIIteration5;
	}

	
	
	
//	public String toString () {
//		return name;
//	}
	
}
