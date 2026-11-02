package com.transportes.controller;

import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.model.Mercancia;
import com.transportes.model.Paquete;
import com.transportes.model.Sobre;
import com.transportes.model.dao.MercanciaDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Guarda y devuelve copias, como la BD: modificar un objeto leído no cambia la fila hasta llamar a update. */
final class MercanciaDaoEnMemoria implements MercanciaDao {

    static final BigDecimal PRECIO_ENVIO = new BigDecimal("20.00");

    private final Map<Integer, Mercancia> filas = new TreeMap<>();
    private final EnvioDaoEnMemoria envios;
    private int siguienteId = 1;

    MercanciaDaoEnMemoria(EnvioDaoEnMemoria envios) {
        this.envios = envios;
    }

    static Sobre sobre(String codigo, int motoId) {
        return new Sobre(null, codigo, "Sobre " + codigo, PRECIO_ENVIO, false, false, motoId, null, null,
                new BigDecimal("0.250"));
    }

    static Sobre sobreLargoRecorrido(String codigo, int motoId, int avionId, int motoDestinoId) {
        return new Sobre(null, codigo, "Sobre " + codigo, PRECIO_ENVIO, false, true, motoId, avionId,
                motoDestinoId, new BigDecimal("0.250"));
    }

    static Paquete paquete(String codigo, int furgonetaId) {
        return new Paquete(null, codigo, "Paquete " + codigo, PRECIO_ENVIO, true, false, furgonetaId, null, null,
                new BigDecimal("40.00"), new BigDecimal("30.00"), new BigDecimal("20.00"));
    }

    /** Inserta directamente, como haría seed.sql. */
    <M extends Mercancia> M sembrar(M mercancia) {
        guardar(mercancia);
        return mercancia;
    }

    @Override
    public void save(Mercancia mercancia) {
        guardar(mercancia);
    }

    @Override
    public void save(Mercancia mercancia, Connection conexion) {
        Objects.requireNonNull(conexion);
        guardar(mercancia);
    }

    @Override
    public boolean update(Mercancia mercancia) {
        return filas.replace(mercancia.getId(), copiar(mercancia)) != null;
    }

    @Override
    public boolean update(Mercancia mercancia, Connection conexion) {
        Objects.requireNonNull(conexion);
        return update(mercancia);
    }

    @Override
    public boolean delete(int id) {
        return filas.remove(id) != null;
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        Objects.requireNonNull(conexion);
        return delete(id);
    }

    @Override
    public Optional<Mercancia> findById(int id) {
        return Optional.ofNullable(filas.get(id)).map(MercanciaDaoEnMemoria::copiar);
    }

    @Override
    public Optional<Mercancia> findByCodigo(String codigo) {
        return filas.values().stream().filter(m -> m.getCodigo().equals(codigo)).findFirst()
                .map(MercanciaDaoEnMemoria::copiar);
    }

    @Override
    public List<Mercancia> findByVehiculo(int vehiculoId) {
        return filas.values().stream()
                .filter(m -> m.getVehiculoId() == vehiculoId || Objects.equals(m.getAvionId(), vehiculoId)
                        || Objects.equals(m.getVehiculoDestinoId(), vehiculoId))
                .map(MercanciaDaoEnMemoria::copiar).toList();
    }

    @Override
    public List<Mercancia> findLargoRecorrido() {
        return filas.values().stream().filter(Mercancia::isLargoRecorrido).map(MercanciaDaoEnMemoria::copiar)
                .toList();
    }

    @Override
    public List<Mercancia> findAll() {
        return filas.values().stream().map(MercanciaDaoEnMemoria::copiar).toList();
    }

    @Override
    public boolean tieneEnvio(int mercanciaId) {
        return envios.figuraMercancia(mercanciaId);
    }

    private void guardar(Mercancia mercancia) {
        if (filas.values().stream().anyMatch(m -> m.getCodigo().equals(mercancia.getCodigo()))) {
            throw new RegistroDuplicadoException("Ya existe una mercancía con ese código.");
        }
        mercancia.asignarId(siguienteId++);
        filas.put(mercancia.getId(), copiar(mercancia));
    }

    private static Mercancia copiar(Mercancia m) {
        return switch (m.getTipo()) {
            case SOBRE -> new Sobre(m.getId(), m.getCodigo(), m.getDescripcion(), m.getPrecioEnvio(),
                    m.isAsegurada(), m.isLargoRecorrido(), m.getVehiculoId(), m.getAvionId(),
                    m.getVehiculoDestinoId(), ((Sobre) m).getPeso());
            case PAQUETE -> new Paquete(m.getId(), m.getCodigo(), m.getDescripcion(), m.getPrecioEnvio(),
                    m.isAsegurada(), m.isLargoRecorrido(), m.getVehiculoId(), m.getAvionId(),
                    m.getVehiculoDestinoId(), ((Paquete) m).getLargo(), ((Paquete) m).getAncho(),
                    ((Paquete) m).getAlto());
        };
    }
}
