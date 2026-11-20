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

    private final ServicioAlquiler servicioAlquiler;
    private final RepositorioGenerico<Cliente, String> repoClientes = new RepositorioGenerico<Cliente, String>();
    private final RepositorioGenerico<Vehiculo, String> repoVehiculos = new RepositorioGenerico<Vehiculo, String>();
    private final RepositorioGenerico<Reserva, String> repoReservas = new RepositorioGenerico<Reserva, String>();
    private final RepositorioGenerico<Empleado, String> repoEmpleados = new RepositorioGenerico<Empleado, String>();
    private final RepositorioGenerico<Ocupacion, String> repoOcupaciones = new RepositorioGenerico<Ocupacion, String>();
    private final RepositorioGenerico<Usuario, String> repoUsuarios = new RepositorioGenerico<Usuario, String>();
    private Usuario usuarioLogueado = null;
    private final GestorBaseDatos gestorBaseDatos = new GestorBaseDatos();

    public SistemaAlquiler() {
        this.servicioAlquiler = new ServicioAlquilerImpl(repoOcupaciones, repoUsuarios);
        inicializarBD();
        crearUsuarioAdministradorPorDefecto();
        cargarDatosDesdeBD();
    }

    private void cargarDatosDesdeBD() {
        java.util.ArrayList<Cliente> clientesBD = gestorBaseDatos.leerClientes();
        java.util.ArrayList<Vehiculo> vehiculosBD = gestorBaseDatos.leerVehiculos();
        java.util.ArrayList<Empleado> empleadosBD = gestorBaseDatos.leerEmpleados();
        java.util.ArrayList<Reserva> reservasBD = gestorBaseDatos.leerReservas(clientesBD, vehiculosBD);
        java.util.ArrayList<Ocupacion> ocupacionesBD = gestorBaseDatos.cargarOcupaciones(clientesBD, vehiculosBD, empleadosBD, reservasBD);
        List<Usuario> usuariosBD;
        try {
            usuariosBD = gestorBaseDatos.leerUsuarios();
        } catch (Exception e) {
            usuariosBD = new ArrayList<Usuario>();
        }

        repoClientes.limpiar();
        for (Cliente cliente : clientesBD) {
            repoClientes.guardar(cliente.getId(), cliente);
        }

        repoVehiculos.limpiar();
        for (Vehiculo vehiculo : vehiculosBD) {
            repoVehiculos.guardar(vehiculo.getPatente(), vehiculo);
        }

        repoEmpleados.limpiar();
        for (Empleado empleado : empleadosBD) {
            repoEmpleados.guardar(empleado.getId(), empleado);
        }

        repoReservas.limpiar();
        for (Reserva reserva : reservasBD) {
            repoReservas.guardar(reserva.getIdReserva(), reserva);
        }

        repoOcupaciones.limpiar();
        for (Ocupacion ocupacion : ocupacionesBD) {
            repoOcupaciones.guardar(ocupacion.getIdOcupacion(), ocupacion);
        }

        repoUsuarios.limpiar();
        if (usuariosBD != null) {
            for (Usuario usuario : usuariosBD) {
                repoUsuarios.guardar(usuario.getIdUsuario(), usuario);
            }
        }

        servicioAlquiler.cargarOcupaciones(new ArrayList<Ocupacion>(repoOcupaciones.listar()));
        reconstruirHistorialOcupaciones(repoOcupaciones.listar());
    }

    private void reconstruirHistorialOcupaciones(List<Ocupacion> ocupaciones) {
        for (Cliente cliente : repoClientes.listar()) {
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
            Usuario existente = gestorBaseDatos.leerUsuarioPorUsername(ADMIN_USERNAME);
            if (existente == null) {
                String idAdmin = UUID.randomUUID().toString();
                Empleado admin = construirEmpleadoAdmin(idAdmin);
                repoEmpleados.guardar(admin.getId(), admin);
                gestorBaseDatos.guardarEmpleado(admin);
                Usuario mariano = new Usuario(idAdmin, ADMIN_USERNAME, ADMIN_PASSWORD, RolUsuario.ADMINISTRADOR, true, admin);
                repoUsuarios.guardar(mariano.getIdUsuario(), mariano);
                gestorBaseDatos.guardarUsuario(mariano);
            } else {
                if (!existente.isActivo()) {
                    existente.setActivo(true);
                    gestorBaseDatos.actualizarActivoUsuario(existente.getIdUsuario(), true);
                }
                Empleado admin = gestorBaseDatos.leerEmpleadoPorId(existente.getIdUsuario());
                if (admin == null || datosEmpleadoAdminPorDefecto(admin)) {
                    admin = construirEmpleadoAdmin(existente.getIdUsuario());
                    repoEmpleados.guardar(admin.getId(), admin);
                    gestorBaseDatos.guardarEmpleado(admin);
                }
            }
        } catch (Exception e) {
            // Ignorar
        }
    }

    private Empleado construirEmpleadoAdmin(String id) {
        return new Empleado(id, ADMIN_NOMBRE, ADMIN_APELLIDO, ADMIN_DNI, ADMIN_TELEFONO, ADMIN_EMAIL,
                ADMIN_LEGAJO, ADMIN_SUCURSAL);
    }

    private boolean datosEmpleadoAdminPorDefecto(Empleado empleado) {
        return LEGACY_APELLIDO_PLACEHOLDER.equals(empleado.getApellido()) || LEGACY_DNI_PLACEHOLDER.equals(empleado.getDni());
    }

    public String login(String user, String pass) {
        try {
            Usuario u = null;
            for (Usuario usuario : repoUsuarios.listar()) {
                if (usuario != null && usuario.getUsername() != null && usuario.getUsername().equals(user)) {
                    u = usuario;
                    break;
                }
            }
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
                    u.setEmpleado(repoEmpleados.buscar(u.getIdUsuario()).orElse(null));
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
        if (idCliente == null) {
            return null;
        }
        Cliente cliente = repoClientes.buscar(idCliente).orElse(null);
        return cliente != null && cliente.isActivo() ? cliente : null;
    }

    private List<Cliente> clientesActivos() {
        List<Cliente> activos = new ArrayList<Cliente>();
        for (Cliente cliente : repoClientes.listar()) {
            if (cliente != null && cliente.isActivo()) {
                activos.add(cliente);
            }
        }
        return activos;
    }

    private List<Vehiculo> vehiculosActivos() {
        List<Vehiculo> activos = new ArrayList<Vehiculo>();
        for (Vehiculo vehiculo : repoVehiculos.listar()) {
            if (vehiculo != null && vehiculo.isActivo()) {
                activos.add(vehiculo);
            }
        }
        return activos;
    }

    private List<Reserva> reservasActivas() {
        List<Reserva> activas = new ArrayList<Reserva>();
        for (Reserva reserva : repoReservas.listar()) {
            if (reserva != null
                    && reserva.getCliente() != null && reserva.getCliente().isActivo()
                    && reserva.getVehiculo() != null && reserva.getVehiculo().isActivo()) {
                activas.add(reserva);
            }
        }
        return activas;
    }

    public String listarDisponibles() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- AUTOMOVILES DISPONIBLES ---\n");
        try {
            boolean hay = false;
            for (Vehiculo v : vehiculosActivos()) {
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
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para listar ocupaciones.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede listar ocupaciones.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- OCUPACIONES ACTIVAS ---\n");
        try {
            ArrayList<Ocupacion> activas = new ArrayList<Ocupacion>();
            for (Ocupacion ocupacion : repoOcupaciones.listar()) {
                if (ocupacion != null && ocupacion.estaActiva()
                        && ocupacion.getCliente() != null && ocupacion.getCliente().isActivo()
                        && ocupacion.getVehiculo() != null && ocupacion.getVehiculo().isActivo()) {
                    activas.add(ocupacion);
                }
            }
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
            boolean hay = false;
            for (Vehiculo v : vehiculosActivos()) {
                if (v != null && v.getEstado() != enums.EstadoVehiculo.DISPONIBLE) {
                    hay = true;
                    String motivo = v.getEstado() == enums.EstadoVehiculo.ALQUILADO ? "ALQUILADO - En poder del cliente" :
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
        try {
            validarDatosPersona(nombre, apellido, dni, telefono, email);
            String dniNormalizado = dni.trim();
            for (Cliente existente : repoClientes.listar()) {
                if (existente != null && existente.getDni() != null
                        && existente.getDni().equalsIgnoreCase(dniNormalizado)) {
                    throw new DatoInvalidoException("Ya existe un cliente con ese DNI.");
                }
            }

            String username = generarUsernameCliente(nombre, apellido);
            for (Usuario usuarioExistente : repoUsuarios.listar()) {
                if (usuarioExistente != null && usuarioExistente.getUsername() != null
                        && usuarioExistente.getUsername().equalsIgnoreCase(username)) {
                    throw new DatoInvalidoException("Ya existe un usuario con ese nombre de usuario.");
                }
            }

            Cliente nuevoCliente = new Cliente(UUID.randomUUID().toString(), nombre.trim(), apellido.trim(), dniNormalizado,
                    telefono != null ? telefono.trim() : "", email.trim(), java.time.LocalDate.now());
            Usuario nuevoUsuario = new Usuario(nuevoCliente.getId(), username, dniNormalizado,
                    RolUsuario.CLIENTE, true, null, nuevoCliente);
            repoClientes.guardar(nuevoCliente.getId(), nuevoCliente);
            repoUsuarios.guardar(nuevoUsuario.getIdUsuario(), nuevoUsuario);
            gestorBaseDatos.guardarCliente(nuevoCliente);
            gestorBaseDatos.guardarUsuario(nuevoUsuario);
            return "Cliente " + nuevoCliente.getNombreCompleto() + " creado correctamente. Usuario: "
                    + username + " | Contraseña: " + dniNormalizado;
        } catch (DatoInvalidoException e) {
            return e.getMessage();
        } catch (UsuarioNoAutorizadoException e) {
            throw e;
        } catch (Exception e) {
            return "Error al crear cliente: " + e.getMessage();
        }
    }
    private String generarUsernameCliente(String nombre, String apellido) {
        String base = (nombre.trim() + "_" + apellido.trim()).toLowerCase(java.util.Locale.ROOT);
        return base.replaceAll("\\s+", "_").replaceAll("_+", "_");
    }

    public String crearReserva(int idxCliente, int idxVehiculo, LocalDate inicio, LocalDate fin) {
        try {
            validarDato(usuarioLogueado == null, "Debe iniciar sesión para crear reservas.");
            validarDato(usuarioLogueado.getRol() == RolUsuario.CLIENTE,
                    "Los usuarios CLIENTE solo pueden reservar con su propia ficha.");
            validarDato(clientesActivos().isEmpty(), "No hay clientes registrados. Cree un cliente primero.");
            List<Cliente> clientesActuales = clientesActivos();
            validarDato(idxCliente < 0 || idxCliente >= clientesActuales.size(), "Índices inválidos.");
            return crearReservaParaCliente(clientesActuales.get(idxCliente), idxVehiculo, inicio, fin);
        } catch (DatoInvalidoException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Error al crear reserva: " + e.getMessage();
        }
    }

    public boolean esCliente() {
        return usuarioLogueado != null && usuarioLogueado.getRol() == RolUsuario.CLIENTE;
    }

    public String reservarComoCliente(int idxVehiculo, LocalDate inicio, LocalDate fin) {
        try {
            validarDato(usuarioLogueado == null, "Debe iniciar sesión para crear reservas.");
            validarDato(!esCliente(), "Esta opción es exclusiva para usuarios CLIENTE.");
            validarDato(usuarioLogueado.getCliente() == null, "El usuario no tiene una ficha de cliente asociada.");
            validarDato(inicio == null || fin == null, "Debe indicar fecha de inicio y fecha de fin.");
            return crearReservaParaCliente(usuarioLogueado.getCliente(), idxVehiculo, inicio, fin);
        } catch (DatoInvalidoException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Error al crear reserva: " + e.getMessage();
        }
    }

    public String crearReservaCliente(int idxVehiculo, LocalDate inicio, LocalDate fin) {
        return reservarComoCliente(idxVehiculo, inicio, fin);
    }

    private String crearReservaParaCliente(Cliente cliente, int idxVehiculo, LocalDate inicio, LocalDate fin) {
        try {
            validarDato(cliente == null, "Cliente inválido.");
            validarDato(!cliente.isActivo(), "El cliente está inactivo.");
            validarDato(inicio == null || fin == null, "Debe indicar fecha de inicio y fecha de fin.");

            List<Vehiculo> vehiculosDisponibles = new ArrayList<Vehiculo>();
            for (Vehiculo vehiculoDisponible : vehiculosActivos()) {
                if (vehiculoDisponible != null && vehiculoDisponible.getEstado() == enums.EstadoVehiculo.DISPONIBLE) {
                    vehiculosDisponibles.add(vehiculoDisponible);
                }
            }
            validarDato(vehiculosDisponibles.isEmpty(), "No hay automóviles disponibles para reservar.");
            validarDato(idxVehiculo < 0 || idxVehiculo >= vehiculosDisponibles.size(), "Índices inválidos.");

            List<Reserva> reservasActuales = reservasActivas();
            for (Reserva r : reservasActuales) {
                if (r != null && r.getCliente() != null && r.getCliente().getId().equals(cliente.getId())
                        && r.getEstado() != enums.EstadoReserva.CANCELADA
                        && r.getEstado() != enums.EstadoReserva.CONCRETADA) {
                    if (r.getFechaInicio() != null && r.getFechaFin() != null) {
                        validarDato(!(fin.isBefore(r.getFechaInicio()) || inicio.isAfter(r.getFechaFin())),
                                "El cliente ya tiene una reserva que se solapa con las fechas.");
                    }
                }
            }

            Vehiculo vehiculoSeleccionado = vehiculosDisponibles.get(idxVehiculo);
            for (Reserva r : reservasActuales) {
                if (r != null && r.getVehiculo() != null
                        && r.getVehiculo().getPatente().equals(vehiculoSeleccionado.getPatente())
                        && r.getEstado() != enums.EstadoReserva.CANCELADA
                        && r.getEstado() != enums.EstadoReserva.CONCRETADA) {
                    if (r.getFechaInicio() != null && r.getFechaFin() != null) {
                        validarDato(!(fin.isBefore(r.getFechaInicio()) || inicio.isAfter(r.getFechaFin())),
                                "El vehículo ya tiene una reserva que se solapa con las fechas.");
                    }
                }
            }

            for (Ocupacion o : repoOcupaciones.listar()) {
                if (o != null && o.getCliente() != null && o.getCliente().getId().equals(cliente.getId())
                        && o.estaActiva()) {
                    throw new DatoInvalidoException("El cliente ya tiene un automóvil alquilado al mismo tiempo.");
                }
            }

            Reserva reserva = new Reserva(UUID.randomUUID().toString(), cliente, vehiculoSeleccionado, inicio, fin,
                    LocalDateTime.now(), EstadoReserva.PENDIENTE,
                    usuarioLogueado != null ? usuarioLogueado.getUsername() : "");
            if (!reserva.validar()) {
                throw new DatoInvalidoException("Fechas inválidas.");
            }
            repoReservas.guardar(reserva.getIdReserva(), reserva);
            gestorBaseDatos.guardarReserva(reserva);
            return "Reserva creada correctamente. ID: " + reserva.getIdReserva().substring(0, 8) +
                    "\nCliente: " + cliente.getNombreCompleto() + " | DNI: " + cliente.getDni() +
                    "\nMonto total estimado: $" + reserva.calcularMontoTotal();
        } catch (DatoInvalidoException e) {
            throw e;
        } catch (Exception e) {
            return "Error al crear reserva: " + e.getMessage();
        }
    }
    public String listarClientes() {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para listar clientes.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Un usuario CLIENTE no puede listar todos los clientes.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- LISTADO DE CLIENTES ---\n");
        List<Cliente> clientesActuales = clientesActivos();
        if (clientesActuales.isEmpty()) {
            sb.append("No hay clientes registrados.\n");
            return sb.toString();
        }
        for (int i = 0; i < clientesActuales.size(); i++) {
            Cliente c = clientesActuales.get(i);
            if (c != null) {
                sb.append(i).append(": ").append(c.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public String listarReservas() {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para listar reservas.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Un usuario CLIENTE no puede listar todas las reservas.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- LISTADO DE RESERVAS ---\n");
        List<Reserva> reservasActuales = reservasActivas();
        if (reservasActuales.isEmpty()) {
            sb.append("No hay reservas.\n");
            return sb.toString();
        }
        for (int i = 0; i < reservasActuales.size(); i++) {
            Reserva r = reservasActuales.get(i);
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
        if (usuarioLogueado == null) {
            return lista;
        }
        for (Reserva reserva : reservasActivas()) {
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
        for (Ocupacion ocupacion : repoOcupaciones.listar()) {
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

    public String desactivarCliente(int indice) {
        if (usuarioLogueado == null) return "Debe iniciar sesión primero";
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede desactivar clientes.");
        }
        List<Cliente> clientesActuales = clientesActivos();
        if (indice < 0 || indice >= clientesActuales.size()) return "Cliente inválido";
        Cliente c = clientesActuales.get(indice);
        if (c == null) return "Cliente inválido";
        for (Ocupacion o : repoOcupaciones.listar()) {
            if (o != null && o.getCliente() != null && o.getCliente().getId().equals(c.getId())
                    && o.estaActiva()) {
                return "No se puede desactivar el cliente: tiene una ocupación activa (vehículo "
                        + (o.getVehiculo() != null ? o.getVehiculo().getPatente() : "sin patente") + ").";
            }
        }
        int reservasCanceladas = 0;
        for (Reserva r : repoReservas.listar()) {
            if (r != null && r.getCliente() != null && r.getCliente().getId().equals(c.getId())
                    && r.getEstado() != EstadoReserva.CANCELADA && r.getEstado() != EstadoReserva.CONCRETADA) {
                r.cancelar();
                gestorBaseDatos.actualizarEstadoReserva(r.getIdReserva(), EstadoReserva.CANCELADA.name());
                reservasCanceladas++;
            }
        }
        c.setActivo(false);
        gestorBaseDatos.actualizarActivoCliente(c.getId(), false);
        Usuario usuarioAsociado = repoUsuarios.buscar(c.getId()).orElse(null);
        if (usuarioAsociado != null && usuarioAsociado.getRol() == RolUsuario.CLIENTE && usuarioAsociado.isActivo()) {
            usuarioAsociado.setActivo(false);
            gestorBaseDatos.actualizarActivoUsuario(usuarioAsociado.getIdUsuario(), false);
        }
        return "Cliente desactivado correctamente. Se cancelaron " + reservasCanceladas + " reserva(s).";
    }

    public String desactivarVehiculo(int indice) {
        if (usuarioLogueado == null) return "Debe iniciar sesión primero";
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede desactivar vehículos.");
        }
        List<Vehiculo> listaVehiculos = vehiculosActivos();
        if (indice < 0 || listaVehiculos.isEmpty() || indice >= listaVehiculos.size()) return "Vehículo inválido";
        Vehiculo v = listaVehiculos.get(indice);
        if (v == null) return "Vehículo inválido";
        for (Ocupacion o : repoOcupaciones.listar()) {
            if (o != null && o.getVehiculo() != null && o.getVehiculo().getPatente().equals(v.getPatente())
                    && o.estaActiva()) {
                return "No se puede desactivar el vehículo: tiene una ocupación activa.";
            }
        }
        int reservasCanceladas = 0;
        for (Reserva r : repoReservas.listar()) {
            if (r != null && r.getVehiculo() != null && r.getVehiculo().getPatente().equals(v.getPatente())
                    && r.getEstado() != EstadoReserva.CANCELADA && r.getEstado() != EstadoReserva.CONCRETADA) {
                r.cancelar();
                gestorBaseDatos.actualizarEstadoReserva(r.getIdReserva(), EstadoReserva.CANCELADA.name());
                reservasCanceladas++;
            }
        }
        v.setActivo(false);
        gestorBaseDatos.actualizarActivoVehiculo(v.getPatente(), false);
        return "Vehículo desactivado correctamente. Se cancelaron " + reservasCanceladas + " reserva(s).";
    }

    private void recargarOcupacionesDesdeBD() {
        try {
            java.util.ArrayList<Vehiculo> vehiculosBD = new java.util.ArrayList<Vehiculo>(repoVehiculos.listar());
            java.util.ArrayList<Cliente> clientesBD = new java.util.ArrayList<Cliente>(repoClientes.listar());
            java.util.ArrayList<Empleado> empleadosBD = new java.util.ArrayList<Empleado>(repoEmpleados.listar());
            java.util.ArrayList<Reserva> reservasBD = new java.util.ArrayList<Reserva>(repoReservas.listar());
            java.util.ArrayList<Ocupacion> ocupacionesBD = new java.util.ArrayList<Ocupacion>(
                    gestorBaseDatos.cargarOcupaciones(clientesBD, vehiculosBD, empleadosBD, reservasBD));
            repoOcupaciones.limpiar();
            for (Ocupacion ocupacion : ocupacionesBD) {
                repoOcupaciones.guardar(ocupacion.getIdOcupacion(), ocupacion);
            }
            servicioAlquiler.cargarOcupaciones(ocupacionesBD);
            reconstruirHistorialOcupaciones(repoOcupaciones.listar());
        } catch (Exception ignored) {
        }
    }

    public String listarUsuarios() {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para listar usuarios.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede listar usuarios.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- USUARIOS ---\n");
        try {
            List<Usuario> listaUsuarios = repoUsuarios.listar();
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

    public String desactivarUsuarioPorIndice(int indice) {
        try {
            List<Usuario> listaUsuarios = repoUsuarios.listar();
            if (listaUsuarios == null || listaUsuarios.isEmpty()) return "No hay usuarios registrados.";
            if (indice < 0 || indice >= listaUsuarios.size()) return "Indice de usuario invalido.";
            return desactivarUsuario(listaUsuarios.get(indice).getUsername());
        } catch (UsuarioNoAutorizadoException ex) {
            throw ex;
        } catch (Exception e) {
            return "Error al desactivar usuario: " + e.getMessage();
        }
    }

    public String desactivarUsuario(String nombreUsuario) {
        if (usuarioLogueado == null) return "Debe iniciar sesión primero";
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede desactivar usuarios.");
        }
        if (nombreUsuario == null || nombreUsuario.isEmpty()) return "Nombre de usuario inválido";
        if (ADMIN_USERNAME.equals(nombreUsuario)) return "No se puede desactivar el usuario por defecto";
        if (usuarioLogueado.getUsername() != null && usuarioLogueado.getUsername().equals(nombreUsuario)) {
            return "No puede desactivar el usuario con el que está logueado.";
        }
        try {
            Usuario u = null;
            for (Usuario usuario : repoUsuarios.listar()) {
                if (usuario != null && nombreUsuario.equals(usuario.getUsername())) {
                    u = usuario;
                    break;
                }
            }
            if (u == null) return "Usuario no encontrado";
            if (!u.isActivo()) return "El usuario ya está inactivo.";
            u.setActivo(false);
            gestorBaseDatos.actualizarActivoUsuario(u.getIdUsuario(), false);
            return "Usuario desactivado correctamente.";
        } catch (Exception e) {
            return "Error al desactivar usuario: " + e.getMessage();
        }
    }

    private ArrayList<Reserva> reservasPendientes() {
        ArrayList<Reserva> pendientes = new ArrayList<Reserva>();
        for (Reserva reserva : reservasActivas()) {
            if (reserva != null && reserva.getEstado() == EstadoReserva.PENDIENTE) {
                pendientes.add(reserva);
            }
        }
        return pendientes;
    }

    public String getReservaInfo(int idx) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para consultar reservas.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede consultar reservas para entrega.");
        }
        List<Reserva> reservasActuales = reservasPendientes();
        if (reservasActuales.isEmpty()) return "No hay reservas pendientes";
        if (idx < 0 || idx >= reservasActuales.size()) return "";
        Reserva r = reservasActuales.get(idx);
        if (r == null) return "";
        String estado = r.getEstado() != null ? r.getEstado().name() : "";
        return idx + ": " + r.getIdReserva().substring(0, 8) + " | Cliente: " + (r.getCliente() != null ? r.getCliente().getNombreCompleto() : "") + " | Vehículo: " + (r.getVehiculo() != null ? r.getVehiculo().getPatente() : "") + " | Estado: " + estado;
    }

    public String realizarEntregaPorIndice(int indiceReserva) {
        return realizarEntregaPorIndice(indiceReserva, "");
    }

    public String realizarEntregaPorIndice(int indiceReserva, String observaciones) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para realizar entregas.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO pueden realizar entregas.");
        }
        List<Reserva> reservasActuales = reservasPendientes();
        if (reservasActuales.isEmpty()) {
            return "No hay reservas pendientes.";
        }
        if (indiceReserva < 0 || indiceReserva >= reservasActuales.size()) {
            return "Índice de reserva inválido.";
        }
        try {
            Reserva reserva = reservasActuales.get(indiceReserva);
            if (reserva.getEstado() == EstadoReserva.CONCRETADA || reserva.getEstado() == EstadoReserva.CANCELADA) {
                return "La reserva ya está " + reserva.getEstado() + " y no puede entregarse.";
            }
            Empleado emp = usuarioLogueado.getEmpleado();
            if (emp == null) {
                return "El usuario '" + usuarioLogueado.getUsername() + "' no tiene un legajo de empleado asociado.";
            }
            reserva.confirmar();
            Ocupacion ocupacion = servicioAlquiler.realizarEntrega(reserva, emp, observaciones);
            if (reserva.getVehiculo() != null) {
                gestorBaseDatos.actualizarVehiculo(reserva.getVehiculo());
            }
            repoOcupaciones.guardar(ocupacion.getIdOcupacion(), ocupacion);
            gestorBaseDatos.guardarOcupacion(ocupacion);
            if (ocupacion.getEntrega() != null) {
                gestorBaseDatos.guardarEntrega(ocupacion.getEntrega());
            }
            repoReservas.eliminar(reserva.getIdReserva());
            GestorBaseDatos.eliminarReserva(reserva.getIdReserva());
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


    public String realizarDevolucionPorIndice(int indiceOcupacion, int kilometrosRecorridos, String danios) {
        return realizarDevolucionPorIndiceInterno(indiceOcupacion, kilometrosRecorridos, danios, "");
    }

    public String realizarDevolucionPorIndice(int indiceOcupacion, int kilometrosRecorridos, String danios, String observaciones) {
        return realizarDevolucionPorIndiceInterno(indiceOcupacion, kilometrosRecorridos, danios, observaciones);
    }

    public String realizarDevolucionPorIndice(int indiceOcupacion, int kilometrosRecorridos, String danios, double cargosExtraIgnorado) {
        return realizarDevolucionPorIndiceInterno(indiceOcupacion, kilometrosRecorridos, danios, "");
    }

    private String realizarDevolucionPorIndiceInterno(int indiceOcupacion, int kilometrosRecorridos, String danios,
                                                      String observaciones) {
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para realizar devoluciones.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO pueden realizar devoluciones.");
        }
        if (kilometrosRecorridos < 0) {
            return "Los kilómetros recorridos no pueden ser negativos.";
        }
        ArrayList<Ocupacion> activas = new ArrayList<Ocupacion>();
        for (Ocupacion ocupacion : repoOcupaciones.listar()) {
            if (ocupacion != null && ocupacion.estaActiva()) {
                activas.add(ocupacion);
            }
        }
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
            double cargosExtra = ocupacion.getVehiculo() != null ? ocupacion.getVehiculo().getTarifaSeguro() : 0.0;
            Devolucion dev = servicioAlquiler.realizarDevolucion(ocupacion, emp, kilometrajeEntradaCalculado, danios, cargosExtra);
            dev.setObservaciones(observaciones);
            if (ocupacion.getVehiculo() != null) {
                ocupacion.getVehiculo().setKilometraje(kilometrajeEntradaCalculado);
                gestorBaseDatos.actualizarVehiculo(ocupacion.getVehiculo());
            }
            repoOcupaciones.guardar(ocupacion.getIdOcupacion(), ocupacion);
            gestorBaseDatos.guardarDevolucion(dev);
            if (dev.getOcupacion() != null) {
                gestorBaseDatos.guardarOcupacion(dev.getOcupacion());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Devolución realizada con éxito!\n");
            sb.append("Kilometraje anterior: ").append(ocupacion.getVehiculo() != null ? (ocupacion.getVehiculo().getKilometraje() - kilometrosRecorridos) : 0).append("\n");
            sb.append("Kilometraje actual: ").append(kilometrajeEntradaCalculado).append("\n");
            sb.append("Cargos extra: $").append(dev.getCargosExtra()).append("\n");
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
        if (usuarioLogueado == null) {
            return "Debe iniciar sesión para ver el historial de clientes.";
        }
        if (usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR && usuarioLogueado.getRol() != RolUsuario.EMPLEADO) {
            throw new UsuarioNoAutorizadoException("Un usuario CLIENTE no puede ver el historial de todos los clientes.");
        }
        List<Cliente> clientesActuales = clientesActivos();
        if (clientesActuales.isEmpty()) {
            return "No hay clientes.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- HISTORIAL DE CLIENTES ---\n");
        java.util.HashSet<String> clientesConHistorial = new java.util.HashSet<String>();
        java.util.ArrayList<Reserva> reservasActuales = new java.util.ArrayList<Reserva>(reservasActivas());
        for (Reserva r : reservasActuales) {
            if (r != null && r.getCliente() != null) {
                clientesConHistorial.add(r.getCliente().getId());
            }
        }
        for (Cliente cli : clientesActuales) {
            if (cli != null && !cli.getHistorialOcupaciones().isEmpty()) {
                clientesConHistorial.add(cli.getId());
            }
        }
        boolean hay = false;
        for (Cliente cli : clientesActuales) {
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
                        String observaciones = o.getDevolucion() != null ? o.getDevolucion().getObservaciones() : null;
                        if (observaciones == null || observaciones.trim().isEmpty()) {
                            observaciones = o.getEntrega() != null ? o.getEntrega().getObservaciones() : null;
                        }
                        if (o.getDevolucion() != null || (observaciones != null && !observaciones.trim().isEmpty())) {
                            sb.append("  OBSERVACIONES: ")
                                    .append(observaciones != null && !observaciones.trim().isEmpty()
                                            ? observaciones.trim() : "sin observaciones")
                                    .append("\n");
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
            List<String> lineas = repoUsuarios.mapear(Usuario::toString);
            if (!lineas.isEmpty()) {
                sb.append(String.join("\n", lineas)).append("\n");
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
            List<Vehiculo> flota = vehiculosActivos();
            for (Vehiculo v : flota) {
                if (v != null) {
                    sb.append(v.getClass().getSimpleName()).append(": ").append(v.getPatente()).append(" - ")
                            .append(v.getMarca()).append(" ").append(v.getModelo())
                            .append(" | Estado: ").append(v.getEstado())
                            .append(" | $/día: $").append(v.getPrecioDia())
                            .append(" | Km: ").append(v.getKilometraje()).append("\n");
                }
            }
            if (flota.isEmpty()) {
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
        try {
            return crearAutomovilValidado(patente, marca, modelo, anio, kilometraje, estadoTexto,
                    precioDia, tarifaSeguro, cantidadPuertas, transmision);
        } catch (DatoInvalidoException e) {
            return e.getMessage();
        } catch (UsuarioNoAutorizadoException e) {
            throw e;
        } catch (Exception e) {
            return "Error al agregar vehículo: " + e.getMessage();
        }
    }

    private String crearAutomovilValidado(String patente, String marca, String modelo, int anio, int kilometraje,
                                          String estadoTexto, double precioDia, double tarifaSeguro,
                                          int cantidadPuertas, String transmision) throws Exception {
        validarDato(patente == null || patente.trim().isEmpty(), "La patente no puede estar vacía.");
        patente = patente.trim();
        validarDato(marca == null || marca.trim().isEmpty(), "La marca no puede estar vacía.");
        validarDato(marca.matches(".*\\d.*"), "La marca no puede contener números.");
        validarDato(modelo == null || modelo.trim().isEmpty(), "El modelo no puede estar vacío.");
        int anioActual = java.time.Year.now().getValue();
        validarDato(anio > anioActual, "El año de fabricación no puede superar el año actual.");
        validarDato(anio < 1900, "El año de fabricación no es válido.");
        validarDato(kilometraje < 0, "El kilometraje no puede ser negativo.");
        validarDato(precioDia <= 0, "El precio por día debe ser mayor a 0.");
        validarDato(tarifaSeguro < 0, "La tarifa de seguro no puede ser negativa.");
        validarDato(cantidadPuertas <= 0, "La cantidad de puertas debe ser mayor a 0.");
        validarDato(cantidadPuertas > 5, "La cantidad de puertas no puede ser mayor a 5.");
        validarDato(transmision == null || transmision.trim().isEmpty(), "La transmisión no puede estar vacía.");
        validarDato(estadoTexto == null || estadoTexto.trim().isEmpty(),
                "Estado inválido. Use: DISPONIBLE, EN_LIMPIEZA, EN_REPARACION o FUERA_SERVICIO.");

        String transmisionNormalizada = transmision.trim().toUpperCase();
        validarDato(!transmisionNormalizada.equals(Automovil.TRANSMISION_MANUAL)
                        && !transmisionNormalizada.equals(Automovil.TRANSMISION_AUTOMATICA),
                "Transmisión inválida. Use MANUAL o AUTOMATICA.");

        for (Vehiculo existente : repoVehiculos.listar()) {
            if (existente != null && existente.getPatente() != null
                    && existente.getPatente().equalsIgnoreCase(patente)) {
                throw new DatoInvalidoException("Ya existe un vehículo con esa patente.");
            }
        }

        EstadoVehiculo estadoVehiculo;
        try {
            estadoVehiculo = EstadoVehiculo.valueOf(estadoTexto.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("Estado inválido. Use: DISPONIBLE, EN_LIMPIEZA, EN_REPARACION o FUERA_SERVICIO.");
        }
        validarDato(estadoVehiculo == EstadoVehiculo.ALQUILADO,
                "No se puede crear un vehículo en estado ALQUILADO.");

        Vehiculo nuevoVehiculo = new Automovil(patente, marca, modelo, anio, kilometraje, estadoVehiculo,
                precioDia, tarifaSeguro, cantidadPuertas, transmisionNormalizada);
        repoVehiculos.guardar(nuevoVehiculo.getPatente(), nuevoVehiculo);
        gestorBaseDatos.guardarVehiculo(nuevoVehiculo);
        return "Automóvil " + patente + " agregado correctamente a la base de datos.";
    }

    private void validarDato(boolean condicion, String mensaje) {
        if (condicion) {
            throw new DatoInvalidoException(mensaje);
        }
    }
    public String getVehiculoInfo(int idx) {
        try {
            java.util.ArrayList<Vehiculo> contenedorGenerico = new java.util.ArrayList<Vehiculo>();
            for (Vehiculo vehiculo : vehiculosActivos()) {
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
            List<Vehiculo> vehiculos = vehiculosActivos();
            if (vehiculos.isEmpty()) {
                sb.append("No hay vehiculos registrados.\n");
                return sb.toString();
            }
            for (int i = 0; i < vehiculos.size(); i++) {
                Vehiculo v = vehiculos.get(i);
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
            throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR o EMPLEADO puede modificar el estado de un vehiculo.");
        }
        if (nuevoEstadoTexto == null || nuevoEstadoTexto.trim().isEmpty()) {
            return "Estado inválido.";
        }
        EstadoVehiculo nuevoEstado;
        try {
            nuevoEstado = EstadoVehiculo.valueOf(nuevoEstadoTexto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Estado inválido. Use: DISPONIBLE, EN_LIMPIEZA, EN_REPARACION o FUERA_SERVICIO.";
        }
        if (nuevoEstado == EstadoVehiculo.ALQUILADO) {
            return "ALQUILADO se gestiona automaticamente con las entregas.";
        }
        try {
            List<Vehiculo> vehiculos = vehiculosActivos();
            if (vehiculos.isEmpty()) {
                return "No hay vehiculos registrados.";
            }
            if (indice < 0 || indice >= vehiculos.size()) {
                return "Indice de vehiculo invalido.";
            }
            Vehiculo v = vehiculos.get(indice);
            if (v == null) {
                return "Vehiculo invalido.";
            }
            for (Ocupacion ocupacion : repoOcupaciones.listar()) {
                if (ocupacion != null && ocupacion.estaActiva() && ocupacion.getVehiculo() != null
                        && v.getPatente() != null
                        && v.getPatente().equals(ocupacion.getVehiculo().getPatente())) {
                    return "No se puede modificar el estado: el vehiculo tiene una ocupacion activa.";
                }
            }
            EstadoVehiculo anterior = v.getEstado();
            v.setEstado(nuevoEstado);
            gestorBaseDatos.actualizarVehiculo(v);
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
        try {
            validarDato(nombreUsuario == null || nombreUsuario.trim().isEmpty(),
                    "El nombre de usuario no puede estar vacío.");
            validarDato(contrasenia == null || contrasenia.trim().isEmpty(),
                    "La contraseña no puede estar vacía.");
            validarDato(rolTexto == null || rolTexto.trim().isEmpty(),
                    "Rol inválido. Roles permitidos: ADMINISTRADOR, EMPLEADO, CLIENTE");

            RolUsuario rolUsuario;
            try {
                rolUsuario = RolUsuario.valueOf(rolTexto.toUpperCase().trim());
            } catch (Exception excepcion) {
                throw new DatoInvalidoException("Rol inválido. Roles permitidos: ADMINISTRADOR, EMPLEADO, CLIENTE");
            }

            validarDato(usuarioLogueado == null && rolUsuario != RolUsuario.CLIENTE,
                    "Sin iniciar sesión solo puede registrarse como CLIENTE.");
            if (rolUsuario == RolUsuario.ADMINISTRADOR
                    && (usuarioLogueado == null || usuarioLogueado.getRol() != RolUsuario.ADMINISTRADOR)) {
                throw new UsuarioNoAutorizadoException("Solo un ADMINISTRADOR puede crear otro ADMINISTRADOR.");
            }

            validarDatosPersona(nombre, apellido, dni, telefono, email);
            if (rolUsuario != RolUsuario.CLIENTE) {
                validarDato(legajo == null || legajo.trim().isEmpty(), "El legajo no puede estar vacío.");
                validarDato(sucursal == null || sucursal.trim().isEmpty(), "La sucursal no puede estar vacía.");
            }

            String username = nombreUsuario.trim();
            for (Usuario usuarioExistente : repoUsuarios.listar()) {
                if (usuarioExistente != null && username.equals(usuarioExistente.getUsername())) {
                    throw new DatoInvalidoException("Ya existe un usuario con ese nombre de usuario.");
                }
            }

            String idUsuario = UUID.randomUUID().toString();
            if (rolUsuario == RolUsuario.CLIENTE) {
                String dniCliente = dni.trim();
                for (Cliente clienteExistente : repoClientes.listar()) {
                    if (clienteExistente != null && clienteExistente.getDni() != null
                            && clienteExistente.getDni().equalsIgnoreCase(dniCliente)) {
                        throw new DatoInvalidoException("Ya existe un cliente con ese DNI.");
                    }
                }
                Cliente cliente = new Cliente(idUsuario, nombre.trim(), apellido.trim(), dniCliente,
                        telefono != null ? telefono.trim() : "", email.trim(), LocalDate.now());
                Usuario nuevoUsuario = new Usuario(idUsuario, username, contrasenia, rolUsuario, true, null, cliente);
                repoClientes.guardar(cliente.getId(), cliente);
                repoUsuarios.guardar(nuevoUsuario.getIdUsuario(), nuevoUsuario);
                gestorBaseDatos.guardarCliente(cliente);
                gestorBaseDatos.guardarUsuario(nuevoUsuario);
                return "Usuario '" + nuevoUsuario.getUsername() + "' creado correctamente con rol CLIENTE" +
                        " | Cliente: " + cliente.getNombreCompleto() + " (DNI " + cliente.getDni() + ").";
            }

            Empleado empleado = new Empleado(idUsuario, nombre.trim(), apellido.trim(), dni.trim(),
                    telefono != null ? telefono.trim() : "", email.trim(), legajo.trim(), sucursal.trim());
            Usuario nuevoUsuario = new Usuario(idUsuario, username, contrasenia, rolUsuario, true, empleado);
            repoEmpleados.guardar(empleado.getId(), empleado);
            repoUsuarios.guardar(nuevoUsuario.getIdUsuario(), nuevoUsuario);
            gestorBaseDatos.guardarEmpleado(empleado);
            gestorBaseDatos.guardarUsuario(nuevoUsuario);
            return "Usuario '" + nuevoUsuario.getUsername() + "' creado correctamente con rol " + rolUsuario +
                    " | Empleado: " + empleado.getNombreCompleto() + " (legajo " + empleado.getLegajo() + ").";
        } catch (DatoInvalidoException e) {
            return e.getMessage();
        } catch (UsuarioNoAutorizadoException e) {
            throw e;
        } catch (Exception e) {
            return "Error al crear usuario: " + e.getMessage();
        }
    }
    private void validarDatosPersona(String nombre, String apellido, String dni, String telefono, String email) {
        validarDato(nombre == null || nombre.trim().isEmpty(), "El nombre no puede estar vacío.");
        validarDato(apellido == null || apellido.trim().isEmpty(), "El apellido no puede estar vacío.");
        validarDato(dni == null || dni.trim().isEmpty(), "El DNI no puede estar vacío.");
        String dniTrim = dni.trim();
        validarDato(dniTrim.length() < 7 || dniTrim.length() > 8,
                "El DNI debe tener entre 7 y 8 caracteres.");
        validarDato(email == null || email.trim().isEmpty(), "El email no puede estar vacío.");
        String emailTrim = email.trim();
        validarDato(!emailTrim.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                "Email inválido. Formato esperado: usuario@dominio.com");
    }
    public boolean hayOcupacionesActivas() {
        for (Ocupacion ocupacion : repoOcupaciones.listar()) {
            if (ocupacion != null && ocupacion.estaActiva()
                    && ocupacion.getCliente() != null && ocupacion.getCliente().isActivo()
                    && ocupacion.getVehiculo() != null && ocupacion.getVehiculo().isActivo()) {
                return true;
            }
        }
        return false;
    }

    public java.util.ArrayList<Reserva> getReservas() {
        return new java.util.ArrayList<Reserva>(reservasActivas());
    }

    public java.util.ArrayList<Cliente> getClientes() {
        return new java.util.ArrayList<Cliente>(clientesActivos());
    }

    public Usuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public void setUsuarioLogueado(Usuario usuarioLogueado) {
        this.usuarioLogueado = usuarioLogueado;
    }



}




