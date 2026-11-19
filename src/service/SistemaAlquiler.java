package service;

import dao.GestorBaseDatos;
import enums.*;
import exception.*;
import model.*;
import service.impl.ServicioAlquilerImpl;
import util.RepositorioGenerico;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SistemaAlquiler {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ADMIN_USERNAME = "mariano";
    private static final String ADMIN_PASSWORD = "1234";
    private static final String ADMIN_NOMBRE = "Mariano";
    private static final String ADMIN_APELLIDO = "Perez";
    private static final String ADMIN_DNI = "30111222";
    private static final String ADMIN_TELEFONO = "2235550000";
    private static final String ADMIN_EMAIL = "mariano@alquiler.com";
    private static final String ADMIN_LEGAJO = "ADMIN-001";
    private static final String ADMIN_SUCURSAL = "Sucursal Central";
    private static final String LEGACY_APELLIDO_PLACEHOLDER = "SISTEMA";
    private static final String LEGACY_DNI_PLACEHOLDER = "00000000";

    private final ServicioAlquiler servicioAlquiler = new ServicioAlquilerImpl();
    private final ArrayList<Cliente> clientes = new ArrayList<Cliente>();
    private final ArrayList<Reserva> reservas = new ArrayList<Reserva>();
    private ArrayList<Empleado> empleados = new ArrayList<Empleado>();
    private RepositorioGenerico<Vehiculo, String> repoVehiculos = new RepositorioGenerico<Vehiculo, String>();
    private Usuario usuarioLogueado = null;
    private final GestorBaseDatos gestorBaseDatos = new GestorBaseDatos();

    public SistemaAlquiler() {
        inicializarBD();
        crearUsuarioAdministradorPorDefecto();
        cargarDatosDesdeBD();
    }

    private void cargarDatosDesdeBD() {
        java.util.ArrayList<Cliente> clientesBD = gestorBaseDatos.cargarClientes();
        java.util.ArrayList<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
        java.util.ArrayList<Empleado> empleadosBD = gestorBaseDatos.cargarEmpleados();
        java.util.ArrayList<Reserva> reservasBD = gestorBaseDatos.cargarReservas(clientesBD, vehiculosBD);
        java.util.ArrayList<Ocupacion> ocupacionesBD = gestorBaseDatos.cargarOcupaciones(clientesBD, vehiculosBD, empleadosBD, reservasBD);

        clientes.clear();
        clientes.addAll(clientesBD);

        for (Vehiculo vehiculo : vehiculosBD) {
            repoVehiculos.guardar(vehiculo.getPatente(), vehiculo);
        }

        empleados.clear();
        empleados.addAll(empleadosBD);

        reservas.clear();
        reservas.addAll(reservasBD);

        servicioAlquiler.cargarOcupaciones(ocupacionesBD);
        reconstruirHistorialOcupaciones(ocupacionesBD);
    }

    private void reconstruirHistorialOcupaciones(List<Ocupacion> ocupaciones) {
        for (Cliente cliente : clientes) {
            if (cliente != null) {
                cliente.limpiarHistorialOcupaciones();
            }
        }
        if (ocupaciones == null) {
            return;
        }
        for (Ocupacion ocupacion : ocupaciones) {
            if (ocupacion != null && ocupacion.getCliente() != null) {
                ocupacion.getCliente().agregarOcupacion(ocupacion);
            }
        }
    }

    private void inicializarBD() {
        gestorBaseDatos.crearTablas();
    }

    private void crearUsuarioAdministradorPorDefecto() {
        try {
            Usuario existente = gestorBaseDatos.cargarUsuarioPorUsername(ADMIN_USERNAME);
            if (existente == null) {
                String idAdmin = UUID.randomUUID().toString();
                Empleado admin = construirEmpleadoAdmin(idAdmin);
                gestorBaseDatos.guardarEmpleado(admin);
                Usuario mariano = new Usuario(idAdmin, ADMIN_USERNAME, ADMIN_PASSWORD, RolUsuario.ADMINISTRADOR, true, admin);
                gestorBaseDatos.guardarUsuario(mariano);
            } else {
                Empleado admin = gestorBaseDatos.cargarEmpleadoPorId(existente.getIdUsuario());
                if (admin == null || datosEmpleadoAdminPorDefecto(admin)) {
                    admin = construirEmpleadoAdmin(existente.getIdUsuario());
                    gestorBaseDatos.guardarEmpleado(admin);
                }
            }
        } catch (Exception e) {
            // Ignorar
        }
    }

    private Empleado construirEmpleadoAdmin(String id) {
        return new Empleado(id, ADMIN_NOMBRE, ADMIN_APELLIDO, ADMIN_DNI, ADMIN_TELEFONO, ADMIN_EMAIL,
                ADMIN_LEGAJO, LocalDate.now(), ADMIN_SUCURSAL);
    }

    private boolean datosEmpleadoAdminPorDefecto(Empleado empleado) {
        return LEGACY_APELLIDO_PLACEHOLDER.equals(empleado.getApellido()) || LEGACY_DNI_PLACEHOLDER.equals(empleado.getDni());
    }

    public String login(String user, String pass) {
        try {
            Usuario u = gestorBaseDatos.cargarUsuarioPorUsername(user);
            if (u == null) {
                return "Usuario no encontrado.";
            }
            if (!u.isActivo()) {
                return "Usuario inactivo.";
            }
            if (u.getPasswordHash().equals(pass)) {
                if (u.getRol() == RolUsuario.CLIENTE) {
                    Cliente clienteAsociado = buscarClientePorId(u.getIdUsuario());
                    if (clienteAsociado == null) {
                        return "El usuario CLIENTE no tiene una ficha de cliente asociada.";
                    }
                    u.setCliente(clienteAsociado);
                } else {
                    u.setEmpleado(gestorBaseDatos.cargarEmpleadoPorId(u.getIdUsuario()));
                }
                usuarioLogueado = u;
                return "Login exitoso! Rol: " + u.getRol();
            }
            return "Credenciales incorrectas.";
        } catch (Exception e) {
            return "Error al iniciar sesión: " + e.getMessage();
        }
    }

    private Cliente buscarClientePorId(String idCliente) {
        if (idCliente == null || clientes == null) {
            return null;
        }
        for (Cliente cliente : clientes) {
            if (cliente != null && idCliente.equals(cliente.getId())) {
                return cliente;
            }
        }
        return null;
    }

    public String listarDisponibles() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- AUTOMOVILES DISPONIBLES ---\n");
        try {
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD == null) {
                vehiculosBD = new java.util.ArrayList<Vehiculo>();
            }
            java.util.ArrayList<Vehiculo> contenedorGenerico = new java.util.ArrayList<Vehiculo>(vehiculosBD);
            boolean hay = false;
            for (Vehiculo v : contenedorGenerico) {
                if (v != null && v.getEstado() == enums.EstadoVehiculo.DISPONIBLE) {
                    hay = true;
                    sb.append(v.getPatente()).append(" - ").append(v.getMarca()).append(" ").append(v.getModelo())
                            .append(" | Estado: ").append(v.getEstado()).append(" | $/día: $").append(v.getPrecioDia())
                            .append(" | Km: ").append(v.getKilometraje()).append("\n");
                }
            }
            if (!hay) sb.append("No hay automóviles disponibles.\n");
        } catch (Exception e) {
            sb.append("Error al consultar vehículos: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public String listarAlquilados() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- OCUPACIONES ACTIVAS ---\n");
        try {
            ArrayList<Ocupacion> activas = new ArrayList<Ocupacion>(servicioAlquiler.listarAlquiladosActualmente());
            if (activas.isEmpty()) {
                sb.append("No hay ocupaciones activas.\n");
                return sb.toString();
            }
            int contador = 0;
            for (Ocupacion ocupacion : activas) {
                if (ocupacion != null && ocupacion.getVehiculo() != null && ocupacion.getCliente() != null) {
                    sb.append(contador).append(": Ocupacion: ").append(ocupacion.getIdOcupacion().substring(0, 8))
                            .append(" | Patente: ").append(ocupacion.getVehiculo().getPatente())
                            .append(" | Cliente: ").append(ocupacion.getCliente().getNombreCompleto())
                            .append(" | Inicio: ").append(ocupacion.getFechaInicio()).append(" | Kilometraje: ").append(ocupacion.getVehiculo().getKilometraje()).append("\n");
                    contador++;
                }
            }
        } catch (Exception e) {
            sb.append("Error al listar alquilados: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public String listarNoDisponibles() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- AUTOMOVILES NO DISPONIBLES (CON MOTIVO) ---\n");
        try {
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD == null) {
                vehiculosBD = new java.util.ArrayList<Vehiculo>();
            }
            java.util.ArrayList<Vehiculo> contenedorGenerico = new java.util.ArrayList<Vehiculo>(vehiculosBD);
            boolean hay = false;
            for (Vehiculo v : contenedorGenerico) {
                if (v != null && v.getEstado() != enums.EstadoVehiculo.DISPONIBLE) {
                    hay = true;
                    String motivo = v.getEstado() == enums.EstadoVehiculo.ALQUILADO ? "ALQUILADO - En poder del cliente" :
                            v.getEstado() == enums.EstadoVehiculo.RESERVADO ? "RESERVADO - Pendiente de entrega" :
                                    v.getEstado() == enums.EstadoVehiculo.EN_LIMPIEZA ? "EN LIMPIEZA - En proceso" :
                                            v.getEstado() == enums.EstadoVehiculo.EN_REPARACION ? "EN REPARACION - Mantenimiento" : "FUERA DE SERVICIO";
                    sb.append(v.getPatente()).append(" - ").append(v.getMarca()).append(" ").append(v.getModelo())
                            .append(" | Estado: ").append(v.getEstado()).append(" | Motivo: ").append(motivo)
                            .append(" | $/día: $").append(v.getPrecioDia()).append(" | Km: ").append(v.getKilometraje()).append("\n");
                }
            }
            if (!hay) sb.append("No hay automóviles no disponibles.\n");
        } catch (Exception e) {
            sb.append("Error al consultar vehículos: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public String crearCliente(String nombre, String apellido, String dni, String telefono, String email) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para crear clientes.";
        }

        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede crear clientes.");
        }

        String errorDatosPersona = validarDatosPersona(nombre, apellido, dni, telefono, email);
        if (errorDatosPersona != null) {
            return errorDatosPersona;
        }

        Cliente nuevoCliente = new Cliente(UUID.randomUUID().toString(), nombre.trim(), apellido.trim(), dni.trim(),
                telefono != null ? telefono.trim() : "", email.trim(), java.time.LocalDate.now());
        java.util.ArrayList<Cliente> contenedorClientes = new java.util.ArrayList<Cliente>(clientes);
        contenedorClientes.add(nuevoCliente);
        clientes.clear();
        clientes.addAll(contenedorClientes);
        gestorBaseDatos.guardarCliente(nuevoCliente);
        return "Cliente " + nuevoCliente.getNombreCompleto() + " creado correctamente.";
    }

    public String crearReserva(int idxCliente, int idxVehiculo, LocalDate inicio, LocalDate fin) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para crear reservas.";
        }
        if (usuarioLogueado.getRol() == RolUsuario.CLIENTE) {
            return "Los usuarios CLIENTE solo pueden reservar con su propia ficha.";
        }
        if (clientes.isEmpty()) {
            return "No hay clientes registrados. Cree un cliente primero.";
        }
        if (idxCliente < 0 || idxCliente >= clientes.size()) {
            return "Índices inválidos.";
        }
        return crearReservaParaCliente(clientes.get(idxCliente), idxVehiculo, inicio, fin);
    }

    public String crearReservaCliente(int idxVehiculo, LocalDate inicio, LocalDate fin) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para crear reservas.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.CLIENTE) {
            return "Esta opción es exclusiva para usuarios CLIENTE.";
        }
        Cliente clienteLogueado = usuarioLogueado.getCliente();
        if (clienteLogueado == null) {
            return "El usuario no tiene una ficha de cliente asociada.";
        }
        return crearReservaParaCliente(clienteLogueado, idxVehiculo, inicio, fin);
    }

    private String crearReservaParaCliente(Cliente cliente, int idxVehiculo, LocalDate inicio, LocalDate fin) {
        try {
            if (cliente == null) {
                return "Cliente inválido.";
            }
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD == null) {
                vehiculosBD = new java.util.ArrayList<Vehiculo>();
            }
            java.util.ArrayList<Vehiculo> contenedorGenericoVehiculos = new java.util.ArrayList<Vehiculo>();
            for (Vehiculo vehiculoDisponible : vehiculosBD) {
                if (vehiculoDisponible != null && vehiculoDisponible.getEstado() == enums.EstadoVehiculo.DISPONIBLE) {
                    contenedorGenericoVehiculos.add(vehiculoDisponible);
                }
            }
            if (contenedorGenericoVehiculos.isEmpty()) {
                return "No hay automóviles disponibles para reservar.";
            }
            if (idxVehiculo < 0 || idxVehiculo >= contenedorGenericoVehiculos.size()) {
                return "Índices inválidos.";
            }
            for (Reserva r : reservas) {
                if (r != null && r.getCliente() != null && r.getCliente().getId().equals(cliente.getId()) && r.getEstado() != enums.EstadoReserva.CANCELADA && r.getEstado() != enums.EstadoReserva.CONCRETADA) {
                    if (r.getFechaInicio() != null && r.getFechaFin() != null) {
                        if (!(fin.isBefore(r.getFechaInicio()) || inicio.isAfter(r.getFechaFin()))) {
                            return "El cliente ya tiene una reserva que se solapa con las fechas.";
                        }
                    }
                }
            }
            Vehiculo vehiculoSeleccionado = contenedorGenericoVehiculos.get(idxVehiculo);
            for (Reserva r : reservas) {
                if (r != null && r.getVehiculo() != null && r.getVehiculo().getPatente().equals(vehiculoSeleccionado.getPatente()) && r.getEstado() != enums.EstadoReserva.CANCELADA && r.getEstado() != enums.EstadoReserva.CONCRETADA) {
                    if (r.getFechaInicio() != null && r.getFechaFin() != null) {
                        if (!(fin.isBefore(r.getFechaInicio()) || inicio.isAfter(r.getFechaFin()))) {
                            return "El vehículo ya tiene una reserva que se solapa con las fechas.";
                        }
                    }
                }
            }
            for (Ocupacion o : servicioAlquiler.listarAlquiladosActualmente()) {
                if (o != null && o.getCliente() != null && o.getCliente().getId().equals(cliente.getId()) && o.estaActiva()) {
                    return "El cliente ya tiene un automóvil alquilado al mismo tiempo.";
                }
            }
            Vehiculo vehiculo = vehiculoSeleccionado;
            Reserva reserva = new Reserva(UUID.randomUUID().toString(), cliente, vehiculo, inicio, fin,
                    LocalDateTime.now(), EstadoReserva.PENDIENTE,
                    usuarioLogueado != null ? usuarioLogueado.getUsername() : "");
            if (reserva.validar()) {
                java.util.ArrayList<Reserva> contenedorReservas = new java.util.ArrayList<Reserva>(reservas);
                contenedorReservas.add(reserva);
                reservas.clear();
                reservas.addAll(contenedorReservas);
                gestorBaseDatos.guardarReserva(reserva);
                return "Reserva creada correctamente. ID: " + reserva.getIdReserva().substring(0, 8) +
                        "\nCliente: " + cliente.getNombreCompleto() + " | DNI: " + cliente.getDni() +
                        "\nMonto total estimado: $" + reserva.calcularMontoTotal();
            }
            return "Fechas inválidas.";
        } catch (Exception e) {
            return "Error al crear reserva: " + e.getMessage();
        }
    }

    public String listarClientes() {
        if (usuarioLogueado != null && usuarioLogueado.getRol() == RolUsuario.CLIENTE) {
            throw new UsuarioNoAutorizadoException("Un usuario CLIENTE no puede listar todos los clientes.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- LISTADO DE CLIENTES ---\n");
        if (clientes == null || clientes.isEmpty()) {
            sb.append("No hay clientes registrados.\n");
            return sb.toString();
        }
        for (int i = 0; i < clientes.size(); i++) {
            Cliente c = clientes.get(i);
            if (c != null) {
                sb.append(i).append(": ").append(c.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public String listarReservas() {
        if (usuarioLogueado != null && usuarioLogueado.getRol() == RolUsuario.CLIENTE) {
            throw new UsuarioNoAutorizadoException("Un usuario CLIENTE no puede listar todas las reservas.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- LISTADO DE RESERVAS ---\n");
        if (reservas == null || reservas.isEmpty()) {
            sb.append("No hay reservas.\n");
            return sb.toString();
        }
        for (int i = 0; i < reservas.size(); i++) {
            Reserva r = reservas.get(i);
            if (r != null) {
                sb.append(i).append(": ").append(r.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public String listarReservasCancelables() {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para cancelar reservas.";
        }
        ArrayList<Reserva> lista = reservasCancelables();
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- RESERVAS CANCELABLES ---\n");
        if (lista.isEmpty()) {
            sb.append("No hay reservas cancelables.\n");
            return sb.toString();
        }
        for (int i = 0; i < lista.size(); i++) {
            sb.append(i).append(": ").append(lista.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

    public String cancelarReserva(int indice) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para cancelar reservas.";
        }
        ArrayList<Reserva> lista = reservasCancelables();
        if (indice < 0 || indice >= lista.size()) {
            return "Índice de reserva inválido.";
        }
        Reserva reserva = lista.get(indice);
        if (!puedeCancelarReserva(reserva)) {
            return "No tiene permiso para cancelar esa reserva.";
        }
        reserva.cancelar();
        gestorBaseDatos.actualizarEstadoReserva(reserva.getIdReserva(), EstadoReserva.CANCELADA.name());
        return "Reserva " + reserva.getIdReserva().substring(0, 8) + " cancelada correctamente.";
    }

    private ArrayList<Reserva> reservasCancelables() {
        ArrayList<Reserva> lista = new ArrayList<Reserva>();
        if (usuarioLogueado == null || reservas == null) {
            return lista;
        }
        for (Reserva reserva : reservas) {
            if (puedeCancelarReserva(reserva)) {
                lista.add(reserva);
            }
        }
        return lista;
    }

    private boolean puedeCancelarReserva(Reserva reserva) {
        if (usuarioLogueado == null || reserva == null || reserva.getEstado() == null) {
            return false;
        }
        if (reserva.getEstado() == EstadoReserva.CONCRETADA || reserva.getEstado() == EstadoReserva.CANCELADA) {
            return false;
        }
        for (Ocupacion ocupacion : servicioAlquiler.listarOcupaciones()) {
            if (ocupacion != null && ocupacion.estaActiva() && ocupacion.getReserva() != null
                    && ocupacion.getReserva().getIdReserva() != null
                    && ocupacion.getReserva().getIdReserva().equals(reserva.getIdReserva())) {
                return false;
            }
        }
        if (usuarioLogueado.getRol() == RolUsuario.CLIENTE) {
            Cliente cliente = usuarioLogueado.getCliente();
            return cliente != null && reserva.getCliente() != null
                    && cliente.getId().equals(reserva.getCliente().getId());
        }
        return usuarioLogueado.getRol() == RolUsuario.ADMINISTRADOR
                || usuarioLogueado.getRol() == RolUsuario.EMPLEADO;
    }

    public String eliminarCliente(int indice) {
        if (usuarioLogueado == null) return "Debe iniciar sesión primero";
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede eliminar clientes.");
        }
        if (indice < 0 || clientes == null || indice >= clientes.size()) return "Cliente inválido";
        Cliente c = clientes.get(indice);
        if (c == null) return "Cliente inválido";
        java.util.ArrayList<Ocupacion> ocupacionesCliente = new java.util.ArrayList<Ocupacion>();
        for (Ocupacion o : servicioAlquiler.listarOcupaciones()) {
            if (o != null && o.getCliente() != null && o.getCliente().getId().equals(c.getId())) {
                if (o.estaActiva()) {
                    return "No se puede eliminar el cliente: tiene una ocupación activa (vehículo "
                            + (o.getVehiculo() != null ? o.getVehiculo().getPatente() : "sin patente") + ").";
                }
                ocupacionesCliente.add(o);
            }
        }
        java.util.ArrayList<Reserva> aEliminar = new java.util.ArrayList<Reserva>();
        for (Reserva r : new java.util.ArrayList<Reserva>(reservas)) {
            if (r != null && r.getCliente() != null && r.getCliente().getId().equals(c.getId())) {
                aEliminar.add(r);
            }
        }
        for (Reserva r : aEliminar) {
            reservas.remove(r);
            GestorBaseDatos.eliminarReserva(r.getIdReserva());
        }
        for (Ocupacion o : ocupacionesCliente) {
            gestorBaseDatos.eliminarOcupacionCompleta(o.getIdOcupacion());
        }
        clientes.remove(indice);
        GestorBaseDatos.eliminarCliente(c.getId());
        try {
            Usuario usuarioAsociado = gestorBaseDatos.cargarUsuarioPorId(c.getId());
            if (usuarioAsociado != null && usuarioAsociado.getRol() == RolUsuario.CLIENTE) {
                gestorBaseDatos.eliminarUsuarioPorId(c.getId());
            }
        } catch (Exception ignored) {
        }
        recargarOcupacionesDesdeBD();
        return "Cliente eliminado correctamente. Se eliminaron " + aEliminar.size() + " reserva(s) y "
                + ocupacionesCliente.size() + " ocupación(es) asociada(s).";
    }

    public String eliminarVehiculo(int indice) {
        if (usuarioLogueado == null) return "Debe iniciar sesión primero";
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede eliminar vehículos.");
        }
        java.util.ArrayList<Vehiculo> listaVehiculos = new java.util.ArrayList<Vehiculo>();
        try {
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD != null) {
                listaVehiculos.addAll(vehiculosBD);
            }
        } catch (Exception e) {
            return "Error al consultar vehículos: " + e.getMessage();
        }
        if (indice < 0 || listaVehiculos.isEmpty() || indice >= listaVehiculos.size()) return "Vehículo inválido";
        Vehiculo v = listaVehiculos.get(indice);
        if (v == null) return "Vehículo inválido";
        java.util.ArrayList<Ocupacion> ocupacionesVehiculo = new java.util.ArrayList<Ocupacion>();
        for (Ocupacion o : servicioAlquiler.listarOcupaciones()) {
            if (o != null && o.getVehiculo() != null && o.getVehiculo().getPatente().equals(v.getPatente())) {
                if (o.estaActiva()) {
                    return "No se puede eliminar el vehículo: tiene una ocupación activa.";
                }
                ocupacionesVehiculo.add(o);
            }
        }
        java.util.ArrayList<Reserva> aEliminar = new java.util.ArrayList<Reserva>();
        for (Reserva r : new java.util.ArrayList<Reserva>(reservas)) {
            if (r != null && r.getVehiculo() != null && r.getVehiculo().getPatente().equals(v.getPatente())) {
                aEliminar.add(r);
            }
        }
        for (Reserva r : aEliminar) {
            reservas.remove(r);
            GestorBaseDatos.eliminarReserva(r.getIdReserva());
        }
        for (Ocupacion o : ocupacionesVehiculo) {
            gestorBaseDatos.eliminarOcupacionCompleta(o.getIdOcupacion());
        }
        if (repoVehiculos != null && v.getPatente() != null) {
            repoVehiculos.eliminar(v.getPatente());
        }
        GestorBaseDatos.eliminarVehiculo(v.getPatente());
        recargarOcupacionesDesdeBD();
        return "Vehículo eliminado correctamente. Se eliminaron " + aEliminar.size() + " reserva(s) y "
                + ocupacionesVehiculo.size() + " ocupación(es) asociada(s).";
    }

    public String eliminarReserva(int indice) {
        if (usuarioLogueado == null) return "Debe iniciar sesión primero";
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede eliminar reservas.");
        }
        if (indice < 0 || reservas == null || indice >= reservas.size()) return "Reserva inválida";
        Reserva r = reservas.get(indice);
        if (r == null) return "Reserva inválida";
        java.util.ArrayList<Ocupacion> ocupacionesReserva = new java.util.ArrayList<Ocupacion>();
        for (Ocupacion o : servicioAlquiler.listarOcupaciones()) {
            if (o != null && o.getReserva() != null && o.getReserva().getIdReserva().equals(r.getIdReserva())) {
                if (o.estaActiva()) {
                    return "No se puede eliminar la reserva: tiene una ocupación activa.";
                }
                ocupacionesReserva.add(o);
            }
        }
        for (Ocupacion o : ocupacionesReserva) {
            gestorBaseDatos.eliminarOcupacionCompleta(o.getIdOcupacion());
        }
        reservas.remove(indice);
        GestorBaseDatos.eliminarReserva(r.getIdReserva());
        recargarOcupacionesDesdeBD();
        return "Reserva eliminada correctamente. Se eliminaron " + ocupacionesReserva.size() + " ocupación(es) asociada(s).";
    }

    private void recargarOcupacionesDesdeBD() {
        try {
            java.util.ArrayList<Vehiculo> vehiculosBD = new java.util.ArrayList<Vehiculo>(gestorBaseDatos.cargarVehiculos());
            java.util.ArrayList<Cliente> clientesBD = new java.util.ArrayList<Cliente>(clientes);
            java.util.ArrayList<Empleado> empleadosBD = new java.util.ArrayList<Empleado>(empleados);
            java.util.ArrayList<Reserva> reservasBD = new java.util.ArrayList<Reserva>(reservas);
            java.util.ArrayList<Ocupacion> ocupacionesBD = new java.util.ArrayList<Ocupacion>(
                    gestorBaseDatos.cargarOcupaciones(clientesBD, vehiculosBD, empleadosBD, reservasBD));
            servicioAlquiler.cargarOcupaciones(ocupacionesBD);
            reconstruirHistorialOcupaciones(ocupacionesBD);
        } catch (Exception ignored) {
        }
    }

    public String listarUsuarios() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- USUARIOS ---\n");
        try {
            List<Usuario> listaUsuarios = gestorBaseDatos.cargarUsuarios();
            if (listaUsuarios == null || listaUsuarios.isEmpty()) {
                sb.append("No hay usuarios registrados.\n");
                return sb.toString();
            }
            int i = 0;
            for (Usuario u : listaUsuarios) {
                sb.append(i).append(": ").append(u.getUsername())
                        .append(" | Rol: ").append(u.getRol())
                        .append(" | Activo: ").append(u.isActivo()).append("\n");
                i++;
            }
        } catch (Exception e) {
            sb.append("Error al consultar usuarios: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public String eliminarUsuarioPorIndice(int indice) {
        try {
            List<Usuario> listaUsuarios = gestorBaseDatos.cargarUsuarios();
            if (listaUsuarios == null || listaUsuarios.isEmpty()) return "No hay usuarios registrados.";
            if (indice < 0 || indice >= listaUsuarios.size()) return "Indice de usuario invalido.";
            return eliminarUsuario(listaUsuarios.get(indice).getUsername());
        } catch (UsuarioNoAutorizadoException ex) {
            throw ex;
        } catch (Exception e) {
            return "Error al eliminar usuario: " + e.getMessage();
        }
    }

    public String eliminarUsuario(String nombreUsuario) {
        if (usuarioLogueado == null) return "Debe iniciar sesión primero";
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede eliminar usuarios.");
        }
        if (nombreUsuario == null || nombreUsuario.isEmpty()) return "Nombre de usuario inválido";
        if (ADMIN_USERNAME.equals(nombreUsuario)) return "No se puede eliminar el usuario por defecto";
        if (usuarioLogueado.getUsername() != null && usuarioLogueado.getUsername().equals(nombreUsuario)) {
            return "No puede eliminar el usuario con el que está logueado.";
        }
        try {
            Usuario u = gestorBaseDatos.cargarUsuarioPorUsername(nombreUsuario);
            if (u == null) return "Usuario no encontrado";
            String idUsuario = u.getIdUsuario();
            gestorBaseDatos.eliminarUsuarioPorId(idUsuario);
            GestorBaseDatos.eliminarEmpleado(idUsuario);
            empleados.removeIf(empleado -> empleado != null && idUsuario.equals(empleado.getId()));
            return "Usuario eliminado correctamente.";
        } catch (Exception e) {
            return "Error al eliminar usuario: " + e.getMessage();
        }
    }

    public String getReservaInfo(int idx) {
        if (reservas == null || reservas.isEmpty()) return "No hay reservas";
        if (idx < 0 || idx >= reservas.size()) return "";
        Reserva r = reservas.get(idx);
        if (r == null) return "";
        String estado = r.getEstado() != null ? r.getEstado().name() : "";
        return idx + ": " + r.getIdReserva().substring(0, 8) + " | Cliente: " + (r.getCliente() != null ? r.getCliente().getNombreCompleto() : "") + " | Vehículo: " + (r.getVehiculo() != null ? r.getVehiculo().getPatente() : "") + " | Estado: " + estado;
    }

    public String realizarEntregaPorIndice(int indiceReserva, String observaciones) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para realizar entregas.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO pueden realizar entregas.");
        }
        if (reservas == null || reservas.isEmpty()) {
            return "No hay reservas.";
        }
        if (indiceReserva < 0 || indiceReserva >= reservas.size()) {
            return "Índice de reserva inválido.";
        }
        try {
            java.util.ArrayList<Reserva> contenedorReservas = new java.util.ArrayList<Reserva>(reservas);
            Reserva reserva = contenedorReservas.get(indiceReserva);
            if (reserva.getEstado() == EstadoReserva.CONCRETADA || reserva.getEstado() == EstadoReserva.CANCELADA) {
                return "La reserva ya está " + reserva.getEstado() + " y no puede entregarse.";
            }
            Empleado emp = usuarioLogueado.getEmpleado();
            if (emp == null) {
                return "El usuario '" + usuarioLogueado.getUsername() + "' no tiene un legajo de empleado asociado.";
            }
            reserva.confirmar();
            Ocupacion ocupacion = servicioAlquiler.realizarEntrega(reserva, emp, observaciones);
            gestorBaseDatos.guardarOcupacion(ocupacion);
            if (ocupacion.getEntrega() != null) {
                gestorBaseDatos.guardarEntrega(ocupacion.getEntrega());
            }
            GestorBaseDatos.eliminarReserva(reserva.getIdReserva());
            reservas.remove(reserva);
            StringBuilder sb = new StringBuilder();
            sb.append("Entrega realizada con éxito!\n");
            sb.append("Ocupación: ").append(ocupacion.getIdOcupacion().substring(0, 8)).append("\n");
            if (ocupacion.getVehiculo() != null) {
                sb.append("Vehículo: ").append(ocupacion.getVehiculo().getPatente());
            }
            if (ocupacion.getCliente() != null) {
                sb.append(" | Cliente: ").append(ocupacion.getCliente().getNombreCompleto());
            }
            sb.append("\nLa reserva asociada se eliminó de la lista y de la base de datos.\n");
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    public String realizarDevolucionPorIndice(int indiceOcupacion, int kilometrosRecorridos, String danios, double cargosExtra) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para realizar devoluciones.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO pueden realizar devoluciones.");
        }
        if (kilometrosRecorridos < 0) {
            return "Los kilómetros recorridos no pueden ser negativos.";
        }
        if (cargosExtra < 0) {
            return "Los cargos extra no pueden ser negativos.";
        }
        ArrayList<Ocupacion> activas = new ArrayList<Ocupacion>(servicioAlquiler.listarAlquiladosActualmente());
        if (activas.isEmpty()) {
            return "No hay ocupaciones activas.";
        }
        if (indiceOcupacion < 0 || indiceOcupacion >= activas.size()) {
            return "Índice de ocupación inválido.";
        }
        try {
            Ocupacion ocupacion = activas.get(indiceOcupacion);
            Empleado emp = usuarioLogueado.getEmpleado();
            if (emp == null) {
                return "El usuario '" + usuarioLogueado.getUsername() + "' no tiene un legajo de empleado asociado.";
            }
            int kilometrajeEntradaCalculado = 0;
            if (ocupacion.getVehiculo() != null) {
                kilometrajeEntradaCalculado = ocupacion.getVehiculo().getKilometraje() + kilometrosRecorridos;
            }
            Devolucion dev = servicioAlquiler.realizarDevolucion(ocupacion, emp, kilometrajeEntradaCalculado, danios, cargosExtra);
            if (ocupacion.getVehiculo() != null) {
                ocupacion.getVehiculo().setKilometraje(kilometrajeEntradaCalculado);
                gestorBaseDatos.actualizarVehiculo(ocupacion.getVehiculo());
            }
            gestorBaseDatos.guardarDevolucion(dev);
            if (dev.getOcupacion() != null) {
                gestorBaseDatos.guardarOcupacion(dev.getOcupacion());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Devolución realizada con éxito!\n");
            sb.append("Kilometraje anterior: ").append(ocupacion.getVehiculo() != null ? (ocupacion.getVehiculo().getKilometraje() - kilometrosRecorridos) : 0).append("\n");
            sb.append("Kilometraje actual: ").append(kilometrajeEntradaCalculado).append("\n");
            sb.append("Monto final: $").append(dev.calcularCargos()).append("\n");
            if (ocupacion.getVehiculo() != null) {
                sb.append("Vehículo ahora en estado: ").append(ocupacion.getVehiculo().getEstado()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    private String formatearFecha(LocalDate fecha) {
        return fecha != null ? FORMATO_FECHA.format(fecha) : "sin fecha";
    }

    private String formatearFechaHora(LocalDateTime fecha) {
        return fecha != null ? FORMATO_FECHA_HORA.format(fecha) : "sin fecha";
    }

    private boolean reservaRepresentadaEnHistorial(Cliente cliente, Reserva reserva) {
        if (cliente == null || reserva == null || reserva.getIdReserva() == null) {
            return false;
        }
        for (Ocupacion ocupacion : cliente.getHistorialOcupaciones()) {
            if (ocupacion != null && ocupacion.getReserva() != null
                    && reserva.getIdReserva().equals(ocupacion.getReserva().getIdReserva())) {
                return true;
            }
        }
        return false;
    }

    public String verHistorialCliente() {
        if (usuarioLogueado != null && usuarioLogueado.getRol() == RolUsuario.CLIENTE) {
            throw new UsuarioNoAutorizadoException("Un usuario CLIENTE no puede ver el historial de todos los clientes.");
        }
        if (clientes == null || clientes.isEmpty()) {
            return "No hay clientes.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- HISTORIAL DE CLIENTES ---\n");
        java.util.HashSet<String> clientesConHistorial = new java.util.HashSet<String>();
        java.util.ArrayList<Reserva> reservasActuales = new java.util.ArrayList<Reserva>();
        if (reservas != null) {
            reservasActuales.addAll(reservas);
        }
        for (Reserva r : reservasActuales) {
            if (r != null && r.getCliente() != null) {
                clientesConHistorial.add(r.getCliente().getId());
            }
        }
        for (Cliente cli : clientes) {
            if (cli != null && !cli.getHistorialOcupaciones().isEmpty()) {
                clientesConHistorial.add(cli.getId());
            }
        }
        boolean hay = false;
        for (Cliente cli : clientes) {
            if (cli != null && clientesConHistorial.contains(cli.getId())) {
                hay = true;
                sb.append("\n").append(cli.getNombreCompleto()).append(" (DNI ").append(cli.getDni()).append(")\n");
                for (Reserva r : reservasActuales) {
                    if (r != null && r.getCliente() != null && r.getCliente().getId().equals(cli.getId())
                            && !reservaRepresentadaEnHistorial(cli, r)) {
                        sb.append("  RESERVA: realizada el ").append(formatearFechaHora(r.getFechaReserva()))
                                .append(" | período ").append(formatearFecha(r.getFechaInicio()))
                                .append(" al ").append(formatearFecha(r.getFechaFin()))
                                .append(" | estado ").append(r.getEstado())
                                .append(r.getVehiculo() != null ? " | vehículo " + r.getVehiculo().getPatente() : "")
                                .append("\n");
                    }
                }
                for (Ocupacion o : cli.getHistorialOcupaciones()) {
                    if (o != null) {
                        if (o.getFechaReserva() != null || o.getFechaInicioReserva() != null
                                || o.getFechaFinReserva() != null) {
                            sb.append("  RESERVA: realizada el ").append(formatearFechaHora(o.getFechaReserva()))
                                    .append(" | período ").append(formatearFecha(o.getFechaInicioReserva()))
                                    .append(" al ").append(formatearFecha(o.getFechaFinReserva())).append("\n");
                        }
                        sb.append("  ENTREGA: ").append(formatearFechaHora(
                                        o.getEntrega() != null ? o.getEntrega().getFechaHoraEntrega() : o.getFechaInicio()))
                                .append(o.getVehiculo() != null ? " | vehículo " + o.getVehiculo().getPatente() : "")
                                .append("\n");
                        if (o.getFechaFinReal() != null && o.getDevolucion() != null) {
                            sb.append("  DEVOLUCIÓN: ").append(formatearFechaHora(o.getFechaFinReal()))
                                    .append(" | cargos extra $").append(o.getDevolucion().getCargosExtra())
                                    .append(" | monto final $").append(o.getDevolucion().getMontoFinal()).append("\n");
                        }
                    }
                }
            }
        }
        if (!hay) {
            sb.append("No hay clientes con historial registrado.\n");
        }
        return sb.toString();
    }

    public String administrarUsuarios() {
        if (usuarioLogueado == null || usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new UsuarioNoAutorizadoException("Solo ADMINISTRADOR puede acceder a esta función.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- ADMINISTRAR USUARIOS ---\n");
        try {
            List<Usuario> listaUsuarios = gestorBaseDatos.cargarUsuarios();
            for (Usuario u : listaUsuarios) {
                sb.append("Usuario: ").append(u.getUsername()).append(" | Rol: ").append(u.getRol())
                        .append(" | Activo: ").append(u.isActivo()).append("\n");
            }
        } catch (Exception e) {
            sb.append("Error al consultar usuarios: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public String verFlotaCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- AUTOMOVILES (POLIMORFISMO) ---\n");
        try {
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD == null) {
                vehiculosBD = new java.util.ArrayList<Vehiculo>();
            }
            java.util.ArrayList<Vehiculo> contenedorGenerico = new java.util.ArrayList<Vehiculo>(vehiculosBD);
            for (Vehiculo v : contenedorGenerico) {
                if (v != null) {
                    sb.append(v.getClass().getSimpleName()).append(": ").append(v.getPatente()).append(" - ")
                            .append(v.getMarca()).append(" ").append(v.getModelo())
                            .append(" | Estado: ").append(v.getEstado())
                            .append(" | $/día: $").append(v.getPrecioDia())
                            .append(" | Km: ").append(v.getKilometraje()).append("\n");
                }
            }
            if (contenedorGenerico.isEmpty()) {
                sb.append("No hay automóviles registrados en la base de datos.\n");
            }
        } catch (Exception e) {
            sb.append("Error al consultar vehículos: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }


    public String agregarAutomovil(String patente, String marca, String modelo, int anio, int kilometraje,
                                   String estadoTexto, double precioDia, double tarifaSeguro,
                                   int cantidadPuertas, String transmision) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para agregar vehículos.";
        }

        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede agregar vehículos.");
        }

        if (patente == null || patente.trim().isEmpty()) {
            return "La patente no puede estar vacía.";
        }

        if (marca == null || marca.trim().isEmpty()) {
            return "La marca no puede estar vacía.";
        }

        if (marca.matches(".*\\d.*")) {
            return "La marca no puede contener números.";
        }

        if (modelo == null || modelo.trim().isEmpty()) {
            return "El modelo no puede estar vacío.";
        }

        int anioActual = java.time.Year.now().getValue();
        if (anio > anioActual) {
            return "El año de fabricación no puede superar el año actual.";
        }

        if (anio < 1900) {
            return "El año de fabricación no es válido.";
        }

        if (kilometraje < 0) {
            return "El kilometraje no puede ser negativo.";
        }

        if (precioDia <= 0) {
            return "El precio por día debe ser mayor a 0.";
        }

        if (tarifaSeguro < 0) {
            return "La tarifa de seguro no puede ser negativa.";
        }

        if (cantidadPuertas <= 0) {
            return "La cantidad de puertas debe ser mayor a 0.";
        }

        if (transmision == null || transmision.trim().isEmpty()) {
            return "La transmisión no puede estar vacía.";
        }

        String transmisionNormalizada = transmision.trim().toUpperCase();
        if (!transmisionNormalizada.equals(Automovil.TRANSMISION_MANUAL) &&
                !transmisionNormalizada.equals(Automovil.TRANSMISION_AUTOMATICA)) {
            return "Transmisión inválida. Use MANUAL o AUTOMATICA.";
        }

        try {
            if (gestorBaseDatos.cargarVehiculoPorPatente(patente) != null) {
                return "Ya existe un vehículo con esa patente.";
            }

            EstadoVehiculo estadoVehiculo;
            try {
                estadoVehiculo = EstadoVehiculo.valueOf(estadoTexto.toUpperCase().trim());
            } catch (Exception excepcion) {
                return "Estado inválido. Use: DISPONIBLE, EN_LIMPIEZA, EN_REPARACION o FUERA_SERVICIO.";
            }
            if (estadoVehiculo == EstadoVehiculo.ALQUILADO || estadoVehiculo == EstadoVehiculo.RESERVADO) {
                return "No se puede crear un vehículo en estado ALQUILADO o RESERVADO.";
            }

            Vehiculo nuevoVehiculo = new Automovil(patente, marca, modelo, anio, kilometraje, estadoVehiculo, precioDia, tarifaSeguro,
                    cantidadPuertas, transmisionNormalizada);

            gestorBaseDatos.guardarVehiculo(nuevoVehiculo);
            repoVehiculos.guardar(nuevoVehiculo.getPatente(), nuevoVehiculo);
            return "Automóvil " + patente + " agregado correctamente a la base de datos.";
        } catch (UsuarioNoAutorizadoException excepcion) {
            throw excepcion;
        } catch (Exception e) {
            return "Error al agregar vehículo: " + e.getMessage();
        }
    }

    public String getVehiculoInfo(int idx) {
        try {
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD == null) {
                vehiculosBD = new java.util.ArrayList<Vehiculo>();
            }
            java.util.ArrayList<Vehiculo> contenedorGenerico = new java.util.ArrayList<Vehiculo>();
            for (Vehiculo vehiculo : vehiculosBD) {
                if (vehiculo != null && vehiculo.getEstado() == enums.EstadoVehiculo.DISPONIBLE) {
                    contenedorGenerico.add(vehiculo);
                }
            }
            if (contenedorGenerico.isEmpty()) {
                return "SIN_VEHICULOS_DISPONIBLES";
            }
            if (idx < 0 || idx >= contenedorGenerico.size()) return "";
            Vehiculo v = contenedorGenerico.get(idx);
            return idx + ": " + v.getPatente() + " - " + v.getMarca() + " " + v.getModelo() + " - Estado: " + v.getEstado() + " | $/día: $" + v.getPrecioDia() + " | Km: " + v.getKilometraje();
        } catch (Exception e) {
            return "SIN_VEHICULOS_DISPONIBLES";
        }
    }

    public String listarTodosVehiculos() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- TODOS LOS VEHICULOS ---\n");
        try {
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD == null) {
                vehiculosBD = new java.util.ArrayList<Vehiculo>();
            }
            if (vehiculosBD.isEmpty()) {
                sb.append("No hay vehiculos registrados.\n");
                return sb.toString();
            }
            for (int i = 0; i < vehiculosBD.size(); i++) {
                Vehiculo v = vehiculosBD.get(i);
                if (v != null) {
                    sb.append(i).append(": ").append(v.getPatente()).append(" - ")
                            .append(v.getMarca()).append(" ").append(v.getModelo())
                            .append(" - Estado: ").append(v.getEstado())
                            .append(" | $/día: $").append(v.getPrecioDia())
                            .append(" | Km: ").append(v.getKilometraje()).append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("Error al consultar vehiculos: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public String modificarEstadoVehiculo(int indice, String nuevoEstadoTexto) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesion primero.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            return "Solo un ADMINISTRADOR o EMPLEADO puede modificar el estado de un vehiculo.";
        }
        if (nuevoEstadoTexto == null || nuevoEstadoTexto.trim().isEmpty()) {
            return "Estado invalido.";
        }
        EstadoVehiculo nuevoEstado;
        try {
            nuevoEstado = EstadoVehiculo.valueOf(nuevoEstadoTexto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Estado invalido. Use: DISPONIBLE, EN_LIMPIEZA, EN_REPARACION o FUERA_SERVICIO.";
        }
        if (nuevoEstado == EstadoVehiculo.ALQUILADO || nuevoEstado == EstadoVehiculo.RESERVADO) {
            return "ALQUILADO y RESERVADO se gestionan automaticamente con reservas y entregas.";
        }
        try {
            List<Vehiculo> vehiculosBD = gestorBaseDatos.cargarVehiculos();
            if (vehiculosBD == null || vehiculosBD.isEmpty()) {
                return "No hay vehiculos registrados.";
            }
            if (indice < 0 || indice >= vehiculosBD.size()) {
                return "Indice de vehiculo invalido.";
            }
            Vehiculo v = vehiculosBD.get(indice);
            if (v == null) {
                return "Vehiculo invalido.";
            }
            EstadoVehiculo anterior = v.getEstado();
            v.setEstado(nuevoEstado);
            gestorBaseDatos.actualizarVehiculo(v);
            if (repoVehiculos != null) {
                java.util.Optional<Vehiculo> enRepo = repoVehiculos.buscar(v.getPatente());
                if (enRepo.isPresent()) {
                    enRepo.get().setEstado(nuevoEstado);
                }
            }
            return "Estado del vehiculo " + v.getPatente() + " actualizado: " + anterior + " -> " + nuevoEstado;
        } catch (Exception e) {
            return "Error al modificar estado: " + e.getMessage();
        }
    }

    public String crearUsuario(String nombreUsuario, String contrasenia, String rolTexto,
                               String nombre, String apellido, String dni, String telefono, String email,
                               String legajo, String sucursal) {
        if (usuarioLogueado != null
                && usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR
                && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede crear usuarios.");
        }

        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            return "El nombre de usuario no puede estar vacío.";
        }

        if (contrasenia == null || contrasenia.trim().isEmpty()) {
            return "La contraseña no puede estar vacía.";
        }

        if (rolTexto == null || rolTexto.trim().isEmpty()) {
            return "Rol inválido. Roles permitidos: ADMINISTRADOR, EMPLEADO, CLIENTE";
        }

        RolUsuario rolUsuario;
        try {
            rolUsuario = RolUsuario.valueOf(rolTexto.toUpperCase().trim());
        } catch (Exception excepcion) {
            return "Rol inválido. Roles permitidos: ADMINISTRADOR, EMPLEADO, CLIENTE";
        }

        if (usuarioLogueado == null && rolUsuario != RolUsuario.CLIENTE) {
            return "Sin iniciar sesión solo puede registrarse como CLIENTE.";
        }

        if (rolUsuario == RolUsuario.ADMINISTRADOR
                && (usuarioLogueado == null || usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR)) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede crear otro ADMINISTRADOR.");
        }

        String errorDatosPersona = validarDatosPersona(nombre, apellido, dni, telefono, email);
        if (errorDatosPersona != null) {
            return errorDatosPersona;
        }

        if (rolUsuario != RolUsuario.CLIENTE) {
            if (legajo == null || legajo.trim().isEmpty()) {
                return "El legajo no puede estar vacío.";
            }
            if (sucursal == null || sucursal.trim().isEmpty()) {
                return "La sucursal no puede estar vacía.";
            }
        }

        try {
            String username = nombreUsuario.trim();
            if (gestorBaseDatos.cargarUsuarioPorUsername(username) != null) {
                return "Ya existe un usuario con ese nombre de usuario.";
            }

            String idUsuario = UUID.randomUUID().toString();
            if (rolUsuario == RolUsuario.CLIENTE) {
                String dniCliente = dni.trim();
                for (Cliente clienteExistente : clientes) {
                    if (clienteExistente != null && clienteExistente.getDni() != null
                            && clienteExistente.getDni().equalsIgnoreCase(dniCliente)) {
                        return "Ya existe un cliente con ese DNI.";
                    }
                }
                Cliente cliente = new Cliente(idUsuario, nombre.trim(), apellido.trim(), dniCliente,
                        telefono != null ? telefono.trim() : "", email.trim(), LocalDate.now());
                Usuario nuevoUsuario = new Usuario(idUsuario, username, contrasenia, rolUsuario, true, null, cliente);
                gestorBaseDatos.guardarCliente(cliente);
                gestorBaseDatos.guardarUsuario(nuevoUsuario);
                clientes.add(cliente);
                return "Usuario '" + nuevoUsuario.getUsername() + "' creado correctamente con rol CLIENTE" +
                        " | Cliente: " + cliente.getNombreCompleto() + " (DNI " + cliente.getDni() + ").";
            }

            Empleado empleado = new Empleado(idUsuario, nombre.trim(), apellido.trim(), dni.trim(),
                    telefono != null ? telefono.trim() : "", email.trim(), legajo.trim(), LocalDate.now(), sucursal.trim());
            Usuario nuevoUsuario = new Usuario(idUsuario, username, contrasenia, rolUsuario, true, empleado);

            gestorBaseDatos.guardarEmpleado(empleado);
            gestorBaseDatos.guardarUsuario(nuevoUsuario);
            empleados.add(empleado);

            return "Usuario '" + nuevoUsuario.getUsername() + "' creado correctamente con rol " + rolUsuario +
                    " | Empleado: " + empleado.getNombreCompleto() + " (legajo " + empleado.getLegajo() + ").";
        } catch (UsuarioNoAutorizadoException excepcion) {
            throw excepcion;
        } catch (Exception e) {
            return "Error al crear usuario: " + e.getMessage();
        }
    }

    private String validarDatosPersona(String nombre, String apellido, String dni, String telefono, String email) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "El nombre no puede estar vacío.";
        }
        if (apellido == null || apellido.trim().isEmpty()) {
            return "El apellido no puede estar vacío.";
        }
        if (dni == null || dni.trim().isEmpty()) {
            return "El DNI no puede estar vacío.";
        }
        if (email == null || email.trim().isEmpty()) {
            return "El email no puede estar vacío.";
        }
        String emailTrim = email.trim();
        if (!emailTrim.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "Email inválido. Formato esperado: usuario@dominio.com";
        }
        return null;
    }

    public String limpiarDatosBD() {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para realizar esta acción.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede limpiar la base de datos.");
        }
        gestorBaseDatos.limpiarDatosExceptoMariano();
        this.clientes.clear();
        this.reservas.clear();
        this.empleados.clear();
        repoVehiculos = new util.RepositorioGenerico<Vehiculo, String>();
        servicioAlquiler.cargarOcupaciones(new ArrayList<Ocupacion>());
        try {
            Usuario admin = gestorBaseDatos.cargarUsuarioPorUsername(ADMIN_USERNAME);
            if (admin != null) {
                Empleado empleadoAdmin = construirEmpleadoAdmin(admin.getIdUsuario());
                gestorBaseDatos.guardarEmpleado(empleadoAdmin);
                empleados.add(empleadoAdmin);
                if (usuarioLogueado != null && ADMIN_USERNAME.equals(usuarioLogueado.getUsername())) {
                    usuarioLogueado.setEmpleado(empleadoAdmin);
                }
            }
        } catch (Exception ignored) {
        }
        if (usuarioLogueado != null && !ADMIN_USERNAME.equals(usuarioLogueado.getUsername())) {
            usuarioLogueado = null;
        }
        return "Datos limpiados correctamente. Solo se mantiene el usuario mariano.";
    }

    public boolean hayOcupacionesActivas() {
        return !servicioAlquiler.listarAlquiladosActualmente().isEmpty();
    }

    public java.util.ArrayList<Reserva> getReservas() {
        return reservas;
    }

    public java.util.ArrayList<Cliente> getClientes() {
        return clientes;
    }

    public Usuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public void setUsuarioLogueado(Usuario usuarioLogueado) {
        this.usuarioLogueado = usuarioLogueado;
    }
}




