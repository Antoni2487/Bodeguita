package io.bootify.my_tiendita.notificacion;

import io.bootify.my_tiendita.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notificaciones")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private String tipo; 

    @Column(nullable = false)
    private Boolean leido = false;

    @Column
    private String urlDestino; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_destino_id", nullable = false)
    private Usuario usuarioDestino;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;
}