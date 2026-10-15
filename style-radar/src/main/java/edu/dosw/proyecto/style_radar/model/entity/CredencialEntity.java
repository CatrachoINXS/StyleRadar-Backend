package edu.dosw.proyecto.style_radar.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credenciales")
@Getter
@NoArgsConstructor
public class CredencialEntity {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private UsuarioEntity usuario;

    @Column(name = "password_hash", nullable = false, length = 60)
    @JsonIgnore
    private String passwordHash;

    // Sin cascade: crear una credencial nunca crea ni modifica la identidad.
    public CredencialEntity(UsuarioEntity usuario, String passwordHash) {
        this.usuario = usuario;
        this.passwordHash = passwordHash;
    }
}
