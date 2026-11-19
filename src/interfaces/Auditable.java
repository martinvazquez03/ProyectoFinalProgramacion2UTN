package interfaces;

import java.time.LocalDateTime;

public interface Auditable {
    LocalDateTime getFechaCreacion();
    String getCreadoPor();
    void marcarModificacion(String usuario);
}
