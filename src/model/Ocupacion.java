package model;

import enums.EstadoOcupacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Ocupacion {
    private String idOcupacion;
    private Cliente cliente;
    private Vehiculo vehiculo;
    private Reserva reserva;
    private Entrega entrega;
    private Devolucion devolucion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinEstimada;
    private LocalDateTime fechaReserva;
    private LocalDate fechaInicioReserva;
    private EstadoOcupacion estado;
    private String creadoPor;

    public Ocupacion(String idOcupacion, Cliente cliente, Vehiculo vehiculo, Reserva reserva,
                     LocalDateTime fechaInicio, LocalDateTime fechaFinEstimada, String creadoPor) {
        this.idOcupacion = idOcupacion;
        this.cliente = cliente;
        this.vehiculo = vehiculo;
        this.reserva = reserva;
        this.fechaInicio = fechaInicio;
        this.fechaFinEstimada = fechaFinEstimada;
        this.estado = EstadoOcupacion.ACTIVA;
        this.creadoPor = creadoPor;
        if (reserva != null) {
            this.fechaReserva = reserva.getFechaReserva();
            this.fechaInicioReserva = reserva.getFechaInicio();
        }
    }

    public void registrarDatosReserva(LocalDateTime fechaReserva, LocalDate fechaInicioReserva) {
        if (fechaReserva != null) {
            this.fechaReserva = fechaReserva;
        }
        if (fechaInicioReserva != null) {
            this.fechaInicioReserva = fechaInicioReserva;
        }
    }

    public boolean estaActiva() {
        return estado == EstadoOcupacion.ACTIVA;
    }

    public long calcularDias() {
        LocalDateTime fin = getFechaFinReal();
        if (fin == null) {
            fin = LocalDateTime.now();
        }
        long dias = ChronoUnit.DAYS.between(fechaInicio.toLocalDate(), fin.toLocalDate());
        return dias < 1 ? 1 : dias;
    }

    public void cerrar(Devolucion devolucion) {
        this.devolucion = devolucion;
        this.estado = EstadoOcupacion.FINALIZADA;
    }

    public String getCreadoPor() { return creadoPor; }

    public String getIdOcupacion() { return idOcupacion; }
    public Cliente getCliente() { return cliente; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public Reserva getReserva() { return reserva; }
    public Entrega getEntrega() { return entrega; }
    public void setEntrega(Entrega entrega) { this.entrega = entrega; }
    public Devolucion getDevolucion() { return devolucion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFinEstimada() { return fechaFinEstimada; }
    public LocalDateTime getFechaReserva() {
        return fechaReserva != null ? fechaReserva : (reserva != null ? reserva.getFechaReserva() : null);
    }
    public LocalDate getFechaInicioReserva() {
        return fechaInicioReserva != null ? fechaInicioReserva : (reserva != null ? reserva.getFechaInicio() : null);
    }
    public LocalDate getFechaFinReserva() {
        if (fechaFinEstimada != null) {
            return fechaFinEstimada.toLocalDate();
        }
        return reserva != null ? reserva.getFechaFin() : null;
    }
    public LocalDateTime getFechaFinReal() {
        return devolucion != null ? devolucion.getFechaHoraDevolucion() : null;
    }
    public EstadoOcupacion getEstado() { return estado; }
    public void setEstado(EstadoOcupacion estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Ocupación: " + idOcupacion.substring(0, 8) + " | Cliente: " + (cliente != null ? cliente.getNombreCompleto() : "") + " | Vehículo: " + (vehiculo != null ? vehiculo.getPatente() : "") + " | Inicio: " + fechaInicio + " | Estado: " + estado;
    }
}
