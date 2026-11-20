# TP Final - Sistema de Alquiler de Vehículos (Programación II - UTN Mar del Plata)

> **Integrante:** Martin Vazquez Perez  
> **Tecnologías:** Java, JDBC, SQLite, Git/GitHub

## 1. Objetivo

Desarrollar un sistema para la administración de una empresa de alquiler de vehículos que permita gestionar autos, conductores/clientes, reservas, entregas, devoluciones, usuarios del sistema y persistencia en base de datos, cumpliendo con todos los pilares y conceptos obligatorios de la materia.

## 1.1 Arquitectura del Sistema

- **Capa Envolvente:** `service/SistemaAlquiler.java` contiene toda la lógica de negocio. Mantiene el encapsulamiento del sistema.
- **Main:** `src/Main.java` únicamente se encarga del menú con `switch`, la entrada de datos por consola y la impresión de lo que retorna `SistemaAlquiler`. Todas las funcionalidades retornan `String`, no realizan `System.out.println` dentro de la lógica.
- **Persistencia:** Toda la lógica SQL está centralizada en `dao/GestorBaseDatos.java`. `SistemaAlquiler` carga la BD en repositorios genéricos y desde ese momento lee de ellos; al crear o modificar datos, primero actualiza el repositorio y luego persiste con `GestorBaseDatos`.
- **Inicializacion:** al arrancar se crean las tablas con `CREATE TABLE IF NOT EXISTS` y se migran columnas faltantes. Se crea `mariano`/`1234` (ADMINISTRADOR) con su `Empleado` si no existe. Se cargan clientes, vehiculos, empleados, reservas, ocupaciones y usuarios en repositorios genéricos.
- **Solo automoviles:** el sistema trabaja unicamente con la clase `Automovil` (no existen camionetas ni furgones).
- **Roles:** `ADMINISTRADOR`, `EMPLEADO`, `CLIENTE` (CONDUCTOR reemplazado por CLIENTE).

## 2. Requisitos Obligatorios a Cumplir

| Concepto | Estado | Implementación |
| --- | --- | --- |
| Diagrama UML | Obligatorio | `DIAGRAMA_UML.txt` (actualizado) |
| Los 4 pilares POO (Herencia, Polimorfismo, Abstracción, Encapsulamiento) | Obligatorio | Ver secciones 4, 5, 6 |
| Mínimo 8 clases (sin contar Main, interfaces ni excepciones) | Obligatorio | Cumplido (Persona, Vehiculo, Cliente, Empleado, Usuario, Reserva, Ocupacion, Entrega, Devolucion, etc.) |
| Mínimo 1 Clase Abstracta, 1 Interfaz, 1 Enumeración | Obligatorio | `Persona`, `Vehiculo` (abstractas) / `Alquilable`, `Auditable` (interfaces) / 4 enumeraciones |
| Al menos 2 Colecciones distintas | Obligatorio | `ArrayList<T>`, `LinkedHashMap<K,V>` (preserva orden de carga) |
| Al menos 2 Excepciones personalizadas | Obligatorio | `VehiculoNoDisponibleException`, `ReservaInvalidaException`, `UsuarioNoAutorizadoException`, `BaseDatosException` |
| Al menos 1 Clase Genérica y 1 Interfaz/Método genérico | Obligatorio | `RepositorioGenerico<T,ID>` con método genérico `<R> mapear(Function<T,R>)` |
| JDBC para persistencia | Obligatorio | Capa `dao/` con `ConnectionFactory` y SQLite |
| Git/GitHub + commits de todos los integrantes | Obligatorio | Flujo de trabajo colaborativo |

## 3. Actores y Tipos de Usuario

| Tipo de Usuario | Alcance | Descripción |
| --- | --- | --- |
| `ADMINISTRADOR` | Total | ABM de usuarios, limpieza de BD, eliminacion de datos, reportes/listados y operacion completa. |
| `EMPLEADO` | Operativo | Reservas, entregas, devoluciones, alta de clientes y vehiculos, consulta de flota. No crea ADMIN ni limpia la BD. |
| `CLIENTE` | Restringido | Ver automoviles disponibles y gestionar sus reservas (crear/cancelar solo las propias); el cliente se toma del usuario logueado. |


