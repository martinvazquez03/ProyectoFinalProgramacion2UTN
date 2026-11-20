package service.impl;

import enums.EstadoReserva;
import enums.EstadoVehiculo;
import exception.ReservaInvalidaException;
import exception.VehiculoNoDisponibleException;
import model.*;
import service.ServicioAlquiler;
import util.RepositorioGenerico;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServicioAlquilerImpl implements ServicioAlquiler {
    private final RepositorioGenerico<Ocupacion, String> repoOcupaciones;
    private final RepositorioGenerico<Usuario, String> repoUsuarios;

    public ServicioAlquilerImpl() {
        this(new RepositorioGenerico<Ocupacion, String>(), new RepositorioGenerico<Usuario, String>());
    }

    public ServicioAlquilerImpl(RepositorioGenerico<Ocupacion, String> repoOcupaciones,
                                RepositorioGenerico<Usuario, String> repoUsuarios) {
        this.repoOcupaciones = repoOcupaciones;
        this.repoUsuarios = repoUsuarios;
    }

    @Override
    public Ocupacion realizarEntrega(Reserva reserva, Empleado empleado, String observaciones) throws VehiculoNoDisponibleException, ReservaInvalidaException {
        if (reserva == null || !reserva.validar()) {
            throw new ReservaInvalidaException("La reserva es inválida o tiene fechas incorrectas.");
        }
        if (empleado == null) {
            throw new ReservaInvalidaException("La entrega debe ser realizada por un empleado registrado.");
        }
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA && reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ReservaInvalidaException("La reserva debe estar CONFIRMADA o PENDIENTE para concretarse.");
        }
        if (reserva.getVehiculo() == null) {
            throw new VehiculoNoDisponibleException("SIN_PATENTE");
        }
        if (reserva.getVehiculo().getEstado() == EstadoVehiculo.ALQUILADO) {
            throw new VehiculoNoDisponibleException(reserva.getVehiculo().getPatente(), reserva.getVehiculo().getEstado().toString());
        }
        if (reserva.getVehiculo().getEstado() == EstadoVehiculo.EN_REPARACION || reserva.getVehiculo().getEstado() == EstadoVehiculo.EN_LIMPIEZA || reserva.getVehiculo().getEstado() == EstadoVehiculo.FUERA_SERVICIO) {
            throw new VehiculoNoDisponibleException(reserva.getVehiculo().getPatente(), reserva.getVehiculo().getEstado().toString());
        }
        if (reserva.getCliente() != null) {
            for (Ocupacion ocupacionActiva : repoOcupaciones.listar()) {
                if (ocupacionActiva.estaActiva() && ocupacionActiva.getCliente() != null
                        && reserva.getCliente().getId().equals(ocupacionActiva.getCliente().getId())) {
                    throw new ReservaInvalidaException("El cliente ya tiene una ocupación activa.");
                }
            }
        }

        LocalDateTime fechaInicioReserva = reserva.getFechaInicio().atStartOfDay();
        Ocupacion ocupacion = new Ocupacion(UUID.randomUUID().toString(), reserva.getCliente(),
                reserva.getVehiculo(), reserva, fechaInicioReserva,
                reserva.getFechaFin().atStartOfDay(), obtenerUsernameEmpleado(empleado));
        Entrega entrega = new Entrega(UUID.randomUUID().toString(), ocupacion, empleado, fechaInicioReserva,
                reserva.getVehiculo().getKilometraje(), observaciones != null ? observaciones : "");
        entrega.validarDatos();
        ocupacion.setEntrega(entrega);
        reserva.getVehiculo().setEstado(EstadoVehiculo.ALQUILADO);
        reserva.setEstado(EstadoReserva.CONCRETADA);
        if (reserva.getCliente() != null) {
            reserva.getCliente().agregarOcupacion(ocupacion);
        }
        repoOcupaciones.guardar(ocupacion.getIdOcupacion(), ocupacion);
        return ocupacion;
    }

    private String obtenerUsernameEmpleado(Empleado empleado) {
        if (empleado == null || empleado.getId() == null) {
            return "";
        }
        for (Usuario usuario : repoUsuarios.listar()) {
            if (usuario != null && empleado.getId().equals(usuario.getIdUsuario())
                    && usuario.getUsername() != null) {
                return usuario.getUsername().trim();
            }
        }
        return "";
    }

    @Override
    public Devolucion realizarDevolucion(Ocupacion ocupacion, Empleado empleado, int kilometrajeEntrada,
                                        String danios, double cargosExtra) throws Exception {
        if (ocupacion == null || !ocupacion.estaActiva()) {
            throw new Exception("La ocupación no es válida o ya fue finalizada.");
        }
        if (empleado == null) {
            throw new Exception("La devolución debe ser realizada por un empleado registrado.");
        }
        Devolucion devolucion = new Devolucion(UUID.randomUUID().toString(), ocupacion, empleado, LocalDateTime.now(),
                kilometrajeEntrada, danios, cargosExtra);
        devolucion.aplicarMultas();
        devolucion.calcularCargos();
        devolucion.cerrarOcupacion();
        ocupacion.getVehiculo().setKilometraje(kilometrajeEntrada);
        if (devolucion.tieneDanios()) {
            ocupacion.getVehiculo().setEstado(EstadoVehiculo.EN_REPARACION);
        } else {
            ocupacion.getVehiculo().setEstado(EstadoVehiculo.EN_LIMPIEZA);
        }
        return devolucion;
    }

    @Override
    public List<Ocupacion> listarAlquiladosActualmente() {
        List<Ocupacion> activas = new ArrayList<Ocupacion>();
        for (Ocupacion o : repoOcupaciones.listar()) {
            if (o.estaActiva()) {
                activas.add(o);
            }
        }
        return activas;
    }

    @Override
    public List<Ocupacion> listarOcupaciones() {
        return new ArrayList<Ocupacion>(repoOcupaciones.listar());
    }

    @Override
    public void cargarOcupaciones(List<Ocupacion> ocupaciones) {
        repoOcupaciones.limpiar();
        if (ocupaciones != null) {
            for (Ocupacion ocupacion : ocupaciones) {
                repoOcupaciones.guardar(ocupacion.getIdOcupacion(), ocupacion);
            }
        }
    }
}
