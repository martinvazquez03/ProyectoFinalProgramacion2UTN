package model;

import enums.EstadoReserva;
import interfaces.Auditable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Reserva implements Auditable {
    private String idReserva;
    private Cliente cliente;
    private Vehiculo vehiculo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDateTime fechaReserva;
    private EstadoReserva estado;
    private String creadoPor;
    private boolean activo;

    public Reserva(String idReserva, Cliente cliente, Vehiculo vehiculo, LocalDate fechaInicio, LocalDate fechaFin,
                   LocalDateTime fechaReserva, EstadoReserva estado, String creadoPor) {
        this.idReserva = idReserva;
        this.cliente = cliente;
        this.vehiculo = vehiculo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.fechaReserva = fechaReserva;
        this.estado = estado;
        this.creadoPor = creadoPor;
        this.activo = true;
    }

    public boolean validar() {
        if (fechaInicio == null || fechaFin == null) return false;
        return !fechaInicio.isAfter(fechaFin);
    }

    public void confirmar() {
        this.estado = EstadoReserva.CONFIRMADA;
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public double calcularMontoTotal() {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin);
        if (dias < 1) dias = 1;
        return vehiculo.calcularCosto((int) dias);
    }

    @Override
    public LocalDateTime getFechaCreacion() { return fechaReserva; }

    @Override
    public String getCreadoPor() { return creadoPor; }

    @Override
    public void marcarModificacion(String usuario) {
        // Registro simple para auditoría
    }

    public String getIdReserva() { return idReserva; }
    public void setIdReserva(String idReserva) { this.idReserva = idReserva; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public LocalDateTime getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(LocalDateTime fechaReserva) { this.fechaReserva = fechaReserva; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public double getMontoSenia() { return vehiculo != null ? vehiculo.getTarifaSeguro() : 0.0; }

    @Override
    public String toString() {
        return "Reserva: " + idReserva.substring(0, 8) + " | Cliente: " + (cliente != null ? cliente.getNombreCompleto() : "") + " | Vehículo: " + (vehiculo != null ? vehiculo.getPatente() : "") + " | " + fechaInicio + " a " + fechaFin + " | Estado: " + estado;
    }
}
