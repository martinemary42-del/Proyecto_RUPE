package mx.edu.unadm.rupe.admin.service;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mx.edu.unadm.rupe.admin.dto.AdminReporteResponse;
import mx.edu.unadm.rupe.admin.dto.AdminResumenResponse;
import mx.edu.unadm.rupe.admin.dto.VisitaPaginaResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class AdminEstadisticasExcelService {
    private final AdminService adminService;

    public AdminEstadisticasExcelService(AdminService adminService) {
        this.adminService = adminService;
    }

    public byte[] generarExcel(HttpSession session, String texto, String estatus, LocalDate desde, LocalDate hasta) {
        AdminResumenResponse resumen = adminService.resumen(session);
        List<AdminReporteResponse> reportes = filtrar(adminService.reportes(session), texto, estatus, desde, hasta);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(workbook);
            crearResumen(workbook, estilos, resumen, reportes);
            crearEstatus(workbook, estilos, reportes);
            crearVigencia(workbook, estilos, reportes);
            crearZonas(workbook, estilos, reportes);
            crearVisitas(workbook, estilos, resumen.getVisitasPorPagina());
            crearSoporte(workbook, estilos, resumen);
            crearDetalle(workbook, estilos, reportes);
            crearNotas(workbook, estilos, texto, estatus, desde, hasta);
            workbook.setActiveSheet(0);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", ex);
        }
    }

    private List<AdminReporteResponse> filtrar(List<AdminReporteResponse> reportes, String texto, String estatus,
            LocalDate desde, LocalDate hasta) {
        String busqueda = normalizar(texto);
        String estatusFiltro = estatus == null ? "" : estatus.trim().toUpperCase(Locale.ROOT);
        return reportes.stream()
            .filter(r -> busqueda.isBlank() || normalizar(String.join(" ",
                valor(r.getFolio()), valor(r.getMascota()), valor(r.getRaza()), valor(r.getZona()))).contains(busqueda))
            .filter(r -> estatusFiltro.isBlank() || valor(r.getEstatus()).toUpperCase(Locale.ROOT).equals(estatusFiltro))
            .filter(r -> {
                LocalDate fecha = r.getFechaRegistro() != null ? r.getFechaRegistro().toLocalDate() : r.getFechaExtravio();
                if (fecha == null) return true;
                return (desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta));
            })
            .toList();
    }

    private void crearResumen(XSSFWorkbook workbook, Estilos estilos, AdminResumenResponse resumen,
            List<AdminReporteResponse> reportes) {
        Sheet sheet = workbook.createSheet("Resumen");
        titulo(sheet, estilos, "RUPE - Resumen general de estadisticas");
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[] {"Indicador", "Valor"});
        filas.add(new Object[] {"Perritos registrados", resumen.getMascotasRegistradas()});
        filas.add(new Object[] {"Reportes activos filtrados", contarEstatus(reportes, "ACTIVO")});
        filas.add(new Object[] {"Reportes por extravio", resumen.getReportesExtravio()});
        filas.add(new Object[] {"Reportes por robo", resumen.getReportesRobo()});
        filas.add(new Object[] {"Perritos recuperados filtrados", contarRecuperados(reportes)});
        filas.add(new Object[] {"Avistamientos filtrados", reportes.stream().mapToLong(r -> nuloCero(r.getAvistamientos())).sum()});
        filas.add(new Object[] {"Avistamientos pendientes de validacion", resumen.getAvistamientosPendientesValidacion()});
        filas.add(new Object[] {"Pendientes de renovacion filtrados", contarPendientes(reportes)});
        filas.add(new Object[] {"Cuentas bloqueadas temporalmente", resumen.getUsuariosBloqueados()});
        filas.add(new Object[] {"Visitas publicas", resumen.getVisitasTotales()});
        filas.add(new Object[] {"Mensajes de contacto", resumen.getMensajesContactoTotal()});
        filas.add(new Object[] {"Mensajes pendientes de soporte", resumen.getMensajesContactoPendientes()});
        filas.add(new Object[] {"Mensajes atendidos de soporte", resumen.getMensajesContactoAtendidos()});
        escribirTabla(sheet, estilos, 2, filas);
        ajustar(sheet, 2);
    }

    private void crearEstatus(XSSFWorkbook workbook, Estilos estilos, List<AdminReporteResponse> reportes) {
        Sheet sheet = workbook.createSheet("Estatus");
        titulo(sheet, estilos, "RUPE - Reportes por estatus");
        List<Object[]> filas = mapaConteo(reportes, r -> valor(r.getEstatus()).toUpperCase(Locale.ROOT), "Estatus", "Cantidad");
        escribirTabla(sheet, estilos, 2, filas);
        agregarGraficaBarras(sheet, "Reportes por estatus", 2, filas.size(), 4, 2, 13, 14);
        ajustar(sheet, 3);
    }

    private void crearVigencia(XSSFWorkbook workbook, Estilos estilos, List<AdminReporteResponse> reportes) {
        Sheet sheet = workbook.createSheet("Vigencia");
        titulo(sheet, estilos, "RUPE - Vigencia y renovacion de reportes");
        Map<String, Long> datos = new LinkedHashMap<>();
        datos.put("Vigentes", 0L);
        datos.put("Por vencer", 0L);
        datos.put("Pendientes de renovacion", 0L);
        datos.put("No aplica", 0L);
        for (AdminReporteResponse r : reportes) {
            String estado = valor(r.getEstatus()).toUpperCase(Locale.ROOT);
            if (!"ACTIVO".equals(estado)) datos.computeIfPresent("No aplica", (k, v) -> v + 1);
            else if (Boolean.TRUE.equals(r.getRequiereRenovacion()) || (r.getDiasParaVencer() != null && r.getDiasParaVencer() < 0)) datos.computeIfPresent("Pendientes de renovacion", (k, v) -> v + 1);
            else if (r.getDiasParaVencer() != null && r.getDiasParaVencer() <= 5) datos.computeIfPresent("Por vencer", (k, v) -> v + 1);
            else datos.computeIfPresent("Vigentes", (k, v) -> v + 1);
        }
        List<Object[]> filas = filasDesdeMapa(datos, "Vigencia", "Cantidad");
        escribirTabla(sheet, estilos, 2, filas);
        agregarGraficaBarras(sheet, "Vigencia de reportes", 2, filas.size(), 4, 2, 13, 14);
        ajustar(sheet, 3);
    }

    private void crearZonas(XSSFWorkbook workbook, Estilos estilos, List<AdminReporteResponse> reportes) {
        Sheet sheet = workbook.createSheet("Zonas");
        titulo(sheet, estilos, "RUPE - Reportes y avistamientos por zona");
        Map<String, Zona> zonas = new LinkedHashMap<>();
        for (AdminReporteResponse r : reportes) {
            Zona zona = zonas.computeIfAbsent(valor(r.getZona(), "Sin zona registrada"), Zona::new);
            zona.reportes++;
            zona.avistamientos += nuloCero(r.getAvistamientos());
            String estado = valor(r.getEstatus()).toUpperCase(Locale.ROOT);
            if ("ACTIVO".equals(estado)) zona.activos++;
            if (estado.contains("RECUPER")) zona.recuperados++;
        }
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[] {"Zona", "Reportes", "Avistamientos", "Activos", "Recuperados"});
        zonas.values().stream()
            .sorted((a, b) -> Long.compare(b.reportes, a.reportes))
            .forEach(z -> filas.add(new Object[] {z.nombre, z.reportes, z.avistamientos, z.activos, z.recuperados}));
        escribirTabla(sheet, estilos, 2, filas);
        agregarGraficaBarras(sheet, "Zonas con mas reportes", 2, Math.min(filas.size(), 11), 7, 2, 17, 14);
        ajustar(sheet, 5);
    }

    private void crearVisitas(XSSFWorkbook workbook, Estilos estilos, List<VisitaPaginaResponse> visitas) {
        Sheet sheet = workbook.createSheet("Visitas");
        titulo(sheet, estilos, "RUPE - Visitas publicas por pagina");
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[] {"Pagina", "Visitas"});
        for (VisitaPaginaResponse visita : visitas) {
            filas.add(new Object[] {valor(visita.getPagina(), "Sin pagina"), visita.getTotal()});
        }
        escribirTabla(sheet, estilos, 2, filas);
        agregarGraficaBarras(sheet, "Visitas por pagina", 2, Math.min(filas.size(), 11), 4, 2, 13, 14);
        ajustar(sheet, 3);
    }

    private void crearDetalle(XSSFWorkbook workbook, Estilos estilos, List<AdminReporteResponse> reportes) {
        Sheet sheet = workbook.createSheet("Detalle");
        titulo(sheet, estilos, "RUPE - Detalle filtrable de reportes");
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[] {
            "ID", "Folio", "Mascota", "Raza", "Zona", "Estatus", "Fecha extravio",
            "Tipo reporte", "Fecha registro", "Vencimiento", "Dias para vencer", "Renovaciones", "Avistamientos"
        });
        for (AdminReporteResponse reporte : reportes) {
            filas.add(new Object[] {
                reporte.getIdReporte(),
                reporte.getFolio(),
                reporte.getMascota(),
                reporte.getRaza(),
                reporte.getZona(),
                reporte.getEstatus(),
                reporte.getFechaExtravio(),
                reporte.getTipoReporte(),
                reporte.getFechaRegistro() == null ? "" : reporte.getFechaRegistro().toLocalDate(),
                reporte.getFechaVencimiento(),
                reporte.getDiasParaVencer(),
                reporte.getRenovaciones(),
                reporte.getAvistamientos()
            });
        }
        escribirTabla(sheet, estilos, 2, filas);
        ajustar(sheet, 13);
    }

    private void crearSoporte(XSSFWorkbook workbook, Estilos estilos, AdminResumenResponse resumen) {
        Sheet sheet = workbook.createSheet("Soporte");
        titulo(sheet, estilos, "RUPE - Seguimiento de soporte y contacto");
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[] {"Estatus", "Cantidad"});
        filas.add(new Object[] {"Pendientes", resumen.getMensajesContactoPendientes()});
        filas.add(new Object[] {"Atendidos", resumen.getMensajesContactoAtendidos()});
        filas.add(new Object[] {"Total", resumen.getMensajesContactoTotal()});
        escribirTabla(sheet, estilos, 2, filas);
        agregarGraficaBarras(sheet, "Mensajes de soporte", 2, filas.size(), 4, 2, 13, 14);
        ajustar(sheet, 2);
    }

    private void crearNotas(XSSFWorkbook workbook, Estilos estilos, String texto, String estatus, LocalDate desde, LocalDate hasta) {
        Sheet sheet = workbook.createSheet("Notas");
        titulo(sheet, estilos, "RUPE - Notas del reporte");

        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[] {"Concepto", "Detalle"});
        filas.add(new Object[] {"Alcance", "El archivo presenta informacion agregada del sistema RUPE para revision administrativa."});
        filas.add(new Object[] {"Privacidad", "No se incluyen contrasenas, datos sensibles completos ni informacion privada de contacto."});
        filas.add(new Object[] {"Filtros aplicados", describirFiltros(texto, estatus, desde, hasta)});
        filas.add(new Object[] {"Fecha de generacion", LocalDate.now().toString()});
        filas.add(new Object[] {"Uso sugerido", "Monitorear reportes, vigencia, zonas con actividad y visitas publicas del prototipo."});

        escribirTabla(sheet, estilos, 2, filas);
        ajustar(sheet, 2);
    }

    private String describirFiltros(String texto, String estatus, LocalDate desde, LocalDate hasta) {
        String filtroTexto = texto == null || texto.isBlank() ? "Sin texto" : texto;
        String filtroEstatus = estatus == null || estatus.isBlank() ? "Todos los estatus" : estatus;
        String filtroDesde = desde == null ? "Sin fecha inicial" : desde.toString();
        String filtroHasta = hasta == null ? "Sin fecha final" : hasta.toString();
        return "Busqueda: " + filtroTexto + "; Estatus: " + filtroEstatus + "; Desde: " + filtroDesde + "; Hasta: " + filtroHasta;
    }

    private void titulo(Sheet sheet, Estilos estilos, String texto) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(24);
        Cell cell = row.createCell(0);
        cell.setCellValue(texto);
        cell.setCellStyle(estilos.titulo);
    }

    private void escribirTabla(Sheet sheet, Estilos estilos, int rowInicio, List<Object[]> filas) {
        for (int i = 0; i < filas.size(); i++) {
            Row row = sheet.createRow(rowInicio + i);
            Object[] valores = filas.get(i);
            for (int j = 0; j < valores.length; j++) {
                Cell cell = row.createCell(j);
                Object valor = valores[j];
                if (valor instanceof Number numero) cell.setCellValue(numero.doubleValue());
                else cell.setCellValue(valor == null ? "" : valor.toString());
                cell.setCellStyle(i == 0 ? estilos.encabezado : estilos.celda);
            }
        }
        if (!filas.isEmpty() && filas.get(0).length > 0) {
            sheet.createFreezePane(0, rowInicio + 1);
            sheet.setAutoFilter(new CellRangeAddress(rowInicio, rowInicio, 0, filas.get(0).length - 1));
        }
    }

    private void agregarGraficaBarras(Sheet sheet, String titulo, int rowInicio, int totalFilas,
            int col1, int row1, int col2, int row2) {
        if (totalFilas <= 1) return;
        XSSFSheet xssfSheet = (XSSFSheet) sheet;
        XSSFDrawing drawing = xssfSheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(titulo);
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.RIGHT);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(org.apache.poi.xddf.usermodel.chart.AxisCrosses.AUTO_ZERO);

        int firstDataRow = rowInicio + 1;
        int lastDataRow = rowInicio + totalFilas - 1;
        XDDFCategoryDataSource categorias = XDDFDataSourcesFactory.fromStringCellRange(
            xssfSheet,
            new CellRangeAddress(firstDataRow, lastDataRow, 0, 0));
        XDDFNumericalDataSource<Double> valores = XDDFDataSourcesFactory.fromNumericCellRange(
            xssfSheet,
            new CellRangeAddress(firstDataRow, lastDataRow, 1, 1));

        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        XDDFChartData.Series series = data.addSeries(categorias, valores);
        series.setTitle("Total", null);
        ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
        chart.plot(data);
    }

    private List<Object[]> mapaConteo(List<AdminReporteResponse> reportes,
            java.util.function.Function<AdminReporteResponse, String> agrupador, String colA, String colB) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (AdminReporteResponse r : reportes) {
            String clave = valor(agrupador.apply(r), "Sin dato");
            mapa.put(clave, mapa.getOrDefault(clave, 0L) + 1);
        }
        return filasDesdeMapa(mapa, colA, colB);
    }

    private List<Object[]> filasDesdeMapa(Map<String, Long> mapa, String colA, String colB) {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[] {colA, colB});
        mapa.forEach((k, v) -> filas.add(new Object[] {k, v}));
        return filas;
    }

    private void ajustar(Sheet sheet, int columnas) {
        for (int i = 0; i < columnas; i++) sheet.autoSizeColumn(i);
    }

    private long contarEstatus(List<AdminReporteResponse> reportes, String estatus) {
        return reportes.stream().filter(r -> estatus.equalsIgnoreCase(valor(r.getEstatus()))).count();
    }

    private long contarRecuperados(List<AdminReporteResponse> reportes) {
        return reportes.stream().filter(r -> valor(r.getEstatus()).toUpperCase(Locale.ROOT).contains("RECUPER")).count();
    }

    private long contarPendientes(List<AdminReporteResponse> reportes) {
        return reportes.stream().filter(r -> Boolean.TRUE.equals(r.getRequiereRenovacion())
            || (r.getDiasParaVencer() != null && r.getDiasParaVencer() < 0)).count();
    }

    private long nuloCero(Long valor) { return valor == null ? 0L : valor; }
    private String valor(String texto) { return valor(texto, ""); }
    private String valor(String texto, String defecto) { return texto == null || texto.isBlank() ? defecto : texto.trim(); }
    private String normalizar(String texto) {
        return valor(texto).toLowerCase(Locale.ROOT)
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u");
    }

    private static class Zona {
        private final String nombre;
        private long reportes;
        private long avistamientos;
        private long activos;
        private long recuperados;
        Zona(String nombre) { this.nombre = nombre; }
    }

    private static class Estilos {
        private final CellStyle titulo;
        private final CellStyle encabezado;
        private final CellStyle celda;

        Estilos(Workbook workbook) {
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