Ver `enums/RolUsuario.java`. Los tres roles inician sesion; CLIENTE solo accede a disponibilidad y gestion de sus reservas.

## 4. Modelo de Dominio (Clases)

> Todas las entidades aplican **encapsulamiento** (atributos `private` con `getters/setters`). Se utiliza **herencia** y **polimorfismo** donde corresponda.

| Clase | Tipo | Responsabilidad | Atributos clave                                                                                                                                                              | Métodos relevantes |
| --- | --- | --- |------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `Persona` | Clase abstracta (ver 5) | Superclase base para personas del sistema. | `id`, `nombre`, `apellido`, `dni`, `telefono`, `email`                                                                                                                       | `getNombreCompleto()` (abstracto o concreto), validaciones base. |
| `Cliente` | Concreta (hereda `Persona`) | Persona que alquila vehiculos. Lleva historial. | `fechaRegistro`, `historialOcupaciones` (`ArrayList<Ocupacion>`) | `agregarOcupacion(Ocupacion)`, `getHistorialOcupaciones()`, `getIdentificacion()` |
| `Empleado` | Concreta (hereda `Persona`) | Usuario operativo del sistema. | `legajo`, `sucursal` | `getIdentificacion()` |
| `Usuario` | Concreta | Autenticacion/autorizacion por rol. | `idUsuario`, `username`, `passwordHash`, `rol` (`RolUsuario`), `empleado` (`Empleado`), `cliente` (`Cliente`), `activo` | `getRol()`, `isActivo()`, `getEmpleado()`, `getCliente()` |
| `Vehiculo` | Clase abstracta (ver 5) | Superclase de la flota. | `patente`, `marca`, `modelo`, `anio`, `kilometraje`, `estado` (`EstadoVehiculo`), `precioDia`, `tarifaSeguro`                                                                | `calcularCosto(int dias)` (abstracto), `estaDisponible()` (concreto/polimórfico) |
| `Automovil` | Concreta (hereda `Vehiculo`) | Vehículo tipo auto. | `cantidadPuertas`, `transmision`                                                                                                                          | `calcularCosto(int dias)` (sobreescrito, aplica recargos) |
| `Reserva` | Concreta | Reserva futura de un vehículo. La seña se deriva de la tarifa de seguro del vehículo. `creadoPor` guarda el username del usuario que la creó. | `idReserva`, `cliente`, `vehiculo`, `fechaInicio`, `fechaFin`, `fechaReserva`, `estado` (`EstadoReserva`), `creadoPor` | `validar()`, `confirmar()`, `cancelar()`, `calcularMontoTotal()`, `getMontoSenia()` |
| `Entrega` | Concreta | Registro de entrega del vehiculo al cliente. `fechaHoraEntrega` se toma de `Reserva.fechaInicio` asignada por el usuario. | `idEntrega`, `ocupacion`, `empleado`, `fechaHoraEntrega`, `kilometrajeSalida`, `observaciones` | `generarComprobante()`, `validarDatos()` |
| `Devolucion` | Concreta | Registro de devolución. Las observaciones se cargan al devolver el vehículo. | `idDevolucion`, `ocupacion`, `empleado`, `fechaHoraDevolucion`, `kilometrajeEntrada`, `danios`, `observaciones`, `cargosExtra`, `montoFinal`                            | `calcularCargos()`, `aplicarMultas()`, `cerrarOcupacion()` |
| `Ocupacion` | Concreta | Periodo activo en el que el cliente tiene el vehiculo (contrato de alquiler). Al entregar, la reserva se elimina de la BD y de `repoReservas`, pero la ocupación conserva una instantánea de sus fechas para el historial. `creadoPor` guarda el username del empleado que realizó la entrega. La fecha de fin real se obtiene de la devolución. | `idOcupacion`, `cliente`, `vehiculo`, `reserva` (nullable), `entrega`, `devolucion` (nullable), `fechaInicio`, `fechaFinEstimada`, `fechaReserva`, `fechaInicioReserva`, `estado` (`EstadoOcupacion`), `creadoPor` | `estaActiva()`, `calcularDias()`, `cerrar(Devolucion)`, `getFechaReserva()`, `getFechaInicioReserva()`, `getFechaFinReserva()`, `getFechaFinReal()` |

