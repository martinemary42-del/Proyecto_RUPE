package mx.edu.unadm.rupe.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalogo_raza")
public class CatalogoRaza {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_raza")
    private Integer idRaza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_mascota", nullable = false)
    private CatalogoTipoMascota tipoMascota;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdRaza() { return idRaza; }
    public void setIdRaza(Integer idRaza) { this.idRaza = idRaza; }
    public CatalogoTipoMascota getTipoMascota() { return tipoMascota; }
    public void setTipoMascota(CatalogoTipoMascota tipoMascota) { this.tipoMascota = tipoMascota; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
