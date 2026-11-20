package util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RepositorioGenerico<T, ID> {
    private Map<ID, T> cache = new LinkedHashMap<ID, T>();

    public void guardar(ID id, T entidad) {
        cache.put(id, entidad);
    }

    public Optional<T> buscar(ID id) {
        return Optional.ofNullable(cache.get(id));
    }

    public List<T> listar() {
        return new ArrayList<T>(cache.values());
    }

    public boolean existe(ID id) {
        return cache.containsKey(id);
    }

    public void eliminar(ID id) {
        cache.remove(id);
    }

    public void limpiar() {
        cache.clear();
    }

    public int tamanio() {
        return cache.size();
    }

    public <R> List<R> mapear(Function<T, R> funcion) {
        return cache.values().stream()
                .map(funcion)
                .collect(Collectors.toList());
    }
}
