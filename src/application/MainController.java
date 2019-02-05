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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXButton;

import de.re.easymodbus.modbusclient.ModbusClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;


public class MainController implements Initializable {
	
	//Instantiate properties and database 
	Properties prop = new Properties();
	SQLiteDB db = new SQLiteDB();

	//Modbus clients for EVCCs and measuring devices
	ModbusClient clientEVCC1 = new ModbusClient();
	ModbusClient clientEVCC2 = new ModbusClient();
	ModbusClient clientKoCos1 = new ModbusClient();
	ModbusClient clientKoCos2 = new ModbusClient();
	
	//Variables measuring data
	double kocosV1_car;
	double kocosV2_car;
	double kocosV3_car;
	
	double kocosI1_car;
	double kocosI2_car;
	double kocosI3_car;
	
	double kocosI1_load;
	double kocosI2_load; 
	double kocosI3_load; 	
	
	
	//Instantiate array for charging stations
	/***
	 * Charging stations are added to array list in chronological order according to their id
	 */
	private ArrayList <ChargingStation> cStationVoltThermal = new ArrayList <ChargingStation> ();
	
	/***
	 * Charging stations in cStationSort are dynamically sorted with every time step according to their already charged amount of energy. Based on this ranking, DynTable determines which charging station is charged first
	 */
	private ArrayList <ChargingStation> cStationSort = new ArrayList <ChargingStation> ();
		
	//Instantiate VRFB, optional
	VRFB VRFB = new VRFB (50);
	
	
	//Create "two days". 288 timeslots á 10 minutes
	Schedule timeTable = new Schedule (0, 288);
		
	Controller controler = new Controller (cStationVoltThermal, cStationSort, db, VRFB);	
 
	int timeSlot = 0;

	int timeSynchron = 0;
	
	private boolean stopAlgorithm;
	
