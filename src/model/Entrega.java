package model;

import java.time.LocalDateTime;

public class Entrega {
    private String idEntrega;
    private Ocupacion ocupacion;
    private Empleado empleado;
    private LocalDateTime fechaHoraEntrega;
    private int kilometrajeSalida;
    private String observaciones;

    public Entrega(String idEntrega, Ocupacion ocupacion, Empleado empleado, LocalDateTime fechaHoraEntrega,
                   int kilometrajeSalida, String observaciones) {
        this.idEntrega = idEntrega;
        this.ocupacion = ocupacion;
        this.empleado = empleado;
        this.fechaHoraEntrega = fechaHoraEntrega;
        this.kilometrajeSalida = kilometrajeSalida;
        this.observaciones = observaciones;
    }

    public String generarComprobante() {
        return "COMPROBANTE ENTREGA - " + idEntrega + " - Vehiculo: " + ocupacion.getVehiculo().getPatente();
    }

    public void validarDatos() {
        if (kilometrajeSalida < 0) throw new IllegalArgumentException("Kilometraje inválido");
    }

    public String getIdEntrega() { return idEntrega; }
    public Ocupacion getOcupacion() { return ocupacion; }
    public Empleado getEmpleado() { return empleado; }
    public LocalDateTime getFechaHoraEntrega() { return fechaHoraEntrega; }
    public int getKilometrajeSalida() { return kilometrajeSalida; }
    public String getObservaciones() { return observaciones; }

    @Override
    public String toString() {
        return "Entrega: " + idEntrega.substring(0, 8) + " | Fecha: " + fechaHoraEntrega + " | Km salida: " + kilometrajeSalida;
    }
}
