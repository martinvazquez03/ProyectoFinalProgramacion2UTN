package model;

public class Automovil extends Vehiculo {
    public static final int CANTIDAD_PUERTAS_POR_DEFECTO = 4;
    public static final String TRANSMISION_MANUAL = "MANUAL";
    public static final String TRANSMISION_AUTOMATICA = "AUTOMATICA";
    public static final String TRANSMISION_POR_DEFECTO = TRANSMISION_MANUAL;
    private static final double RECARGO_TRANSMISION_AUTOMATICA = 0.10;

    private int cantidadPuertas;
    private String transmision;

    public Automovil(String patente, String marca, String modelo, int anio, int kilometraje,
                     enums.EstadoVehiculo estado, double precioDia, double tarifaSeguro,
                     int cantidadPuertas, String transmision) {
        super(patente, marca, modelo, anio, kilometraje, estado, precioDia, tarifaSeguro);
        this.cantidadPuertas = cantidadPuertas;
        this.transmision = transmision;
    }

    @Override
    public double calcularCosto(int dias) {
        double costoBase = precioDia * dias;
        double costoSeguro = tarifaSeguro * dias;
        double recargo = 0;
        if (transmision != null && transmision.equalsIgnoreCase(TRANSMISION_AUTOMATICA)) {
            recargo = costoBase * RECARGO_TRANSMISION_AUTOMATICA;
        }
        return costoBase + costoSeguro + recargo;
    }

    public int getCantidadPuertas() { return cantidadPuertas; }
    public void setCantidadPuertas(int cantidadPuertas) { this.cantidadPuertas = cantidadPuertas; }
    public String getTransmision() { return transmision; }
    public void setTransmision(String transmision) { this.transmision = transmision; }
}
