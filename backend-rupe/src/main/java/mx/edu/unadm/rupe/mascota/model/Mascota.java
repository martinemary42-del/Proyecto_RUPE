package mx.edu.unadm.rupe.mascota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColor;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRaza;
import mx.edu.unadm.rupe.catalogo.model.CatalogoTipoMascota;
import mx.edu.unadm.rupe.usuario.model.Usuario;

@Entity
@Table(name = "mascota")
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mascota")
    private Integer idMascota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String sexo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_mascota", nullable = false)
    private CatalogoTipoMascota tipoMascota;

    @Column(name = "edad_aproximada", length = 60)
    private String edadAproximada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_raza", nullable = false)
    private CatalogoRaza raza;

    @Column(name = "mezcla_raza", length = 120)
    private String mezclaRaza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_color", nullable = false)
    private CatalogoColor color;

    @Column(name = "descripcion_color", length = 180)
    private String descripcionColor;

    @Column(name = "senas_particulares", nullable = false, columnDefinition = "TEXT")
    private String senasParticulares;

    @Column(name = "condicion_medica", columnDefinition = "TEXT")
    private String condicionMedica;

    @Column(name = "collar_placa", nullable = false, length = 40)
    private String collarPlaca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_colonia_referencia", nullable = false)
    private CatalogoColonia coloniaReferencia;

    @Column(length = 120)
    private String calle;

    @Column(length = 20)
    private String numero;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public Integer getIdMascota() { return idMascota; }
    public void setIdMascota(Integer idMascota) { this.idMascota = idMascota; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getTipoMascota() { return tipoMascota != null ? tipoMascota.getNombre() : null; }
    public CatalogoTipoMascota getTipoMascotaCatalogo() { return tipoMascota; }
    public void setTipoMascota(CatalogoTipoMascota tipoMascota) { this.tipoMascota = tipoMascota; }
    public String getEdadAproximada() { return edadAproximada; }
    public void setEdadAproximada(String edadAproximada) { this.edadAproximada = edadAproximada; }
    public String getRaza() { return raza != null ? raza.getNombre() : null; }
    public CatalogoRaza getRazaCatalogo() { return raza; }
    public void setRaza(CatalogoRaza raza) { this.raza = raza; }
    public String getMezclaRaza() { return mezclaRaza; }
    public void setMezclaRaza(String mezclaRaza) { this.mezclaRaza = mezclaRaza; }
    public String getColor() { return color != null ? color.getNombre() : null; }
    public CatalogoColor getColorCatalogo() { return color; }
    public void setColor(CatalogoColor color) { this.color = color; }
    public String getDescripcionColor() { return descripcionColor; }
    public void setDescripcionColor(String descripcionColor) { this.descripcionColor = descripcionColor; }
    public String getSenasParticulares() { return senasParticulares; }
    public void setSenasParticulares(String senasParticulares) { this.senasParticulares = senasParticulares; }
    public String getCondicionMedica() { return condicionMedica; }
    public void setCondicionMedica(String condicionMedica) { this.condicionMedica = condicionMedica; }
    public String getCollarPlaca() { return collarPlaca; }
    public void setCollarPlaca(String collarPlaca) { this.collarPlaca = collarPlaca; }
    public String getEstadoReferencia() {
        return coloniaReferencia != null ? coloniaReferencia.getMunicipio().getEstado().getNombre() : null;
    }
    public String getMunicipioReferencia() {
        return coloniaReferencia != null ? coloniaReferencia.getMunicipio().getNombre() : null;
    }
    public String getColoniaReferencia() { return coloniaReferencia != null ? coloniaReferencia.getNombre() : null; }
    public String getCodigoPostal() { return coloniaReferencia != null ? coloniaReferencia.getCodigoPostal() : null; }
    public CatalogoColonia getColoniaReferenciaCatalogo() { return coloniaReferencia; }
    public void setColoniaReferencia(CatalogoColonia coloniaReferencia) { this.coloniaReferencia = coloniaReferencia; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
