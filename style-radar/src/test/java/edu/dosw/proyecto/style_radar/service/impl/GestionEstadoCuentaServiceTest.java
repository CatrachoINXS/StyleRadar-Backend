package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import edu.dosw.proyecto.style_radar.exception.*;
import edu.dosw.proyecto.style_radar.mapper.CuentaAdministrativaMapper;
import edu.dosw.proyecto.style_radar.model.domain.*;
import edu.dosw.proyecto.style_radar.model.dto.request.CambioEstadoCuentaRequestDTO;
import edu.dosw.proyecto.style_radar.model.entity.*;
import edu.dosw.proyecto.style_radar.repository.*;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.BloqueoCuentaService;

class GestionEstadoCuentaServiceTest {
    static final Instant FECHA = Instant.parse("2026-10-13T12:00:00Z");
    static final String HASH = new BCryptPasswordEncoder(4).encode("fixture password");
    static final jakarta.validation.ValidatorFactory VALIDACION = jakarta.validation.Validation.buildDefaultValidatorFactory();
    @AfterAll static void cerrarValidacion() { VALIDACION.close(); }
    UsuarioRepository usuarios;
    CredencialRepository credenciales;
    BloqueoCuentaService bloqueo;
    GestionEstadoCuentaService service;
    UsuarioEntity actor, objetivo;
    UsuarioPrincipal principal;
    Map<Long, UsuarioEntity> filas;

    @BeforeEach void preparar() {
        // Arrange
        usuarios = mock(UsuarioRepository.class); credenciales = mock(CredencialRepository.class);
        bloqueo = mock(BloqueoCuentaService.class);
        service = new GestionEstadoCuentaService(usuarios, credenciales, bloqueo,
                new CuentaAdministrativaMapper(), Clock.fixed(FECHA, ZoneOffset.UTC), VALIDACION.getValidator());
        actor = cuenta(1L, Rol.ADMIN_STYLERADAR); objetivo = cuenta(2L, Rol.COMPRADOR);
        principal = new UsuarioPrincipal(actor, ""); filas = new HashMap<>(Map.of(1L, actor, 2L, objetivo));
        when(usuarios.findIdsByRol(Rol.ADMIN_STYLERADAR)).thenReturn(List.of(1L));
        when(bloqueo.bloquear(any())).thenAnswer(i -> filas);
        when(usuarios.findAdministracionById(1L)).thenReturn(Optional.of(actor));
        when(usuarios.findDatosActuales(anyLong())).thenAnswer(i -> Optional.ofNullable(filas.get(i.getArgument(0)))
                .map(new CuentaAdministrativaMapper()::toResponse));
        when(usuarios.findRolesActuales(anyLong())).thenAnswer(i -> filas.get(i.getArgument(0)).getRoles());
        when(usuarios.actualizarEstado(anyLong(), any(), any(), anyLong(), nullable(String.class))).thenAnswer(i -> {
            var u = filas.get(i.getArgument(0)); u.setEstadoCuenta(i.getArgument(1));
            u.setFechaCambioEstado(i.getArgument(2)); u.setAdministradorCambioEstadoId(i.getArgument(3));
            u.setMotivoCambioEstado(i.getArgument(4)); return 1;
        });
    }

    UsuarioEntity cuenta(Long id, Rol rol) {
        return UsuarioEntity.builder().id(id).nombre("Fixture").email(id + "@example.co")
                .fechaRegistro(FECHA.minusSeconds(100)).roles(new HashSet<>(Set.of(rol))).build();
    }
    CambioEstadoCuentaRequestDTO request(EstadoCuenta estado, String motivo) {
        var r = new CambioEstadoCuentaRequestDTO(); r.setEstado(estado); r.setMotivo(motivo); return r;
    }
    void credencialOperativa(UsuarioEntity u, String hash) {
        when(credenciales.findById(u.getId())).thenReturn(Optional.of(new CredencialEntity(u, hash)));
        when(usuarios.findAllByEmailNormalizado(u.getEmail())).thenReturn(List.of(u));
    }