	/***
	 * Thread that simulates voltage dips and transformer loading
	 */
	Thread operationalConditions = new Thread(new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
				for (int time = 0; time < (timeTable.getEndZeitpunkt()*12000); time++) {
					
					//load data from DB
					db.selectData(timeSynchron);
					
					//calculate EV load
		            int carLoad = 0;
		            for (int m = 0; m < cStationVoltThermal.size(); m++) {
		               	carLoad += cStationVoltThermal.get(m).getCharCurrSum()*230;
		            }
		              
		            //set transformer load
		            controler.setTransformatorLoading(db.getTransformerLoad() + carLoad + VRFB.getPower());

		            
					if (stopAlgorithm == true) {
						break;
					}
					
					//2860 entspricht 286 time slot. not to the end due to irgendwas
					if (timeSynchron == 2860) {
						break;
					}

					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
		}
	});
	 
	Thread simulation = new Thread (new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			

			/***
			 * 	resolution is for example 144*20 = 2880. this is 30 seconds per loop
				resolution for laboratory work could be one second. voltage control is performed every 10 seconds. thermal management is performed every 30 seconds
				for real time application the factor needs to be 600. 144*60 = 86400 = 60*60*24
			 */
			for (int time = 0; time < (timeTable.getEndZeitpunkt()*20); time++) {

				timeSynchron = time;
				
				//choose algorithm defined in the property file
				controler.chooseAlgorithm(Integer.parseInt(prop.getProperty("ChooseAlgorithm")), time, timeSlot);

				//define length of time slot
				if (time % 10 == 0) {
					timeSlot++;
							
					db.setOnce(0);
					
					//save current app configuration
					try {
						OutputStream out = new FileOutputStream("appConfiguration.properties");
						prop.store(out, null);
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				//Calculate EV load
	            int eVLoad = 0;
	            for (int m = 0; m < cStationVoltThermal.size(); m++) {
	              	eVLoad += cStationVoltThermal.get(m).getCharCurrSum()*230;
	            }

                // Get system time
                DateFormat df = new SimpleDateFormat("HH:mm:ss");
                Date dateobj = new Date();
                String systemTimeString = df.format(dateobj);
                
////                Optional: Read measuring data from device
//            	try {
//					kocosV1_car = clientKoCos2.ConvertRegistersToFloat(clientKoCos2.ReadInputRegisters(4402, 2));
//	            	kocosV2_car = clientKoCos2.ConvertRegistersToFloat(clientKoCos2.ReadInputRegisters(4404, 2));
//	            	kocosV3_car = clientKoCos2.ConvertRegistersToFloat(clientKoCos2.ReadInputRegisters(4406, 2));
//	            	
//	            	kocosI1_car = clientKoCos2.ConvertRegistersToFloat(clientKoCos2.ReadInputRegisters(5234, 2));
//	            	kocosI2_car = clientKoCos2.ConvertRegistersToFloat(clientKoCos2.ReadInputRegisters(5236, 2));
//	            	kocosI3_car = clientKoCos2.ConvertRegistersToFloat(clientKoCos2.ReadInputRegisters(5238, 2));
//	            	
//	            	kocosI1_load = clientKoCos1.ConvertRegistersToFloat(clientKoCos1.ReadInputRegisters(5234, 2));
//	            	kocosI2_load = clientKoCos1.ConvertRegistersToFloat(clientKoCos1.ReadInputRegisters(5236, 2));
//	            	kocosI3_load = clientKoCos1.ConvertRegistersToFloat(clientKoCos1.ReadInputRegisters(5238, 2));
//				} catch (IllegalArgumentException | ModbusException | IOException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}

            db.insertResidentialArea("residentialArea",  cStationVoltThermal.get(0).getLoadAmount(),  cStationVoltThermal.get(1).getLoadAmount(),  cStationVoltThermal.get(2).getLoadAmount(),  cStationVoltThermal.get(3).getLoadAmount(),  cStationVoltThermal.get(4).getLoadAmount(),  cStationVoltThermal.get(5).getLoadAmount(),  cStationVoltThermal.get(6).getLoadAmount(),  cStationVoltThermal.get(7).getLoadAmount(),  cStationVoltThermal.get(8).getLoadAmount(),  cStationVoltThermal.get(9).getLoadAmount(),  cStationVoltThermal.get(10).getLoadAmount(),  cStationVoltThermal.get(11).getLoadAmount(),  cStationVoltThermal.get(12).getLoadAmount(),  cStationVoltThermal.get(13).getLoadAmount(),  cStationVoltThermal.get(14).getLoadAmount(),  cStationVoltThermal.get(15).getLoadAmount(),  cStationVoltThermal.get(16).getLoadAmount(),  cStationVoltThermal.get(17).getLoadAmount(),  cStationVoltThermal.get(18).getLoadAmount(),  cStationVoltThermal.get(19).getLoadAmount(), controler.getTransformatorLoading(), 0, systemTimeString, VRFB.getPower(), eVLoad, controler.getStockPrice(timeSlot));
				
        
            
            //Virtually charge cars
			for (int l = 0; l < cStationVoltThermal.size(); l++) {
				cStationVoltThermal.get(l).ladeAuto();
			}

			//2860 entspricht 286 time slot. not to the end due to irgendwas	
			if (timeSynchron == 2860) {
				break;
			}

			if (stopAlgorithm == true) {
				break;
			}
				
			cStationVoltThermal.get(0).printValues();
				
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}				
			}
			db.closeConnection();				
		}
		
	});
	

    @FXML
    private JFXButton startButton;
    
    @FXML
    private JFXButton cancelButton;
        
    @FXML
    private AnchorPane arrivalDeparture;
        
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub
		
		//Modbus: Connect to Slaves
