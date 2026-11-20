import enums.RolUsuario;
import service.SistemaAlquiler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner scannerEntrada = new Scanner(System.in);
    private static final SistemaAlquiler sistemaAlquiler = new SistemaAlquiler();

    private static final int CAT_CLIENTES = 1;
    private static final int CAT_AUTOMOVILES = 2;
    private static final int CAT_RESERVAS = 3;
    private static final int CAT_ENTREGAS = 4;
    private static final int CAT_DEVOLUCIONES = 5;
    private static final int CAT_USUARIOS = 6;

    public static void main(String[] args) {
        mostrarMenuPrincipal();
    }

    private static void mostrarMenuPrincipal() {
        while (true) {
            if (sistemaAlquiler.getUsuarioLogueado() == null) {
                mostrarMenuInvitado();
                String opcionMenu = scannerEntrada.nextLine();
                try {
                    if (opcionMenu.equals("0")) {
                        System.out.println("Hasta luego!");
                        System.exit(0);
                    } else if (opcionMenu.equals("1")) {
                        iniciarSesion();
                    } else if (opcionMenu.equals("2")) {
                        crearUsuario(true);
                    } else {
                        System.out.println("Opcion invalida.");
                    }
                } catch (Exception excepcion) {
                    System.out.println("Error: " + excepcion.getMessage());
                }
                continue;
            }

            List<Integer> categorias = categoriasPermitidas();
            mostrarMenuLogueado(categorias);
            String opcionMenu = scannerEntrada.nextLine();
            if (opcionMenu.equals("0")) {
                System.out.println("Hasta luego!");
                System.exit(0);
            }
            if (opcionMenu.equals("1")) {
                sistemaAlquiler.setUsuarioLogueado(null);
                System.out.println("Sesion cerrada correctamente.");
                continue;
            }
            try {
                int numeroCategoria = Integer.parseInt(opcionMenu);
                int indiceCategoria = numeroCategoria - 2;
                if (indiceCategoria < 0 || indiceCategoria >= categorias.size()) {
                    System.out.println("Opcion invalida.");
                    continue;
                }
                switch (categorias.get(indiceCategoria)) {
                    case CAT_CLIENTES: submenuClientes(); break;
                    case CAT_AUTOMOVILES: submenuAutomoviles(); break;
                    case CAT_RESERVAS: submenuReservas(); break;
                    case CAT_ENTREGAS: submenuEntregas(); break;
                    case CAT_DEVOLUCIONES: submenuDevoluciones(); break;
                    case CAT_USUARIOS: submenuUsuarios(); break;
                    default: System.out.println("Opcion invalida."); break;
                }
            } catch (NumberFormatException excepcion) {
                System.out.println("Opcion invalida.");
            } catch (Exception excepcion) {
                System.out.println("Error: " + excepcion.getMessage());
            }
        }
    }

    private static void mostrarMenuInvitado() {
        System.out.println("\n=== SISTEMA DE ALQUILER DE VEHICULOS ===");
        System.out.println("1. Iniciar sesion");
        System.out.println("2. Crear usuario");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opcion: ");
    }

    private static void mostrarMenuLogueado(List<Integer> categorias) {
        System.out.println("\n=== SISTEMA DE ALQUILER DE VEHICULOS ===");
        System.out.println("Usuario: " + sistemaAlquiler.getUsuarioLogueado().getUsername()
                + " | Rol: " + sistemaAlquiler.getUsuarioLogueado().getRol());
        System.out.println("1. Cerrar sesion");
        for (int i = 0; i < categorias.size(); i++) {
            System.out.println((i + 2) + ". " + nombreCategoria(categorias.get(i)));
        }
        System.out.println("0. Salir");
        System.out.print("Seleccione una opcion: ");
    }

    private static List<Integer> categoriasPermitidas() {
        List<Integer> categorias = new ArrayList<Integer>();
        if (esAdmin() || esEmpleado()) {
            categorias.add(CAT_CLIENTES);
        }
        categorias.add(CAT_AUTOMOVILES);
        categorias.add(CAT_RESERVAS);
        if (esAdmin() || esEmpleado()) {
            categorias.add(CAT_ENTREGAS);
            categorias.add(CAT_DEVOLUCIONES);
            categorias.add(CAT_USUARIOS);
        }
        return categorias;
    }

    private static String nombreCategoria(int categoria) {
        if (categoria == CAT_CLIENTES) return "Clientes";
        if (categoria == CAT_AUTOMOVILES) return "Automoviles";
        if (categoria == CAT_RESERVAS) return "Reservas";
        if (categoria == CAT_ENTREGAS) return "Entregas";
        if (categoria == CAT_DEVOLUCIONES) return "Devoluciones";
        if (categoria == CAT_USUARIOS) return esAdmin() ? "Usuarios y sistema" : "Usuarios";
        return "Opcion";
    }

    private static void submenuClientes() {
        while (true) {
            System.out.println("\n--- CLIENTES ---");
            System.out.println("1. Listar clientes");
            System.out.println("2. Crear cliente");
            System.out.println("3. Ver historial de clientes");
            System.out.println("4. Eliminar cliente");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            String opcion = scannerEntrada.nextLine();
            if (opcion.equals("0")) return;
            try {
                if (opcion.equals("1")) {
                    System.out.print(sistemaAlquiler.listarClientes());
                } else if (opcion.equals("2")) {
                    crearClienteDesdeMenu();
                } else if (opcion.equals("3")) {
                    System.out.print(sistemaAlquiler.verHistorialCliente());
                } else if (opcion.equals("4")) {
                    System.out.print(sistemaAlquiler.listarClientes());
                    System.out.print("Indice del cliente a eliminar: ");
                    System.out.print(sistemaAlquiler.eliminarCliente(Integer.parseInt(scannerEntrada.nextLine())));
                } else {
                    System.out.println("Opcion invalida.");
                }
            } catch (Exception excepcion) {
                System.out.println("Error: " + excepcion.getMessage());
            }
        }
    }

    private static void submenuAutomoviles() {
        while (true) {
            System.out.println("\n--- AUTOMOVILES ---");
            if (esCliente()) {
                System.out.println("1. Ver automoviles disponibles");
                System.out.println("0. Volver");
                System.out.print("Seleccione una opcion: ");
                String opcion = scannerEntrada.nextLine();
                if (opcion.equals("0")) return;
                if (opcion.equals("1")) {
                    System.out.print(sistemaAlquiler.listarDisponibles());
                } else {
                    System.out.println("Opcion invalida.");
                }
                continue;
            }

            System.out.println("1. Listar vehiculos");
            System.out.println("2. Ver flota completa");
            System.out.println("3. Agregar automovil");
            System.out.println("4. Modificar estado de vehiculo");
            System.out.println("5. Eliminar automovil");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            String opcion = scannerEntrada.nextLine();
            if (opcion.equals("0")) return;
            try {
                if (opcion.equals("1")) {
                    listarVehiculosDesdeMenu();
                } else if (opcion.equals("2")) {
                    System.out.print(sistemaAlquiler.verFlotaCompleta());
                } else if (opcion.equals("3")) {
                    agregarAutomovilDesdeMenu();
                } else if (opcion.equals("4")) {
                    modificarEstadoDesdeMenu();
                } else if (opcion.equals("5")) {
                    System.out.print(sistemaAlquiler.listarTodosVehiculos());
                    System.out.print("Indice del vehiculo a eliminar: ");
                    System.out.print(sistemaAlquiler.eliminarVehiculo(Integer.parseInt(scannerEntrada.nextLine())));
                } else {
                    System.out.println("Opcion invalida.");
                }
            } catch (Exception excepcion) {
                System.out.println("Error: " + excepcion.getMessage());
            }
        }
    }

    private static void listarVehiculosDesdeMenu() {
        System.out.println("1. Automoviles disponibles");
        System.out.println("2. Automoviles alquilados");
        System.out.println("3. Automoviles no disponibles");
        System.out.print("Seleccione: ");
        String opcion = scannerEntrada.nextLine();
        if (opcion.equals("1")) System.out.print(sistemaAlquiler.listarDisponibles());
        else if (opcion.equals("2")) System.out.print(sistemaAlquiler.listarAlquilados());
        else if (opcion.equals("3")) System.out.print(sistemaAlquiler.listarNoDisponibles());
        else System.out.println("Opcion invalida.");
    }

    private static void submenuReservas() {
        while (true) {
            System.out.println("\n--- RESERVAS ---");
            if (esCliente()) {
                System.out.println("1. Crear reserva");
                System.out.println("2. Cancelar reserva");
                System.out.println("0. Volver");
                System.out.print("Seleccione una opcion: ");
                String opcion = scannerEntrada.nextLine();
                if (opcion.equals("0")) return;
                try {
                    if (opcion.equals("1")) crearReservaDesdeMenu();
                    else if (opcion.equals("2")) cancelarReservaDesdeMenu();
                    else System.out.println("Opcion invalida.");
                } catch (Exception excepcion) {
                    System.out.println("Error: " + excepcion.getMessage());
                }
                continue;
            }

            System.out.println("1. Listar reservas");
            System.out.println("2. Crear reserva");
            System.out.println("3. Cancelar reserva");
            System.out.println("4. Eliminar reserva");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            String opcion = scannerEntrada.nextLine();
            if (opcion.equals("0")) return;
            try {
                if (opcion.equals("1")) {
                    System.out.print(sistemaAlquiler.listarReservas());
                } else if (opcion.equals("2")) {
                    crearReservaDesdeMenu();
                } else if (opcion.equals("3")) {
                    cancelarReservaDesdeMenu();
                } else if (opcion.equals("4")) {
                    System.out.print(sistemaAlquiler.listarReservas());
                    System.out.print("Indice de la reserva a eliminar: ");
                    System.out.print(sistemaAlquiler.eliminarReserva(Integer.parseInt(scannerEntrada.nextLine())));
                } else {
                    System.out.println("Opcion invalida.");
                }
            } catch (Exception excepcion) {
                System.out.println("Error: " + excepcion.getMessage());
            }
        }
    }

    private static void submenuEntregas() {
        while (true) {
            System.out.println("\n--- ENTREGAS ---");
            System.out.println("1. Realizar entrega");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            String opcion = scannerEntrada.nextLine();
            if (opcion.equals("0")) return;
            if (!opcion.equals("1")) {
                System.out.println("Opcion invalida.");
                continue;
            }
            try {
                System.out.println("\n--- RESERVAS PENDIENTES ---");
                boolean hayReservas = false;
                for (int i = 0; ; i++) {
                    String infoReserva = sistemaAlquiler.getReservaInfo(i);
                    if (infoReserva == null || infoReserva.isEmpty() || infoReserva.startsWith("No hay reservas")) break;
                    hayReservas = true;
                    System.out.println(infoReserva);
                }
                if (!hayReservas) {
                    System.out.println("No hay reservas pendientes.");
                    continue;
                }
                System.out.print("Seleccione reserva (indice): ");
                int indiceReservaEntrega = Integer.parseInt(scannerEntrada.nextLine());
                System.out.print(sistemaAlquiler.realizarEntregaPorIndice(indiceReservaEntrega));
            } catch (Exception excepcion) {
                System.out.println("Error: " + excepcion.getMessage());
            }
        }
    }

    private static void submenuDevoluciones() {
        while (true) {
            System.out.println("\n--- DEVOLUCIONES ---");
            System.out.println("1. Realizar devolucion");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            String opcion = scannerEntrada.nextLine();
            if (opcion.equals("0")) return;
            if (!opcion.equals("1")) {
                System.out.println("Opcion invalida.");
                continue;
            }
            try {
                if (!sistemaAlquiler.hayOcupacionesActivas()) {
                    System.out.println("No hay ocupaciones activas.");
                    continue;
                }
                System.out.print(sistemaAlquiler.listarAlquilados());
                System.out.print("Seleccione: ");
                int indiceOcupacionDevolucion = Integer.parseInt(scannerEntrada.nextLine());
                System.out.print("Kilometros recorridos: ");
                int kilometrosRecorridos = Integer.parseInt(scannerEntrada.nextLine());
                System.out.print("Danios: ");
                String danosRegistrados = scannerEntrada.nextLine();
                System.out.print("Observaciones: ");
                String observacionesDevolucion = scannerEntrada.nextLine();
                System.out.print(sistemaAlquiler.realizarDevolucionPorIndice(indiceOcupacionDevolucion,
                        kilometrosRecorridos, danosRegistrados, observacionesDevolucion));
            } catch (Exception excepcion) {
                System.out.println("Error: " + excepcion.getMessage());
            }
        }
    }

    private static void submenuUsuarios() {
        while (true) {
            System.out.println("\n--- " + (esAdmin() ? "USUARIOS Y SISTEMA" : "USUARIOS") + " ---");
            if (esAdmin()) {
                System.out.println("1. Ver usuarios");
                System.out.println("2. Crear usuario");
                System.out.println("3. Eliminar usuario");
                System.out.println("4. Limpiar base de datos");
                System.out.println("0. Volver");
                System.out.print("Seleccione una opcion: ");
                String opcion = scannerEntrada.nextLine();
                if (opcion.equals("0")) return;
                try {
                    if (opcion.equals("1")) {
                        System.out.print(sistemaAlquiler.administrarUsuarios());
                    } else if (opcion.equals("2")) {
                        crearUsuario(false);
                    } else if (opcion.equals("3")) {
                        System.out.print(sistemaAlquiler.listarUsuarios());
                        System.out.print("Indice del usuario a eliminar: ");
                        System.out.print(sistemaAlquiler.eliminarUsuarioPorIndice(Integer.parseInt(scannerEntrada.nextLine())));
                    } else if (opcion.equals("4")) {
                        System.out.print(sistemaAlquiler.limpiarDatosBD());
                        if (sistemaAlquiler.getUsuarioLogueado() == null) return;
                    } else {
                        System.out.println("Opcion invalida.");
                    }
                } catch (Exception excepcion) {
                    System.out.println("Error: " + excepcion.getMessage());
                }
                continue;
            }

            System.out.println("1. Crear usuario");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            String opcion = scannerEntrada.nextLine();
            if (opcion.equals("0")) return;
            if (opcion.equals("1")) {
                try {
                    crearUsuario(false);
                } catch (Exception excepcion) {
                    System.out.println("Error: " + excepcion.getMessage());
                }
            } else {
                System.out.println("Opcion invalida.");
            }
        }
    }

    private static void crearClienteDesdeMenu() {
        System.out.print("Nombre: ");
        String nombre = scannerEntrada.nextLine();
        System.out.print("Apellido: ");
        String apellido = scannerEntrada.nextLine();
        System.out.print("DNI: ");
        String dni = scannerEntrada.nextLine();
        System.out.print("Telefono: ");
        String telefono = scannerEntrada.nextLine();
        System.out.print("Email: ");
        String email = scannerEntrada.nextLine();
        System.out.println(sistemaAlquiler.crearCliente(nombre, apellido, dni, telefono, email));
    }

    private static void agregarAutomovilDesdeMenu() {
        System.out.print("Patente: ");
        String patente = scannerEntrada.nextLine();
        System.out.print("Marca: ");
        String marca = scannerEntrada.nextLine();
        System.out.print("Modelo: ");
        String modelo = scannerEntrada.nextLine();
        System.out.print("Año: ");
        int anio = Integer.parseInt(scannerEntrada.nextLine());
        System.out.print("Kilometraje: ");
        int kilometraje = Integer.parseInt(scannerEntrada.nextLine());
        System.out.print("Estado: DISPONIBLE/EN_LIMPIEZA/EN_REPARACION/FUERA_SERVICIO: ");
        String estado = scannerEntrada.nextLine();
        System.out.print("Precio por dia: ");
        double precioDia = Double.parseDouble(scannerEntrada.nextLine());
        System.out.print("Tarifa seguro: ");
        double tarifaSeguro = Double.parseDouble(scannerEntrada.nextLine());
        System.out.print("Cantidad de puertas: ");
        int cantidadPuertas = Integer.parseInt(scannerEntrada.nextLine());
        System.out.print("Transmision (MANUAL/AUTOMATICA): ");
        String transmision = scannerEntrada.nextLine();
        System.out.print(sistemaAlquiler.agregarAutomovil(patente, marca, modelo, anio, kilometraje, estado,
                precioDia, tarifaSeguro, cantidadPuertas, transmision));
    }

    private static void modificarEstadoDesdeMenu() {
        System.out.print(sistemaAlquiler.listarTodosVehiculos());
        System.out.print("Indice del vehiculo a modificar: ");
        int indiceEstado = Integer.parseInt(scannerEntrada.nextLine());
        System.out.print("Nuevo estado (DISPONIBLE/EN_LIMPIEZA/EN_REPARACION/FUERA_SERVICIO): ");
        String nuevoEstado = scannerEntrada.nextLine();
        System.out.println(sistemaAlquiler.modificarEstadoVehiculo(indiceEstado, nuevoEstado));
    }

    private static void crearReservaDesdeMenu() {
        if (esCliente()) {
            System.out.print(sistemaAlquiler.listarDisponibles());
            model.Cliente clienteLogueado = sistemaAlquiler.getUsuarioLogueado() != null
                    ? sistemaAlquiler.getUsuarioLogueado().getCliente() : null;
            if (clienteLogueado != null) {
                System.out.println("Cliente: " + clienteLogueado.getNombreCompleto()
                        + " | DNI: " + clienteLogueado.getDni());
            }
            System.out.print("Seleccione vehiculo (indice): ");
            int indiceVehiculoCliente = Integer.parseInt(scannerEntrada.nextLine());
            System.out.print("Fecha inicio (YYYY-MM-DD): ");
            LocalDate fechaInicioCliente = LocalDate.parse(scannerEntrada.nextLine());
            System.out.print("Fecha fin (YYYY-MM-DD): ");
            LocalDate fechaFinCliente = LocalDate.parse(scannerEntrada.nextLine());
            System.out.print(sistemaAlquiler.reservarComoCliente(
                    indiceVehiculoCliente, fechaInicioCliente, fechaFinCliente));
            return;
        }

        System.out.print("¿Desea crear un cliente nuevo? (S/N): ");
        String crearClienteNuevo = scannerEntrada.nextLine().toUpperCase();
        if (crearClienteNuevo.equals("S")) {
            crearClienteDesdeMenu();
        }
        System.out.println("\n--- CLIENTES SIN RESERVA ACTIVA ---");
        ArrayList<model.Cliente> clientesSistema = sistemaAlquiler.getClientes();
        for (int i = 0; i < clientesSistema.size(); i++) {
            model.Cliente c = clientesSistema.get(i);
            if (c != null) {
                boolean tieneReservaActiva = false;
                for (model.Reserva r : sistemaAlquiler.getReservas()) {
                    if (r != null && r.getCliente() != null && r.getCliente().getId().equals(c.getId())) {
                        if (r.getEstado() != enums.EstadoReserva.CANCELADA
                                && r.getEstado() != enums.EstadoReserva.CONCRETADA) {
                            tieneReservaActiva = true;
                            break;
                        }
                    }
                }
                if (!tieneReservaActiva) {
                    System.out.println(i + ": " + c.getNombre() + " " + c.getApellido()
                            + " - DNI: " + c.getDni());
                }
            }
        }
        System.out.println("\n--- AUTOMOVILES DISPONIBLES ---");
        for (int i = 0; ; i++) {
            String infoVehiculo = sistemaAlquiler.getVehiculoInfo(i);
            if (infoVehiculo == null || infoVehiculo.isEmpty()
                    || infoVehiculo.equals("SIN_VEHICULOS_DISPONIBLES")) break;
            System.out.println(infoVehiculo);
        }
        if (sistemaAlquiler.getClientes().isEmpty()) {
            System.out.println("No hay clientes registrados. Cree un cliente primero.");
            return;
        }
        if (sistemaAlquiler.getVehiculoInfo(0).equals("SIN_VEHICULOS_DISPONIBLES")) {
            System.out.println("No hay automóviles disponibles para reservar.");
            return;
        }
        System.out.print("Seleccione cliente (indice): ");
        int indiceClienteReserva = Integer.parseInt(scannerEntrada.nextLine());
        System.out.print("Seleccione vehiculo (indice): ");
        int indiceVehiculoReserva = Integer.parseInt(scannerEntrada.nextLine());
        System.out.print("Fecha inicio (YYYY-MM-DD): ");
        LocalDate fechaInicioReserva = LocalDate.parse(scannerEntrada.nextLine());
        System.out.print("Fecha fin (YYYY-MM-DD): ");
        LocalDate fechaFinReserva = LocalDate.parse(scannerEntrada.nextLine());
        System.out.print(sistemaAlquiler.crearReserva(indiceClienteReserva, indiceVehiculoReserva,
                fechaInicioReserva, fechaFinReserva));
    }

    private static void cancelarReservaDesdeMenu() {
        System.out.print(sistemaAlquiler.listarReservasCancelables());
        System.out.print("Indice de reserva a cancelar: ");
        System.out.print(sistemaAlquiler.cancelarReserva(Integer.parseInt(scannerEntrada.nextLine())));
    }

    private static void iniciarSesion() {
        System.out.print("Usuario: ");
        String nombreUsuario = scannerEntrada.nextLine();
        System.out.print("Password: ");
        String contrasenia = scannerEntrada.nextLine();
        System.out.println(sistemaAlquiler.login(nombreUsuario, contrasenia));
    }

    private static void crearUsuario(boolean invitado) {
        if (invitado) {
            System.out.println("Sin sesion iniciada solo puede registrarse como CLIENTE.");
        }
        System.out.print("Nombre de usuario: ");
        String nombreUsuario = scannerEntrada.nextLine();
        System.out.print("Contraseña: ");
        String contrasenia = scannerEntrada.nextLine();

        String rol;
        if (invitado) {
            rol = "CLIENTE";
        } else if (esAdmin()) {
            System.out.println("1. ADMINISTRADOR 2. EMPLEADO 3. CLIENTE");
            System.out.print("Opcion: ");
            String opcionRol = scannerEntrada.nextLine();
            if (opcionRol.equals("1")) rol = "ADMINISTRADOR";
            else if (opcionRol.equals("2")) rol = "EMPLEADO";
            else if (opcionRol.equals("3")) rol = "CLIENTE";
            else { System.out.println("Opcion de rol invalida."); return; }
        } else if (esEmpleado()) {
            System.out.println("1. EMPLEADO 2. CLIENTE");
            System.out.print("Opcion: ");
            String opcionRol = scannerEntrada.nextLine();
            if (opcionRol.equals("1")) rol = "EMPLEADO";
            else if (opcionRol.equals("2")) rol = "CLIENTE";
            else { System.out.println("Opcion de rol invalida."); return; }
        } else {
            System.out.println("No tiene permisos para crear usuarios.");
            return;
        }

        System.out.print("Nombre: ");
        String nombre = scannerEntrada.nextLine();
        System.out.print("Apellido: ");
        String apellido = scannerEntrada.nextLine();
        System.out.print("DNI: ");
        String dni = scannerEntrada.nextLine();
        System.out.print("Telefono: ");
        String telefono = scannerEntrada.nextLine();
        System.out.print("Email: ");
        String email = scannerEntrada.nextLine();
        String legajo = "";
        String sucursal = "";
        if (!rol.equals("CLIENTE")) {
            System.out.print("Legajo: ");
            legajo = scannerEntrada.nextLine();
            System.out.print("Sucursal: ");
            sucursal = scannerEntrada.nextLine();
        }
        System.out.print(sistemaAlquiler.crearUsuario(nombreUsuario, contrasenia, rol, nombre, apellido,
                dni, telefono, email, legajo, sucursal));
    }

    private static boolean esAdmin() {
        return sistemaAlquiler.getUsuarioLogueado() != null
                && sistemaAlquiler.getUsuarioLogueado().getRol() == RolUsuario.ADMINISTRADOR;
    }

    private static boolean esEmpleado() {
        return sistemaAlquiler.getUsuarioLogueado() != null
                && sistemaAlquiler.getUsuarioLogueado().getRol() == RolUsuario.EMPLEADO;
    }

    private static boolean esCliente() {
        return sistemaAlquiler.esCliente();
    }
}
