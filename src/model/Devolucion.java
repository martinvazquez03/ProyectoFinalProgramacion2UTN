package model;

import java.time.LocalDateTime;

public class Devolucion {
    public static final double MULTA_POR_DANIOS = 5000.0;

    private String idDevolucion;
    private Ocupacion ocupacion;
    private Empleado empleado;
    private LocalDateTime fechaHoraDevolucion;
    private int kilometrajeEntrada;
    private String danios;
    private String observaciones;
    private double cargosExtra;
    private double montoFinal;

    public Devolucion(String idDevolucion, Ocupacion ocupacion, Empleado empleado, LocalDateTime fechaHoraDevolucion,
                      int kilometrajeEntrada, String danios, double cargosExtra) {
        this(idDevolucion, ocupacion, empleado, fechaHoraDevolucion, kilometrajeEntrada, danios, cargosExtra, "");
    }

    public Devolucion(String idDevolucion, Ocupacion ocupacion, Empleado empleado, LocalDateTime fechaHoraDevolucion,
                      int kilometrajeEntrada, String danios, double cargosExtra, String observaciones) {
        this.idDevolucion = idDevolucion;
        this.ocupacion = ocupacion;
        this.empleado = empleado;
        this.fechaHoraDevolucion = fechaHoraDevolucion;
        this.kilometrajeEntrada = kilometrajeEntrada;
        this.danios = danios;
        this.observaciones = observaciones;
        this.cargosExtra = cargosExtra;
    }

    public double calcularCargos() {
        double base = ocupacion.getVehiculo().calcularCosto((int) ocupacion.calcularDias());
        double total = base + cargosExtra;
        this.montoFinal = total;
        return montoFinal;
    }

    public void aplicarMultas() {
        if (tieneDanios()) {
            cargosExtra += MULTA_POR_DANIOS;
        }
    }

    public boolean tieneDanios() {
        if (danios == null) return false;
        String valor = danios.trim().toUpperCase();
        if (valor.isEmpty()) return false;
        return !(valor.equals("NINGUNO") || valor.equals("NINGUNO.") || valor.equals("NO")
                || valor.equals("SIN DANIOS") || valor.equals("SIN DAÑOS") || valor.equals("-"));
    }

    public void cerrarOcupacion() {
        ocupacion.cerrar(this);
    }

    public String getIdDevolucion() { return idDevolucion; }
    public Ocupacion getOcupacion() { return ocupacion; }
    public Empleado getEmpleado() { return empleado; }
    public LocalDateTime getFechaHoraDevolucion() { return fechaHoraDevolucion; }
    public int getKilometrajeEntrada() { return kilometrajeEntrada; }
    public String getDanios() { return danios; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public double getCargosExtra() { return cargosExtra; }
    public void setCargosExtra(double cargosExtra) { this.cargosExtra = cargosExtra; }
    public double getMontoFinal() { return montoFinal; }
    public void setMontoFinal(double montoFinal) { this.montoFinal = montoFinal; }

    @Override
    public String toString() {
        return "Devolución: " + idDevolucion.substring(0, 8) + " | Fecha: " + fechaHoraDevolucion + " | Km entrada: " + kilometrajeEntrada + " | Cargos extra: $" + cargosExtra + " | Monto final: $" + montoFinal;
    }
}
