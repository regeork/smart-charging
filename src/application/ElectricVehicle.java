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

public class ElectricVehicle {

	/***
	 * maximum allowable charging current [A]
	 */
	private final int MAXCHARCURREV;
	
	/***
	 * Maximum allowable charging power [W]
	 */
	private final int MAXCHARPOWER;
	
	/***
	 * Name of EV
	 */
	private String name;
	
	/***
	 * State of Charge (SoC) of EV [%]
	 */
	private double soc;
	
	/***
	 * Capacity of battery [Ah]
	 */
	private final int BATTERYCAPACITY;
	
	/***
	 * Rated voltage of battery [V]
	 */
	private final int BATTERYRATEDVOLTAGE;
	
	/***
	 * Capacity of battery [Wh]
	 */
	private final int GESAMTCAPACITY;
	
	/***
	 * Amount of energy in battery [Wh]
	 */
	private double ladung;
	
	/***
	 * Amount of energy missing in battery [Wh]
	 */
	private double restLadung;
		
	/***
	 * Represents the connection parameter of the EV
	 * false = single phase
	 * true = three phase
	 */
	private boolean connection;
	
		

	
	/***
	 * Instanziert ein Auto mit folgenden Parametern
	 * @param m Name
	 * @param maxChargeCurrEV Maximal charging current [A]
	 * @param maxChargepowerEV Maximal charging power [kW]
	 * @param capacity Capacity [Ah]
	 * @param soc SOC [%]
	 * @param batRatVol Rated voltage of the battery [V]
	 * @param connection Conncection 1-Phase or 3-Phase
	 */
	public ElectricVehicle(String m, int maxChargeCurrEV, int maxChargepowerEV, int capacity, double soc, int batRatVol, boolean connection) {
		this.name = m;
		this.MAXCHARCURREV = maxChargeCurrEV;
		this.MAXCHARPOWER = maxChargepowerEV;
		this.BATTERYCAPACITY = capacity;
		this.soc = soc;
		this.BATTERYRATEDVOLTAGE = batRatVol;
		this.GESAMTCAPACITY = (BATTERYCAPACITY * BATTERYRATEDVOLTAGE);
		this.ladung = GESAMTCAPACITY * (soc/100);
		this.setConnection(connection);
	}

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMAXCHARCURREV() {
		return MAXCHARCURREV;
	}

	public int getMAXCHARPOWER() {
		return MAXCHARPOWER;
	}

	public double getSoc() {
		return soc;
	}

	public void setSoc(double soc) {
		this.soc = soc;
	}

	public int getBATTERYCAPACITY() {
		return BATTERYCAPACITY;
	}

	public int getBATTERYRATEDVOLTAGE() {
		return BATTERYRATEDVOLTAGE;
	}

	public int getGESAMTCAPACITY() {
		return GESAMTCAPACITY;
	}
	


	public double getLadung() {
		return ladung;
	}
	


	public void setLadung(int ladung) {
		this.ladung = ladung;
	}

	public double getRestLadung() {
		return restLadung;
	}

	public void setRestLadung(int restLadung) {
		this.restLadung = restLadung;
	}
	
	/***
	 * Lädt das Auto mit der vorgegebenen Stromstärke und Spannung aus der ChargingStation.
	 * @param charCurr [A]
	 * @param voltageCStation [V]
	 */
	public void laden (double charCurr, double voltageCStation) {
		ladung = ladung + (charCurr * voltageCStation)/(6*600);
		soc = 100*(ladung/GESAMTCAPACITY);
	}

	public void curtailment () {
		
	}


	public boolean isConnection() {
		return connection;
	}


	public void setConnection(boolean connection) {
		this.connection = connection;
	}


	
	
	
}
