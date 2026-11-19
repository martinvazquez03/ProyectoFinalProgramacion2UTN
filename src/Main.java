import service.SistemaAlquiler;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    private static final Scanner scannerEntrada = new Scanner(System.in);
    private static final SistemaAlquiler sistemaAlquiler = new SistemaAlquiler();

    public static void main(String[] args) {
        mostrarMenuPrincipal();
    }

    private static void mostrarMenuPrincipal() {
        while (true) {
            System.out.println("\n=== SISTEMA DE ALQUILER DE VEHICULOS ===");
            if (sistemaAlquiler.getUsuarioLogueado() == null) {
                System.out.println("1. Login");
            } else {
                System.out.println("1. Cerrar sesion");
            }
            if (esCliente()) {
                System.out.println("2. Ver automoviles disponibles");
                System.out.println("12. Gestionar reserva");
                System.out.println("0. Salir");
            } else {
                System.out.println("2. Listar vehiculos");
                System.out.println("3. Listar clientes");
                System.out.println("4. Listar reservas");
                System.out.println("5. Ver usuarios (solo ADMIN)");
                System.out.println("6. Ver flota completa");
                System.out.println("7. Ver historial clientes");
                System.out.println("8. Crear usuario (invitado: solo CLIENTE)");
                System.out.println("9. Crear cliente (ADMIN o EMPLEADO)");
                System.out.println("10. Agregar automovil (ADMIN o EMPLEADO)");
                System.out.println("11. Modificar estado de vehiculo (ADMIN o EMPLEADO)");
                        System.out.println("12. Gestionar reserva");
                        System.out.println("13. Realizar entrega");
                System.out.println("14. Realizar devolucion");
                System.out.println("15. Eliminar Datos");
                System.out.println("16. Limpiar BD (solo ADMIN)");
                System.out.println("0. Salir");
            }
            System.out.print("Seleccione una opcion: ");
            String opcionMenu = scannerEntrada.nextLine();
            if (esCliente() && !opcionMenu.equals("0") && !opcionMenu.equals("1")
                    && !opcionMenu.equals("2") && !opcionMenu.equals("12")) {
                System.out.println("Opcion no disponible para su rol.");
                continue;
            }
            try {
                switch (opcionMenu) {
                    case "1":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) {
                            System.out.print("Usuario: ");
                            String nombreUsuarioLogin = scannerEntrada.nextLine();
                            System.out.print("Password: ");
                            String contraseniaLogin = scannerEntrada.nextLine();
                            System.out.println(sistemaAlquiler.login(nombreUsuarioLogin, contraseniaLogin));
                        } else {
                            sistemaAlquiler.setUsuarioLogueado(null);
                            System.out.println("Sesion cerrada correctamente.");
                        }
                        break;
                    case "2":
                        if (esCliente()) {
                            System.out.print(sistemaAlquiler.listarDisponibles());
                            break;
                        }
                        System.out.println("1. Listar automoviles disponibles");
                        System.out.println("2. Listar automoviles alquilados");
                        System.out.println("3. Listar automoviles no disponibles (con motivo)");
                        System.out.print("Seleccione: ");
                        String subMenuVehiculo = scannerEntrada.nextLine();
                        if (subMenuVehiculo.equals("1")) System.out.print(sistemaAlquiler.listarDisponibles());
                        else if (subMenuVehiculo.equals("2")) System.out.print(sistemaAlquiler.listarAlquilados());
                        else if (subMenuVehiculo.equals("3")) System.out.print(sistemaAlquiler.listarNoDisponibles());
                        else System.out.println("Opcion invalida.");
                        break;
                    case "3": System.out.print(sistemaAlquiler.listarClientes()); break;
                    case "4": System.out.print(sistemaAlquiler.listarReservas()); break;
                    case "5":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.print(sistemaAlquiler.administrarUsuarios());
                        break;
                    case "6": System.out.print(sistemaAlquiler.verFlotaCompleta()); break;
                    case "7":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.print(sistemaAlquiler.verHistorialCliente());
                        break;
                    case "8":
                        boolean registroInvitado = sistemaAlquiler.getUsuarioLogueado() == null;
                        if (registroInvitado) {
                            System.out.println("Sin sesion iniciada solo puede registrarse como CLIENTE.");
                        }
                        System.out.print("Nombre de usuario: ");
                        String nuevoNombreUsuario = scannerEntrada.nextLine();
                        System.out.print("Contraseña: ");
                        String nuevaContraseniaUsuario = scannerEntrada.nextLine();
                        String nuevoRolUsuario;
                        if (registroInvitado) {
                            nuevoRolUsuario = "CLIENTE";
                        } else {
                            System.out.println("1. ADMINISTRADOR 2. EMPLEADO 3. CLIENTE");
                            System.out.print("Opcion: ");
                            String opcionRol = scannerEntrada.nextLine();
                            if (opcionRol.equals("1")) nuevoRolUsuario = "ADMINISTRADOR";
                            else if (opcionRol.equals("2")) nuevoRolUsuario = "EMPLEADO";
                            else if (opcionRol.equals("3")) nuevoRolUsuario = "CLIENTE";
                            else { System.out.println("Opcion de rol invalida."); break; }
                        }
                        System.out.print("Nombre: "); String nombreEmpleadoUsuario = scannerEntrada.nextLine();
                        System.out.print("Apellido: "); String apellidoEmpleadoUsuario = scannerEntrada.nextLine();
                        System.out.print("DNI: "); String dniEmpleadoUsuario = scannerEntrada.nextLine();
                        System.out.print("Telefono: "); String telefonoEmpleadoUsuario = scannerEntrada.nextLine();
                        System.out.print("Email: "); String emailEmpleadoUsuario = scannerEntrada.nextLine();
                        String legajoEmpleadoUsuario = "";
                        String sucursalEmpleadoUsuario = "";
                        if (!nuevoRolUsuario.equals("CLIENTE")) {
                            System.out.print("Legajo: "); legajoEmpleadoUsuario = scannerEntrada.nextLine();
                            System.out.print("Sucursal: "); sucursalEmpleadoUsuario = scannerEntrada.nextLine();
                        }
                        System.out.print(sistemaAlquiler.crearUsuario(nuevoNombreUsuario, nuevaContraseniaUsuario, nuevoRolUsuario,
                                nombreEmpleadoUsuario, apellidoEmpleadoUsuario, dniEmpleadoUsuario, telefonoEmpleadoUsuario,
                                emailEmpleadoUsuario, legajoEmpleadoUsuario, sucursalEmpleadoUsuario));
                        break;
                    case "9":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.print("Nombre: "); String nombreClienteNuevo = scannerEntrada.nextLine();
                        System.out.print("Apellido: "); String apellidoClienteNuevo = scannerEntrada.nextLine();
                        System.out.print("DNI: "); String dniClienteNuevo = scannerEntrada.nextLine();
                        System.out.print("Telefono: "); String telefonoClienteNuevo = scannerEntrada.nextLine();
                        System.out.print("Email: "); String emailClienteNuevo = scannerEntrada.nextLine();
                        System.out.println(sistemaAlquiler.crearCliente(nombreClienteNuevo, apellidoClienteNuevo, dniClienteNuevo, telefonoClienteNuevo, emailClienteNuevo));
                        break;
                    case "10":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.print("Patente: "); String patenteVehiculo = scannerEntrada.nextLine();
                        System.out.print("Marca: "); String marcaVehiculo = scannerEntrada.nextLine();
                        System.out.print("Modelo: "); String modeloVehiculo = scannerEntrada.nextLine();
                        System.out.print("Año: "); int anioVehiculo = Integer.parseInt(scannerEntrada.nextLine());
                        System.out.print("Kilometraje: "); int kilometrajeVehiculo = Integer.parseInt(scannerEntrada.nextLine());
                        System.out.print("Estado: DISPONIBLE/ALQUILADO/EN_LIMPIEZA/EN_REPARACION/FUERA_SERVICIO"); String estadoVehiculoAgregar = scannerEntrada.nextLine();
                        System.out.print("Precio por dia: "); double precioDiaVehiculo = Double.parseDouble(scannerEntrada.nextLine());
                        System.out.print("Tarifa seguro: "); double tarifaSeguroVehiculo = Double.parseDouble(scannerEntrada.nextLine());
                        System.out.print("Cantidad de puertas: "); int cantidadPuertasVehiculo = Integer.parseInt(scannerEntrada.nextLine());
                        System.out.print("Transmision (MANUAL/AUTOMATICA): "); String transmisionVehiculo = scannerEntrada.nextLine();
                        System.out.print(sistemaAlquiler.agregarAutomovil(patenteVehiculo, marcaVehiculo, modeloVehiculo, anioVehiculo, kilometrajeVehiculo, estadoVehiculoAgregar, precioDiaVehiculo, tarifaSeguroVehiculo, cantidadPuertasVehiculo, transmisionVehiculo));
                        break;
                    case "11":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.print(sistemaAlquiler.listarTodosVehiculos());
                        System.out.print("Indice del vehiculo a modificar: ");
                        int indiceEstado = Integer.parseInt(scannerEntrada.nextLine());
                        System.out.print("Nuevo estado (DISPONIBLE/ALQUILADO/RESERVADO/EN_LIMPIEZA/EN_REPARACION/FUERA_SERVICIO): ");
                        String nuevoEstado = scannerEntrada.nextLine();
                        System.out.println(sistemaAlquiler.modificarEstadoVehiculo(indiceEstado, nuevoEstado));
                        break;
                    case "12":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.println("1. Crear reserva");
                        System.out.println("2. Cancelar reserva");
                        System.out.print("Seleccione: ");
                        String subMenuReserva = scannerEntrada.nextLine();
                        if (subMenuReserva.equals("1")) {
                            if (esCliente()) {
                                reservarComoCliente();
                                break;
                            }
                            try {
                                System.out.print("¿Desea crear un cliente nuevo? (S/N): ");
                                String crearClienteNuevo = scannerEntrada.nextLine().toUpperCase();
                                if (crearClienteNuevo.equals("S")) {
                                    System.out.print("Nombre: "); String nombreCliente = scannerEntrada.nextLine();
                                    System.out.print("Apellido: "); String apellidoCliente = scannerEntrada.nextLine();
                                    System.out.print("DNI: "); String dniCliente = scannerEntrada.nextLine();
                                    System.out.print("Telefono: "); String telefonoCliente = scannerEntrada.nextLine();
                                    System.out.print("Email: "); String emailCliente = scannerEntrada.nextLine();
                                    System.out.println(sistemaAlquiler.crearCliente(nombreCliente, apellidoCliente, dniCliente, telefonoCliente, emailCliente));
                                }
                                System.out.println("\n--- CLIENTES SIN RESERVA ACTIVA ---");
                                java.util.ArrayList<model.Cliente> clientesSistema = sistemaAlquiler.getClientes();
                                for (int i = 0; i < clientesSistema.size(); i++) {
                                    model.Cliente c = clientesSistema.get(i);
                                    if (c != null) {
                                        boolean tieneReservaActiva = false;
                                        for (model.Reserva r : sistemaAlquiler.getReservas()) {
                                            if (r != null && r.getCliente() != null && r.getCliente().getId().equals(c.getId())) {
                                                if (r.getEstado() != enums.EstadoReserva.CANCELADA && r.getEstado() != enums.EstadoReserva.CONCRETADA) { tieneReservaActiva = true; break; }
                                            }
                                        }
                                        if (!tieneReservaActiva) System.out.println(i + ": " + c.getNombre() + " " + c.getApellido() + " - DNI: " + c.getDni());
                                    }
                                }
                                System.out.println("\n--- AUTOMOVILES DISPONIBLES ---");
                                for (int i = 0; ; i++) {
                                    String infoVehiculo = sistemaAlquiler.getVehiculoInfo(i);
                                    if (infoVehiculo == null || infoVehiculo.isEmpty() || infoVehiculo.equals("SIN_VEHICULOS_DISPONIBLES")) break;
                                    System.out.println(infoVehiculo);
                                }
                                if (sistemaAlquiler.getClientes().isEmpty()) {
                                    System.out.println("No hay clientes registrados. Cree un cliente primero.");
                                    break;
                                }
                                if (sistemaAlquiler.getVehiculoInfo(0).equals("SIN_VEHICULOS_DISPONIBLES")) {
                                    System.out.println("No hay automóviles disponibles para reservar.");
                                    break;
                                }
                                System.out.print("Seleccione cliente (indice): "); int indiceClienteReserva = Integer.parseInt(scannerEntrada.nextLine());
                                System.out.print("Seleccione vehiculo (indice): "); int indiceVehiculoReserva = Integer.parseInt(scannerEntrada.nextLine());
                                System.out.print("Fecha inicio (YYYY-MM-DD): "); LocalDate fechaInicioReserva = LocalDate.parse(scannerEntrada.nextLine());
                                System.out.print("Fecha fin (YYYY-MM-DD): "); LocalDate fechaFinReserva = LocalDate.parse(scannerEntrada.nextLine());
                                System.out.print(sistemaAlquiler.crearReserva(indiceClienteReserva, indiceVehiculoReserva, fechaInicioReserva, fechaFinReserva));
                            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
                        } else if (subMenuReserva.equals("2")) {
                            System.out.print(sistemaAlquiler.listarReservasCancelables());
                            System.out.print("Indice de reserva a cancelar: ");
                            System.out.print(sistemaAlquiler.cancelarReserva(Integer.parseInt(scannerEntrada.nextLine())));
                        } else {
                            System.out.println("Opcion invalida.");
                        }
                        break;
                    case "13":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.println("\n--- RESERVAS DISPONIBLES ---");
                        boolean hayReservas = false;
                        for (int i = 0; ; i++) {
                            String infoReserva = sistemaAlquiler.getReservaInfo(i);
                            if (infoReserva == null || infoReserva.isEmpty() || infoReserva.equals("No hay reservas")) break;
                            hayReservas = true;
                            System.out.println(infoReserva);
                        }
                        if (!hayReservas) { System.out.println("No hay reservas."); break; }
                        System.out.print("Seleccione reserva (indice): ");
                        int indiceReservaEntrega = Integer.parseInt(scannerEntrada.nextLine());
                        System.out.print("Observaciones: ");
                        String observacionesEntrega = scannerEntrada.nextLine();
                        System.out.print(sistemaAlquiler.realizarEntregaPorIndice(indiceReservaEntrega, observacionesEntrega));
                        break;
                    case "14":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        if (!sistemaAlquiler.hayOcupacionesActivas()) { System.out.println("No hay ocupaciones activas."); break; }
                        System.out.print(sistemaAlquiler.listarAlquilados());
                        System.out.print("Seleccione: ");
                        int indiceOcupacionDevolucion = Integer.parseInt(scannerEntrada.nextLine());
                        System.out.print("Kilometros recorridos: "); int kilometrosRecorridos = Integer.parseInt(scannerEntrada.nextLine());
                        System.out.print("Danios: "); String danosRegistrados = scannerEntrada.nextLine();
                        System.out.print("Cargos extra: "); double cargosExtraDevolucion = Double.parseDouble(scannerEntrada.nextLine());
                        System.out.print(sistemaAlquiler.realizarDevolucionPorIndice(indiceOcupacionDevolucion, kilometrosRecorridos, danosRegistrados, cargosExtraDevolucion));
                        break;
                    case "15":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.println("1. Eliminar cliente\n2. Eliminar usuario (solo ADMIN)\n3. Eliminar automovil\n4. Eliminar reserva");
                        System.out.print("Seleccione: ");
                        String subElim = scannerEntrada.nextLine();
                        try {
                            if (subElim.equals("1")) { System.out.print(sistemaAlquiler.listarClientes()); System.out.print("Indice: "); System.out.print(sistemaAlquiler.eliminarCliente(Integer.parseInt(scannerEntrada.nextLine()))); }
                            else if (subElim.equals("2")) { System.out.print(sistemaAlquiler.listarUsuarios()); System.out.print("Indice: "); System.out.print(sistemaAlquiler.eliminarUsuarioPorIndice(Integer.parseInt(scannerEntrada.nextLine()))); }
                            else if (subElim.equals("3")) { System.out.print(sistemaAlquiler.listarTodosVehiculos()); System.out.print("Indice: "); System.out.print(sistemaAlquiler.eliminarVehiculo(Integer.parseInt(scannerEntrada.nextLine()))); }
                            else if (subElim.equals("4")) { System.out.print(sistemaAlquiler.listarReservas()); System.out.print("Indice: "); System.out.print(sistemaAlquiler.eliminarReserva(Integer.parseInt(scannerEntrada.nextLine()))); }
                            else System.out.println("Opcion invalida");
                        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
                        break;
                    case "16":
                        if (sistemaAlquiler.getUsuarioLogueado() == null) { System.out.println("Debe iniciar sesion primero."); break; }
                        System.out.print(sistemaAlquiler.limpiarDatosBD());
                        break;
                    case "0": System.out.println("Hasta luego!"); System.exit(0); break;
                    default: System.out.println("Opcion invalida."); break;
                }
            } catch (Exception excepcion) {
                System.out.println("Error: " + excepcion.getMessage());
            }
        }
    }

    private static boolean esCliente() {
        return sistemaAlquiler.getUsuarioLogueado() != null
                && sistemaAlquiler.getUsuarioLogueado().getRol() == enums.RolUsuario.CLIENTE;
    }

    private static void reservarComoCliente() {
        model.Usuario usuario = sistemaAlquiler.getUsuarioLogueado();
        if (usuario == null || usuario.getCliente() == null) {
            System.out.println("El usuario no tiene una ficha de cliente asociada.");
            return;
        }
        System.out.println("\n--- AUTOMOVILES DISPONIBLES ---");
        for (int i = 0; ; i++) {
            String infoVehiculo = sistemaAlquiler.getVehiculoInfo(i);
            if (infoVehiculo == null || infoVehiculo.isEmpty() || infoVehiculo.equals("SIN_VEHICULOS_DISPONIBLES")) break;
            System.out.println(infoVehiculo);
        }
        if (sistemaAlquiler.getVehiculoInfo(0).equals("SIN_VEHICULOS_DISPONIBLES")) {
            System.out.println("No hay automoviles disponibles para reservar.");
            return;
        }
        System.out.println("Cliente: " + usuario.getCliente().getNombreCompleto()
                + " | DNI: " + usuario.getCliente().getDni());
        try {
            System.out.print("Seleccione vehiculo (indice): ");
            int indiceVehiculo = Integer.parseInt(scannerEntrada.nextLine());
            System.out.print("Fecha inicio (YYYY-MM-DD): ");
            LocalDate fechaInicio = LocalDate.parse(scannerEntrada.nextLine());
            System.out.print("Fecha fin (YYYY-MM-DD): ");
            LocalDate fechaFin = LocalDate.parse(scannerEntrada.nextLine());
            System.out.print(sistemaAlquiler.crearReservaCliente(indiceVehiculo, fechaInicio, fechaFin));
        } catch (Exception excepcion) {
            System.out.println("Error: " + excepcion.getMessage());
        }
    }
}