> **Nota:** `Main.java` solo contiene el punto de entrada. La logica se reparte entre `service/SistemaAlquiler`, `service/impl/ServicioAlquilerImpl`, `dao/` y `util/`.

## 5. Clases Abstractas y Herencia/Polimorfismo

### `abstract class Persona`
```java
public abstract class Persona {
  protected String id;
  protected String nombre;
  protected String apellido;
  // ...
  public abstract String getIdentificacion(); // Polimórfico
  public String getNombreCompleto() { return nombre + " " + apellido; }
}
```
- **Herencia:** `Cliente`, `Empleado` extienden `Persona`.
- **Polimorfismo:** `getIdentificacion()` se implementa distinto (ej. muestra `DNI: ...` o `LEGAJO: ...`).

### `abstract class Vehiculo`
```java
public abstract class Vehiculo implements Alquilable { // ver interfaz
  protected String patente;
  // ...
  public abstract double calcularCosto(int dias); // Obligatorio implementar en subclases
  public boolean estaDisponible() { return estado == EstadoVehiculo.DISPONIBLE; }
}
```
- **Polimorfismo en tiempo de ejecucion:** al calcular costo desde una referencia `Vehiculo`, se ejecuta `Automovil.calcularCosto()`.

## 6. Interfaces (Abstracción)

| Interfaz | Método(s) | Implementado por | Descripción |
| --- | --- | --- | --- |
| `Alquilable` | `double calcularCosto(int dias)`, `boolean requiereSeguro()` | `Vehiculo` (abstracta) e implícitamente por sus subclases | Contrato para objetos que pueden ser alquilados. Permite abstraer la lógica de tarifación. |
| `Auditable` | `LocalDateTime getFechaCreacion()`, `String getCreadoPor()`, `void marcarModificacion(String usuario)` | `Reserva`, `Ocupacion` | Estandariza auditoria de entidades. |

Ejemplo:
```java
public interface Alquilable {
    double calcularCosto(int dias);
    boolean requiereSeguro();
}
```

## 7. Enumeraciones

| Enum | Valores | Uso |
| --- | --- | --- |
| `RolUsuario` | `ADMINISTRADOR`, `EMPLEADO`, `CLIENTE` | Control de acceso por rol. |
| `EstadoVehiculo` | `DISPONIBLE`, `ALQUILADO`, `EN_LIMPIEZA`, `EN_REPARACION`, `FUERA_SERVICIO` | Estado operativo de la flota. Las reservas no cambian el estado del vehículo; se controlan por solapamiento de fechas. |
| `EstadoReserva` | `PENDIENTE`, `CONFIRMADA`, `CANCELADA`, `CONCRETADA` | Ciclo de vida de una reserva. |
| `EstadoOcupacion` | `ACTIVA`, `FINALIZADA`, `CANCELADA` | Control de alquileres vigentes/históricos. |

## 8. Colecciones Utilizadas

| Colección | Uso en el sistema | Justificación |
| --- | --- | --- |
| `ArrayList<T>` | `Cliente.historialOcupaciones`, listados de vehículos/reservas/ocupaciones, resultados de búsquedas. | Ordenada, permite duplicados, acceso por índice y recorrido eficiente para listados. |
| `LinkedHashMap<K,V>` | `RepositorioGenerico.cache` para clientes, vehículos, reservas, empleados, ocupaciones y usuarios. | Búsqueda por clave O(1) promedio y orden de inserción estable para listados y flota. |


## 9. Diagrama UML (Requerido)

Diagrama cuenta con:

- Clases, atributos, métodos y visibilidad (`+ - #`)
- Relaciones: Herencia (`Generalization`), Implementación (`Realization`), Asociación, Composición/Agregación


## 10. Manejo de Errores - Excepciones Personalizadas

Todas heredan de `Exception` (checked) o `RuntimeException` (unchecked) según corresponda. Ubicadas en paquete `exception/`.

