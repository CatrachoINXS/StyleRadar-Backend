package edu.dosw.proyecto.style_radar.model.entity;

import edu.dosw.proyecto.style_radar.model.domain.UnidadPeriodoSuscripcion;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity @Table(name = "planes_suscripcion")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Check(constraints = "periodo > 0 and unidad_duracion = 'MESES_CALENDARIO'")
public class PlanSuscripcionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150)
    private String nombre;
    @NotNull @Positive @Column(nullable = false)
    private Integer periodo;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "unidad_duracion", nullable = false)
    private UnidadPeriodoSuscripcion unidadDuracion;
    @Column(nullable = false)
    private boolean activo;
}
