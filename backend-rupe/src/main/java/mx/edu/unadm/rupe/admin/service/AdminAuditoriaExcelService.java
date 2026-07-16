package mx.edu.unadm.rupe.admin.service;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import mx.edu.unadm.rupe.admin.dto.AdminBitacoraResponse;
import mx.edu.unadm.rupe.admin.dto.AdminCatalogoResponse;
import mx.edu.unadm.rupe.contacto.dto.MensajeContactoResponse;
import mx.edu.unadm.rupe.contacto.service.MensajeContactoService;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditoriaExcelService {
    private final AdminService adminService;
    private final AdminCatalogoService catalogoService;
    private final MensajeContactoService contactoService;

    public AdminAuditoriaExcelService(AdminService adminService, AdminCatalogoService catalogoService,
            MensajeContactoService contactoService) {
        this.adminService = adminService;
        this.catalogoService = catalogoService;
        this.contactoService = contactoService;
    }

    public byte[] generarBitacora(HttpSession session) {
        List<AdminBitacoraResponse> eventos = adminService.bitacora(session);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(workbook);
            Sheet sheet = workbook.createSheet("Bitacora");
            titulo(sheet, estilos, "RUPE - Bitacora de seguridad");
            Object[][] filas = new Object[eventos.size() + 1][7];
            filas[0] = new Object[] {"Fecha", "Correo", "Modulo", "Accion", "Resultado", "IP", "Descripcion"};
            for (int i = 0; i < eventos.size(); i++) {
                AdminBitacoraResponse e = eventos.get(i);
                filas[i + 1] = new Object[] {e.getFechaHora(), e.getCorreo(), e.getModulo(), e.getAccion(), e.getResultado(), e.getIp(), e.getDescripcion()};
            }
            escribirTabla(sheet, estilos, 2, filas);
            ajustar(sheet, 7);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel de bitacora.", ex);
        }
    }

    public byte[] generarSoporte(HttpSession session) {
        List<MensajeContactoResponse> mensajes = contactoService.listarAdmin(session);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(workbook);
            Sheet sheet = workbook.createSheet("Soporte");
            titulo(sheet, estilos, "RUPE - Mensajes de soporte");
            Object[][] filas = new Object[mensajes.size() + 1][8];
            filas[0] = new Object[] {"Fecha", "Nombre", "Correo", "Telefono", "Asunto", "Mensaje", "Estatus", "Respuesta admin"};
            for (int i = 0; i < mensajes.size(); i++) {
                MensajeContactoResponse m = mensajes.get(i);
                filas[i + 1] = new Object[] {m.getFechaRegistro(), m.getNombre(), m.getCorreo(), m.getTelefono(), m.getAsunto(), m.getMensaje(), m.getEstatus(), m.getRespuestaAdmin()};
            }
            escribirTabla(sheet, estilos, 2, filas);
            ajustarSoporte(sheet);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel de soporte.", ex);
        }
    }

    public byte[] generarCatalogos(HttpSession session) {
        String[] catalogos = {"tipo", "raza", "color", "estado", "municipio", "colonia", "estatus", "rol"};
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(workbook);
            for (String catalogo : catalogos) {
                List<AdminCatalogoResponse> registros = catalogoService.listar(catalogo, session);
                Sheet sheet = workbook.createSheet(nombreHoja(catalogo));
                titulo(sheet, estilos, "RUPE - Catalogo " + nombreHoja(catalogo));
                Object[][] filas = new Object[registros.size() + 1][7];
                filas[0] = new Object[] {"ID", "Catalogo", "Nombre", "Activo", "Editable", "Dato asociado", "Detalle"};
                for (int i = 0; i < registros.size(); i++) {
                    AdminCatalogoResponse r = registros.get(i);
                    filas[i + 1] = new Object[] {
                        r.getId(), r.getCatalogo(), r.getNombre(), Boolean.TRUE.equals(r.getActivo()) ? "Activo" : "Inactivo",
                        Boolean.TRUE.equals(r.getEditable()) ? "Si" : "No", r.getPadre(), r.getExtra()
                    };
                }
                escribirTabla(sheet, estilos, 2, filas);
                ajustar(sheet, 7);
            }
            workbook.setActiveSheet(0);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel de catalogos.", ex);
        }
    }

    private void titulo(Sheet sheet, Estilos estilos, String texto) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(texto);
        cell.setCellStyle(estilos.titulo);
    }

    private void escribirTabla(Sheet sheet, Estilos estilos, int rowInicio, Object[][] filas) {
        for (int i = 0; i < filas.length; i++) {
            Row row = sheet.createRow(rowInicio + i);
            for (int j = 0; j < filas[i].length; j++) {
                Cell cell = row.createCell(j);
                Object valor = filas[i][j];
                cell.setCellValue(valor == null ? "" : valor.toString());
                cell.setCellStyle(i == 0 ? estilos.encabezado : estilos.celda);
            }
        }
        if (filas.length > 0) {
            sheet.createFreezePane(0, rowInicio + 1);
            sheet.setAutoFilter(new CellRangeAddress(rowInicio, rowInicio, 0, filas[0].length - 1));
        }
    }

    private void ajustar(Sheet sheet, int columnas) {
        for (int i = 0; i < columnas; i++) sheet.autoSizeColumn(i);
    }

    private void ajustarSoporte(Sheet sheet) {
        int[] anchos = {18, 24, 30, 16, 24, 55, 16, 55};
        for (int i = 0; i < anchos.length; i++) {
            sheet.setColumnWidth(i, anchos[i] * 256);
        }
        for (int i = 3; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                row.setHeightInPoints(72);
            }
        }
    }

    private String nombreHoja(String catalogo) {
        return switch (catalogo) {
            case "tipo" -> "Tipo";
            case "raza" -> "Razas";
            case "color" -> "Colores";
            case "estado" -> "Estados";
            case "municipio" -> "Municipios";
            case "colonia" -> "Colonias";
            case "estatus" -> "Estatus";
            case "rol" -> "Roles";
            default -> "Catalogo";
        };
    }

    private static class Estilos {
        private final CellStyle titulo;
        private final CellStyle encabezado;
        private final CellStyle celda;

        Estilos(XSSFWorkbook workbook) {
            Font tituloFont = workbook.createFont();
            tituloFont.setBold(true);
            tituloFont.setFontHeightInPoints((short) 14);
            tituloFont.setColor(IndexedColors.WHITE.getIndex());
            titulo = workbook.createCellStyle();
            titulo.setFont(tituloFont);
            ((XSSFCellStyle) titulo).setFillForegroundColor(new XSSFColor(new byte[] {(byte) 0x61, (byte) 0x12, (byte) 0x32}, null));
            titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            encabezado = workbook.createCellStyle();
            encabezado.setFont(headerFont);
            ((XSSFCellStyle) encabezado).setFillForegroundColor(new XSSFColor(new byte[] {(byte) 0xA5, (byte) 0x7F, (byte) 0x2C}, null));
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bordes(encabezado);

            celda = workbook.createCellStyle();
            celda.setWrapText(true);
            celda.setVerticalAlignment(VerticalAlignment.TOP);
            bordes(celda);
        }

        private void bordes(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
        }
    }
}
