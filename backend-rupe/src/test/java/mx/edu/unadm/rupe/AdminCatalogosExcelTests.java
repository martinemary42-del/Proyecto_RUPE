package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.util.List;
import mx.edu.unadm.rupe.admin.dto.AdminCatalogoResponse;
import mx.edu.unadm.rupe.admin.service.AdminAuditoriaExcelService;
import mx.edu.unadm.rupe.admin.service.AdminCatalogoService;
import mx.edu.unadm.rupe.admin.service.AdminService;
import mx.edu.unadm.rupe.contacto.service.MensajeContactoService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class AdminCatalogosExcelTests {

    @Test
    void generaExcelDeCatalogosConHojasYFiltros() throws Exception {
        AdminCatalogoService catalogoService = mock(AdminCatalogoService.class);
        AdminAuditoriaExcelService excelService = new AdminAuditoriaExcelService(
            mock(AdminService.class), catalogoService, mock(MensajeContactoService.class));

        when(catalogoService.listar(eq("raza"), any(HttpSession.class))).thenReturn(List.of(
            new AdminCatalogoResponse(1, "raza", "Criollo", true, true, "MASCOTA.id_raza", 1, "Perro", null)));
        for (String catalogo : List.of("tipo", "color", "estado", "municipio", "colonia", "estatus", "rol")) {
            when(catalogoService.listar(eq(catalogo), any(HttpSession.class))).thenReturn(List.of());
        }

        byte[] archivo = excelService.generarCatalogos(mock(HttpSession.class));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(8);
            assertThat(workbook.getSheet("Razas").getCTWorksheet().isSetAutoFilter()).isTrue();
            assertThat(workbook.getSheet("Razas").getRow(2).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(workbook.getSheet("Razas").getRow(3).getCell(2).getStringCellValue()).isEqualTo("Criollo");
        }
    }
}
