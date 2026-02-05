package com.bicakclc.main;

import javax.swing.*;

import com.bicakclc.ui.MainWindow;
import com.bicakclc.util.DatabaseConnection;

import java.sql.SQLException;

public class BicakClcApplication {
    public static void main(String[] args) {
        System.out.println("TLS protocols: " + System.getProperty("jdk.tls.client.protocols"));
        // Test database connection
        try {
            DatabaseConnection.testConnection();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Database connection failed: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Start application
        SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });
    }
}
