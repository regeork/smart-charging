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
			

				
/***
 * The design of the methods voltageControl(), thermalManagementControl() and dynTable()
 * has been developed by García Veloso, César. 
 * 
 * For more information regarding these methods, please refer to:
 * "Real Time Voltage and Thermal Management of Low Voltage Distribution Networks through Plug-in Electric Vehicles"
 * https://upcommons.upc.edu/handle/2117/109082
 * García Veloso, César
 */

package application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.Random;

public class Controller {

	//Classes
	//Database
	private SQLiteDB db;
	
	//Battery storage
	private VRFB VRFB;
	
	private ArrayList <ChargingStation> cStationSort = new ArrayList <ChargingStation> ();
	private ArrayList <ChargingStation> cStationFree = new ArrayList <ChargingStation> ();
	private ArrayList <ChargingStation> cStationVoltThermal = new ArrayList <ChargingStation> ();
	
	private ArrayList <ChargingStation> optScheduleQueque = new ArrayList <ChargingStation> ();
	
	private ArrayList <ElectricVehicle> autoList = new ArrayList <ElectricVehicle> ();
	
	private double [] stockPrice = new double [288];
	
	//Variables
	//Import stock data once
	private int ecopx = 0;
	
	//used for Monte Carlo
	private boolean neuerLauf = false;
		
	//VoltageControl
	/***
	 * DSO control value
	 */
	private double voltageControlThreshold = 0.905;

	//ThermalManagement variables
	/***
	 * Rated Power of Transformator
	 */
	private double transformatorLoadingRated;
	
	/***
	 * Security factor specified by the DSO; Although this factor can be
	tuned and optimized for each particular network by trial and error, a value of 95%
	has been considered appropriate.
	 */
	private double transformatorSecurityFactor = 0.95;
	
	/***
	 * Present loading of the transformer.
	 */
	private double transformatorLoading;
	
	/***
	 * Utility Factor Transformer. Every execution time, the transformer agent receives the power and current
measurements from the transformer and the head feeders and estimates the assets’
utilization factors. These utilization factors are used as a metric to quantify the loading
of the assets and determine the necessary course of action.
	 */
	private double utFactorTransformatorPower;
	
	//Utility factor custom threshold
	private double utFactorDSOThreshold;
	
	//Cumulative load from EVs
	private double cumulativeLoadingPower;
	
	//Cumulative load from EVs basic scheduling only
	private double ssloadingpower;
		
	//Custom load threshold specified by the DSO or an operator
	private double thresholdDSO;
	
	//Threshold used for basic scheduling
	private double thresholdScheduling;
	
	private double utFactorThresholdScheduling;
	
	//Security factor for DSO threshold
	private double dSOThresholdSecurityFactor = 0.95;
	
	//Test variables for battery control
	boolean test;
	double utFactorTrafoVRFB;
	
	
	//Constructors	
	public Controller (ArrayList<ChargingStation> cStationVoltThermal, ArrayList<ChargingStation> cStationSort, SQLiteDB f, VRFB VRFB) {//, Auto auto1, Auto auto2, Auto auto3) {
		this.cStationVoltThermal = cStationVoltThermal;
		this.cStationSort = cStationSort;
		this.db = f;
		this.VRFB = VRFB;
	}

	
	//Methods
	
