package exception;

public class VehiculoNoDisponibleException extends Exception {
    public VehiculoNoDisponibleException(String patente) {
        super("El vehículo con patente " + patente + " no está disponible para alquiler.");
    }

    public VehiculoNoDisponibleException(String patente, String motivo) {
        super("El vehículo con patente " + patente + " no está disponible. Motivo: " + motivo);
    }
}
