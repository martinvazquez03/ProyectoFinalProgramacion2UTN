package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private static final String URL = "jdbc:sqlite:alquiler_vehiculos.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Throwable e) {
            System.err.println("Error al cargar driver SQLite: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Throwable ignored) {
        }
        return DriverManager.getConnection(URL);
    }

    public static void cerrar(AutoCloseable recurso) {
        if (recurso != null) {
            try {
                recurso.close();
            } catch (Exception e) {
                System.err.println("Error al cerrar recurso: " + e.getMessage());
            }
        }
    }
}
