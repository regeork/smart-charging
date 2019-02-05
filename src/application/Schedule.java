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

public class Schedule {

	
	//Variablen
	/***
	 * Creates an Array with 144 positions. Represents a day divided in parts of 10 minutes.
	 */
	public int[] timeTable = new int[288];
	
	public int[] timeTableCar = new int[288];
	
	/***
	 * Gibt die vom Benutzer angegebenen Ladeintervalle an. z.B 5 Intervalle entsprechen 5*10 Minute Aufladen.
	 */
	public int intervall;
	/***
	 * Gibt die restlichen Ladeintervalle an.
	 */
	public int restIntervall;
	/***
	 * Startzeitpunkt, ab dem das Auto an die Ladestation angeschlossen ist. 
	 */
	private int startZeitpunkt = 0;
	/***
	 * Zeitpunkt, ab dem das Auto von der Ladestation genommen wird
	 */
	private int endZeitpunkt = 0;
	
	
	//Konstruktoren
	
	/***
	 * Verbesserter Konstruktor
	 * 
	 * @param startZeitpunkt : Startzeitpunkt aus Scanner
	 * @param endZeitpunkt : Endzeitpunkt aus Scanner
	 */
	public Schedule (int startZeitpunkt, int endZeitpunkt) {
		this.startZeitpunkt = startZeitpunkt;
		this.endZeitpunkt = endZeitpunkt;
		this.intervall = endZeitpunkt - startZeitpunkt;
		this.restIntervall = this.intervall;
		for (int i = startZeitpunkt; i < endZeitpunkt; i++) {
			timeTable[i]=i;
		}
	}
	
	public Schedule (int startZeitpunkt) {
		this.startZeitpunkt = startZeitpunkt;
		this.restIntervall = 288 - startZeitpunkt;
		this.endZeitpunkt = 288;
		for (int i = startZeitpunkt; i < 288; i++) {
			timeTableCar[i]=i;
		}
	}
	
	
	
	
	//Methoden
	/***
	 * reduziert verbleibende Anzahl Intervalle um 1 
	 */
	public void restIntervalleRunter() {
		restIntervall--;
	}


	public int getStartZeitpunkt() {
		return startZeitpunkt;
	}


	public void setStartZeitpunkt(int startZeitpunkt) {
		this.startZeitpunkt = startZeitpunkt;
	}


	public int getEndZeitpunkt() {
		return endZeitpunkt;
	}


	public int getRestIntervalle(int k) {
		this.restIntervall = endZeitpunkt - k;
		return restIntervall;
	}


	public void setEndZeitpunkt(int endZeitpunkt) {
		this.endZeitpunkt = endZeitpunkt;
	}


	
}
