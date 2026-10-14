package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.CambioEstadoCuentaRequestDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.support.AlertasFixtures;

@SpringBootTest @ActiveProfiles("test") @Transactional
class GestionEstadoCuentaAlertasIntegrationTest extends AlertasFixtures {
    @Autowired GestionEstadoCuentaService gestion;
    @MockitoSpyBean Clock clock;
    UsuarioPrincipal admin;
    @BeforeEach void prepararAdministradorYFechas() {
        // Arrange: fechas separadas sin depender de resolución del reloj ni sleeps.
        var a = usuario("admin-eventos-rf66@example.co"); a.setRoles(Set.of(Rol.ADMIN_STYLERADAR)); admin = new UsuarioPrincipal(a, "");
        doReturn(Instant.parse("2026-10-13T12:00:00Z")).when(clock).instant();
    }
    void cambiar(EstadoCuenta estado) {
        var r = new CambioEstadoCuentaRequestDTO(); r.setEstado(estado); r.setMotivo("Motivo");
        gestion.cambiar(usuario.getId(), admin, r); releer();
    }
    Long nuevaPublicacion() {
        doReturn(Instant.parse("2026-10-13T12:01:00Z")).when(clock).instant(); return publicar();
    }

    @ParameterizedTest @NullSource @EnumSource(EstadoCuenta.class)
    void eventosSoloParaEstadoEfectivoActivo(EstadoCuenta estado) {
        // Arrange
        var busqueda = alertaBusqueda(b -> b.setMarca("Radar")); Long id = nuevaPublicacion();
        var seguimiento = service.crear(usuario.getId(), null, id);
        usuarios.findById(usuario.getId()).orElseThrow().setEstadoCuenta(estado); releer();
        // Act
        stock(id, 3); releer();
        // Assert
        if (estado == null || estado == EstadoCuenta.ACTIVA) {
            assertThat(eventos()).hasSize(2).extracting(Notificacion::tipoEvento)
                    .containsExactlyInAnyOrder(TipoEventoNotificacion.NUEVA_COINCIDENCIA, TipoEventoNotificacion.DISPONIBILIDAD_RESTABLECIDA);
        } else assertThat(eventos()).isEmpty();
        assertThat(alertas.findById(busqueda.id()).orElseThrow().isActiva()).isTrue();
        assertThat(alertas.findById(seguimiento.id()).orElseThrow().isActiva()).isTrue();
        assertThat(busquedas.existsById(busqueda.identificadorObjetivo())).isTrue();
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void suspenderConservaHistorialYReactivarNoReproduceEventosOmitidos(EstadoCuenta estado) {
        // Arrange
        var b = alertaBusqueda(x -> x.setMarca("Radar")); Long anterior = nuevaPublicacion(); stock(anterior, 3); releer();
        var previas = eventos(); assertThat(previas).hasSize(1);
        Long omitido = nuevaPublicacion(); var seguimiento = service.crear(usuario.getId(), null, omitido);
        // Act
        cambiar(estado); stock(omitido, 2); releer();
        // Assert
        assertThat(eventos()).isEqualTo(previas);
        cambiar(EstadoCuenta.ACTIVA);
        assertThat(eventos()).isEqualTo(previas); // La reactivación no invoca ni reproduce inventario.
        stock(omitido, 2); // Mismo stock tampoco reproduce el evento omitido.
        assertThat(eventos()).isEqualTo(previas);
        assertThat(service.consultar(usuario.getId())).hasSize(2).allSatisfy(a -> assertThat(a.activa()).isTrue());
        assertThat(busquedas.existsById(b.identificadorObjetivo())).isTrue();
        // Un evento nuevo de reposición puede participar; ciclo omitido se conserva, sin recuperar su notificación.
        stock(omitido, 0); stock(omitido, 4); releer();
        assertThat(eventos()).hasSize(3); assertThat(disponibilidad.findById(omitido).orElseThrow().getCiclo()).isEqualTo(2);
        assertThat(notificaciones.findAll().stream().filter(n -> n.getAlerta().getId().equals(seguimiento.id())))
                .extracting(n -> n.getCiclo()).containsExactly(2L);
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void tallaTardiaYDeduplicacionTrasReactivacion(EstadoCuenta estado) {
        // Arrange
        var a = alertaBusqueda(b -> { b.setMarca("Radar"); b.setTalla(Talla.M); });
        Long id = nuevaPublicacion(); stock(id, Talla.L, 3); assertThat(eventos()).isEmpty();
        cambiar(estado); stock(id, Talla.M, 2); assertThat(eventos()).isEmpty();
        // Act: activar no reproduce la coincidencia omitida; un aumento futuro sí es un nuevo evento.
        cambiar(EstadoCuenta.ACTIVA); assertThat(eventos()).isEmpty(); stock(id, Talla.M, 5); releer();
        // Assert
        assertThat(eventos()).singleElement().satisfies(n -> {
            assertThat(n.alertaId()).isEqualTo(a.id()); assertThat(n.tipoEvento()).isEqualTo(TipoEventoNotificacion.NUEVA_COINCIDENCIA);
        });
        stock(id, Talla.M, 8); stock(id, Talla.M, 8); stock(id, Talla.M, 0); stock(id, Talla.M, 4);
        assertThat(eventos()).hasSize(1);
    }
}
