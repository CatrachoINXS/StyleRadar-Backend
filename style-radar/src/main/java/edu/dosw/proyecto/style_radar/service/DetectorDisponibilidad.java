package edu.dosw.proyecto.style_radar.service;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.repository.specification.ItemCatalogoSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Invocado sincrónicamente bajo el bloqueo del item y dentro de su transacción. */
@Service @RequiredArgsConstructor @Slf4j
@Transactional(propagation = Propagation.MANDATORY)
public class DetectorDisponibilidad {
    private final DisponibilidadItemRepository disponibilidad;
    private final AlertaRepository alertas;
    private final NotificacionRepository notificaciones;
    private final BusquedaGuardadaRepository busquedas;
    private final ItemCatalogoRepository items;
    private final Clock clock;

    public void registrarPublicacion(ItemCatalogoEntity item) {
        disponibilidad.save(nuevoEstado(item, false));
    }

    public void inventarioActualizado(ItemCatalogoEntity item, int stockAnterior, int stockActual) {
        // Un aumento puede habilitar por primera vez la talla de una búsqueda aunque ya haya stock en otra.
        if (stockActual <= 0 || stockActual <= stockAnterior) return;
        boolean reposicionTotal = stockAnterior == 0;
        long ciclo = 0;
        if (reposicionTotal) {
            var estado = disponibilidad.findById(item.getId()).orElseGet(() -> nuevoEstado(item, true));
            estado.setPrimeraDisponibilidad(true);
            estado.setCiclo(Math.incrementExact(estado.getCiclo()));
            disponibilidad.save(estado);
            ciclo = estado.getCiclo();
        }
        // El flush hace visibles unidades nuevas a los predicados SQL; sigue siendo reversible.
        items.flush();
        var ids = new TreeSet<>(alertas.findIdsBusquedasCandidatas(item.getFechaPublicacion(), item.getId()));
        if (reposicionTotal) ids.addAll(alertas.findIdsActivasDelObjetivo(TipoObjetivoAlerta.ITEM_CATALOGO, item.getId()));
        var candidatas = ids.stream().map(id -> alertas.findForUpdate(id).orElseThrow())
                .filter(AlertaEntity::isActiva).toList();
        var busquedasPorId = busquedas.findAllById(candidatas.stream()
                .filter(a -> a.getTipoObjetivo() == TipoObjetivoAlerta.BUSQUEDA_GUARDADA)
                .map(AlertaEntity::getIdentificadorObjetivo).distinct().toList()).stream()
                .collect(Collectors.toMap(BusquedaGuardadaEntity::getId, Function.identity()));
        for (var alerta : candidatas) {
            if (alerta.getTipoObjetivo() == TipoObjetivoAlerta.ITEM_CATALOGO) {
                registrar(alerta, item.getId(), TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA, ciclo);
            } else if (coincide(alerta, item, busquedasPorId.get(alerta.getIdentificadorObjetivo()))) {
                registrar(alerta, item.getId(), TipoEventoNotificacion.NUEVA_COINCIDENCIA, 0);
            }
        }
    }

    public void objetivoEliminado(TipoObjetivoAlerta tipo, Long id) {
        for (Long alertaId : alertas.findIdsActivasDelObjetivo(tipo, id)) {
            var alerta = alertas.findForUpdate(alertaId).orElseThrow();
            if (alerta.isActiva()) {
                alerta.desactivar(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
                log.info("Alerta desactivada por eliminacion de objetivo alertaId={} tipo={}", alertaId, tipo);
            }
        }
        if (tipo == TipoObjetivoAlerta.ITEM_CATALOGO) {
            disponibilidad.findById(id).ifPresent(disponibilidad::delete);
            disponibilidad.flush();
        }
    }

    private DisponibilidadItemEntity nuevoEstado(ItemCatalogoEntity item, boolean previa) {
        var estado = new DisponibilidadItemEntity();
        estado.setItem(item);
        estado.setPrimeraDisponibilidad(previa);
        return estado;
    }

    private boolean coincide(AlertaEntity alerta, ItemCatalogoEntity item, BusquedaGuardadaEntity busqueda) {
        if (busqueda == null || !busqueda.getUsuario().getId().equals(alerta.getUsuario().getId())
                || !item.getFechaPublicacion().isAfter(alerta.getFechaCreacion())) return false;
        var criterios = CriteriosBusquedaGuardada.criterios(busqueda);
        return CriteriosBusquedaGuardada.significativa(criterios)
                && items.exists(ItemCatalogoSpecifications.coincidenciaExacta(criterios)
                        .and((root, query, cb) -> cb.equal(root.get("id"), item.getId())));
    }

    private void registrar(AlertaEntity alerta, Long itemId, TipoEventoNotificacion tipo, long ciclo) {
        var n = new NotificacionEntity();
        n.setUsuario(alerta.getUsuario());
        n.setAlerta(alerta);
        n.setTipoEvento(tipo);
        n.setItemCatalogoId(itemId);
        n.setCiclo(ciclo);
        n.setMensaje(tipo == TipoEventoNotificacion.NUEVA_COINCIDENCIA
                ? "Una nueva publicacion coincide con tu busqueda guardada"
                : "La publicacion seguida vuelve a tener unidades disponibles");
        n.setFechaCreacion(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        notificaciones.saveAndFlush(n);
        log.info("Notificacion registrada notificacionId={} alertaId={} itemId={} evento={} ciclo={}",
                n.getId(), alerta.getId(), itemId, tipo, ciclo);
    }
}