| Excepción | Tipo | Cuándo se lanza | Manejo esperado |
| --- | --- | --- | --- |
| `VehiculoNoDisponibleException` | Checked/Unchecked | Intentar alquilar/reservar un vehículo con estado distinto a `DISPONIBLE` o ya ocupado. | Capturar en `ServicioAlquiler`, informar al usuario, loggear. |
| `ReservaInvalidaException` | Checked | Fechas invalidas (inicio > fin), solapamiento de reservas, reserva ya concretada/cancelada o cliente con ocupacion activa. | Validacion en `Reserva.validar()` y `SistemaAlquiler.crearReserva()`/`realizarEntregaPorIndice()`. |
| `UsuarioNoAutorizadoException` | Unchecked | Acceso a funcionalidad no permitida para el `RolUsuario`. | Validacion en `SistemaAlquiler` (login, crearUsuario, eliminarUsuario, limpiarDatosBD). |
| `BaseDatosException` | Checked | Errores SQL, conexión caída, transacción fallida. | Capturar en la capa de persistencia, envolver excepciones JDBC. |

Ejemplo:
```java
public class VehiculoNoDisponibleException extends Exception {
    public VehiculoNoDisponibleException(String patente, EstadoVehiculo estado) {
        super("El vehículo " + patente + " no está disponible. Estado actual: " + estado);
    }
}
```

## 11. Genéricos (Clase Genérica y Método Genérico)

### Clase genérica `RepositorioGenerico<T, ID>`
```java
public class RepositorioGenerico<T, ID> {
    private Map<ID, T> cache = new LinkedHashMap<>();
    
    public void guardar(ID id, T entidad) { cache.put(id, entidad); }
    public Optional<T> buscar(ID id) { return Optional.ofNullable(cache.get(id)); }
    public List<T> listar() { return new ArrayList<>(cache.values()); }
    public boolean existe(ID id) { return cache.containsKey(id); }
    public void limpiar() { cache.clear(); }
    public int tamanio() { return cache.size(); }
    
    // Método genérico
    public <R> List<R> mapear(Function<T,R> funcion) {
        return cache.values().stream().map(funcion).collect(Collectors.toList());
    }
}
```

Uso: `RepositorioGenerico<Cliente, String>`, `RepositorioGenerico<Vehiculo, String>`, `RepositorioGenerico<Reserva, String>`, `RepositorioGenerico<Empleado, String>`, `RepositorioGenerico<Ocupacion, String>` y `RepositorioGenerico<Usuario, String>`. `listar()` conserva el orden de carga/alta y `mapear()` transforma la colección.

## 12. Persistencia con JDBC

### Estructura de paquetes
```text
src/
├── config/        # ConnectionFactory
├── dao/           # GestorBaseDatos
├── model/         # Entidades
├── enums/         # Enumeraciones
├── interfaces/    # Interfaces (Alquilable, Auditable)
├── exception/     # Excepciones personalizadas
├── service/       # SistemaAlquiler, ServicioAlquiler
├── service/impl/  # ServicioAlquilerImpl
├── util/          # RepositorioGenerico
└── Main.java
```

### `config/ConnectionFactory.java`
- Conexión a `jdbc:sqlite:alquiler_vehiculos.db`
- Método estático `getConnection()` y `cerrar(AutoCloseable)`

### `dao/GestorBaseDatos.java`
| Responsabilidad | Operaciones |
| --- | --- |
| Usuarios | `guardarUsuario`, `leerUsuarioPorId`, `leerUsuarioPorUsername`, `leerUsuarios`, `eliminarUsuarioPorId` |
| Vehiculos | `guardarVehiculo`, `leerVehiculoPorPatente`, `leerVehiculos`, `actualizarVehiculo` |
| Resto del sistema | Clientes, empleados, reservas, ocupaciones, entregas, devoluciones, cascadas y limpieza de BD |

El mapeo de vehiculos segun `tipo`/`datos_especificos` tambien vive en `GestorBaseDatos`; instancia `Automovil` y parsea `"<puertas>|<transmision>"`, con compatibilidad para datos legados.



## 13. Capa de Servicio (Logica de Negocio)

