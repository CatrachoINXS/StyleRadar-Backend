package edu.dosw.proyecto.style_radar.validator;

import org.springframework.stereotype.Component;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.model.entity.BusquedaGuardadaEntity;
import edu.dosw.proyecto.style_radar.service.CriteriosBusquedaGuardada;
import lombok.extern.slf4j.Slf4j;

@Component @Slf4j
public class AlertaValidator {
    public void objetivo(Long busquedaId, Long itemId) {
        if ((busquedaId == null) == (itemId == null) || (busquedaId != null && busquedaId <= 0)
                || (itemId != null && itemId <= 0)) {
            log.warn("Configuracion de alerta rechazada: objetivo invalido");
            throw new SolicitudAlertaInvalidaException("Debe indicar exactamente un objetivo con ID positivo");
        }
    }

    public void busqueda(BusquedaGuardadaEntity busqueda) {
        if (!CriteriosBusquedaGuardada.significativa(CriteriosBusquedaGuardada.criterios(busqueda))) {
            log.warn("Configuracion de alerta rechazada: busqueda sin criterios efectivos");
            throw new ReglaDeNegocioException("La busqueda debe contener al menos un criterio efectivo");
        }
    }

    public void agotado(int stock) {
        if (stock != 0) {
            log.warn("Configuracion de alerta rechazada: item disponible");
            throw new ReglaDeNegocioException("El item ya tiene unidades disponibles");
        }
    }

    public void desactivacion(Boolean activa) {
        if (!Boolean.FALSE.equals(activa)) {
            throw new SolicitudAlertaInvalidaException("Solo se permite desactivar la alerta con activa=false");
        }
    }

    public void paginacion(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new SolicitudAlertaInvalidaException("Paginacion invalida: page>=0 y size entre 1 y 100");
        }
    }
}
