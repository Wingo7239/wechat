package com.weixin.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcUtil {
	String user = "root";
	String password = "root";
	String url = "jdbc:mysql://localhost:3306/wechat";
	String driver = "";
	Connection conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	
	
	public Connection getConn() throws ClassNotFoundException, SQLException{
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(url, user, password);
		return conn;
	}
	
	public boolean getUser(String userid){
		int count = 0;
		try {
			String sql = "Select count(userid) from user where userid = '"+ userid +"'" ; 
			Connection conn = getConn();
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				count = rs.getInt(1);
			}
			
			return count>0?true:false;
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	
	public boolean saveUser(String userid){
		boolean  b = false;
		try {
			String sql = "insert into user(userid) values( '"+ userid +"')" ; 
			Connection conn = getConn();
			PreparedStatement ps = conn.prepareStatement(sql);
			b =  ps.execute();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return b;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
