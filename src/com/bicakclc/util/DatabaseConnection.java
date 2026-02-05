package com.bicakclc.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DatabaseConnection {
    private static final String CONFIG_FILE = "config.properties";
    private static String url;
    private static String username;
    private static String password;
    private static boolean isInitialized = false;
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        if (!isInitialized) {
            try {
                // Default values for SQL Server Express with Windows Authentication
                url = "jdbc:sqlserver://DESKTOP-LPTSAV1\\SQLEXPRESS;" +
                      "encrypt=true;" +
                      "trustServerCertificate=true;" +
                      "integratedSecurity=true";
                username = null;
                password = null;
                
                // Try to load from properties file if it exists
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                    props.load(fis);
                    
                    // Override with values from properties file if they exist
                    url = props.getProperty("db.url", url);
                    username = props.getProperty("db.username");
                    password = props.getProperty("db.password");
                } catch (IOException e) {
                    System.out.println("Warning: config.properties not found. Using default values.");
                }
                
                // Load the SQL Server JDBC driver
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                isInitialized = true;
                
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Error: SQL Server JDBC Driver not found. Please add the JDBC driver to your project.", e);
            }
        }
    }
    
    public static Connection getConnection() throws SQLException {
        try {
            // If db.username is provided, use SQL Authentication; otherwise assume URL contains integratedSecurity or other auth method
            if (username != null && !username.trim().isEmpty()) {
                return DriverManager.getConnection(url, username, password != null ? password : "");
            }
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            boolean usingSqlAuth = username != null && !username.trim().isEmpty();
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to connect to database. Please check your connection settings.\n");
            sb.append("Make sure:\n");
            sb.append("1. SQL Server is running and reachable\n");
            sb.append("2. db.url (server/port/databaseName) is correct\n");
            if (usingSqlAuth) {
                sb.append("3. SQL login username/password are correct and have permissions\n");
            } else {
                sb.append("3. If using Windows Authentication, ensure integratedSecurity=true\n");
                sb.append("4. For Windows Authentication, the mssql-jdbc_auth DLL must be in your Java bin directory\n");
            }
            sb.append("Error: ").append(e.getMessage());
            String errorMessage = sb.toString();
            throw new SQLException(errorMessage, e);
        }
    }
    
    public static void testConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connection test successful!");
                // Print connection details for debugging
                System.out.println("Connected to: " + conn.getMetaData().getDatabaseProductName() + " " +
                                 conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed!");
            throw e;
        }
    }
}
