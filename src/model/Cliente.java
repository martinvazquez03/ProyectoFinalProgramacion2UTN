package model;

import java.time.LocalDate;
import java.util.ArrayList;

public class Cliente extends Persona {
    private LocalDate fechaRegistro;
    private boolean activo;
    private ArrayList<Ocupacion> historialOcupaciones;

    public Cliente(String id, String nombre, String apellido, String dni, String telefono, String email,
                   LocalDate fechaRegistro) {
        super(id, nombre, apellido, dni, telefono, email);
        this.fechaRegistro = fechaRegistro;
        this.activo = true;
        this.historialOcupaciones = new ArrayList<Ocupacion>();
    }

    @Override
    public String getIdentificacion() {
        return "DNI: " + dni;
    }

    public void agregarOcupacion(Ocupacion ocupacion) {
        if (ocupacion == null) {
            return;
        }
        for (Ocupacion existente : historialOcupaciones) {
            if (existente != null && existente.getIdOcupacion() != null
                    && existente.getIdOcupacion().equals(ocupacion.getIdOcupacion())) {
                return;
            }
        }
        this.historialOcupaciones.add(ocupacion);
    }

    public void limpiarHistorialOcupaciones() {
        this.historialOcupaciones.clear();
    }

    public ArrayList<Ocupacion> getHistorialOcupaciones() {
        return historialOcupaciones;
    }

    @Override
    public String toString() {
        return super.toString() + " | Cliente | Fecha Registro: " + fechaRegistro;
    }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
