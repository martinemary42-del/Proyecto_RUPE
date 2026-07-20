package mx.edu.unadm.rupe.avistamiento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;

@Entity
@Table(name = "avistamiento")
public class Avistamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avistamiento")
    private Integer idAvistamiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reporte")
    private ReporteExtravio reporte;

    @Column(name = "folio_avistamiento", length = 20, unique = true)
    private String folioAvistamiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estatus", nullable = false)
    private CatalogoEstatus estatus;

    @Column(name = "fecha_avistamiento", nullable = false)
    private LocalDate fechaAvistamiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_colonia", nullable = false)
    private CatalogoColonia colonia;

    @Column(length = 120)
    private String calle;

    @Column(length = 20)
    private String numero;

    @Column(columnDefinition = "TEXT")
    private String referencias;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "foto_avistamiento", length = 250)
    private String fotoAvistamiento;

    @Lob
    @Column(name = "foto_contenido", columnDefinition = "LONGBLOB")
    private byte[] fotoContenido;

    @Column(name = "foto_tipo_contenido", length = 80)
    private String fotoTipoContenido;

    @Column(name = "foto_nombre_archivo", length = 180)
    private String fotoNombreArchivo;

    @Column(nullable = false)
    private Boolean resguardado = false;

    @Column(name = "nombre_resguardante", length = 120)
    private String nombreResguardante;

    @Column(name = "correo_resguardante", length = 120)
    private String correoResguardante;

    @Column(name = "telefono_resguardante", length = 20)
    private String telefonoResguardante;

    @Column(name = "validado_dueno", nullable = false)
    private Boolean validadoDueno = false;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "comentario_validacion", length = 500)
    private String comentarioValidacion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public Integer getIdAvistamiento() { return idAvistamiento; }
    public String getFolioAvistamiento() { return folioAvistamiento; }
    public void setFolioAvistamiento(String folioAvistamiento) { this.folioAvistamiento = folioAvistamiento; }
    public ReporteExtravio getReporte() { return reporte; }
    public void setReporte(ReporteExtravio reporte) { this.reporte = reporte; }
    public CatalogoEstatus getEstatus() { return estatus; }
    public void setEstatus(CatalogoEstatus estatus) { this.estatus = estatus; }
    public LocalDate getFechaAvistamiento() { return fechaAvistamiento; }
    public void setFechaAvistamiento(LocalDate fechaAvistamiento) { this.fechaAvistamiento = fechaAvistamiento; }
    public String getEstado() { return colonia != null ? colonia.getMunicipio().getEstado().getNombre() : null; }
    public String getMunicipio() { return colonia != null ? colonia.getMunicipio().getNombre() : null; }
    public String getColonia() { return colonia != null ? colonia.getNombre() : null; }
    public String getCodigoPostal() { return colonia != null ? colonia.getCodigoPostal() : null; }
    public CatalogoColonia getColoniaCatalogo() { return colonia; }
    public void setColonia(CatalogoColonia colonia) { this.colonia = colonia; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getReferencias() { return referencias; }
    public void setReferencias(String referencias) { this.referencias = referencias; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getFotoAvistamiento() { return fotoAvistamiento; }
    public void setFotoAvistamiento(String fotoAvistamiento) { this.fotoAvistamiento = fotoAvistamiento; }
    public byte[] getFotoContenido() { return fotoContenido; }
    public void setFotoContenido(byte[] fotoContenido) { this.fotoContenido = fotoContenido; }
    public String getFotoTipoContenido() { return fotoTipoContenido; }
    public void setFotoTipoContenido(String fotoTipoContenido) { this.fotoTipoContenido = fotoTipoContenido; }
    public String getFotoNombreArchivo() { return fotoNombreArchivo; }
    public void setFotoNombreArchivo(String fotoNombreArchivo) { this.fotoNombreArchivo = fotoNombreArchivo; }
    public Boolean getResguardado() { return resguardado; }
    public void setResguardado(Boolean resguardado) { this.resguardado = resguardado; }
    public String getNombreResguardante() { return nombreResguardante; }
    public void setNombreResguardante(String nombreResguardante) { this.nombreResguardante = nombreResguardante; }
    public String getCorreoResguardante() { return correoResguardante; }
    public void setCorreoResguardante(String correoResguardante) { this.correoResguardante = correoResguardante; }
    public String getTelefonoResguardante() { return telefonoResguardante; }
    public void setTelefonoResguardante(String telefonoResguardante) { this.telefonoResguardante = telefonoResguardante; }
    public Boolean getValidadoDueno() { return validadoDueno; }
    public void setValidadoDueno(Boolean validadoDueno) { this.validadoDueno = validadoDueno; }
    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
    public void setFechaValidacion(LocalDateTime fechaValidacion) { this.fechaValidacion = fechaValidacion; }
    public String getComentarioValidacion() { return comentarioValidacion; }
    public void setComentarioValidacion(String comentarioValidacion) { this.comentarioValidacion = comentarioValidacion; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