    @ParameterizedTest @CsvSource({"ACTIVA,BLOQUEADA", "ACTIVA,INACTIVA", "INACTIVA,ACTIVA",
            "BLOQUEADA,ACTIVA", "INACTIVA,BLOQUEADA", "BLOQUEADA,INACTIVA"})
    void seisTransicionesConFechaServidorYActorAutenticado(EstadoCuenta anterior, EstadoCuenta nuevo) {
        // Arrange
        objetivo.setEstadoCuenta(anterior); objetivo.setTelefono("123");
        objetivo.getPreferenciasEstilo().add(Estilo.FORMAL); objetivo.getTallasHabitualesPorTipo().clear();
        var roles = Set.copyOf(objetivo.getRoles()); var fechaRegistro = objetivo.getFechaRegistro();
        // Act
        var r = service.cambiar(2L, principal, request(nuevo, "  Revisión  "));
        // Assert
        assertThat(r.estadoCuenta()).isEqualTo(nuevo); assertThat(r.fechaCambioEstado()).isEqualTo(FECHA);
        assertThat(r.administradorCambioEstadoId()).isEqualTo(1L); assertThat(r.motivoCambioEstado()).isEqualTo("Revisión");
        assertThat(objetivo.getRoles()).isEqualTo(roles); assertThat(objetivo.getFechaRegistro()).isEqualTo(fechaRegistro);
        assertThat(objetivo.getPreferenciasEstilo()).containsExactly(Estilo.FORMAL);
        assertThat(objetivo.getTallasHabitualesPorTipo()).isEmpty(); assertThat(objetivo.getTelefono()).isEqualTo("123");
        verifyNoInteractions(credenciales); verify(usuarios).flush(); verify(usuarios, never()).delete(any());
    }

