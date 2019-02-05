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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteDB {

	private double transformerLoad;
	
	private double transformerLoadAvg;
	
	private int arrivaltime;
	private int arrivaltimeTwo;
	
	private int departuretime;
	private int departuretimeTwo;
	
	private boolean newTask = false;
	
	private double stockPrice;
	
	private boolean twoTask = false;
	
	
	
	//Constructors
	public SQLiteDB() {
		Connection conn = null;

		try {
			String url = "jdbc:sqlite:C:/Users/user/eclipse-workspace/SmartCharging/SmartCharging.db";

			conn = DriverManager.getConnection(url);

			System.out.println("Connection to SQLite has been established!");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	}
	
	public void createNewTableResults5EVs2Real() {
		String url = "jdbc:sqlite:C:/Users/user/eclipse-workspace/SmartCharging/SmartCharging.db";
		String sql = "CREATE TABLE `Results5EVs2Real` (\r\n" + 
				"	`id`	INTEGER PRIMARY KEY AUTOINCREMENT,\r\n" + 
				"	`time`	BLOB,\r\n" + 
				"	`i1_EV1_virt`	INTEGER,\r\n" + 
				"	`i2_EV1_virt`	INTEGER,\r\n" + 
				"	`i3_EV1_virt`	INTEGER,\r\n" + 
				"	`i1_EV2_virt`	INTEGER,\r\n" + 
				"	`i2_EV2_virt`	INTEGER,\r\n" + 
				"	`i3_EV2_virt`	INTEGER,\r\n" + 
				"	`i_EV3_virt`	INTEGER,\r\n" + 
				"	`i_EV4_virt`	INTEGER,\r\n" + 
				"	`i_EV5_virt`	INTEGER,\r\n" + 
				"	`i1_EV12_real`	REAL,\r\n" + 
				"	`i2_EV12_real`	REAL,\r\n" + 
				"	`i3_EV12_real`	REAL,\r\n" + 
				"	`v1_EV12_virt`	REAL,\r\n" + 
				"	`v2_EV12_virt`	REAL,\r\n" + 
				"	`v3_EV12_virt`	REAL,\r\n" + 
				"	`v1_EV12_real`	REAL,\r\n" + 
				"	`v2_EV12_real`	REAL,\r\n" + 
				"	`v3_EV12_real`	REAL,\r\n" + 
				"	`loadAmount_EV1`	REAL,\r\n" + 
				"	`loadAmount_EV2`	REAL,\r\n" + 
				"	`loadAmount_EV3`	REAL,\r\n" + 
				"	`loadAmount_EV4`	REAL,\r\n" + 
				"	`loadAmount_EV5`	REAL \r\n" + 
				");";
		try (Connection conn = DriverManager.getConnection(url);
				Statement stmt = conn.createStatement()){
			stmt.execute(sql);
		}catch (SQLException e) {
			System.out.println(e.getMessage());
		}					
	}
		
	public void insertResults5EVs2Real(String time, int i1_EV1_virt, int i2_EV1_virt, int i3_EV1_virt, int i1_EV2_virt, int i2_EV2_virt, int i3_EV2_virt, int i_EV3_virt, int i_EV4_virt, int i_EV5_virt, double i1_EV12_real, double i2_EV12_real, double i3_EV12_real, double v1_EV12_virt, double v2_EV12_virt, double v3_EV12_virt, double v1_EV12_real, double v2_EV12_real, double v3_EV12_real, double loadAmount_EV1, double loadAmount_EV2, double loadAmount_EV3, double loadAmount_EV4, double loadAmount_EV5) {

		String sql = "INSERT INTO Results5EVs2Real (time, i1_EV1_virt, i2_EV1_virt, i3_EV1_virt, i1_EV2_virt, i2_EV2_virt, i3_EV2_virt, i_EV3_virt, i_EV4_virt, i_EV5_virt, i1_EV12_real, i2_EV12_real, i3_EV12_real, v1_EV12_virt, v2_EV12_virt, v3_EV12_virt, v1_EV12_real, v2_EV12_real, v3_EV12_real, loadAmount_EV1, loadAmount_EV2, loadAmount_EV3, loadAmount_EV4, loadAmount_EV5) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, time);
			pstmt.setInt(2, i1_EV1_virt);
			pstmt.setInt(3, i2_EV1_virt);
			pstmt.setInt(4, i3_EV1_virt);
			pstmt.setInt(5, i1_EV2_virt);
			pstmt.setInt(6, i2_EV2_virt);
			pstmt.setInt(7, i3_EV2_virt);
			pstmt.setInt(8, i_EV3_virt);
			pstmt.setInt(9, i_EV4_virt);
			pstmt.setInt(10, i_EV5_virt);
			pstmt.setDouble(11, i1_EV12_real);
			pstmt.setDouble(12, i2_EV12_real);
			pstmt.setDouble(13, i3_EV12_real);
			pstmt.setDouble(14, v1_EV12_virt);
			pstmt.setDouble(15, v2_EV12_virt);
			pstmt.setDouble(16, v3_EV12_virt);
			pstmt.setDouble(17, v1_EV12_real);
			pstmt.setDouble(18, v2_EV12_real);
			pstmt.setDouble(19, v3_EV12_real);
			pstmt.setDouble(20, loadAmount_EV1);
			pstmt.setDouble(21, loadAmount_EV2);
			pstmt.setDouble(22, loadAmount_EV3);
			pstmt.setDouble(23, loadAmount_EV4);
			pstmt.setDouble(24, loadAmount_EV5);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void createNewTable5EVs() {
		String url = "jdbc:sqlite:C:/Users/user/eclipse-workspace/SmartCharging/SmartCharging.db";
		String sql = "CREATE TABLE `5Evs` (\r\n" + 
				"	`id`	INTEGER PRIMARY KEY AUTOINCREMENT,\r\n" + 
				"	`currentsauto1`	INTEGER,\r\n" + 
				"	`currentsauto2`	INTEGER,\r\n" + 
				"	`currentsauto3`	INTEGER,\r\n" + 
				"	`currentsauto4`	INTEGER,\r\n" + 
				"	`currentsauto5`	INTEGER,\r\n" + 
				"	`flexibility1`	INTEGER,\r\n" + 
				"	`decisionVectorA`	INTEGER,\r\n" + 
				"	`flexibility2`	INTEGER,\r\n" + 
				"	`decisionVectorB`	INTEGER,\r\n" + 
				"	`flexibility3`	INTEGER,\r\n" + 
				"	`decisionVectorC`	INTEGER,\r\n" + 
				"	`flexibility4`	INTEGER,\r\n" + 
				"	`decisionVectorD`	INTEGER,\r\n" + 
				"	`flexibility5`	INTEGER,\r\n" + 
				"	`decisionVectorE`	INTEGER,\r\n" + 
				"	`loadAmount1`	INTEGER,\r\n" + 
				"	`loadAmount2`	INTEGER,\r\n" + 
				"	`loadAmount3`	INTEGER,\r\n" + 
				"	`loadAmount4`	INTEGER,\r\n" + 
				"	`loadAmount5`	INTEGER,\r\n" + 
				"	`transformerLoad`	INTEGER,\r\n" + 
				"	`stockPrice`	REAL,\r\n" + 
				"	`lauf`	INTEGER,\r\n" + 
				"	`time`	BLOB\r\n" +
				");";
		try (Connection conn = DriverManager.getConnection(url);
				Statement stmt = conn.createStatement()){
			stmt.execute(sql);
		}catch (SQLException e) {
			System.out.println(e.getMessage());
		}				
	}
	
	public void createNewTableResidentialArea() {
		String url = "jdbc:sqlite:C:/Users/user/eclipse-workspace/SmartCharging/SmartCharging.db";
		String sql = "CREATE TABLE `residentialArea` (\r\n" + 
				"	`id`	INTEGER PRIMARY KEY AUTOINCREMENT,\r\n" + 
				"	`loadAmount1`	INTEGER,\r\n" +
				"	`loadAmount2`	INTEGER,\r\n" +
				"	`loadAmount3`	INTEGER,\r\n" +
				"	`loadAmount4`	INTEGER,\r\n" +
				"	`loadAmount5`	INTEGER,\r\n" +
				"	`loadAmount6`	INTEGER,\r\n" +
				"	`loadAmount7`	INTEGER,\r\n" +
				"	`loadAmount8`	INTEGER,\r\n" +
				"	`loadAmount9`	INTEGER,\r\n" +
				"	`loadAmount10`	INTEGER,\r\n" +
				"	`loadAmount11`	INTEGER,\r\n" +
				"	`loadAmount12`	INTEGER,\r\n" +
				"	`loadAmount13`	INTEGER,\r\n" +
				"	`loadAmount14`	INTEGER,\r\n" +
				"	`loadAmount15`	INTEGER,\r\n" +
				"	`loadAmount16`	INTEGER,\r\n" +
				"	`loadAmount17`	INTEGER,\r\n" +
				"	`loadAmount18`	INTEGER,\r\n" +
				"	`loadAmount19`	INTEGER,\r\n" +
				"	`loadAmount20`	INTEGER,\r\n" +
				"	`transformerLoad`	INTEGER,\r\n" + 
				"	`lauf`	INTEGER,\r\n" + 
				"	`time`	BLOB,\r\n" +
				"	`VRFBPower`	REAL,\r\n" +
				"	`EVPower`	INTEGER,\r\n" +
				"	`stockPrice`	REAL\r\n" + 
				");";
		try (Connection conn = DriverManager.getConnection(url);
				Statement stmt = conn.createStatement()){
			stmt.execute(sql);
		}catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void insertResidentialArea(String table, double load1, double load2, double load3, double load4, double load5, double load6, double load7, double load8, double load9, double load10, double load11, double load12, double load13, double load14, double load15, double load16, double load17, double load18, double load19, double load20, double trafLoad, int lauf, String time, double VRFBPower, int EVPower, double stockPrice) {

		String sql = "INSERT INTO residentialArea (loadAmount1, loadAmount2, loadAmount3, loadAmount4, loadAmount5, loadAmount6, loadAmount7, loadAmount8, loadAmount9, loadAmount10, loadAmount11, loadAmount12, loadAmount13, loadAmount14, loadAmount15, loadAmount16, loadAmount17, loadAmount18, loadAmount19, loadAmount20, transformerLoad, lauf, time, VRFBPower, EVPower, stockPrice) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, (int) load1);
			pstmt.setInt(2, (int) load2);
			pstmt.setInt(3, (int) load3);
			pstmt.setInt(4, (int) load4);
			pstmt.setInt(5, (int) load5);
			pstmt.setInt(6, (int) load6);
			pstmt.setInt(7, (int) load7);
			pstmt.setInt(8, (int) load8);
			pstmt.setInt(9, (int) load9);
			pstmt.setInt(10, (int) load10);
			pstmt.setInt(11, (int) load11);
			pstmt.setInt(12, (int) load12);
			pstmt.setInt(13, (int) load13);
			pstmt.setInt(14, (int) load14);
			pstmt.setInt(15, (int) load15);
			pstmt.setInt(16, (int) load16);
			pstmt.setInt(17, (int) load17);
			pstmt.setInt(18, (int) load18);
			pstmt.setInt(19, (int) load19);
			pstmt.setInt(20, (int) load20);
			pstmt.setInt(21, (int) trafLoad);
			pstmt.setInt(22, lauf);
			pstmt.setString(23, time);
			pstmt.setDouble(24, VRFBPower);
			pstmt.setInt(25, EVPower);
			pstmt.setDouble(26, stockPrice);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	
	public void createNewTableKoCosDevice() {
		String url = "jdbc:sqlite:C:/Users/user/eclipse-workspace/SmartCharging/SmartCharging.db";
		String sql = "CREATE TABLE `KoCos` (\r\n" + 
				"	`id`	INTEGER PRIMARY KEY AUTOINCREMENT,\r\n" + 
				"	`v1`	REAL,\r\n" + 
				"	`v2`	REAL,\r\n" + 
				"	`v3`	REAL,\r\n" + 
				"	`pTotal`	REAL,\r\n" + 
				"	`i1`	REAL,\r\n" + 
				"	`i2`	REAL,\r\n" + 
				"	`i3`	REAL,\r\n" + 
				"	`time`	BLOB\r\n" + 
				");";
		try (Connection conn = DriverManager.getConnection(url);
				Statement stmt = conn.createStatement()){
			stmt.execute(sql);
		}catch (SQLException e) {
			System.out.println(e.getMessage());
		}
						
	}
	
	public void insertKoCos(String table, double v1, double v2, double v3, double ptotal, double i1, double i2, double i3, String time) {

		String sql = "INSERT INTO " + table + " (v1, v2, v3, pTotal, i1, i2, i3, time) VALUES (?,?,?,?,?,?,?,?)";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setDouble(1, v1);
			pstmt.setDouble(2, v2);
			pstmt.setDouble(3, v3);
			pstmt.setDouble(4, ptotal);
			pstmt.setDouble(5, i1);
			pstmt.setDouble(6, i2);
			pstmt.setDouble(7, i3);
			pstmt.setString(8, time);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}
	
	
	public void closeConnection () {
		try {
			this.connect().close();
		}catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}	
	
	private Connection connect () {
		
		String url = "jdbc:sqlite:C:/Users/user/eclipse-workspace/SmartCharging/SmartCharging.db";
		Connection conn = null;
		
		try {
			conn = DriverManager.getConnection(url);
		}catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return conn;
	}
	
	/***
	 * 
	 * @param table
	 * @param curr1
	 * @param curr2
	 * @param curr3
	 * @param curr4
	 * @param curr5
	 * @param flex1
	 * @param deca
	 * @param flex2
	 * @param decb
	 * @param flex3
	 * @param decc
	 * @param flex4
	 * @param decd
	 * @param flex5
	 * @param dece
	 * @param load1
	 * @param load2
	 * @param load3
	 * @param load4
	 * @param load5
	 * @param trafLoad
	 */
	public void insert(String table, int curr1, int curr2, int curr3, int curr4, int curr5, int flex1, int deca, int flex2, int decb, int flex3, int decc, int flex4, int decd, int flex5, int dece, double load1, double load2, double load3, double load4, double load5, double trafLoad, double stockPrice, int lauf, String time) {

		String sql = "INSERT INTO " + table + " (currentsauto1, currentsauto2, currentsauto3, currentsauto4, currentsauto5, flexibility1, decisionVectorA, flexibility2, decisionVectorB, flexibility3, decisionVectorC, flexibility4, decisionVectorD, flexibility5, decisionVectorE, loadAmount1, loadAmount2, loadAmount3, loadAmount4, loadAmount5, transformerLoad, stockPrice, lauf, time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, curr1);
			pstmt.setInt(2, curr2);
			pstmt.setInt(3, curr3);
			pstmt.setInt(4, curr4);
			pstmt.setInt(5, curr5);
			pstmt.setInt(6, flex1);
			pstmt.setInt(7, deca);
			pstmt.setInt(8, flex2);
			pstmt.setInt(9, decb);
			pstmt.setInt(10, flex3);
			pstmt.setInt(11, decc);
			pstmt.setInt(12, flex4);
			pstmt.setInt(13, decd);
			pstmt.setInt(14, flex5);
			pstmt.setInt(15, dece);
			pstmt.setInt(16, (int) load1);
			pstmt.setInt(17, (int) load2);
			pstmt.setInt(18, (int) load3);
			pstmt.setInt(19, (int) load4);
			pstmt.setInt(20, (int) load5);
			pstmt.setInt(21, (int) trafLoad);
			pstmt.setDouble(22, stockPrice);
			pstmt.setInt(23, lauf);
			pstmt.setString(24, time);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}
	

	
	public void selectData(int i) {
		String sql = "SELECT Ptrafo FROM ExemplaryTransformerData WHERE id == ?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, i);

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				transformerLoad = rs.getInt("Ptrafo");

			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}
	

	
	public void newTask (int i) {
		String sql = "SELECT arrivaltime, departuretime FROM ArrivalDeparture WHERE arrivaltime == " + i;

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				setArrivaltime(rs.getInt("arrivaltime"));
				setDeparturetime(rs.getInt("departuretime"));	
				newTask = true;
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private int once = 0;
	
	public int getOnce() {
		return once;
	}

	public void setOnce(int once) {
		this.once = once;
	}

	//Only for Monte Carlo
	public void mCEx (int i, int j, String table) {
		if (once == 0) {
		String sql = "SELECT arrtime, deptime FROM " + table + " WHERE lauf == " + i + " and arrtime == " + j;

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					if (rs.getRow() == 1) {
						System.out.println("ROWNumber: " + rs.getRow());
						setArrivaltime(rs.getInt("arrtime"));
						setDeparturetime(rs.getInt("deptime"));
						newTask = true;
						once = 1;
					}
					if (rs.getRow() == 2) {
						setTwoTask(true);
						setArrivaltimeTwo(rs.getInt("arrtime"));
						setDeparturetimeTwo(rs.getInt("deptime"));
					}
				}
			
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}}
	}
	
	public void stockPrice(int timeslot) {
		String sql = "SELECT mittel FROM ExemplaryStockData WHERE id == " + timeslot;

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				setStockPrice(rs.getDouble("mittel"));
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public double getTransformerLoad() {
		return transformerLoad;
	}

	public void setTransformerLoad(double transformerLoad) {
		this.transformerLoad = transformerLoad;
	}

	public int getArrivaltime() {
		return arrivaltime;
	}

	public void setArrivaltime(int arrivaltime) {
		this.arrivaltime = arrivaltime;
	}

	public int getDeparturetime() {
		return departuretime;
	}

	public void setDeparturetime(int departuretime) {
		this.departuretime = departuretime;
	}

	public boolean isNewTask() {
		return newTask;
	}

	public void setNewTask(boolean newTask) {
		this.newTask = newTask;
	}

	public double getStockPrice() {
		return stockPrice;
	}

	public void setStockPrice(double stockPrice) {
		this.stockPrice = stockPrice;
	}

	public boolean isTwoTask() {
		return twoTask;
	}

	public void setTwoTask(boolean twoTask) {
		this.twoTask = twoTask;
	}

	/**
	 * @return the arrivaltimeTwo
	 */
	public int getArrivaltimeTwo() {
		return arrivaltimeTwo;
	}

	/**
	 * @param arrivaltimeTwo the arrivaltimeTwo to set
	 */
	public void setArrivaltimeTwo(int arrivaltimeTwo) {
		this.arrivaltimeTwo = arrivaltimeTwo;
	}

	/**
	 * @return the departuretimeTwo
	 */
	public int getDeparturetimeTwo() {
		return departuretimeTwo;
	}

	/**
	 * @param departuretimeTwo the departuretimeTwo to set
	 */
	public void setDeparturetimeTwo(int departuretimeTwo) {
		this.departuretimeTwo = departuretimeTwo;
	}

	/**
	 * @return the transformerLoadAvg
	 */
	public double getTransformerLoadAvg() {
		return transformerLoadAvg;
	}

	/**
	 * @param transformerLoadAvg the transformerLoadAvg to set
	 */
	public void setTransformerLoadAvg(double transformerLoadAvg) {
		this.transformerLoadAvg = transformerLoadAvg;
	}	
}
