package model;

import java.time.LocalDate;

public class Empleado extends Persona {
    private String legajo;
    private LocalDate fechaContratacion;
    private String sucursal;

    public Empleado(String id, String nombre, String apellido, String dni, String telefono, String email,
                    String legajo, LocalDate fechaContratacion, String sucursal) {
        super(id, nombre, apellido, dni, telefono, email);
        this.legajo = legajo;
        this.fechaContratacion = fechaContratacion;
        this.sucursal = sucursal;
    }

    @Override
    public String getIdentificacion() {
        return "LEGAJO: " + legajo;
    }

    @Override
    public String toString() {
        return super.toString() + " | Empleado | Legajo: " + legajo + " | Sucursal: " + sucursal;
    }

    public String getLegajo() { return legajo; }
    public void setLegajo(String legajo) { this.legajo = legajo; }
    public LocalDate getFechaContratacion() { return fechaContratacion; }
    public void setFechaContratacion(LocalDate fechaContratacion) { this.fechaContratacion = fechaContratacion; }
    public String getSucursal() { return sucursal; }
    public void setSucursal(String sucursal) { this.sucursal = sucursal; }
}