    @Test void nullHistoricoTransicionExplicita() {
        // Arrange
        objetivo.setEstadoCuenta(null);
        // Act / Assert
        assertThat(service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null)).estadoCuenta()).isEqualTo(EstadoCuenta.INACTIVA);
        assertThat(objetivo.getEstadoCuenta()).isEqualTo(EstadoCuenta.INACTIVA);
    }

    @ParameterizedTest @EnumSource(EstadoCuenta.class)
    void idempotenciaNoReescribeDecision(EstadoCuenta estado) {
        // Arrange
        objetivo.setEstadoCuenta(estado); objetivo.setFechaCambioEstado(FECHA.minusSeconds(5));
        objetivo.setAdministradorCambioEstadoId(9L); objetivo.setMotivoCambioEstado("Original");
        // Act
        var r = service.cambiar(2L, principal, request(estado, "Nuevo motivo"));
        // Assert
        assertThat(r.fechaCambioEstado()).isEqualTo(FECHA.minusSeconds(5));
        assertThat(r.administradorCambioEstadoId()).isEqualTo(9L); assertThat(r.motivoCambioEstado()).isEqualTo("Original");
        verify(usuarios, never()).flush();
    }

    @Test void idempotenciaNullNoFabricaBackfillNiDecision() {
        // Arrange
        objetivo.setEstadoCuenta(null);
        // Act
        var r = service.cambiar(2L, principal, request(EstadoCuenta.ACTIVA, null));
        // Assert
        assertThat(r.estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); assertThat(objetivo.getEstadoCuenta()).isNull();
        assertThat(r.fechaCambioEstado()).isNull(); verify(usuarios, never()).flush();
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t\n"})
    void bloqueoExigeMotivo(String motivo) {
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.BLOQUEADA, motivo)))
                .isInstanceOf(ReglaDeNegocioException.class); verifyNoInteractions(bloqueo);
    }

    @ParameterizedTest @EnumSource(EstadoCuenta.class)
    void longitudExcesivaRechazada(EstadoCuenta estado) {
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(estado, "x".repeat(1001))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @ParameterizedTest @EnumSource(EstadoCuenta.class)
    void motivoExactamenteMilCaracteresPermitido(EstadoCuenta estado) {
        // Arrange
        objetivo.setEstadoCuenta(estado == EstadoCuenta.ACTIVA ? EstadoCuenta.INACTIVA : EstadoCuenta.ACTIVA);
        // Act / Assert
        assertThat(service.cambiar(2L, principal, request(estado, "x".repeat(1000))).motivoCambioEstado()).hasSize(1000);
    }

    @Test void escrituraSinFilaNoDevuelveExito() {
        // Arrange
        doReturn(0).when(usuarios).actualizarEstado(anyLong(), any(), any(), anyLong(), nullable(String.class));
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"ACTIVA", "INACTIVA"})
    void motivoOpcionalSeNormaliza(EstadoCuenta estado) {
        // Arrange
        objetivo.setEstadoCuenta(EstadoCuenta.BLOQUEADA);
        // Act / Assert
        assertThat(service.cambiar(2L, principal, request(estado, "  ")).motivoCambioEstado()).isNull();
    }

    @ParameterizedTest @EnumSource(EstadoCuenta.class)
    void autocambioRechazadoInclusoIdempotente(EstadoCuenta estado) {
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(1L, principal, request(estado, "Motivo")))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("propia cuenta");
        verifyNoInteractions(bloqueo);
    }

    @ParameterizedTest @EnumSource(value = Rol.class, names = {"COMPRADOR", "ADMIN_ALMACEN", "REPRESENTANTE_FUNDACION"})
    void rolAjenoRechazadoEnServicio(Rol rol) {
        // Arrange
        actor.setRoles(Set.of(rol)); var ajeno = new UsuarioPrincipal(actor, "");
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, ajeno, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.consultar(null, 0, 20, ajeno)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.consultarUno(2L, ajeno)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void actorNuloNoAutorizado() {
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, null, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void requestOEstadoNulosRechazados() {
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, null)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(null, null))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test void objetivoInexistente404() {
        // Arrange
        filas.remove(2L);
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.consultarUno(2L, principal)).isInstanceOf(RecursoNoEncontradoException.class);
    }
    @Test void actorInexistente401() {
        // Arrange
        filas.remove(1L); when(usuarios.findAdministracionById(1L)).thenReturn(Optional.empty());
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.consultarUno(2L, principal)).isInstanceOf(BadCredentialsException.class);
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void actorSeRevalidaEnBD(EstadoCuenta estado) {
        // Arrange: principal creado cuando la cuenta seguía activa.
        actor.setEstadoCuenta(estado);
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.consultarUno(2L, principal)).isInstanceOf(BadCredentialsException.class);
        assertThat(objetivo.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
    }
    @Test void rolActualSeRevalida() {
        // Arrange
        actor.setRoles(Set.of(Rol.COMPRADOR));
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest @EnumSource(Rol.class)
    void objetivoConCualquierRolConservaRolesYNoSeEscribenCredenciales(Rol rol) {
        // Arrange
        objetivo.setRoles(Set.of(rol, rol == Rol.COMPRADOR ? Rol.ADMIN_ALMACEN : Rol.COMPRADOR));
        var originales = Set.copyOf(objetivo.getRoles()); credencialOperativa(actor, HASH);
        // Act
        var r = service.cambiar(2L, principal, request(EstadoCuenta.BLOQUEADA, "Motivo"));
        // Assert
        assertThat(r.roles()).isEqualTo(originales); assertThat(objetivo.getRoles()).isEqualTo(originales);
        verify(credenciales, never()).save(any()); verify(credenciales, never()).delete(any());
    }

    @ParameterizedTest @EnumSource(value = EstadoCuenta.class, names = {"INACTIVA", "BLOQUEADA"})
    void ultimoAdministradorNoCuentaActorSinCredencial(EstadoCuenta estado) {
        // Arrange
        objetivo.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(estado, "Motivo")))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("administrador operativo");
        assertThat(objetivo.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); verify(usuarios, never()).flush();
    }

    @Test void administradorHistoricoConCredencialSiCuenta() {
        // Arrange
        objetivo.setRoles(Set.of(Rol.ADMIN_STYLERADAR)); actor.setEstadoCuenta(null); credencialOperativa(actor, HASH);
        // Act / Assert
        assertThat(service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null)).estadoCuenta()).isEqualTo(EstadoCuenta.INACTIVA);
        assertThat(actor.getEstadoCuenta()).isNull();
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"hash-inválido", "$2b$00$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void hashNoUtilizableNoCuentaComoRecuperable(String hash) {
        // Arrange
        objetivo.setRoles(Set.of(Rol.ADMIN_STYLERADAR)); credencialOperativa(actor, hash);
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test void correoAmbiguoNoCuentaComoOperativo() {
        // Arrange
        objetivo.setRoles(Set.of(Rol.ADMIN_STYLERADAR)); credencialOperativa(actor, HASH);
        when(usuarios.findAllByEmailNormalizado(actor.getEmail())).thenReturn(List.of(actor, objetivo));
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test void correoHistoricoInvalidoNoCuentaComoOperativo() {
        // Arrange
        objetivo.setRoles(Set.of(Rol.ADMIN_STYLERADAR)); actor.setEmail("no-es-email"); credencialOperativa(actor, HASH);
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test void administradoresInactivosYCompradoresNoCuentan() {
        // Arrange
        objetivo.setRoles(Set.of(Rol.ADMIN_STYLERADAR));
        var inactivo = cuenta(3L, Rol.ADMIN_STYLERADAR); inactivo.setEstadoCuenta(EstadoCuenta.INACTIVA); filas.put(3L, inactivo);
        filas.put(4L, cuenta(4L, Rol.COMPRADOR));
        // Act / Assert
        assertThatThrownBy(() -> service.cambiar(2L, principal, request(EstadoCuenta.INACTIVA, null))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test void consultaPaginaIdsYReordenaCargaPorLote() {
        // Arrange
        var pageable = PageRequest.of(0, 2);
        when(usuarios.findIdsAdministracion(null, pageable)).thenReturn(new PageImpl<>(List.of(1L, 2L), pageable, 3));
        when(usuarios.findAdministracionByIdIn(List.of(1L, 2L))).thenReturn(List.of(objetivo, actor));
        // Act / Assert
        var p = service.consultar(null, 0, 2, principal);
        assertThat(p).extracting(r -> r.id()).containsExactly(1L, 2L); assertThat(p.getTotalElements()).isEqualTo(3);
    }
    @Test void paginaVaciaYDesbordada() {
        // Arrange
        when(usuarios.findIdsAdministracion(null, PageRequest.of(0, 20))).thenReturn(Page.empty(PageRequest.of(0, 20)));
        when(usuarios.countAdministracion(null)).thenReturn(3L);
        // Act / Assert
        assertThat(service.consultar(null, 0, 20, principal)).isEmpty();
        assertThat(service.consultar(null, Integer.MAX_VALUE, 100, principal).getTotalElements()).isEqualTo(3);
        verify(usuarios, never()).findAdministracionByIdIn(any());
    }
    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,101"})
    void paginacionInvalidaEnServicio(int page, int size) {
        // Act / Assert
        assertThatThrownBy(() -> service.consultar(null, page, size, principal)).isInstanceOf(ReglaDeNegocioException.class);
    }
    @Test void detalleHistoricoYRolesLegadosSinModificarEntidad() {
        // Arrange
        objetivo.setEstadoCuenta(null); objetivo.setRoles(null);
        when(usuarios.findAdministracionById(2L)).thenReturn(Optional.of(objetivo));
        // Act / Assert
        var r = service.consultarUno(2L, principal);
        assertThat(r.estadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA); assertThat(r.roles()).containsExactly(Rol.COMPRADOR);
        assertThat(r.fechaCambioEstado()).isNull(); assertThat(objetivo.getEstadoCuenta()).isNull();
    }
}