| Componente | Funcionalidades | Reglas |
| --- | --- | --- |
| `SistemaAlquiler` | login/logout, crearUsuario, crearCliente, agregarAutomovil, esCliente, crearReserva, reservarComoCliente, crearReservaCliente, realizarEntregaPorIndice, realizarDevolucionPorIndice, listados, historial, eliminaciones y limpiarDatosBD | Mantiene repositorios genéricos para clientes, vehículos, reservas, empleados, ocupaciones y usuarios. Lee de ellos y, antes de persistir, actualiza el repositorio correspondiente. Valida rol, datos de Persona, patente unica, solapamientos, fechas, estados y ocupaciones activas antes de eliminar. El listado de entrega incluye unicamente reservas PENDIENTES y no pide observaciones. Al entregar, elimina la reserva asociada del repositorio y de la BD. En la devolución, los cargos extra se toman de `Vehiculo.tarifaSeguro` y las observaciones se cargan por teclado. |
| `ServicioAlquiler` / `ServicioAlquilerImpl` | realizarEntrega(reserva, empleado, observaciones), realizarDevolucion(ocupacion, empleado, km, danios, cargos), listarAlquiladosActualmente, listarOcupaciones, cargarOcupaciones | Comparte el repositorio de ocupaciones y el de usuarios con `SistemaAlquiler`. Genera Ocupacion/Entrega/Devolucion, actualiza estado y km del vehiculo, aplica multas y registra la ocupación en el repositorio antes de que `SistemaAlquiler` persista. Los cargos extra recibidos se calculan desde la tarifa de seguro del vehículo. |

## 14. Flujo Funcional Principal

1. **Inicializacion** - `SistemaAlquiler` crea tablas (con migracion automatica de columnas), crea usuario `mariano`/`1234` (ADMIN) con su legajo de empleado y carga clientes, vehiculos, empleados, reservas, ocupaciones y usuarios en repositorios genéricos.
2. **Autenticacion** - Login busca el usuario en `repoUsuarios` y adjunta el `Empleado` o `Cliente` desde su repositorio. El rol define el nivel de acceso.
3. **Gestion de usuarios** - Sin sesion, el menu solo permite iniciar sesion o crear usuario CLIENTE. Con sesion, Usuarios > Crear usuario: ADMIN puede crear ADMINISTRADOR/EMPLEADO/CLIENTE; EMPLEADO solo EMPLEADO/CLIENTE. Para ADMIN/EMPLEADO pide datos de Persona + legajo/sucursal y crea Empleado + Usuario con el mismo id; para CLIENTE crea Cliente + Usuario (mismo id, sin legajo) usando sus constructores. Solo ADMIN crea ADMIN. Cada alta actualiza primero `repoClientes`/`repoEmpleados`/`repoUsuarios` y luego la BD.
4. **Gestion de vehiculos** - Automoviles > Agregar automovil: pide patente, marca, modelo, anio, km, estado, precio/dia, seguro, cantidad de puertas y transmision; se guarda en `repoVehiculos` y luego en `datos_especificos`. Automoviles > Modificar estado actualiza el objeto del repositorio y persiste.
5. **Consulta de flota** - Automoviles > Listar vehiculos y Ver flota completa leen desde `repoVehiculos`; `verFlotaCompleta()` conserva el orden de carga/alta.
6. **Clientes y reservas** - Clientes > Crear cliente (ADMIN/EMPLEADO); Reservas > Crear reserva: ADMIN/EMPLEADO elige cliente y vehiculo, mientras que un usuario CLIENTE crea la reserva con su propia ficha (sin elegir cliente) y `creadoPor` = usuario logueado. La reserva se guarda primero en `repoReservas` y luego en la BD; el control de solapamientos y ocupaciones activas también consulta los repositorios.
7. **Entrega/Devolucion** - Entregas > Realizar entrega y Devoluciones > Realizar devolucion usan el `Empleado` del usuario logueado (sin datos ficticios) y persisten entrega, devolucion y estado de la ocupacion. La entrega lista solo reservas PENDIENTES, no pide observaciones y toma como fecha de inicio la `fechaInicio` de la reserva asignada por el usuario; guarda una instantanea de las fechas de la reserva en la ocupacion y luego elimina la reserva de `repoReservas` y de la BD. La devolucion pide daños y observaciones, no pide cargos extra por teclado: usa `Vehiculo.tarifaSeguro` como cargo extra y sigue aplicando la multa automatica por daños. El historial muestra reserva, entrega, devolucion y las observaciones de la devolucion.
8. **Administracion** - Usuarios y sistema > Ver usuarios lee desde `repoUsuarios`; Eliminar usuario y Limpiar BD solo estan disponibles para ADMIN; limpiar restaura el legajo de mariano.