//		try {
//			//EVCC
//			clientEVCC1.Connect("129.217.210.206", 502);
//			//EVCC needs adress to be 180
//			clientEVCC1.setUnitIdentifier((byte) 180);
//			
//			clientEVCC2.Connect("129.217.210.205", 502);
//			//EVCC needs adress to be 180
//			clientEVCC2.setUnitIdentifier((byte) 180);
//			
//			//KoCos
//			clientKoCos1.Connect("129.217.210.194", 502);
//			clientKoCos2.Connect("129.217.210.193", 502);
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		try {
//			System.out.println("inetadress get local host: " + InetAddress.getLocalHost());
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		//Initiate charging stations
		
		//Real charging station with modbus interface
	    ChargingStation cStationReal1 = new ChargingStation(0, 4, clientEVCC1);
	    ChargingStation cStationReal2 = new ChargingStation(1, 4, clientEVCC2);
	    cStationVoltThermal.add(cStationReal1);
	    cStationVoltThermal.add(cStationReal2);
		
	    //Virtual charging stations
		for (int l = 2; l < 20; l++) {
		    ChargingStation cStation = new ChargingStation(l, 4);
			cStationVoltThermal.add(cStation);
		}
		
		ElectricVehicle auto1 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 20 %pen; 50% yes
		Schedule schedule1 = new Schedule (117, 188); //arrival 19:30 ; dep 7:20
		cStationVoltThermal.get(0).assignEV(auto1, schedule1, 25000);
		cStationVoltThermal.get(0).setControlTMBS(true);
//		cStationVoltThermal.get(0).setTouControl(true);
		
		ElectricVehicle auto2 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//single phase; 20% pen; 50% yes
		Schedule schedule2 = new Schedule (126, 193); //arrival 21:00 ; dep 8:10
		cStationVoltThermal.get(1).assignEV(auto2, schedule2, 25000);
		cStationVoltThermal.get(1).setControlTMBS(true);
//		cStationVoltThermal.get(1).setTouControl(true);
		
		ElectricVehicle auto3 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 20% pen; 50% no
		Schedule schedule3 = new Schedule (127, 194); //arrival 21:10 ; dep 8:20
		cStationVoltThermal.get(2).assignEV(auto3, schedule3, 25000);
		cStationVoltThermal.get(2).setControlTMBS(true);
//		cStationVoltThermal.get(2).setTouControl(true);
		
		ElectricVehicle auto4 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//single phase; 20% pen; 50% yes
		Schedule schedule4 = new Schedule (113, 196); //arrival 18:50 ; dep 8:40
		cStationVoltThermal.get(3).assignEV(auto4, schedule4, 25000);
		cStationVoltThermal.get(3).setControlTMBS(true);
//		cStationVoltThermal.get(3).setTouControl(true);
		
		ElectricVehicle auto5 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 40% pen; 50% yes
		Schedule schedule5 = new Schedule (121, 200); //arrival 20:10 ; dep 9:20
		cStationVoltThermal.get(4).assignEV(auto5, schedule5, 25000);
		cStationVoltThermal.get(4).setControlTMBS(true);
//		cStationVoltThermal.get(4).setTouControl(true);
		
		ElectricVehicle auto6 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//three phase; 40% pen; 50% no
		Schedule schedule6 = new Schedule (120, 193); //arrival 20:00 ; dep 8:10
		cStationVoltThermal.get(5).assignEV(auto6, schedule6, 25000);
		cStationVoltThermal.get(5).setControlTMBS(true);
//		cStationVoltThermal.get(5).setTouControl(true);
		
		ElectricVehicle auto7 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 40% pen; 50% yes
		Schedule schedule7 = new Schedule (122, 187); //arrival 20:20 ; dep 7:10
		cStationVoltThermal.get(6).assignEV(auto7, schedule7, 25000);
		cStationVoltThermal.get(6).setControlTMBS(true);
//		cStationVoltThermal.get(6).setTouControl(true);
		
		ElectricVehicle auto8 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//single phase; 40% pen; 50% no
		Schedule schedule8 = new Schedule (123, 187); //arrival 20:30 ; dep 7:10
		cStationVoltThermal.get(7).assignEV(auto8, schedule8, 25000);
		cStationVoltThermal.get(7).setControlTMBS(true);
//		cStationVoltThermal.get(7).setTouControl(true);
		
		ElectricVehicle auto9 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//three phase; 60% pen; 50% yes
		Schedule schedule9 = new Schedule (124, 188); //arrival 20:40 ; dep 7:20
		cStationVoltThermal.get(8).assignEV(auto9, schedule9, 25000);
		cStationVoltThermal.get(8).setControlTMBS(true);
//		cStationVoltThermal.get(8).setTouControl(true);
		
		ElectricVehicle auto10 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//three phase; 60% pen; 50% yes
		Schedule schedule10 = new Schedule (115, 195); //arrival 19:10 ; dep 8:30
		cStationVoltThermal.get(9).assignEV(auto10, schedule10, 25000);
		cStationVoltThermal.get(9).setControlTMBS(true);
//		cStationVoltThermal.get(9).setTouControl(true);
		
		ElectricVehicle auto11 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 60% pen; 50% no
		Schedule schedule11 = new Schedule (123, 188); //arrival 20:30 ; dep 7:20
		cStationVoltThermal.get(10).assignEV(auto11, schedule11, 25000);
		cStationVoltThermal.get(10).setControlTMBS(true);
//		cStationVoltThermal.get(10).setTouControl(true);
		
		ElectricVehicle auto12 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//single phase; 60% pen; 50% no
		Schedule schedule12 = new Schedule (122, 195); //arrival 20:20 ; dep 8:30
		cStationVoltThermal.get(11).assignEV(auto12, schedule12, 25000);
		cStationVoltThermal.get(11).setControlTMBS(true);
//		cStationVoltThermal.get(11).setTouControl(true);
		
		ElectricVehicle auto13 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 80% pen; 50% yes
		Schedule schedule13 = new Schedule (111, 188); //arrival 18:30 ; dep 7:20
		cStationVoltThermal.get(12).assignEV(auto13, schedule13, 25000);
		cStationVoltThermal.get(12).setControlTMBS(true);
//		cStationVoltThermal.get(12).setTouControl(true);
		
		ElectricVehicle auto14 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//single phase; 80% pen; 50% no
		Schedule schedule14 = new Schedule (110, 199); //arrival 18:20 ; dep 9:10
		cStationVoltThermal.get(13).assignEV(auto14, schedule14, 25000);
		cStationVoltThermal.get(13).setControlTMBS(true);
//		cStationVoltThermal.get(13).setTouControl(true);
		
		ElectricVehicle auto15 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 80% pen; 50% yes
		Schedule schedule15 = new Schedule (121, 189); //arrival 20:10 ; dep 7:30
		cStationVoltThermal.get(14).assignEV(auto15, schedule15, 25000);
		cStationVoltThermal.get(14).setControlTMBS(true);
//		cStationVoltThermal.get(14).setTouControl(true);
		
		ElectricVehicle auto16 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//single phase; 80% pen; 50% no
		Schedule schedule16 = new Schedule (121, 189); //arrival 20:10 ; dep 7:30
		cStationVoltThermal.get(15).assignEV(auto16, schedule16, 25000);
		cStationVoltThermal.get(15).setControlTMBS(true);
//		cStationVoltThermal.get(15).setTouControl(true);
		
		ElectricVehicle auto17 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//three phase; 100% pen; 50% yes
		Schedule schedule17 = new Schedule (116, 188); //arrival 19:20 ; dep 7:20
		cStationVoltThermal.get(16).assignEV(auto17, schedule17, 25000);
		cStationVoltThermal.get(16).setControlTMBS(true);
//		cStationVoltThermal.get(16).setTouControl(true);
		
		ElectricVehicle auto18 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//single phase; 100% pen; 50% no
		Schedule schedule18 = new Schedule (122, 193); //arrival 20:20 ; dep 8:10
		cStationVoltThermal.get(17).assignEV(auto18, schedule18, 25000);
		cStationVoltThermal.get(17).setControlTMBS(true);
//		cStationVoltThermal.get(17).setTouControl(true);
		
		ElectricVehicle auto19 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, false);//single phase; 100% pen; 50% no
		Schedule schedule19 = new Schedule (115, 192); //arrival 19:10 ; dep 8:00
		cStationVoltThermal.get(18).assignEV(auto19, schedule19, 25000);
		cStationVoltThermal.get(18).setControlTMBS(true);
