package model;

import interfaces.Alquilable;
import enums.EstadoVehiculo;

public abstract class Vehiculo implements Alquilable {
    protected String patente;
    protected String marca;
    protected String modelo;
    protected int anio;
    protected int kilometraje;
    protected EstadoVehiculo estado;
    protected double precioDia;
    protected double tarifaSeguro;

    public Vehiculo(String patente, String marca, String modelo, int anio, int kilometraje,
                    EstadoVehiculo estado, double precioDia, double tarifaSeguro) {
        this.patente = patente;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.kilometraje = kilometraje;
        this.estado = estado;
        this.precioDia = precioDia;
        this.tarifaSeguro = tarifaSeguro;
    }

    public boolean estaDisponible() {
        return estado == EstadoVehiculo.DISPONIBLE;
    }

    public abstract double calcularCosto(int dias);

    @Override
    public boolean requiereSeguro() {
        return true;
    }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public int getKilometraje() { return kilometraje; }
    public void setKilometraje(int kilometraje) { this.kilometraje = kilometraje; }
    public EstadoVehiculo getEstado() { return estado; }
    public void setEstado(EstadoVehiculo estado) { this.estado = estado; }
    public double getPrecioDia() { return precioDia; }
    public void setPrecioDia(double precioDia) { this.precioDia = precioDia; }
    public double getTarifaSeguro() { return tarifaSeguro; }
    public void setTarifaSeguro(double tarifaSeguro) { this.tarifaSeguro = tarifaSeguro; }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " | Patente: " + patente + " | " + marca + " " + modelo + " | Año: " + anio + " | Km: " + kilometraje + " | Estado: " + estado + " | $/día: " + precioDia;
    }
}
