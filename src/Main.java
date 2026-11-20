import service.SistemaAlquiler;

public class Main {
    public static void main(String[] args) {
        MenuConsola consola = new MenuConsola(new SistemaAlquiler());
        while (true) {
            int opcion = consola.leerOpcionPrincipal();
            try {
                switch (opcion) {
                    case MenuConsola.OPCION_SALIR:
                        consola.despedirse();
                        return;
                    case MenuConsola.OPCION_INICIAR_SESION:
                        consola.iniciarSesion();
                        break;
                    case MenuConsola.OPCION_CERRAR_SESION:
                        consola.cerrarSesion();
                        break;
                    case MenuConsola.OPCION_CREAR_USUARIO_INVITADO:
                        consola.crearUsuario(true);
                        break;
                    case MenuConsola.CAT_CLIENTES:
                        consola.submenuClientes();
                        break;
                    case MenuConsola.CAT_AUTOMOVILES:
                        consola.submenuAutomoviles();
                        break;
                    case MenuConsola.CAT_RESERVAS:
                        consola.submenuReservas();
                        break;
                    case MenuConsola.CAT_ENTREGAS:
                        consola.submenuEntregas();
                        break;
                    case MenuConsola.CAT_DEVOLUCIONES:
                        consola.submenuDevoluciones();
                        break;
                    case MenuConsola.CAT_USUARIOS:
                        consola.submenuUsuarios();
                        break;
                    default:
                        consola.opcionInvalida();
                        break;
                }
            } catch (Exception excepcion) {
                consola.mostrarError(excepcion);
            }
        }
    }
}
