package edu.dosw.proyecto.style_radar.validator;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.entity.InventarioTallaEntity;

@Component
public class InventarioValidator {

    public void validarTallasEliminables(List<InventarioTallaEntity> inventario, Set<Talla> tallasSolicitadas) {
        boolean eliminaTallaConUnidades = inventario.stream()
                .anyMatch(item -> !tallasSolicitadas.contains(item.getTalla()) && item.getUnidades() > 0);
        if (eliminaTallaConUnidades) {
            throw new ReglaDeNegocioException("No se puede eliminar una talla que tiene unidades disponibles");
        }
    }

    public InventarioTallaEntity obtenerTallaRegistrada(List<InventarioTallaEntity> inventario, Talla talla) {
        return inventario.stream()
                .filter(item -> item.getTalla() == talla)
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "La talla " + talla + " no está registrada para el item"));
    }
}
