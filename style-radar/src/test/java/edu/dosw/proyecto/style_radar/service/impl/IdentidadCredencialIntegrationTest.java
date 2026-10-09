package edu.dosw.proyecto.style_radar.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.mapper.UsuarioEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.domain.Estilo;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.entity.CredencialEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.CredencialRepository;
import edu.dosw.proyecto.style_radar.repository.UsuarioRepository;
import edu.dosw.proyecto.style_radar.service.ICredencialService;
import edu.dosw.proyecto.style_radar.service.IUsuarioService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentidadCredencialIntegrationTest {

    private static final String PASSWORD = "clave de integración";

    @Autowired
    private IUsuarioService usuarioService;
    @Autowired
    private ICredencialService credencialService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CredencialRepository credencialRepository;
    @Autowired
    private UsuarioEntityMapper usuarioEntityMapper;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void registroDebePersistirCompradorYActivaSinExigirCredencial() {
        // Arrange
        Usuario nuevo = Usuario.builder().nombre("Comprador").email("nuevo@ejemplo.co").build();

        // Act
        Usuario registrado = usuarioService.registrarUsuario(nuevo);
        entityManager.flush();
        entityManager.clear();
        UsuarioEntity persistido = usuarioRepository.findById(registrado.getId()).orElseThrow();

        // Assert
        assertThat(persistido.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(persistido.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(registrado.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(registrado.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(credencialRepository.existsById(registrado.getId())).isFalse();
    }

    @Test
    void servicioDeRegistroNoDebeConfiarEnRolesOEstadoDeEntrada() {
        // Arrange
        Usuario entrada = Usuario.builder().nombre("Registro").email("entrada@ejemplo.co")
                .roles(Set.of(Rol.ADMIN_STYLERADAR, Rol.ADMIN_ALMACEN, Rol.REPRESENTANTE_FUNDACION))
                .estadoCuenta(EstadoCuenta.BLOQUEADA).build();

        // Act
        Usuario registrado = usuarioService.registrarUsuario(entrada);

        // Assert
        assertThat(registrado.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(registrado.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
    }

    @Test
    void clienteNoDebeAsignarRolesPrivilegiadosNiEstadoEnElRegistroPublico() throws Exception {
        // Arrange
        String request = """
                {"nombre":"Registro HTTP","email":"http@ejemplo.co",
                 "roles":["ADMIN_STYLERADAR","ADMIN_ALMACEN","REPRESENTANTE_FUNDACION"],
                 "estadoCuenta":"BLOQUEADA"}
                """;

        // Act
        mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        UsuarioEntity registrado = usuarioRepository.findByEmail("http@ejemplo.co").orElseThrow();

        // Assert
        assertThat(registrado.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(registrado.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(credencialRepository.existsById(registrado.getId())).isFalse();
    }

    @ParameterizedTest
    @EnumSource(EstadoCuenta.class)
    void debePersistirMultiplesRolesYEstadoSinAlterarPreferencias(EstadoCuenta estado) {
        // Arrange
        UsuarioEntity usuario = usuario("roles@ejemplo.co");
        Set<Rol> roles = Set.of(Rol.values());
        usuario.setRoles(new HashSet<>(roles));
        usuario.setEstadoCuenta(estado);
        usuario.setPreferenciasEstilo(new HashSet<>(Set.of(Estilo.CASUAL)));
        usuario.setTallasHabituales(new HashSet<>(Set.of(Talla.M)));

        // Act
        usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();
        Usuario recuperado = usuarioService.obtenerPorId(usuario.getId());

        // Assert
        assertThat(recuperado.getRoles()).containsExactlyInAnyOrderElementsOf(roles);
        assertThat(recuperado.getEstadoCuenta()).isEqualTo(estado);
        assertThat(recuperado.getPreferenciasEstilo()).containsExactly(Estilo.CASUAL);
        assertThat(recuperado.getTallasHabituales()).containsExactly(Talla.M);
    }

    @Test
    void usuarioAnteriorDebeLeerseSinCredencialYConCompatibilidadExplicita() {
        // Arrange
        UsuarioEntity antiguo = usuario("antiguo@ejemplo.co");
        antiguo.setRoles(new HashSet<>());
        antiguo.setEstadoCuenta(null);
        antiguo.setPreferenciasEstilo(new HashSet<>(Set.of(Estilo.FORMAL)));
        usuarioRepository.saveAndFlush(antiguo);
        entityManager.clear();

        // Act
        Usuario leido = usuarioService.obtenerPorId(antiguo.getId());
        UsuarioEntity persistido = usuarioRepository.findById(antiguo.getId()).orElseThrow();

        // Assert
        assertThat(leido.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(leido.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(leido.getPreferenciasEstilo()).containsExactly(Estilo.FORMAL);
        assertThat(persistido.getRoles()).isEmpty();
        assertThat(persistido.getEstadoCuenta()).isNull();
        assertThat(credencialRepository.existsById(antiguo.getId())).isFalse();
        assertThat(credencialService.verificarPassword(antiguo.getId(), PASSWORD)).isFalse();
    }

    @Test
    void mapperDebeCompatibilizarRolesNulosSinModificarLaEntidad() {
        // Arrange
        UsuarioEntity antiguo = usuario("mapper@ejemplo.co");
        antiguo.setRoles(null);
        antiguo.setEstadoCuenta(null);

        // Act
        Usuario leido = usuarioEntityMapper.toDomain(antiguo);

        // Assert
        assertThat(leido.getRoles()).containsExactly(Rol.COMPRADOR);
        assertThat(leido.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(antiguo.getRoles()).isNull();
        assertThat(antiguo.getEstadoCuenta()).isNull();
    }

    @Test
    void credencialDebeCompartirElIdYGuardarBcryptConSalt() {
        // Arrange
        UsuarioEntity primero = usuarioRepository.saveAndFlush(usuario("primero@ejemplo.co"));
        UsuarioEntity segundo = usuarioRepository.saveAndFlush(usuario("segundo@ejemplo.co"));

        // Act
        credencialService.crearCredencial(primero.getId(), PASSWORD);
        credencialService.crearCredencial(segundo.getId(), PASSWORD);
        entityManager.clear();
        CredencialEntity credencial = credencialRepository.findById(primero.getId()).orElseThrow();
        CredencialEntity otra = credencialRepository.findById(segundo.getId()).orElseThrow();

        // Assert
        assertThat(credencial.getUsuarioId()).isEqualTo(primero.getId());
        assertThat(credencial.getUsuario().getId()).isEqualTo(primero.getId());
        assertThat(credencial.getPasswordHash()).startsWith("$2b$10$").hasSize(60).isNotEqualTo(PASSWORD);
        assertThat(credencial.getPasswordHash()).isNotEqualTo(otra.getPasswordHash());
        assertThat(passwordEncoder.matches(PASSWORD, otra.getPasswordHash())).isTrue();
        assertThat(credencialService.verificarPassword(primero.getId(), PASSWORD)).isTrue();
        assertThat(credencialService.verificarPassword(primero.getId(), "clave incorrecta")).isFalse();
    }

    @Test
    void credencialDuplicadaDebeRechazarseSinReemplazarElHash() {
        // Arrange
        UsuarioEntity usuario = usuarioRepository.saveAndFlush(usuario("duplicado@ejemplo.co"));
        credencialService.crearCredencial(usuario.getId(), PASSWORD);
        String hashOriginal = credencialRepository.findById(usuario.getId()).orElseThrow().getPasswordHash();

        // Act
        // Assert
        assertThatThrownBy(() -> credencialService.crearCredencial(usuario.getId(), "clave de reemplazo"))
                .isInstanceOf(ReglaDeNegocioException.class);
        entityManager.clear();
        assertThat(credencialRepository.findById(usuario.getId()).orElseThrow().getPasswordHash())
                .isEqualTo(hashOriginal);
    }

    @Test
    void persistenciaDebeRechazarCredencialSinUsuario() {
        // Arrange
        CredencialEntity huerfana = new CredencialEntity(null, passwordEncoder.encode(PASSWORD));

        // Act
        // Assert
        assertThatThrownBy(() -> {
            entityManager.persist(huerfana);
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    void respuestaDeUsuarioNoDebeExponerHashNiCambiarSuContrato() throws Exception {
        // Arrange
        Usuario registrado = usuarioService.registrarUsuario(
                Usuario.builder().nombre("Perfil").email("perfil@ejemplo.co").build());
        credencialService.crearCredencial(registrado.getId(), PASSWORD);
        String hash = credencialRepository.findById(registrado.getId()).orElseThrow().getPasswordHash();

        // Act
        String response = mockMvc.perform(get("/api/v1/usuarios/{id}", registrado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("perfil@ejemplo.co"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(jsonPath("$.estadoCuenta").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(response).doesNotContain(PASSWORD, hash, "password", "credencial");
    }

    @Test
    void entidadCredencialNoDebeSerializarNiImprimirElHash() {
        // Arrange
        UsuarioEntity usuario = usuarioRepository.saveAndFlush(usuario("serializacion@ejemplo.co"));
        credencialService.crearCredencial(usuario.getId(), PASSWORD);
        CredencialEntity credencial = credencialRepository.findById(usuario.getId()).orElseThrow();

        // Act
        String json = JsonMapper.builder().build().writeValueAsString(credencial);
        String descripcion = credencial.toString();

        // Assert
        assertThat(json).doesNotContain("passwordHash", "password", credencial.getPasswordHash(), PASSWORD);
        assertThat(descripcion).doesNotContain(credencial.getPasswordHash(), PASSWORD);
    }

    private UsuarioEntity usuario(String email) {
        return UsuarioEntity.builder().nombre("Usuario").email(email)
                .fechaRegistro(Instant.parse("2026-10-09T12:00:00Z")).build();
    }
}