## 15. Documentación de Código (Obligatoria)

Para cumplir el requisito "código fuente correctamente documentado/comentado":

- **Javadoc:** documentar clases públicas, interfaces y métodos complejos (`ServicioAlquiler`, `GestorBaseDatos`).
- **Comentarios:** explicar reglas de negocio complejas (solapamiento de reservas, cálculo de cargos, transacciones).
- **Convenciones:** nombres claros en español/inglés consistente, atributos privados, constantes en `UPPER_CASE`.
- **Sin comentarios obvios:** evitar `// incrementa i`.

Ejemplo Javadoc:
```java
/**
 * Servicio encargado de gestionar el proceso de alquiler (entrega y devolución).
 * @author Grupo X
 * @version 1.0
 */
public class ServicioAlquiler {
    /**
     * Realiza la entrega de un vehículo generando una ocupación activa.
     * @param reserva Reserva confirmada a concretar
     * @param empleado Empleado que realiza la entrega
     * @return Ocupacion activa generada
     * @throws VehiculoNoDisponibleException si el vehículo no está disponible
     */
    public Ocupacion realizarEntrega(Reserva reserva, Empleado empleado, String observaciones)
            throws VehiculoNoDisponibleException, ReservaInvalidaException { ... }
    // ...
```

## 16. Git y Trabajo Colaborativo (Obligatorio)