//		cStationVoltThermal.get(18).setTouControl(true);
//		
		ElectricVehicle auto20 = new ElectricVehicle ("Leaf", 16, 3700, 67, 25, 360, true);//three phase; 100% pen; 50% no
		Schedule schedule20 = new Schedule (124, 189); //arrival 20:40 ; dep 7:30
		cStationVoltThermal.get(19).assignEV(auto20, schedule20, 25000);
		cStationVoltThermal.get(19).setControlTMBS(true);
//		cStationVoltThermal.get(19).setTouControl(true);
		

		cStationSort.addAll(cStationVoltThermal);
						
		db.createNewTableResidentialArea();


		try {
			InputStream is = new FileInputStream("defaultConfiguration.properties");
			prop.load(is);
			is.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		controler.setVoltageControlThreshold(Double.parseDouble(prop.getProperty("VoltageControlThreshold")));
		controler.setTransformatorLoadingRated(Double.parseDouble(prop.getProperty("transformatorLoadingRated")));
		controler.setTransformatorSecurityFactor(Double.parseDouble(prop.getProperty("transformatorSecurityFactor")));
		controler.setdSOThresholdSecurityFactor(Double.parseDouble(prop.getProperty("dSOThresholdSecurityFactor")));

		for (int l = 0; l < cStationVoltThermal.size(); l++) {
			cStationVoltThermal.get(l).setVoltageCStation(230);
		}
		controler.thresholdDSO(1000000);
	}
        
    @FXML
    void cancelDeparture(ActionEvent event) {
    	stopAlgorithm = true;
		try {
			clientEVCC1.Disconnect();
			clientEVCC2.Disconnect();
			clientKoCos1.Disconnect();
			clientKoCos2.Disconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    @FXML
    void startAlgorithm(ActionEvent event) {
    	operationalConditions.start();
    	simulation.start();
    	
    }

    
    @FXML
    void openArrivalDeparture(MouseEvent event) {
        System.out.println("Arrival Departure");    	
    }
    


    	
    }
	

