package mx.edu.unadm.rupe.reporte.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import mx.edu.unadm.rupe.mascota.model.Mascota;

@Entity
@Table(name = "reporte_extravio")
public class ReporteExtravio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mascota", nullable = false)
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estatus", nullable = false)
    private CatalogoEstatus estatus;

    @Column(nullable = false, unique = true, length = 40)
    private String folio;

    @Column(name = "qr_url", length = 250)
    private String qrUrl;

    @Column(name = "fecha_extravio", nullable = false)
    private LocalDate fechaExtravio;

    @Column(name = "tipo_reporte", nullable = false, length = 20)
    private String tipoReporte = "EXTRAVIO";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_colonia", nullable = false)
    private CatalogoColonia colonia;

    @Column(length = 120)
    private String calle;

    @Column(length = 20)
    private String numero;

    @Column(columnDefinition = "TEXT")
    private String referencias;

    @Column(name = "descripcion_hechos", nullable = false, columnDefinition = "TEXT")
    private String descripcionHechos;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento = LocalDate.now().plusDays(30);

    @Column(name = "fecha_ultima_renovacion")
    private LocalDateTime fechaUltimaRenovacion;

    @Column(name = "renovaciones", nullable = false)
    private Integer renovaciones = 0;

    @Column(name = "requiere_renovacion", nullable = false)
    private Boolean requiereRenovacion = false;

    public Integer getIdReporte() { return idReporte; }
    public void setIdReporte(Integer idReporte) { this.idReporte = idReporte; }
    public Mascota getMascota() { return mascota; }
    public void setMascota(Mascota mascota) { this.mascota = mascota; }
    public CatalogoEstatus getEstatus() { return estatus; }
    public void setEstatus(CatalogoEstatus estatus) { this.estatus = estatus; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public String getQrUrl() { return qrUrl; }
    public void setQrUrl(String qrUrl) { this.qrUrl = qrUrl; }
    public LocalDate getFechaExtravio() { return fechaExtravio; }
    public void setFechaExtravio(LocalDate fechaExtravio) { this.fechaExtravio = fechaExtravio; }
    public String getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }
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
    public String getDescripcionHechos() { return descripcionHechos; }
    public void setDescripcionHechos(String descripcionHechos) { this.descripcionHechos = descripcionHechos; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public LocalDateTime getFechaUltimaRenovacion() { return fechaUltimaRenovacion; }
    public void setFechaUltimaRenovacion(LocalDateTime fechaUltimaRenovacion) { this.fechaUltimaRenovacion = fechaUltimaRenovacion; }
    public Integer getRenovaciones() { return renovaciones; }
    public void setRenovaciones(Integer renovaciones) { this.renovaciones = renovaciones; }
    public Boolean getRequiereRenovacion() { return requiereRenovacion; }
    public void setRequiereRenovacion(Boolean requiereRenovacion) { this.requiereRenovacion = requiereRenovacion; }
}
