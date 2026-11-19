package service;

import exception.ReservaInvalidaException;
import exception.VehiculoNoDisponibleException;
import model.*;

import java.util.List;

public interface ServicioAlquiler {
    Ocupacion realizarEntrega(Reserva reserva, Empleado empleado, String observaciones) throws VehiculoNoDisponibleException, ReservaInvalidaException;
    Devolucion realizarDevolucion(Ocupacion ocupacion, Empleado empleado, int kilometrajeEntrada, String danios, double cargosExtra) throws Exception;
    List<Ocupacion> listarAlquiladosActualmente();
    List<Ocupacion> listarOcupaciones();
    void cargarOcupaciones(List<Ocupacion> ocupaciones);
}