	//Superordinate algorithm
	//Load data from properties file and choose Algorithm accordingly (auswahl)
	//timeSlot = x * time, dependent on what is defined in MainController() and by the general sample time
	public void chooseAlgorithm(int auswahl, int time, int timeSlot) {
		if (auswahl == 3) {
			voltageControl();
//			thermalControlVRFB();
				thermalManagementControl(timeSlot);
				dynTable();
			if (time % 10 == 0) {
				adjustAlgorithms(timeSlot);
			}
			controlAlgorithm(timeSlot);
//			calibrateSensitivityParameter(time);
		}
		System.out.println("Time units running: " + time);
		System.out.println("Timeslot: " + timeSlot);
	}
	
	
	public void voltageControl() {
		double tempCumulativeLoadingPower = 0;
		
		for (int i = 0; i < cStationVoltThermal.size(); i++) {
			tempCumulativeLoadingPower += cStationVoltThermal.get(i).getCharCurrSum() * cStationVoltThermal.get(i).getVoltageCStation();
		}
		
		cumulativeLoadingPower = tempCumulativeLoadingPower;
	
		//Reloading operation
		for (int i = 0; i < cStationVoltThermal.size(); i++) {
			cStationVoltThermal.get(i)
					.setMinVoltageCStation(Math.min(cStationVoltThermal.get(i).getVoltageCStationA(),
							Math.min(cStationVoltThermal.get(i).getVoltageCStationB(),
									cStationVoltThermal.get(i).getVoltageCStationC())));
			if ((cStationVoltThermal.get(i).getCurrentEV() != null)
					&& (cStationVoltThermal.get(i).getMinVoltageCStation() / 230) >= voltageControlThreshold) {
				int deltaCharCurr = (int) Math
						.ceil(((cStationVoltThermal.get(i).getMinVoltageCStation() / 230) - voltageControlThreshold)
								/ (cStationVoltThermal.get(i).getDeltaVDeltaI()/230));
				int tempMaxCharCurrVoltControlReload = Math.max(cStationVoltThermal.get(i).getCharCurr(),
						Math.min((cStationVoltThermal.get(i).getCharCurr() + deltaCharCurr),
								cStationVoltThermal.get(i).getCurrentEV().getMAXCHARCURREV()));

				if (cStationVoltThermal.get(i).getCharCurr() < 6 && tempMaxCharCurrVoltControlReload < 7) {
					cStationVoltThermal.get(i).setMaxCharCurrVoltControl(0);
				}
				if (cStationVoltThermal.get(i).getCharCurr() < 6 && tempMaxCharCurrVoltControlReload >= 7) {
					cStationVoltThermal.get(i).setMaxCharCurrVoltControl(6);
				}
				if (cStationVoltThermal.get(i).getCharCurr() >= 6 && tempMaxCharCurrVoltControlReload >= 7) {
					if (cStationVoltThermal.get(i).getMaxCharCurrVoltControl() < tempMaxCharCurrVoltControlReload) {
						cStationVoltThermal.get(i).incrementMaxCharCurrVoltControl();
					}
				}
			}
		}

		//Curtailment operation
		for (int i = 0; i < cStationVoltThermal.size(); i++) {
			if ((cStationVoltThermal.get(i).getCurrentEV() != null)
					&& (cStationVoltThermal.get(i).getMinVoltageCStation() / 230) < voltageControlThreshold) {
				int deltaCharCurr = (int) Math
						.ceil((voltageControlThreshold - cStationVoltThermal.get(i).getMinVoltageCStation() / 230)
								/ (cStationVoltThermal.get(i).getDeltaVDeltaI()/230));
				int tempMaxCharCurrVoltControlCurt = Math.max(6,
						Math.min(cStationVoltThermal.get(i).getCharCurr() - deltaCharCurr,
								cStationVoltThermal.get(i).getCharCurr()));

				if ((cStationVoltThermal.get(i).getCharCurr() - deltaCharCurr) < 6) {
					cStationVoltThermal.get(i).setMaxCharCurrVoltControl(0);
				}
				if ((cStationVoltThermal.get(i).getCharCurr() - deltaCharCurr) >= 6) {
					cStationVoltThermal.get(i).setMaxCharCurrVoltControl(tempMaxCharCurrVoltControlCurt);
				}
			}
		}
	}

	
	public void thermalManagementControl(int i) {
		
		utFactorTransformatorPower = transformatorLoading - transformatorLoadingRated*transformatorSecurityFactor;
		System.out.println("transformatorLoading: " + transformatorLoading);
		System.out.println("transformatorRated: " + transformatorLoadingRated);

		double tempCumLoadingPower = 0;
		for (int j = 0; j < cStationVoltThermal.size(); j++) {
			tempCumLoadingPower += cStationVoltThermal.get(j).getCharCurrSum() * cStationVoltThermal.get(j).getVoltageCStation();
		}
		cumulativeLoadingPower = tempCumLoadingPower;
		
		double thresholdloadingpower = 0;
		for (int j = 0; j < optScheduleQueque.size(); j++) {
			thresholdloadingpower += optScheduleQueque.get(j).getCharCurrSum() * optScheduleQueque.get(j).getVoltageCStation();
		}
		ssloadingpower = thresholdloadingpower;
		
		utFactorDSOThreshold = cumulativeLoadingPower - thresholdDSO*dSOThresholdSecurityFactor;
		
		utFactorThresholdScheduling = ssloadingpower - thresholdScheduling;
		
		//Reloading offer
		if (utFactorTransformatorPower < 0 && utFactorDSOThreshold < 0 && utFactorThresholdScheduling < 0) {

			for (int k = 0; k < cStationVoltThermal.size(); k++) {
				if (cStationVoltThermal.get(k).isControlTMBS() == true) {
				if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 2) {

					if (cStationVoltThermal.get(k).getMaxCharCurrVoltControl() >= 6
							&& cStationVoltThermal.get(k).getCharCurr() < 1) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(6);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(6);
					}
					if (cStationVoltThermal.get(k).getMaxCharCurrVoltControl() >= cStationVoltThermal.get(k)
							.getCharCurr() && cStationVoltThermal.get(k).getCharCurr() >= 6) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(
								cStationVoltThermal.get(k).getMaxCharCurrVoltControl()
										- cStationVoltThermal.get(k).getCharCurr());
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
					}
					if (cStationVoltThermal.get(k).getCharCurr() == cStationVoltThermal.get(k).getCurrentEV()
							.getMAXCHARCURREV()) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(0);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
					}
					if (cStationVoltThermal.get(k).getMaxCharCurrVoltControl() >= 0
							&& cStationVoltThermal.get(k).getMaxCharCurrVoltControl() < 6) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(6);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(6);
					}
				}

				if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 1) {
					if (cStationVoltThermal.get(k).getMaxCharCurrVoltControl() >= 6
							&& cStationVoltThermal.get(k).getCharCurr() < 1) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(6);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(6);
					}
					if (cStationVoltThermal.get(k).getCharCurr() >= 6) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(0);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
					}
				}

				if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 0) {
					cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(0);
					cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
				}
			}
				}
		}
					
			
		if (utFactorDSOThreshold > 0 || utFactorTransformatorPower > 0 || utFactorThresholdScheduling > 0) {

			//Curtailment offer
			for (int k = 0; k < cStationVoltThermal.size(); k++) {
				if (cStationVoltThermal.get(k).isControlTMBS() == true) {
				if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 2) {
					
					if (cStationVoltThermal.get(k).getCharCurr() > 6) {
						cStationVoltThermal.get(k)
								.setDeltaCurrentFlexibilityBidStation(cStationVoltThermal.get(k).getCharCurr() - 6);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
					}
					if (cStationVoltThermal.get(k).getCharCurr() == 6) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(6);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(6);
					}
					if (cStationVoltThermal.get(k).getCharCurr() < 6) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(0);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
					}
				}

				if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 1) {

					if (cStationVoltThermal.get(k).getCharCurr() == 6) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(6);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(6);
					}
					if (cStationVoltThermal.get(k).getCharCurr() < 6) {
						cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(0);
						cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
					}

				}

				if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 0) {
					cStationVoltThermal.get(k).setDeltaCurrentFlexibilityBidStation(0);
					cStationVoltThermal.get(k).setCurrentMinimumStepBidStation(1);
				}
			}
			}
		}	
	}
	
	public void dynTable() {
		// 1.) Sort charging station-EV tandem according to amount of energy charged since beginning
		System.out.println(Arrays.toString(cStationSort.toArray()));
		int i, j;
		double key;
		ChargingStation temp;
		for (i = 1; i < cStationSort.size(); i++) {
			key = cStationSort.get(i).getLoadAmount();
			j = i - 1;

			while (j >= 0 && key < cStationSort.get(j).getLoadAmount()) {
				temp = cStationSort.get(j);
				cStationSort.set(j, cStationSort.get(j + 1));
				cStationSort.set(j + 1, temp);
				j--;
			}
		}

		// 2.) Calculate upper limit participation factor and define loop conditions
		for (int k = 0; k < cStationSort.size(); k++) {
			cStationSort.get(k).setParticipationFactor(0);
			cStationSort.get(k).setCarCorrectionFactor(0);
			cStationSort.get(k).calculateUpperLimitParticipationFactor();
		}
		
		int[] upperLimitPartFactorArray = new int[cStationSort.size()];
		for (int l = 0; l < cStationSort.size(); l++) {
			upperLimitPartFactorArray [l] = cStationSort.get(l).getUpperLimitParticipationFactor();
		}
        IntSummaryStatistics stat = Arrays.stream(upperLimitPartFactorArray).summaryStatistics();
        
        int max = stat.getMax();
        
		// 3.) Reloading and curtailment operations
        //Reloading operation
		if ((utFactorTransformatorPower < 0 && utFactorDSOThreshold < 0 && utFactorThresholdScheduling < 0)) {
			boolean breaker = false;
			for (int m = 0; m < max; m++) {
				for (int k = 0; k < cStationSort.size(); k++) {
					//charging stations in ascending order
					if (cStationSort.get(k).getParticipationFactor() < cStationSort.get(k).getUpperLimitParticipationFactor()) {
						cStationSort.get(k).incrementParticipationFactor();
						synchronizePhases();

						double tempUtFactorTransformatorPower = utFactorTransformatorPower;
						double tempUtFactorDSOThreshold = utFactorDSOThreshold;
						double tempUtFactorThresholdScheduling = utFactorThresholdScheduling;

						for (int l = 0; l < cStationVoltThermal.size(); l++) {
							tempUtFactorTransformatorPower += cStationSort.get(l).getCurrentStepA() * 230
									+ cStationSort.get(l).getCurrentStepB() * 230
									+ cStationSort.get(l).getCurrentStepC() * 230
									+ cStationSort.get(l).getCurrentStep3() * 230 * 3;
							tempUtFactorDSOThreshold += cStationSort.get(l).getCurrentStepA() * 230
									+ cStationSort.get(l).getCurrentStepB() * 230
									+ cStationSort.get(l).getCurrentStepC() * 230
									+ cStationSort.get(l).getCurrentStep3() * 230 * 3;
							tempUtFactorThresholdScheduling += cStationSort.get(l).getCurrentStepA() * 230
									+ cStationSort.get(l).getCurrentStepB() * 230
									+ cStationSort.get(l).getCurrentStepC() * 230
									+ cStationSort.get(l).getCurrentStep3() * 230 * 3;
						}
						System.out.println("RELOADING: temputtrafo: " + tempUtFactorTransformatorPower + "temputfactordso: " + tempUtFactorDSOThreshold + "temputfactorScheduling: " + tempUtFactorThresholdScheduling);

						if (tempUtFactorTransformatorPower > 0 || tempUtFactorDSOThreshold > 0 || tempUtFactorThresholdScheduling > 0) {

							cStationSort.get(k).incrementCarCorrectionFactor();
							cStationSort.get(k).setParticipationFactor(cStationSort.get(k).getParticipationFactor()
									- cStationSort.get(k).getCarCorrectionFactor());
							breaker = true;
							break;
						}

					}
				}
				if (breaker == true) {
					break;
				}
			}
			
			
			// Assign charging currents
			for (int k = 0; k < cStationSort.size(); k++) {
				cStationSort.get(k).setcStationSortmaxCharCurrThermalControl(
						cStationSort.get(k).getCharCurr() + cStationSort.get(k).getParticipationFactor()
								* cStationSort.get(k).getCurrentMinimumStepBidStation());
				cStationSort.get(k)
						.setMaxCharCurrThermalControl(cStationSort.get(k).getcStationSortmaxCharCurrThermalControl());
			}
		}

		
		// Curtailment operation
		if (utFactorDSOThreshold > 0 || utFactorTransformatorPower > 0 || utFactorThresholdScheduling > 0) {

			boolean breaker = false;	
			for (int m = 0; m < max; m++) {
				for (int k = (cStationSort.size()-1); k > -1; k--) {
					
					// charging stations in descending order
					if (cStationSort.get(k).getParticipationFactor() < cStationSort.get(k).getUpperLimitParticipationFactor()) {
						cStationSort.get(k).incrementParticipationFactor();
						synchronizePhases();

						double tempUtFactorTransformatorPower = utFactorTransformatorPower;
						double tempUtFactorDSOThreshold = utFactorDSOThreshold;
						double tempUtFactorThresholdScheduling = utFactorThresholdScheduling;

						for (int l = 0; l < cStationVoltThermal.size(); l++) {
							tempUtFactorTransformatorPower += -cStationSort.get(l).getCurrentStepA() * 230
									- cStationSort.get(l).getCurrentStepB() * 230
									- cStationSort.get(l).getCurrentStepC() * 230
									- cStationSort.get(l).getCurrentStep3() * 230 * 3;
							tempUtFactorDSOThreshold += -cStationSort.get(l).getCurrentStepA() * 230
									- cStationSort.get(l).getCurrentStepB() * 230
									- cStationSort.get(l).getCurrentStepC() * 230
									- cStationSort.get(l).getCurrentStep3() * 230 * 3;
							tempUtFactorThresholdScheduling += -cStationSort.get(l).getCurrentStepA() * 230
							- cStationSort.get(l).getCurrentStepB() * 230
							- cStationSort.get(l).getCurrentStepC() * 230
							- cStationSort.get(l).getCurrentStep3() * 230 * 3;
						}
						System.out.println("CURTAILMENT: temputtrafo: " + tempUtFactorTransformatorPower + "temputfactordso: " + tempUtFactorDSOThreshold + "tempUtFactorThresholdScheduling: " + tempUtFactorThresholdScheduling);


						if (tempUtFactorDSOThreshold < 0 && tempUtFactorTransformatorPower < 0 && tempUtFactorThresholdScheduling < 0) {
							breaker = true;
							break;
						}
					}
				}
				if (breaker == true)
					break;
			}

			// Assign charging currents
			for (int k = 0; k < cStationSort.size(); k++) {
				cStationSort.get(k).setcStationSortmaxCharCurrThermalControl(
						cStationSort.get(k).getCharCurr() - cStationSort.get(k).getParticipationFactor()
								* cStationSort.get(k).getCurrentMinimumStepBidStation());
				cStationSort.get(k)
						.setMaxCharCurrThermalControl(cStationSort.get(k).getcStationSortmaxCharCurrThermalControl());
			}
		}
	}
	
	private void synchronizePhases() {
		for (int l = 0; l < cStationSort.size(); l++) {
			cStationSort.get(l).setCurrentStepA(0);
			cStationSort.get(l).setCurrentStepB(0);
			cStationSort.get(l).setCurrentStepC(0);
			cStationSort.get(l).setCurrentStep3(0);
			if (cStationSort.get(l).getConnectionParameter() == 1) {
				cStationSort.get(l).setCurrentStepA(cStationSort.get(l).getCurrentMinimumStepBidStation()*cStationSort.get(l).getParticipationFactor());
			}
			if (cStationSort.get(l).getConnectionParameter() == 2) {
				cStationSort.get(l).setCurrentStepB(cStationSort.get(l).getCurrentMinimumStepBidStation()*cStationSort.get(l).getParticipationFactor());
			}
			if (cStationSort.get(l).getConnectionParameter() == 3) {
				cStationSort.get(l).setCurrentStepC(cStationSort.get(l).getCurrentMinimumStepBidStation()*cStationSort.get(l).getParticipationFactor());
			}
			if (cStationSort.get(l).getConnectionParameter() == 4) {
				cStationSort.get(l).setCurrentStep3(cStationSort.get(l).getCurrentMinimumStepBidStation()*cStationSort.get(l).getParticipationFactor());
			}
		}				
	}
	
	public void adjustAlgorithms (int k) {
		//Used for dynamic simulations, like MC
//		newArrivalDeparture(k);	
		
//		basicScheduling(k);

//		touScheduling(k);
		
		for (int l = 0; l < cStationVoltThermal.size(); l++) {
				if (cStationVoltThermal.get(l).getCurrentEV() != null && cStationVoltThermal.get(l).getLoadAmount() < 25000 && (cStationVoltThermal.get(l).getLadeplan().getStartZeitpunkt() <= k) && cStationVoltThermal.get(l).isControlTMBS() == true) {
				cStationVoltThermal.get(l).setDecisionVectorAtI(k + 1, 2);
				}
				System.out.println("LOAD AMOUNT Station " + cStationVoltThermal.get(l).getId() + ": " + cStationVoltThermal.get(l).getLoadAmount());
		}

//		Define a threshold out of limit
		thresholdScheduling = 1000000;		
	}
	
	
	private void basicScheduling (int k) {

		//Add and remove charging stations from scheduling queue
		for (int j = 0; j < cStationVoltThermal.size(); j++) {
			if (cStationVoltThermal.get(j).getCurrentEV() != null && cStationVoltThermal.get(j).getLadeplan().getStartZeitpunkt() <= k) {
				if ((!optScheduleQueque.contains(cStationVoltThermal.get(j)) && cStationVoltThermal.get(j).isControlTMBS() == true)) { // && k+1 < cStationVoltThermal.get(j).getLadeplan().getEndZeitpunkt()) {
					optScheduleQueque.add(cStationVoltThermal.get(j));
				}
			}
			
			if (cStationVoltThermal.get(j).getLoadAmount() >= 25000) {
					if (optScheduleQueque.contains(cStationVoltThermal.get(j))) {
						optScheduleQueque.remove(cStationVoltThermal.get(j));
					}
			}
		}
			
		//Calculate amount of energy charged in time slots
		for (int j = 0; j < optScheduleQueque.size(); j++) {	
			if (optScheduleQueque.get(j).getConnectionParameter() == 1 || optScheduleQueque.get(j).getConnectionParameter() == 2  || optScheduleQueque.get(j).getConnectionParameter() == 3)	{
				optScheduleQueque.get(j).setLoadAmountInTimeslots((int) (optScheduleQueque.get(j).getLoadAmount()/(16*230/6)));
			}
			if (optScheduleQueque.get(j).getConnectionParameter() == 4) {
				optScheduleQueque.get(j).setLoadAmountInTimeslots((int) (optScheduleQueque.get(j).getLoadAmount()/(48*230/6)));
			}
			
			//Calculate flexibility
			if (optScheduleQueque.get(j).getCurrentEV() != null && optScheduleQueque.get(j).getLadeplan().getStartZeitpunkt() <= k) {
				if (optScheduleQueque.get(j).getLoadAmountInTimeslots() <= optScheduleQueque.get(j).getChargingRequirement()) {
//					station1.setFlexibility(station1.getLadeplan().getEndZeitpunkt() - (station1.getChargingRequirement() - station1.getLoadAmountInTimeslots()) - k);
					optScheduleQueque.get(j).setFlexibility(optScheduleQueque.get(j).getLadeplan().getEndZeitpunkt() - (optScheduleQueque.get(j).getChargingRequirement() - optScheduleQueque.get(j).getLoadAmountInTimeslots()) - k - 1);
				}
			}
		}
		
		//Calculate charging current per time slot
		for (int j = 0; j < optScheduleQueque.size(); j++) {
			if (optScheduleQueque.get(j).getCurrentEV() != null && optScheduleQueque.get(j).getLadeplan().getStartZeitpunkt() <= k) {
				optScheduleQueque.get(j).setAvgLoadAmountPerTimeslot(Math.max(0, (int) ((optScheduleQueque.get(j).getChargingRequirementAbs() - optScheduleQueque.get(j).getLoadAmount())/(optScheduleQueque.get(j).getLadeplan().getRestIntervalle(k) - 1))));
//				cStationVoltThermal.get(j).setAvgLoadAmountPerTimeslot(Math.max(0, (int) ((cStationVoltThermal.get(j).getChargingRequirementAbs() - cStationVoltThermal.get(j).getLoadAmount())/(cStationVoltThermal.get(j).getLadeplan().getRestIntervalle(k) - cStationVoltThermal.get(j).getEmpiricTimeslotCounter()))));
			if (optScheduleQueque.get(j).getConnectionParameter() == 4) {
				optScheduleQueque.get(j).setPowerPerTimeslot(Math.min((optScheduleQueque.get(j).getAvgLoadAmountPerTimeslot()*6/230), 48));
			}
			if (optScheduleQueque.get(j).getConnectionParameter() == 1 || optScheduleQueque.get(j).getConnectionParameter() == 2 || optScheduleQueque.get(j).getConnectionParameter() == 3) {
				optScheduleQueque.get(j).setPowerPerTimeslot(Math.min((optScheduleQueque.get(j).getAvgLoadAmountPerTimeslot()*6/230), 16));
			}
			System.out.println("chR: " + optScheduleQueque.get(j).getChargingRequirement() + "      a: " + optScheduleQueque.get(j).getLoadAmountInTimeslots() + "     restslots: " + optScheduleQueque.get(j).getLadeplan().getRestIntervalle(k));
			System.out.println("AVG Load Amount: " + optScheduleQueque.get(j).getAvgLoadAmountPerTimeslot() + "   absolute load amount: " + optScheduleQueque.get(j).getLoadAmount());
			System.out.println("PPT: " + optScheduleQueque.get(j).getPowerPerTimeslot() + "    flexibility: " + optScheduleQueque.get(j).getFlexibility());
			}					
		}
		
		//Calculate sum of charging current per time slot
		double sumCCpT = 0;
		for (int j = 0; j < optScheduleQueque.size(); j++) {
			if (optScheduleQueque.get(j).getCurrentEV() != null) {
			sumCCpT += optScheduleQueque.get(j).getPowerPerTimeslot();
		}}
				
		//Sort charging stations accordingn to their flexibility
		Collections.sort(optScheduleQueque, new Comparator<ChargingStation>() {

			@Override
			public int compare(ChargingStation station1, ChargingStation station2) {
				// TODO Auto-generated method stub
				return station1.getFlexibility().compareTo(station2.getFlexibility());
			}
		});
		
		//Calculate number of cars for allocation of sum CCpT
		double sumCCpTTemp = 0;
		sumCCpTTemp = sumCCpT;
		
		int zähler = 0;
		while (sumCCpTTemp >= 0 && !optScheduleQueque.isEmpty()) {
			if (zähler > (optScheduleQueque.size()-1)) {
				break;
			}
				if (optScheduleQueque.get(zähler).getConnectionParameter() == 4) {
					sumCCpTTemp += -48;
				}
				if (optScheduleQueque.get(zähler).getConnectionParameter() == 1 || optScheduleQueque.get(zähler).getConnectionParameter() == 2 || optScheduleQueque.get(zähler).getConnectionParameter() == 3) {
					sumCCpTTemp += -16;
				}
				zähler++;
		}
		
		double counter = Math.min(zähler, optScheduleQueque.size());
		
		int numberOfEVsThreePhase = 0;
		int numberOfEVsOnePhase = 0;
		
		for (int b = 0; b < counter; b++) {
			if (optScheduleQueque.get(b).getConnectionParameter() == 4){
				numberOfEVsThreePhase += 1;
			}
			else {
				numberOfEVsOnePhase += 1;
			}
		}
		
		//Set internal scheduling threshold with a security margin
		thresholdScheduling = ((230*numberOfEVsThreePhase*48 + 230*numberOfEVsOnePhase*16)/0.90);
		
		//Set decision vector of EVs
		for (int j = 0; j < counter; j++) {
			if (!optScheduleQueque.isEmpty()) {
				optScheduleQueque.get(j).setDecisionVectorAtI(k + 1, 2);	
			}
		}
	}

	public void touScheduling (int i) {

		// Load stock data from database once
		if (ecopx == 0) {
			for (int j = 0; j < 288; j++) {
				db.stockPrice(j);
				stockPrice[j] = db.getStockPrice();
				if (j > 0) {
					stockPrice[j-1] = db.getStockPrice();
				}
			}
			ecopx = 1;
		}
				
		
		for (int l = 0; l < cStationVoltThermal.size(); l++) {
			if (cStationVoltThermal.get(l).isTouControl() == true) {
		if (cStationVoltThermal.get(l).getCurrentEV() != null) {
			if (cStationVoltThermal.get(l).getConnectionParameter() == 1 || cStationVoltThermal.get(l).getConnectionParameter() == 2  || cStationVoltThermal.get(l).getConnectionParameter() == 3)	{
				cStationVoltThermal.get(l).setLoadAmountInTimeslots((int) (cStationVoltThermal.get(l).getLoadAmount()/(16*230/6)));
			}
			if (cStationVoltThermal.get(l).getConnectionParameter() == 4) {
				cStationVoltThermal.get(l).setLoadAmountInTimeslots((int) (cStationVoltThermal.get(l).getLoadAmount()/(48*230/6)));
			}

			if (cStationVoltThermal.get(l).getLadeplan().getStartZeitpunkt() > i) {
			double [][] stockPriceSortA = new double [cStationVoltThermal.get(l).getLadeplan().getEndZeitpunkt()-cStationVoltThermal.get(l).getLadeplan().getStartZeitpunkt()][2];
			for (int j = 0; j < cStationVoltThermal.get(l).getLadeplan().getEndZeitpunkt()-cStationVoltThermal.get(l).getLadeplan().getStartZeitpunkt(); j++) {
				stockPriceSortA [j][1] = stockPrice [cStationVoltThermal.get(l).getLadeplan().getStartZeitpunkt()+j];
				stockPriceSortA [j][0] = cStationVoltThermal.get(l).getLadeplan().getStartZeitpunkt()+j;
//				System.out.println("stock price" + stockPriceSortA [j][1] + "            " + stockPriceSortA [j][0]);
			}
			Arrays.sort(stockPriceSortA, (a, b) -> Double.compare(a[1], b[1]));
			int counter = Math.max(0, cStationVoltThermal.get(l).getChargingRequirement() - cStationVoltThermal.get(l).getLoadAmountInTimeslots());
			for (int j = 0; j < counter; j++) {
//				decisionVectorA [(int) stockPriceSortA [j][0]] = 2;
					cStationVoltThermal.get(l).setDecisionVectorAtI((int) stockPriceSortA [j][0], 2);
				}
			
			}
			
						
			if (cStationVoltThermal.get(l).getLadeplan().getStartZeitpunkt() <= i) {
			double [][] stockPriceSortA = new double [cStationVoltThermal.get(l).getLadeplan().getEndZeitpunkt()-i][2];
			for (int j = 0; j < cStationVoltThermal.get(l).getLadeplan().getEndZeitpunkt()-i; j++) {
				stockPriceSortA [j][1] = stockPrice [i+j];
				stockPriceSortA [j][0] = i+j;
				System.out.println("stock price" + stockPriceSortA [j][1] + "            " + stockPriceSortA [j][0]);
				
			}
			Arrays.sort(stockPriceSortA, (a, b) -> Double.compare(a[1], b[1]));
			int counter = 0;
			counter = Math.max(0, Math.min(cStationVoltThermal.get(l).getLadeplan().getEndZeitpunkt()-i, cStationVoltThermal.get(l).getChargingRequirement() - cStationVoltThermal.get(l).getLoadAmountInTimeslots()));
			for (int j = 0; j < counter; j++) {
//				decisionVectorA [(int) stockPriceSortA [j][0]] = 2;
					cStationVoltThermal.get(l).setDecisionVectorAtI((int) stockPriceSortA [j][0], 2);
				}
			}	
		}		
			}
	}
	}
	
	//used for MC simulations or dynamic simulations with arriving and leaving EVs
	public void newArrivalDeparture(int k) {
		
		//arrival of new task
		for (int l = 0; l < cStationVoltThermal.size(); l++) {
			if ((cStationVoltThermal.get(l).getLadeplan() == null || k + 1 == cStationVoltThermal.get(l).getLadeplan().getEndZeitpunkt())) {
			if (!cStationFree.contains(cStationVoltThermal.get(l))) {
				cStationFree.add(cStationVoltThermal.get(l));
				cStationVoltThermal.get(l).setCurrentEV(null);
//				cStationFree.add(station1);
				cStationVoltThermal.get(l).setFlexibility(100);
				cStationVoltThermal.get(l).setLoadAmountInTimeslots(0);
				cStationVoltThermal.get(l).setLoadAmount(0);
				cStationVoltThermal.get(l).setMaxCharCurrVoltControl(0);
				cStationVoltThermal.get(l).setMaxCharCurrThermalControl(0);
				optScheduleQueque.remove(cStationVoltThermal.get(l));
				neuerLauf = false;
				System.out.println("Ladestation " + cStationVoltThermal.get(l).getId() +" frei!");		
			}
		}}

		if (db.isNewTask() == true && !cStationFree.isEmpty()) {
			System.out.println("arrtime: " + db.getArrivaltime() + "deptime: " + db.getDeparturetime());
			System.out.println("arrtimetwo: " + db.getArrivaltimeTwo() + "deptimetwo: " + db.getDeparturetimeTwo());
			int tempStartZeitpunkt = db.getArrivaltime();
			int tempL = db.getDeparturetime();
			int tempEndzeitpunkt = tempL + tempStartZeitpunkt;
			ElectricVehicle auto1 = new ElectricVehicle("Nissan Leaf", 16, 3700, 67, 40, 360, false);
			ElectricVehicle auto2 = new ElectricVehicle("BMW i3", 16, 3700, 94, 30, 353, false);
			ElectricVehicle auto3 = new ElectricVehicle("Smart", 16, 3700, 67, 25, 360, true);
			autoList.add(auto1);
			autoList.add(auto2);
			autoList.add(auto3);
			Random rn = new Random();
			int tempAuto = rn.nextInt(3);
//			Auto auto = new Auto ("Smart", 16, 3700, 67, 25, 360, rn.nextBoolean());
//			autoList.get(rn.nextInt(3-1 + 1) + 1);
			Schedule scheduel = new Schedule(tempStartZeitpunkt, tempL);
			cStationFree.get(0).assignEV(autoList.get(tempAuto), scheduel, 15000);
			cStationFree.remove(0);
			autoList.clear();
//			autoList.remove(tempAuto);
			if (db.isTwoTask() == true && !cStationFree.isEmpty()) {
//				System.out.println("twotask: " + db.isTwoTask());
				ElectricVehicle auto1Two = new ElectricVehicle("Nissan Leaf", 16, 3700, 67, 40, 360, false);
				ElectricVehicle auto2Two = new ElectricVehicle("BMW i3", 16, 3700, 94, 30, 353, false);
				ElectricVehicle auto3Two = new ElectricVehicle("Smart", 16, 3700, 67, 25, 360, true);
				autoList.add(auto1Two);
				autoList.add(auto2Two);
				autoList.add(auto3Two);
				int tempAutoTwo = rn.nextInt(3);
				int tempStartZeitpunktTwo = db.getArrivaltimeTwo();
				int tempLTwo = db.getDeparturetimeTwo();
				Schedule scheduelTwo = new Schedule(tempStartZeitpunktTwo, tempLTwo);
				cStationFree.get(0).assignEV(autoList.get(tempAutoTwo), scheduelTwo, 15000);
				cStationFree.remove(0);
				autoList.clear();
				
				System.out.println("ArrTime Auto 1: " + db.getArrivaltime() + "DepTime Auto 1: " + db.getDeparturetime());
				System.out.println("ArrTime Auto 1: " + db.getArrivaltimeTwo() + "DepTime Auto 2: " + db.getDeparturetimeTwo());
			}

		}
		
		db.setNewTask(false);
		db.setTwoTask(false);
		db.setArrivaltime(0);
		db.setArrivaltimeTwo(0);
		db.setDeparturetime(0);
		db.setDeparturetimeTwo(0);
		
		if (db.isNewTask() == true && cStationFree.isEmpty()) {
			db.setNewTask(false);
			System.out.println("Auto kann nicht angenommen werden, da alle Ladestationen besetzt sind!");
		}
	}

	//Modified version of thermalManagement + DynTable for the control of a battery 
	public void thermalControlVRFB() {
	utFactorTrafoVRFB = transformatorLoading - transformatorLoadingRated*0.9;	
	
	//Feeding offer
	if (utFactorTrafoVRFB > 0) {
		test = true;
				if (VRFB.getPower() == 0) {
					VRFB.setDeltaCurrentFlexibilityBidStation(6);
					VRFB.setCurrentMinimumStepBidStation(6);
				}
				if (VRFB.getPower() < 0) {
					VRFB.setDeltaCurrentFlexibilityBidStation(16);
					VRFB.setCurrentMinimumStepBidStation(1);
				}
				if (VRFB.getPower() <= VRFB.getMAXDISCHARGEPOWER()) {
					VRFB.setDeltaCurrentFlexibilityBidStation(0);
					VRFB.setCurrentMinimumStepBidStation(1);
				}
	}
	
	//Feeding stop offer
	if (utFactorTrafoVRFB < 0  && test == true) {
		if (VRFB.getPower() == 0) {
			VRFB.setDeltaCurrentFlexibilityBidStation(0);
			VRFB.setCurrentMinimumStepBidStation(1);
			test = false;
		}
		else {
			VRFB.setDeltaCurrentFlexibilityBidStation(16);
			VRFB.setCurrentMinimumStepBidStation(1);
		}
		if (VRFB.getPower() > 0) {
			VRFB.setDeltaCurrentFlexibilityBidStation(6);
			VRFB.setCurrentMinimumStepBidStation(1);
//			VRFB.setPower(0);
//			test = false;
		}
	}
		
	VRFB.setParticipationFactor(0);
	VRFB.setCorrectionFactor(0);
	VRFB.calculateUpperLimitParticipationFactor();
	
	//Feeding offer admission
	if (utFactorTrafoVRFB > 0) {
		
			for (int i = 0; i < VRFB.getUpperLimitParticipationFactor(); i++) {
				if (VRFB.getParticipationFactor() < VRFB.getUpperLimitParticipationFactor()) {
					VRFB.incrementParticipationFactor();
					VRFB.setCurrentStep(VRFB.getCurrentMinimumStepBidStation() * VRFB.getParticipationFactor());

					double tempUtFactorTrafoVRFB = utFactorTrafoVRFB;
					tempUtFactorTrafoVRFB += -VRFB.getCurrentStep() * 230;

					System.out.println("tempUtFactorTrafoVRFB : " + tempUtFactorTrafoVRFB);

					if (tempUtFactorTrafoVRFB < 0) {
						VRFB.incrementCorrectionFactor();
						VRFB.setParticipationFactor(VRFB.getParticipationFactor() - VRFB.getCorrectionFactor());
//			breaker = true;
						break;
					}
				}
			}
	double temppower = VRFB.getPower() - VRFB.getParticipationFactor() * VRFB.getCurrentMinimumStepBidStation() * 230;
	VRFB.setPower(Math.max(VRFB.getMAXDISCHARGEPOWER(), temppower));
	}
	
	//Feeding stop admission
	if (utFactorTrafoVRFB < 0 && VRFB.getPower() <= 0) {
		
			for (int i = 0; i < VRFB.getUpperLimitParticipationFactor(); i++) {
				if (VRFB.getParticipationFactor() < VRFB.getUpperLimitParticipationFactor()) {
					VRFB.incrementParticipationFactor();
					VRFB.setCurrentStep(VRFB.getCurrentMinimumStepBidStation() * VRFB.getParticipationFactor());

					double temppower1 = VRFB.getPower();
					double tempUtFactorTrafoVRFB = utFactorTrafoVRFB;
					
					tempUtFactorTrafoVRFB += +VRFB.getCurrentStep() * 230;
					temppower1 += +VRFB.getParticipationFactor() * VRFB.getCurrentMinimumStepBidStation() * 230;
					
					System.out.println("tempUtFactorTrafoVRFB : " + tempUtFactorTrafoVRFB);
					
					if (tempUtFactorTrafoVRFB > 0) {
						VRFB.incrementCorrectionFactor();
						VRFB.setParticipationFactor(VRFB.getParticipationFactor() - VRFB.getCorrectionFactor());
						if (temppower1 >= 0) {
							VRFB.setParticipationFactor(0);
							VRFB.setPower(0);
						}
//			breaker = true;
						break;
					}
					if (temppower1 >= 0) {
						VRFB.setParticipationFactor(0);
						VRFB.setPower(0);
						break;
					}
				}
			}	
	double temppower = VRFB.getPower() + VRFB.getParticipationFactor() * VRFB.getCurrentMinimumStepBidStation() * 230;
	VRFB.setPower(temppower);
	
	}
	}
	

	//Superordinate control algorithm; change lines here to send charging current to EVCC
	//Calculate amount of energy according to the defined sampling time
	public void controlAlgorithm (int i) {

		for (int k = 0; k < cStationVoltThermal.size(); k++) {
			for (ChargingStation station : cStationSort) {
				if (station.getId().equals(cStationVoltThermal.get(k).getId())) {
					cStationVoltThermal.get(k).setMaxCharCurrThermalControl(station.getMaxCharCurrThermalControl());
				}
			}
		}
		
		for (int k = 0; k < cStationVoltThermal.size(); k++) {
			cStationVoltThermal.get(k).setMostRestrictiveCurrLimitVoltThermal(Math.min(cStationVoltThermal.get(k).getMaxCharCurrVoltControl(), cStationVoltThermal.get(k).getMaxCharCurrThermalControl()));
		}
						
		for (int k = 0; k < cStationVoltThermal.size(); k++) {
		if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 2) {
//			cStationVoltThermal.get(k).setCharCurrReal(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
			cStationVoltThermal.get(k).setCharCurr(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
			if (cStationVoltThermal.get(k).getConnectionParameter() == 4) {
				cStationVoltThermal.get(k).setCharCurrA(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
				cStationVoltThermal.get(k).setCharCurrB(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
				cStationVoltThermal.get(k).setCharCurrC(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 1) {
				cStationVoltThermal.get(k).setCharCurrA(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
				cStationVoltThermal.get(k).setCharCurrB(0);
				cStationVoltThermal.get(k).setCharCurrC(0);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 2) {
				cStationVoltThermal.get(k).setCharCurrB(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
				cStationVoltThermal.get(k).setCharCurrA(0);
				cStationVoltThermal.get(k).setCharCurrC(0);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 3) {
				cStationVoltThermal.get(k).setCharCurrC(cStationVoltThermal.get(k).getMostRestrictiveCurrLimitVoltThermal());
				cStationVoltThermal.get(k).setCharCurrA(0);
				cStationVoltThermal.get(k).setCharCurrB(0);
			}
		}

		if (cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 0) {
//			cStationVoltThermal.get(k).setCharCurrReal(0);
			cStationVoltThermal.get(k).setCharCurr(0);
			cStationVoltThermal.get(k).setCharCurrA(0);
			cStationVoltThermal.get(k).setCharCurrB(0);
			cStationVoltThermal.get(k).setCharCurrC(0);
		}

		
		if (cStationVoltThermal.get(k).isControlTMBS() == false && cStationVoltThermal.get(k).isTouControl() == false) {
			
//			if (cStationVoltThermal.get(k).getAktuellesAuto() != null && cStationVoltThermal.get(k).getLoadAmount() < 25000 && cStationVoltThermal.get(k).getLadeplan().getStartZeitpunkt() <= i && cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 2) {//iscontrol
			if (cStationVoltThermal.get(k).getCurrentEV() != null && cStationVoltThermal.get(k).getLoadAmount() < 25000 && cStationVoltThermal.get(k).getLadeplan().getStartZeitpunkt() <= i) {//iscontrol

			int manualControlValue50 = cStationVoltThermal.get(k).getCurrentEV().getMAXCHARCURREV();
			cStationVoltThermal.get(k).setCharCurr(manualControlValue50);
			cStationVoltThermal.get(k).setMostRestrictiveCurrLimitVoltThermal(0);
			cStationVoltThermal.get(k).setMaxCharCurrVoltControl(0);
			cStationVoltThermal.get(k).setMaxCharCurrThermalControl(0);
			
			if (cStationVoltThermal.get(k).getConnectionParameter() == 4) {
				cStationVoltThermal.get(k).setCharCurrA(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrB(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrC(manualControlValue50);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 1) {
				cStationVoltThermal.get(k).setCharCurrA(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrB(0);
				cStationVoltThermal.get(k).setCharCurrC(0);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 2) {
				cStationVoltThermal.get(k).setCharCurrB(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrA(0);
				cStationVoltThermal.get(k).setCharCurrC(0);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 3) {
				cStationVoltThermal.get(k).setCharCurrC(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrA(0);
				cStationVoltThermal.get(k).setCharCurrB(0);
			}
		}
		}
		
		if (cStationVoltThermal.get(k).isControlTMBS() == false && cStationVoltThermal.get(k).isTouControl() == true) {

			if (cStationVoltThermal.get(k).getCurrentEV() != null && cStationVoltThermal.get(k).getLoadAmount() < 25000 && cStationVoltThermal.get(k).getLadeplan().getStartZeitpunkt() <= i  && cStationVoltThermal.get(k).getDecisionVectorAtI(i) == 2) {//iscontrol

			int manualControlValue50 = cStationVoltThermal.get(k).getCurrentEV().getMAXCHARCURREV();
			cStationVoltThermal.get(k).setCharCurr(manualControlValue50);
			cStationVoltThermal.get(k).setMostRestrictiveCurrLimitVoltThermal(0);
			cStationVoltThermal.get(k).setMaxCharCurrVoltControl(0);
			cStationVoltThermal.get(k).setMaxCharCurrThermalControl(0);
			
			if (cStationVoltThermal.get(k).getConnectionParameter() == 4) {
				cStationVoltThermal.get(k).setCharCurrA(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrB(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrC(manualControlValue50);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 1) {
				cStationVoltThermal.get(k).setCharCurrA(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrB(0);
				cStationVoltThermal.get(k).setCharCurrC(0);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 2) {
				cStationVoltThermal.get(k).setCharCurrB(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrA(0);
				cStationVoltThermal.get(k).setCharCurrC(0);
			}
			if (cStationVoltThermal.get(k).getConnectionParameter() == 3) {
				cStationVoltThermal.get(k).setCharCurrC(manualControlValue50);
				cStationVoltThermal.get(k).setCharCurrA(0);
				cStationVoltThermal.get(k).setCharCurrB(0);
			}
		}
		}
		
		
//		cStationVoltThermal.get(k).setLoadAmount(cStationVoltThermal.get(k).getLoadAmount() + (cStationVoltThermal.get(k).getCharCurrTest() * cStationVoltThermal.get(k).getVoltageCStation() /3600));
//		cStationVoltThermal.get(k).setLoadAmount(cStationVoltThermal.get(k).getLoadAmount() + (cStationVoltThermal.get(k).getCharCurrTest() * cStationVoltThermal.get(k).getVoltageCStation() /120));
		cStationVoltThermal.get(k).setLoadAmount(cStationVoltThermal.get(k).getLoadAmount() + (cStationVoltThermal.get(k).getCharCurrSum() * cStationVoltThermal.get(k).getVoltageCStation() /60));
		}
		}
	
	//Only relevant for MC
	public void neuerDurchgang() {
		if (neuerLauf == true) {
			for (int l = 0; l < cStationVoltThermal.size(); l++) {
				cStationVoltThermal.get(l).setCurrentEV(null);
				cStationFree.add(cStationVoltThermal.get(l));
				cStationVoltThermal.get(l).setFlexibility(100);
				cStationVoltThermal.get(l).setLoadAmountInTimeslots(0);
				cStationVoltThermal.get(l).setLoadAmount(0);
				cStationVoltThermal.get(l).setMaxCharCurrVoltControl(0);
				optScheduleQueque.remove(cStationVoltThermal.get(l));
				System.out.println("Ladestation  " + cStationVoltThermal.get(l).getId() + " frei!");
				
				for (int i = 0; i < 288; i++) {
					cStationVoltThermal.get(l).setDecisionVectorAtI(i, 0);
				}	
			}
			neuerLauf = false;	
		}
	}
			
	public void thresholdDSO (double thresholdDSO) {
		this.thresholdDSO = thresholdDSO;	
	}
		
	public double getThresholdDSO() {
		return thresholdDSO;
	}

	public double getTransformatorLoading() {
		return transformatorLoading;
	}

	public void setTransformatorLoading(double transformatorLoading) {
		this.transformatorLoading = transformatorLoading;
	}

	public double getStockPrice(int i) {
		return stockPrice [i];
	}

	public boolean isNeuerLauf() {
		return neuerLauf;
	}

	public void setNeuerLauf(boolean neuerLauf) {
		this.neuerLauf = neuerLauf;
	}

	public double getVoltageControlThreshold() {
		return voltageControlThreshold;
	}

	public void setVoltageControlThreshold(double voltageControlThreshold) {
		this.voltageControlThreshold = voltageControlThreshold;
	}

	public double getTransformatorLoadingRated() {
		return transformatorLoadingRated;
	}

	public void setTransformatorLoadingRated(double transformatorLoadingRated) {
		this.transformatorLoadingRated = transformatorLoadingRated;
	}

	public double getTransformatorSecurityFactor() {
		return transformatorSecurityFactor;
	}

	public void setTransformatorSecurityFactor(double transformatorSecurityFactor) {
		this.transformatorSecurityFactor = transformatorSecurityFactor;
	}

	public double getdSOThresholdSecurityFactor() {
		return dSOThresholdSecurityFactor;
	}

	public void setdSOThresholdSecurityFactor(double dSOThresholdSecurityFactor) {
		this.dSOThresholdSecurityFactor = dSOThresholdSecurityFactor;
	}

}