- **Repositorio:** `https://github.com/<usuario>/<nombre-repo>` (privado/público según consigna)
- **Ramas:** `main`, `develop`, ramas por feature (`feature/servicio-alquiler`, `feature/dao-jdbc`).
- **Commits:** atómicos, descriptivos (Convencional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`).
- **Participación:** todos los integrantes deben commitear. Se revisará historial (`git log --oneline --graph`).
- **Protecciones:** PRs con revisión, evitar un solo commit masivo.
- **Archivo .gitignore:** incluir `*.class`, `out/`, `.idea/`, `*.db`, `*.properties` con credenciales, `bin/`.

## 17. Checklist para Entrega/Defensa

- [ ] Diagrama UML entregado
- [ ] 4 pilares aplicados (demostrar con ejemplo)
- [ ] >=8 clases, >=1 abstracta, >=1 interfaz, >=1 enum
- [ ] >=2 colecciones (`ArrayList`, `LinkedHashMap`)
- [ ] >=2 excepciones personalizadas
- [ ] Clase genérica + interfaz/método genérico
- [ ] JDBC funcional (CRUD, migraciones automaticas y carga inicial)
- [ ] Todos los integrantes commitearon
- [ ] Código documentado (Javadoc/comentarios)
- [ ] Funcionalidades: reservar, entregar, devolver, listar alquilados/disponibles/no disponibles + motivo, historial cliente
- [ ] Roles ADMIN/EMPLEADO/CLIENTE (los tres inician sesion; CLIENTE solo ve disponibles y crea sus reservas)
- [ ] README.md (este documento) + instrucciones de ejecución
- [ ] Esquema SQL embebido en `GestorBaseDatos.crearTablas()` (sin script externo)

## 18. Sugerencias para Sacar Buena Nota

- **Diseño:** mostrar claramente polimorfismo (lista `ArrayList<Vehiculo>` y llamada a `calcularCosto()`).
- **Validaciones:** demostrar detección de solapamiento de reservas.
- **Transacciones:** usar `commit/rollback` en entrega/devolución.
- **Seguridad:** hashes de contraseñas, validación por roles.
- **Defensa:** cada integrante debe explicar su capa (modelo/persistencia/servicio/UI) sin leer todo el código.
- **Calidad:** código limpio, sin duplicación, principios básicos SOLID.

## 19. Estructura de Tablas SQL (Implementada)

```sql
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    rol TEXT NOT NULL,
    activo INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS vehiculo (
    patente TEXT PRIMARY KEY,
    marca TEXT NOT NULL,
    modelo TEXT NOT NULL,
    anio INTEGER NOT NULL,
    kilometraje INTEGER NOT NULL,
    estado TEXT NOT NULL,
    precio_dia REAL NOT NULL,
    tarifa_seguro REAL NOT NULL,
    tipo TEXT NOT NULL,
    datos_especificos TEXT
);
```

Las tablas restantes (`empleado`, `cliente`, `reserva`, `ocupacion`, `entrega`, `devolucion`) se crean en `GestorBaseDatos.crearTablas()` con sus claves foraneas. La tabla `reserva` no guarda una seña duplicada: se calcula con `Vehiculo.tarifaSeguro`; si una BD existente tenia `monto_senia`, la migracion elimina esa columna legacy al iniciar. La tabla `ocupacion` incluye `fecha_reserva` y `fecha_inicio_reserva` para conservar los datos de la reserva aun cuando la entrega la elimine. Las columnas `reserva.creado_por` y `ocupacion.creado_por` guardan el username; al iniciar, una migracion convierte valores legacy que sean id de usuario o legajo de empleado. La tabla `devolucion` incluye `observaciones`; en bases previas la columna se agrega automaticamente.

**Usuario por defecto:** `mariano` / `1234` / `ADMINISTRADOR` (creado automáticamente si no existe).


- Automoviles > Modificar estado de vehiculo (ADMIN/EMPLEADO): lista todos los vehiculos y permite cambiar su estado con persistencia en SQLite.
- Correcciones: indices de reserva alineados con el listado de disponibles, eliminacion de vehiculo por orden de BD y borrado de usuario por id/username.

## 20. Estado actual (19/11/2026)

- Menu dinamico y agrupado: sin sesion solo muestra iniciar sesion, crear usuario CLIENTE y salir. Con sesion muestra categorias: Clientes, Automoviles, Reservas, Entregas, Devoluciones y Usuarios (ADMIN la ve como "Usuarios y sistema"). Cada categoria abre su propio submenu; CLIENTE solo ve Automoviles y Reservas, y EMPLEADO no accede a limpieza de BD ni a eliminacion de usuarios.
- Las eliminaciones bloquean clientes/vehiculos/reservas con ocupacion activa; las ocupaciones finalizadas se borran en cascada con su entrega y devolucion.
- Validaciones: email `usuario@dominio.com`; estados manuales permitidos DISPONIBLE/EN_LIMPIEZA/EN_REPARACION/FUERA_SERVICIO; "NINGUNO" no aplica multa.
- La BD incluida trae datos de demostracion (usuarios, clientes, flota, reservas e historial).
- Un usuario CLIENTE inicia sesion con menu reducido (disponibles + gestionar reserva) y sus reservas usan automaticamente su ficha de cliente. EMPLEADO no ve usuarios ni limpieza de BD; ADMIN ve todas las opciones.
- El submenu Reservas permite crear, cancelar y (ADMIN/EMPLEADO) listar o eliminar reservas. Cancelar pasa la reserva a CANCELADA y persiste el cambio; CLIENTE solo puede cancelar las propias, ADMIN/EMPLEADO cualquiera. Las reservas CONCRETADAS o con ocupacion activa no se pueden cancelar.
- Sin sesion iniciada, la opcion 2 solo permite autorregistrarse como CLIENTE (no crea ADMIN ni EMPLEADO).
- Al entregar, la reserva asociada se elimina de la BD y de `repoReservas`; la ocupación conserva las fechas de reserva, inicio y fin estimado. El historial muestra `RESERVA`, `ENTREGA`, `DEVOLUCIÓN` y `OBSERVACIONES` de la devolución en formato `dd/MM/yyyy` / `dd/MM/yyyy HH:mm`.
- `reserva.creado_por` y `ocupacion.creado_por` almacenan usernames. La migración inicial traduce legajos/ids legacy a username cuando encuentra el usuario correspondiente.