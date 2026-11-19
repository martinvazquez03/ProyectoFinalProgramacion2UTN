package model;

import enums.RolUsuario;

public class Usuario {
    private String idUsuario;
    private String username;
    private String passwordHash;
    private RolUsuario rol;
    private boolean activo;
    private Empleado empleado;
    private Cliente cliente;

    public Usuario(String idUsuario, String username, String passwordHash, RolUsuario rol, boolean activo, Empleado empleado) {
        this(idUsuario, username, passwordHash, rol, activo, empleado, null);
    }

    public Usuario(String idUsuario, String username, String passwordHash, RolUsuario rol, boolean activo,
                   Empleado empleado, Cliente cliente) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
        this.empleado = empleado;
        this.cliente = cliente;
    }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    @Override
    public String toString() {
        String datosPersona = "";
        if (empleado != null) {
            datosPersona = " | Empleado: " + empleado.getNombreCompleto();
        } else if (cliente != null) {
            datosPersona = " | Cliente: " + cliente.getNombreCompleto();
        }
        return "Usuario: " + username + " | Rol: " + rol + " | Activo: " + activo + datosPersona;
    }
}
