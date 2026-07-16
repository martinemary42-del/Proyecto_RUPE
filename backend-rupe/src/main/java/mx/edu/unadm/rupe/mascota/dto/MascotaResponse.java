package mx.edu.unadm.rupe.mascota.dto;

import java.time.LocalDateTime;
import mx.edu.unadm.rupe.mascota.model.Mascota;

public class MascotaResponse {

    private Integer idMascota;
    private String nombre;
    private String sexo;
    private String tipoMascota;
    private String edadAproximada;
    private String raza;
    private String mezclaRaza;
    private String color;
    private String descripcionColor;
    private String senasParticulares;
    private String condicionMedica;
    private String collarPlaca;
    private String estadoReferencia;
    private String municipioReferencia;
    private String coloniaReferencia;
    private String codigoPostal;
    private String calle;
    private String numero;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private String fotoPrincipalUrl;
    private String mensaje;

    public MascotaResponse(Mascota mascota, String mensaje) {
        this(mascota, mensaje, null);
    }

    public MascotaResponse(Mascota mascota, String mensaje, String fotoPrincipalUrl) {
        this.idMascota = mascota.getIdMascota();
        this.nombre = mascota.getNombre();
        this.sexo = mascota.getSexo();
        this.tipoMascota = mascota.getTipoMascota();
        this.edadAproximada = mascota.getEdadAproximada();
        this.raza = mascota.getRaza();
        this.mezclaRaza = mascota.getMezclaRaza();
        this.color = mascota.getColor();
        this.descripcionColor = mascota.getDescripcionColor();
        this.senasParticulares = mascota.getSenasParticulares();
        this.condicionMedica = mascota.getCondicionMedica();
        this.collarPlaca = mascota.getCollarPlaca();
        this.estadoReferencia = mascota.getEstadoReferencia();
        this.municipioReferencia = mascota.getMunicipioReferencia();
        this.coloniaReferencia = mascota.getColoniaReferencia();
        this.codigoPostal = mascota.getCodigoPostal();
        this.calle = mascota.getCalle();
        this.numero = mascota.getNumero();
        this.activo = mascota.getActivo();
        this.fechaRegistro = mascota.getFechaRegistro();
        this.fechaActualizacion = mascota.getFechaActualizacion();
        this.mensaje = mensaje;
        this.fotoPrincipalUrl = fotoPrincipalUrl;
    }

    public Integer getIdMascota() { return idMascota; }
    public String getNombre() { return nombre; }
    public String getSexo() { return sexo; }
    public String getTipoMascota() { return tipoMascota; }
    public String getEdadAproximada() { return edadAproximada; }
    public String getRaza() { return raza; }
    public String getMezclaRaza() { return mezclaRaza; }
    public String getColor() { return color; }
    public String getDescripcionColor() { return descripcionColor; }
    public String getSenasParticulares() { return senasParticulares; }
    public String getCondicionMedica() { return condicionMedica; }
    public String getCollarPlaca() { return collarPlaca; }
    public String getEstadoReferencia() { return estadoReferencia; }
    public String getMunicipioReferencia() { return municipioReferencia; }
    public String getColoniaReferencia() { return coloniaReferencia; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getCalle() { return calle; }
    public String getNumero() { return numero; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public String getMensaje() { return mensaje; }
    public String getFotoPrincipalUrl() { return fotoPrincipalUrl; }
}
