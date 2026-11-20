package dao;

import config.ConnectionFactory;
import enums.EstadoVehiculo;
import enums.RolUsuario;
import exception.BaseDatosException;
import model.Automovil;
import model.Cliente;
import model.Empleado;
import model.Reserva;
import model.Ocupacion;
import model.Entrega;
import model.Devolucion;
import model.Usuario;
import model.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class GestorBaseDatos {

    public void crearTablas() {
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.Statement stmt = conexion.createStatement()) {
            String sqlVehiculo = "CREATE TABLE IF NOT EXISTS vehiculo (" +
                    "patente TEXT PRIMARY KEY," +
                    "marca TEXT," +
                    "modelo TEXT," +
                    "anio INTEGER," +
                    "kilometraje INTEGER," +
                    "estado TEXT," +
                    "precio_dia REAL," +
                    "tarifa_seguro REAL," +
                    "tipo TEXT," +
                    "datos_especificos TEXT," +
                    "activo INTEGER DEFAULT 1" +
                    ")";
            stmt.execute(sqlVehiculo);
            String sqlUsuario = "CREATE TABLE IF NOT EXISTS usuario (" +
                    "id_usuario TEXT PRIMARY KEY," +
                    "username TEXT UNIQUE," +
                    "password_hash TEXT," +
                    "rol TEXT," +
                    "activo INTEGER DEFAULT 1" +
                    ")";
            stmt.execute(sqlUsuario);
            String sqlCliente = "CREATE TABLE IF NOT EXISTS cliente (" +
                    "id_cliente TEXT PRIMARY KEY," +
                    "nombre TEXT," +
                    "apellido TEXT," +
                    "dni TEXT," +
                    "telefono TEXT," +
                    "email TEXT," +
                    "fecha_registro TEXT," +
                    "activo INTEGER DEFAULT 1" +
                    ")";
            stmt.execute(sqlCliente);
            String sqlEmpleado = "CREATE TABLE IF NOT EXISTS empleado (" +
                    "id_empleado TEXT PRIMARY KEY," +
                    "nombre TEXT," +
                    "apellido TEXT," +
                    "dni TEXT," +
                    "telefono TEXT," +
                    "email TEXT," +
                    "legajo TEXT," +
                    "sucursal TEXT" +
                    ")";
            stmt.execute(sqlEmpleado);
            String sqlReserva = "CREATE TABLE IF NOT EXISTS reserva (" +
                    "id_reserva TEXT PRIMARY KEY," +
                    "id_cliente TEXT," +
                    "patente TEXT," +
                    "fecha_inicio TEXT," +
                    "fecha_fin TEXT," +
                    "fecha_reserva TEXT," +
                    "estado TEXT," +
                    "creado_por TEXT," +
                    "activo INTEGER DEFAULT 1" +
                    ")";
            stmt.execute(sqlReserva);
            String sqlOcupacion = "CREATE TABLE IF NOT EXISTS ocupacion (" +
                    "id_ocupacion TEXT PRIMARY KEY," +
                    "id_cliente TEXT," +
                    "patente TEXT," +
                    "id_reserva TEXT," +
                    "fecha_inicio TEXT," +
                    "fecha_fin_estimada TEXT," +
                    "fecha_reserva TEXT," +
                    "fecha_inicio_reserva TEXT," +
                    "estado TEXT," +
                    "creado_por TEXT" +
                    ")";
            stmt.execute(sqlOcupacion);
            String sqlEntrega = "CREATE TABLE IF NOT EXISTS entrega (" +
                    "id_entrega TEXT PRIMARY KEY," +
                    "id_ocupacion TEXT," +
                    "id_empleado TEXT," +
                    "fecha_hora_entrega TEXT," +
                    "kilometraje_salida INTEGER," +
                    "observaciones TEXT" +
                    ")";
            stmt.execute(sqlEntrega);
            String sqlDevolucion = "CREATE TABLE IF NOT EXISTS devolucion (" +
                    "id_devolucion TEXT PRIMARY KEY," +
                    "id_ocupacion TEXT," +
                    "id_empleado TEXT," +
                    "fecha_hora_devolucion TEXT," +
                    "kilometraje_entrada INTEGER," +
                    "danios TEXT," +
                    "observaciones TEXT," +
                    "cargos_extra REAL," +
                    "monto_final REAL" +
                    ")";
            stmt.execute(sqlDevolucion);
            agregarColumnaSiFalta(conexion, "reserva", "creado_por", "TEXT");
            agregarColumnaSiFalta(conexion, "ocupacion", "id_reserva", "TEXT");
            agregarColumnaSiFalta(conexion, "ocupacion", "creado_por", "TEXT");
            agregarColumnaSiFalta(conexion, "ocupacion", "fecha_reserva", "TEXT");
            agregarColumnaSiFalta(conexion, "ocupacion", "fecha_inicio_reserva", "TEXT");
            agregarColumnaSiFalta(conexion, "devolucion", "observaciones", "TEXT");
            agregarColumnaSiFalta(conexion, "cliente", "activo", "INTEGER DEFAULT 1");
            agregarColumnaSiFalta(conexion, "vehiculo", "activo", "INTEGER DEFAULT 1");
            agregarColumnaSiFalta(conexion, "reserva", "activo", "INTEGER DEFAULT 1");
            agregarColumnaSiFalta(conexion, "usuario", "activo", "INTEGER DEFAULT 1");
            inicializarActivos(conexion);
            migrarCreadoPorAUsername(conexion);
            eliminarColumnaSiExiste(conexion, "reserva", "monto_senia");
            eliminarColumnaSiExiste(conexion, "empleado", "fecha_contratacion");
            eliminarEstadoVehiculoReservado(conexion);
        } catch (SQLException excepcion) {
            System.err.println("Error al crear tablas: " + excepcion.getMessage());
        }
    }

    private void migrarCreadoPorAUsername(Connection conexion) {
        String sqlOcupacion = "UPDATE ocupacion SET creado_por = COALESCE(" +
                "(SELECT u.username FROM usuario u WHERE u.username = ocupacion.creado_por LIMIT 1)," +
                "(SELECT u.username FROM usuario u WHERE u.id_usuario = ocupacion.creado_por LIMIT 1)," +
                "(SELECT u.username FROM usuario u JOIN empleado e ON e.id_empleado = u.id_usuario " +
                "WHERE e.legajo = ocupacion.creado_por LIMIT 1)," +
                "ocupacion.creado_por) " +
                "WHERE creado_por IS NOT NULL AND TRIM(creado_por) <> ''";
        String sqlReserva = "UPDATE reserva SET creado_por = COALESCE(" +
                "(SELECT u.username FROM usuario u WHERE u.username = reserva.creado_por LIMIT 1)," +
                "(SELECT u.username FROM usuario u WHERE u.id_usuario = reserva.creado_por LIMIT 1)," +
                "(SELECT u.username FROM usuario u JOIN empleado e ON e.id_empleado = u.id_usuario " +
                "WHERE e.legajo = reserva.creado_por LIMIT 1)," +
                "reserva.creado_por) " +
                "WHERE creado_por IS NOT NULL AND TRIM(creado_por) <> ''";
        try (java.sql.Statement stmt = conexion.createStatement()) {
            stmt.executeUpdate(sqlOcupacion);
            stmt.executeUpdate(sqlReserva);
        } catch (SQLException excepcion) {
            System.err.println("No se pudo migrar creado_por a username: " + excepcion.getMessage());
        }
    }

    private void agregarColumnaSiFalta(Connection conexion, String tabla, String columna, String definicion) throws SQLException {
        if (!existeColumna(conexion, tabla, columna)) {
            try (java.sql.Statement stmt = conexion.createStatement()) {
                stmt.executeUpdate("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
            }
        }
    }

    private void inicializarActivos(Connection conexion) {
        try (java.sql.Statement stmt = conexion.createStatement()) {
            stmt.executeUpdate("UPDATE cliente SET activo = 1 WHERE activo IS NULL");
            stmt.executeUpdate("UPDATE vehiculo SET activo = 1 WHERE activo IS NULL");
            stmt.executeUpdate("UPDATE reserva SET activo = 1 WHERE activo IS NULL");
            stmt.executeUpdate("UPDATE usuario SET activo = 1 WHERE activo IS NULL");
        } catch (SQLException excepcion) {
            System.err.println("No se pudo inicializar el estado activo: " + excepcion.getMessage());
        }
    }

    private void eliminarEstadoVehiculoReservado(Connection conexion) {
        try (java.sql.Statement stmt = conexion.createStatement()) {
            stmt.executeUpdate("UPDATE vehiculo SET estado = 'DISPONIBLE' WHERE estado = 'RESERVADO'");
        } catch (SQLException excepcion) {
            System.err.println("No se pudo eliminar el estado RESERVADO: " + excepcion.getMessage());
        }
    }

    private void eliminarColumnaSiExiste(Connection conexion, String tabla, String columna) {
        if (!existeColumna(conexion, tabla, columna)) {
            return;
        }
        try (java.sql.Statement stmt = conexion.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tabla + " DROP COLUMN " + columna);
        } catch (SQLException excepcion) {
            System.err.println("No se pudo eliminar la columna " + columna + " de " + tabla + ": "
                    + excepcion.getMessage());
        }
    }

    private boolean existeColumna(Connection conexion, String tabla, String columna) {
        try (java.sql.Statement stmt = conexion.createStatement();
             java.sql.ResultSet resultado = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (resultado.next()) {
                if (columna.equalsIgnoreCase(resultado.getString("name"))) {
                    return true;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public void guardarCliente(Cliente cliente) {
        String sql = "INSERT OR REPLACE INTO cliente (id_cliente, nombre, apellido, dni, telefono, email, fecha_registro, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, cliente.getId());
            sentencia.setString(2, cliente.getNombre());
            sentencia.setString(3, cliente.getApellido());
            sentencia.setString(4, cliente.getDni());
            sentencia.setString(5, cliente.getTelefono());
            sentencia.setString(6, cliente.getEmail());
            sentencia.setString(7, cliente.getFechaRegistro() != null ? cliente.getFechaRegistro().toString() : java.time.LocalDate.now().toString());
            sentencia.setBoolean(8, cliente.isActivo());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al guardar cliente en BD: " + excepcion.getMessage());
        }
    }

    public void guardarEmpleado(Empleado empleado) {
        String sql = "INSERT OR REPLACE INTO empleado (id_empleado, nombre, apellido, dni, telefono, email, legajo, sucursal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, empleado.getId());
            sentencia.setString(2, empleado.getNombre());
            sentencia.setString(3, empleado.getApellido());
            sentencia.setString(4, empleado.getDni());
            sentencia.setString(5, empleado.getTelefono());
            sentencia.setString(6, empleado.getEmail());
            sentencia.setString(7, empleado.getLegajo());
            sentencia.setString(8, empleado.getSucursal());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al guardar empleado en BD: " + excepcion.getMessage());
        }
    }

    public void guardarReserva(Reserva reserva) {
        String sql = "INSERT OR IGNORE INTO reserva (id_reserva, id_cliente, patente, fecha_inicio, fecha_fin, fecha_reserva, estado, creado_por, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (reserva.getCliente() == null || reserva.getVehiculo() == null) {
                return;
            }
            sentencia.setString(1, reserva.getIdReserva());
            sentencia.setString(2, reserva.getCliente().getId());
            sentencia.setString(3, reserva.getVehiculo().getPatente());
            sentencia.setString(4, reserva.getFechaInicio() != null ? reserva.getFechaInicio().toString() : null);
            sentencia.setString(5, reserva.getFechaFin() != null ? reserva.getFechaFin().toString() : null);
            sentencia.setString(6, reserva.getFechaReserva() != null ? reserva.getFechaReserva().toString() : java.time.LocalDateTime.now().toString());
            sentencia.setString(7, reserva.getEstado() != null ? reserva.getEstado().name() : null);
            sentencia.setString(8, reserva.getCreadoPor());
            sentencia.setBoolean(9, reserva.isActivo());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al guardar reserva en BD: " + excepcion.getMessage());
        }
    }

    public void actualizarEstadoReserva(String idReserva, String estado) {
        if (idReserva == null || estado == null) return;
        String sql = "UPDATE reserva SET estado = ? WHERE id_reserva = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estado);
            sentencia.setString(2, idReserva);
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al actualizar estado de reserva en BD: " + excepcion.getMessage());
        }
    }

    public void actualizarActivoCliente(String idCliente, boolean activo) {
        actualizarActivo("cliente", "id_cliente", idCliente, activo);
    }

    public void actualizarActivoVehiculo(String patente, boolean activo) {
        actualizarActivo("vehiculo", "patente", patente, activo);
    }

    public void actualizarActivoReserva(String idReserva, boolean activo) {
        actualizarActivo("reserva", "id_reserva", idReserva, activo);
    }

    public void actualizarActivoUsuario(String idUsuario, boolean activo) {
        actualizarActivo("usuario", "id_usuario", idUsuario, activo);
    }

    private void actualizarActivo(String tabla, String columnaId, String id, boolean activo) {
        if (id == null) return;
        String sql = "UPDATE " + tabla + " SET activo = ? WHERE " + columnaId + " = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBoolean(1, activo);
            sentencia.setString(2, id);
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al actualizar estado activo de " + tabla + ": " + excepcion.getMessage());
        }
    }

    public static void eliminarReserva(String idReserva) {
        if (idReserva == null) return;
        String sql = "DELETE FROM reserva WHERE id_reserva = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idReserva);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error al eliminar reserva: " + e.getMessage());
        }
    }


    public void guardarUsuario(Usuario usuario) throws BaseDatosException {
        String sql = "INSERT INTO usuario (id_usuario, username, password_hash, rol, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario.getIdUsuario());
            sentencia.setString(2, usuario.getUsername());
            sentencia.setString(3, usuario.getPasswordHash());
            sentencia.setString(4, usuario.getRol().name());
            sentencia.setBoolean(5, usuario.isActivo());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            throw new BaseDatosException("Error al guardar usuario", excepcion);
        }
    }

    public Usuario leerUsuarioPorId(String idUsuario) throws BaseDatosException {
        String sql = "SELECT * FROM usuario WHERE id_usuario = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    return mapearUsuario(resultado);
                }
            }
        } catch (SQLException excepcion) {
            throw new BaseDatosException("Error al cargar usuario", excepcion);
        }
        return null;
    }

    public Usuario leerUsuarioPorUsername(String nombreUsuario) throws BaseDatosException {
        String sql = "SELECT * FROM usuario WHERE username = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombreUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    return mapearUsuario(resultado);
                }
            }
        } catch (SQLException excepcion) {
            throw new BaseDatosException("Error al cargar usuario por username", excepcion);
        }
        return null;
    }

    public ArrayList<Usuario> leerUsuarios() throws BaseDatosException {
        ArrayList<Usuario> listaUsuarios = new ArrayList<Usuario>();
        String sql = "SELECT * FROM usuario";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                listaUsuarios.add(mapearUsuario(resultado));
            }
        } catch (SQLException excepcion) {
            throw new BaseDatosException("Error al cargar usuarios", excepcion);
        }
        return listaUsuarios;
    }


    private Usuario mapearUsuario(ResultSet resultado) throws SQLException {
        return new Usuario(
                resultado.getString("id_usuario"),
                resultado.getString("username"),
                resultado.getString("password_hash"),
                RolUsuario.valueOf(resultado.getString("rol")),
                resultado.getBoolean("activo"),
                null
        );
    }

    public void guardarOcupacion(Ocupacion ocupacion) {
        String sql = "INSERT OR REPLACE INTO ocupacion (id_ocupacion, id_cliente, patente, id_reserva, fecha_inicio, fecha_fin_estimada, fecha_reserva, fecha_inicio_reserva, estado, creado_por) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (ocupacion.getCliente() == null || ocupacion.getVehiculo() == null) {
                return;
            }
            sentencia.setString(1, ocupacion.getIdOcupacion());
            sentencia.setString(2, ocupacion.getCliente().getId());
            sentencia.setString(3, ocupacion.getVehiculo().getPatente());
            sentencia.setString(4, ocupacion.getReserva() != null ? ocupacion.getReserva().getIdReserva() : null);
            sentencia.setString(5, ocupacion.getFechaInicio() != null ? ocupacion.getFechaInicio().toString() : java.time.LocalDateTime.now().toString());
            sentencia.setString(6, ocupacion.getFechaFinEstimada() != null ? ocupacion.getFechaFinEstimada().toString() : null);
            sentencia.setString(7, ocupacion.getFechaReserva() != null ? ocupacion.getFechaReserva().toString() : null);
            sentencia.setString(8, ocupacion.getFechaInicioReserva() != null ? ocupacion.getFechaInicioReserva().toString() : null);
            sentencia.setString(9, ocupacion.getEstado() != null ? ocupacion.getEstado().name() : null);
            sentencia.setString(10, ocupacion.getCreadoPor());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al guardar ocupacion en BD: " + excepcion.getMessage());
        }
    }

    public void guardarEntrega(Entrega entrega) {
        String sql = "INSERT OR REPLACE INTO entrega (id_entrega, id_ocupacion, id_empleado, fecha_hora_entrega, kilometraje_salida, observaciones) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (entrega.getOcupacion() == null || entrega.getEmpleado() == null) {
                return;
            }
            sentencia.setString(1, entrega.getIdEntrega());
            sentencia.setString(2, entrega.getOcupacion().getIdOcupacion());
            sentencia.setString(3, entrega.getEmpleado().getId());
            sentencia.setString(4, entrega.getFechaHoraEntrega() != null ? entrega.getFechaHoraEntrega().toString() : java.time.LocalDateTime.now().toString());
            sentencia.setInt(5, entrega.getKilometrajeSalida());
            sentencia.setString(6, entrega.getObservaciones());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al guardar entrega en BD: " + excepcion.getMessage());
        }
    }

    public void guardarDevolucion(Devolucion devolucion) {
        String sql = "INSERT OR REPLACE INTO devolucion (id_devolucion, id_ocupacion, id_empleado, fecha_hora_devolucion, kilometraje_entrada, danios, observaciones, cargos_extra, monto_final) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (devolucion.getOcupacion() == null || devolucion.getEmpleado() == null) {
                return;
            }
            sentencia.setString(1, devolucion.getIdDevolucion());
            sentencia.setString(2, devolucion.getOcupacion().getIdOcupacion());
            sentencia.setString(3, devolucion.getEmpleado().getId());
            sentencia.setString(4, devolucion.getFechaHoraDevolucion() != null ? devolucion.getFechaHoraDevolucion().toString() : java.time.LocalDateTime.now().toString());
            sentencia.setInt(5, devolucion.getKilometrajeEntrada());
            sentencia.setString(6, devolucion.getDanios());
            sentencia.setString(7, devolucion.getObservaciones());
            sentencia.setDouble(8, devolucion.getCargosExtra());
            sentencia.setDouble(9, devolucion.getMontoFinal());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al guardar devolucion en BD: " + excepcion.getMessage());
        }
    }


    public java.util.ArrayList<model.Cliente> leerClientes() {
        java.util.ArrayList<model.Cliente> lista = new java.util.ArrayList<model.Cliente>();
        String sql = "SELECT * FROM cliente";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql);
             java.sql.ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                java.time.LocalDate fecha = java.time.LocalDate.now();
                try {
                    if (resultado.getString("fecha_registro") != null) {
                        fecha = java.time.LocalDate.parse(resultado.getString("fecha_registro"));
                    }
                } catch (Exception ignored) {
                }
                model.Cliente cliente = new model.Cliente(
                        resultado.getString("id_cliente"),
                        resultado.getString("nombre"),
                        resultado.getString("apellido"),
                        resultado.getString("dni"),
                        resultado.getString("telefono"),
                        resultado.getString("email"),
                        fecha
                );
                cliente.setActivo(resultado.getInt("activo") != 0);
                lista.add(cliente);
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar clientes: " + excepcion.getMessage());
        }
        return lista;
    }

    public void guardarVehiculo(Vehiculo vehiculo) throws BaseDatosException {
        String sql = "INSERT INTO vehiculo (patente, marca, modelo, anio, kilometraje, estado, precio_dia, tarifa_seguro, tipo, datos_especificos, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, vehiculo.getPatente());
            sentencia.setString(2, vehiculo.getMarca());
            sentencia.setString(3, vehiculo.getModelo());
            sentencia.setInt(4, vehiculo.getAnio());
            sentencia.setInt(5, vehiculo.getKilometraje());
            sentencia.setString(6, vehiculo.getEstado().name());
            sentencia.setDouble(7, vehiculo.getPrecioDia());
            sentencia.setDouble(8, vehiculo.getTarifaSeguro());
            sentencia.setString(9, tipoDe(vehiculo));
            sentencia.setString(10, datosEspecificosDe(vehiculo));
            sentencia.setBoolean(11, vehiculo.isActivo());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            throw new BaseDatosException("Error al guardar vehiculo", excepcion);
        }
    }

    public Vehiculo leerVehiculoPorPatente(String patente) throws BaseDatosException {
        String sql = "SELECT * FROM vehiculo WHERE patente = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, patente);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    return mapearVehiculo(resultado);
                }
            }
        } catch (SQLException excepcion) {
            throw new BaseDatosException("Error al cargar vehiculo", excepcion);
        }
        return null;
    }

    public void actualizarVehiculo(Vehiculo vehiculo) throws BaseDatosException {
        String sql = "UPDATE vehiculo SET marca = ?, modelo = ?, anio = ?, kilometraje = ?, estado = ?, precio_dia = ?, tarifa_seguro = ?, tipo = ?, datos_especificos = ?, activo = ? WHERE patente = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, vehiculo.getMarca());
            sentencia.setString(2, vehiculo.getModelo());
            sentencia.setInt(3, vehiculo.getAnio());
            sentencia.setInt(4, vehiculo.getKilometraje());
            sentencia.setString(5, vehiculo.getEstado().name());
            sentencia.setDouble(6, vehiculo.getPrecioDia());
            sentencia.setDouble(7, vehiculo.getTarifaSeguro());
            sentencia.setString(8, tipoDe(vehiculo));
            sentencia.setString(9, datosEspecificosDe(vehiculo));
            sentencia.setBoolean(10, vehiculo.isActivo());
            sentencia.setString(11, vehiculo.getPatente());
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            throw new BaseDatosException("Error al actualizar vehiculo", excepcion);
        }
    }

    public ArrayList<Vehiculo> leerVehiculos() {
        ArrayList<Vehiculo> listaVehiculos = new ArrayList<Vehiculo>();
        String sql = "SELECT * FROM vehiculo";
        try (Connection conexion = ConnectionFactory.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                Vehiculo vehiculo = mapearVehiculo(resultado);
                if (vehiculo != null) {
                    listaVehiculos.add(vehiculo);
                }
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar vehiculos: " + excepcion.getMessage());
        }
        return listaVehiculos;
    }

    private static String tipoDe(Vehiculo vehiculo) {
        return vehiculo.getClass().getSimpleName().toUpperCase();
    }

    private static String datosEspecificosDe(Vehiculo vehiculo) {
        if (vehiculo instanceof Automovil) {
            Automovil automovil = (Automovil) vehiculo;
            return automovil.getCantidadPuertas() + "|" + automovil.getTransmision() + "|" + automovil.getKilometraje();
        }
        return null;
    }

    private Vehiculo mapearVehiculo(ResultSet resultado) throws SQLException {
        String tipo = resultado.getString("tipo");
        if (tipo != null && !"AUTOMOVIL".equalsIgnoreCase(tipo)) {
            return null;
        }
        String patente = resultado.getString("patente");
        String marca = resultado.getString("marca");
        String modelo = resultado.getString("modelo");
        int anio = resultado.getInt("anio");
        int kilometraje = resultado.getInt("kilometraje");
        EstadoVehiculo estado = EstadoVehiculo.DISPONIBLE;
        try {
            estado = EstadoVehiculo.valueOf(resultado.getString("estado"));
        } catch (Exception ignored) {
        }
        double precioDia = resultado.getDouble("precio_dia");
        double tarifaSeguro = resultado.getDouble("tarifa_seguro");
        int cantidadPuertas = Automovil.CANTIDAD_PUERTAS_POR_DEFECTO;
        String transmision = Automovil.TRANSMISION_POR_DEFECTO;
        String datosEspecificos = resultado.getString("datos_especificos");
        if (datosEspecificos != null && !datosEspecificos.trim().isEmpty()) {
            String[] partes = datosEspecificos.split("\\|");
            if (partes.length >= 2) {
                try {
                    cantidadPuertas = Integer.parseInt(partes[0].trim());
                } catch (NumberFormatException ignored) {
                }
                transmision = partes[1].trim();
            } else {
                transmision = partes[0].trim();
            }
        }
        if ("AUTOMATICO".equalsIgnoreCase(transmision)) {
            transmision = Automovil.TRANSMISION_AUTOMATICA;
        }
        Automovil automovil = new Automovil(patente, marca, modelo, anio, kilometraje, estado, precioDia,
                tarifaSeguro, cantidadPuertas, transmision);
        automovil.setActivo(resultado.getInt("activo") != 0);
        return automovil;
    }

    public java.util.ArrayList<model.Empleado> leerEmpleados() {
        java.util.ArrayList<model.Empleado> lista = new java.util.ArrayList<model.Empleado>();
        String sql = "SELECT * FROM empleado";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql);
             java.sql.ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                lista.add(mapearEmpleado(resultado));
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar empleados: " + excepcion.getMessage());
        }
        return lista;
    }

    public model.Empleado leerEmpleadoPorId(String idEmpleado) {
        if (idEmpleado == null) {
            return null;
        }
        String sql = "SELECT * FROM empleado WHERE id_empleado = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, idEmpleado);
            try (java.sql.ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    return mapearEmpleado(resultado);
                }
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar empleado: " + excepcion.getMessage());
        }
        return null;
    }

    private model.Empleado mapearEmpleado(java.sql.ResultSet resultado) throws SQLException {
        return new model.Empleado(
                resultado.getString("id_empleado"),
                resultado.getString("nombre"),
                resultado.getString("apellido"),
                resultado.getString("dni"),
                resultado.getString("telefono"),
                resultado.getString("email"),
                resultado.getString("legajo"),
                resultado.getString("sucursal")
        );
    }


    public java.util.ArrayList<model.Reserva> leerReservas(java.util.ArrayList<model.Cliente> listaClientes, java.util.ArrayList<model.Vehiculo> listaVehiculos) {
        java.util.ArrayList<model.Reserva> lista = new java.util.ArrayList<model.Reserva>();
        String sql = "SELECT * FROM reserva";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql);
             java.sql.ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                model.Cliente clienteReserva = null;
                model.Vehiculo vehiculoReserva = null;
                String idCliente = resultado.getString("id_cliente");
                String patente = resultado.getString("patente");
                for (model.Cliente c : listaClientes) {
                    if (c.getId().equals(idCliente)) {
                        clienteReserva = c;
                        break;
                    }
                }
                for (model.Vehiculo v : listaVehiculos) {
                    if (v.getPatente().equals(patente)) {
                        vehiculoReserva = v;
                        break;
                    }
                }
                if (clienteReserva == null || vehiculoReserva == null) {
                    continue;
                }
                java.time.LocalDate inicio = java.time.LocalDate.now();
                java.time.LocalDate fin = java.time.LocalDate.now();
                try {
                    if (resultado.getString("fecha_inicio") != null) inicio = java.time.LocalDate.parse(resultado.getString("fecha_inicio"));
                    if (resultado.getString("fecha_fin") != null) fin = java.time.LocalDate.parse(resultado.getString("fecha_fin"));
                } catch (Exception ignored) {
                }
                enums.EstadoReserva estado = enums.EstadoReserva.PENDIENTE;
                try {
                    if (resultado.getString("estado") != null) estado = enums.EstadoReserva.valueOf(resultado.getString("estado"));
                } catch (Exception ignored) {
                }
                java.time.LocalDateTime fechaReserva = parseFechaHora(resultado.getString("fecha_reserva"), java.time.LocalDateTime.now());
                String creadoPor = resultado.getString("creado_por");
                model.Reserva reserva = new model.Reserva(
                        resultado.getString("id_reserva"),
                        clienteReserva,
                        vehiculoReserva,
                        inicio,
                        fin,
                        fechaReserva,
                        estado,
                        creadoPor != null ? creadoPor : ""
                );
                reserva.setActivo(resultado.getInt("activo") != 0);
                lista.add(reserva);
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar reservas: " + excepcion.getMessage());
        }
        return lista;
    }

    public java.util.ArrayList<model.Ocupacion> cargarOcupaciones(java.util.ArrayList<model.Cliente> listaClientes,
                                                                  java.util.ArrayList<model.Vehiculo> listaVehiculos,
                                                                  java.util.ArrayList<model.Empleado> listaEmpleados,
                                                                  java.util.ArrayList<model.Reserva> listaReservas) {
        java.util.ArrayList<model.Ocupacion> lista = new java.util.ArrayList<model.Ocupacion>();
        java.util.ArrayList<String> ocupacionesAFinalizar = new java.util.ArrayList<String>();
        String sql = "SELECT * FROM ocupacion";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql);
             java.sql.ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                model.Cliente cliente = buscarCliente(listaClientes, resultado.getString("id_cliente"));
                model.Vehiculo vehiculo = buscarVehiculo(listaVehiculos, resultado.getString("patente"));
                if (cliente == null || vehiculo == null) {
                    continue;
                }
                model.Reserva reserva = buscarReserva(listaReservas, resultado.getString("id_reserva"));
                java.time.LocalDateTime fechaInicio = parseFechaHora(resultado.getString("fecha_inicio"), java.time.LocalDateTime.now());
                java.time.LocalDateTime fechaFinEstimada = parseFechaHora(resultado.getString("fecha_fin_estimada"), null);
                java.time.LocalDateTime fechaReservaOcupacion = parseFechaHora(resultado.getString("fecha_reserva"), null);
                java.time.LocalDate fechaInicioReserva = null;
                try {
                    String fechaInicioReservaTexto = resultado.getString("fecha_inicio_reserva");
                    if (fechaInicioReservaTexto != null && !fechaInicioReservaTexto.trim().isEmpty()) {
                        fechaInicioReserva = java.time.LocalDate.parse(fechaInicioReservaTexto);
                    }
                } catch (Exception ignored) {
                }
                String creadoPor = resultado.getString("creado_por");
                model.Ocupacion ocupacion = new model.Ocupacion(
                        resultado.getString("id_ocupacion"),
                        cliente,
                        vehiculo,
                        reserva,
                        fechaInicio,
                        fechaFinEstimada,
                        creadoPor != null ? creadoPor : ""
                );
                ocupacion.registrarDatosReserva(fechaReservaOcupacion, fechaInicioReserva);
                String estadoGuardado = resultado.getString("estado");
                try {
                    ocupacion.setEstado(enums.EstadoOcupacion.valueOf(estadoGuardado));
                } catch (Exception ignored) {
                }
                model.Entrega entrega = leerEntrega(ocupacion, listaEmpleados);
                if (entrega != null) {
                    ocupacion.setEntrega(entrega);
                }
                model.Devolucion devolucion = leerDevolucion(ocupacion, listaEmpleados);
                if (devolucion != null) {
                    ocupacion.cerrar(devolucion);
                    if (!enums.EstadoOcupacion.FINALIZADA.name().equals(estadoGuardado)) {
                        ocupacionesAFinalizar.add(ocupacion.getIdOcupacion());
                    }
                }
                lista.add(ocupacion);
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar ocupaciones: " + excepcion.getMessage());
        }
        for (String idOcupacion : ocupacionesAFinalizar) {
            actualizarEstadoOcupacion(idOcupacion, enums.EstadoOcupacion.FINALIZADA.name());
        }
        return lista;
    }

    private void actualizarEstadoOcupacion(String idOcupacion, String estado) {
        String sql = "UPDATE ocupacion SET estado = ? WHERE id_ocupacion = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estado);
            sentencia.setString(2, idOcupacion);
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            System.err.println("Error al actualizar estado de ocupacion: " + excepcion.getMessage());
        }
    }

    private model.Entrega leerEntrega(model.Ocupacion ocupacion, java.util.ArrayList<model.Empleado> listaEmpleados) {
        String sql = "SELECT * FROM entrega WHERE id_ocupacion = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, ocupacion.getIdOcupacion());
            try (java.sql.ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    model.Empleado empleado = buscarEmpleado(listaEmpleados, resultado.getString("id_empleado"));
                    return new model.Entrega(
                            resultado.getString("id_entrega"),
                            ocupacion,
                            empleado,
                            parseFechaHora(resultado.getString("fecha_hora_entrega"), null),
                            resultado.getInt("kilometraje_salida"),
                            resultado.getString("observaciones")
                    );
                }
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar entrega: " + excepcion.getMessage());
        }
        return null;
    }

    private model.Devolucion leerDevolucion(model.Ocupacion ocupacion, java.util.ArrayList<model.Empleado> listaEmpleados) {
        String sql = "SELECT * FROM devolucion WHERE id_ocupacion = ?";
        try (Connection conexion = ConnectionFactory.getConnection();
             java.sql.PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, ocupacion.getIdOcupacion());
            try (java.sql.ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    model.Empleado empleado = buscarEmpleado(listaEmpleados, resultado.getString("id_empleado"));
                    model.Devolucion devolucion = new model.Devolucion(
                            resultado.getString("id_devolucion"),
                            ocupacion,
                            empleado,
                            parseFechaHora(resultado.getString("fecha_hora_devolucion"), null),
                            resultado.getInt("kilometraje_entrada"),
                            resultado.getString("danios"),
                            resultado.getDouble("cargos_extra"),
                            resultado.getString("observaciones")
                    );
                    devolucion.setMontoFinal(resultado.getDouble("monto_final"));
                    return devolucion;
                }
            }
        } catch (SQLException excepcion) {
            System.err.println("Error al cargar devolucion: " + excepcion.getMessage());
        }
        return null;
    }

    private model.Cliente buscarCliente(java.util.List<model.Cliente> lista, String id) {
        if (id == null) return null;
        for (model.Cliente cliente : lista) {
            if (cliente != null && id.equals(cliente.getId())) return cliente;
        }
        return null;
    }

    private model.Vehiculo buscarVehiculo(java.util.List<model.Vehiculo> lista, String patente) {
        if (patente == null) return null;
        for (model.Vehiculo vehiculo : lista) {
            if (vehiculo != null && patente.equals(vehiculo.getPatente())) return vehiculo;
        }
        return null;
    }

    private model.Empleado buscarEmpleado(java.util.List<model.Empleado> lista, String id) {
        if (id == null) return null;
        for (model.Empleado empleado : lista) {
            if (empleado != null && id.equals(empleado.getId())) return empleado;
        }
        return null;
    }

    private model.Reserva buscarReserva(java.util.List<model.Reserva> lista, String id) {
        if (id == null) return null;
        for (model.Reserva reserva : lista) {
            if (reserva != null && id.equals(reserva.getIdReserva())) return reserva;
        }
        return null;
    }

    private java.time.LocalDateTime parseFechaHora(String valor, java.time.LocalDateTime porDefecto) {
        if (valor == null || valor.trim().isEmpty()) return porDefecto;
        try {
            return java.time.LocalDateTime.parse(valor);
        } catch (Exception excepcion) {
            return porDefecto;
        }
    }
}
