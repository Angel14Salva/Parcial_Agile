package com.municipalidad.licencias.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.municipalidad.licencias.model.Licencia;
import com.municipalidad.licencias.model.Solicitud;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

public class PdfLicenciaUtil {

    private static final BaseColor AZUL       = new BaseColor(0, 51, 102);
    private static final BaseColor AZUL_CLARO = new BaseColor(0, 70, 140);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd 'de' MMMM 'del' yyyy",
        java.util.Locale.forLanguageTag("es"));
    private static final DateTimeFormatter FMT_CORTO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static byte[] generar(Licencia licencia) {
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 40);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            Solicitud s = licencia.getSolicitud();

            // ── Fuentes ───────────────────────────────────────────────────────
            Font fMunicipio  = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD,   AZUL);
            Font fTitulo     = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   AZUL);
            Font fNroLic     = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   AZUL);
            Font fLey        = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   AZUL);
            Font fNormal     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
            Font fBold       = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.BLACK);
            Font fLabel      = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
            Font fValorBold  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.BLACK);
            Font fSmall      = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.DARK_GRAY);
            Font fProhib     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BaseColor.BLACK);
            Font fFooter     = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, BaseColor.DARK_GRAY);

            // ── Encabezado con logo y título ──────────────────────────────────
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{20, 80});
            headerTable.setSpacingAfter(8);

            // Logo (escudo placeholder)
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(5);
            // Círculo azul como placeholder del escudo
            logoCell.addElement(new Paragraph("⚙", new Font(Font.FontFamily.SYMBOL, 36, Font.NORMAL, AZUL)));
            headerTable.addCell(logoCell);

            // Título institucional
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPaddingLeft(10);
            titleCell.setPaddingTop(5);

            Paragraph pMunicipio = new Paragraph("MUNICIPALIDAD PROVINCIAL DE TRUJILLO", fMunicipio);
            pMunicipio.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(pMunicipio);

            Paragraph pSubtitulo = new Paragraph("\n", fNormal);
            titleCell.addElement(pSubtitulo);

            Paragraph pTitulo = new Paragraph("LICENCIA DE FUNCIONAMIENTO", fTitulo);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(pTitulo);

            Paragraph pNro = new Paragraph("Nro. " + licencia.getNumeroLicencia(), fNroLic);
            pNro.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(pNro);

            Paragraph pLey = new Paragraph("Ley Nro. 28976", fLey);
            pLey.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(pLey);

            headerTable.addCell(titleCell);
            doc.add(headerTable);

            // Línea separadora azul
            PdfPTable lineaAzul = new PdfPTable(1);
            lineaAzul.setWidthPercentage(100);
            PdfPCell lineaCell = new PdfPCell(new Phrase(" "));
            lineaCell.setBackgroundColor(AZUL);
            lineaCell.setBorder(Rectangle.NO_BORDER);
            lineaCell.setFixedHeight(3f);
            lineaAzul.addCell(lineaCell);
            doc.add(lineaAzul);
            doc.add(Chunk.NEWLINE);

            // ── Texto de facultades ───────────────────────────────────────────
            Font fItalic = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.DARK_GRAY);
            Paragraph facultades = new Paragraph(
                "En uso de las Facultades conferidas mediante la Ordenanza Municipal y la Ley Orgánica de Municipalidades.",
                fItalic);
            facultades.setAlignment(Element.ALIGN_JUSTIFIED);
            doc.add(facultades);
            doc.add(Chunk.NEWLINE);

            // ── CONCEDE A ─────────────────────────────────────────────────────
            Paragraph concedeA = new Paragraph("CONCEDE A:", fBold);
            doc.add(concedeA);
            doc.add(Chunk.NEWLINE);

            // ── Tabla de datos ────────────────────────────────────────────────
            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new float[]{32, 68});
            datos.setSpacingAfter(15);

            String razonSocial    = s != null ? s.getRazonSocial() : "-";
            String ruc            = s != null && s.getRuc() != null ? "RUC: " + s.getRuc() : "-";
            String representante  = s != null && s.getNombreRepresentante() != null ? s.getNombreRepresentante() : "-";
            String dniRep         = s != null && s.getDniRepresentante() != null ? "DNI: " + s.getDniRepresentante() : "-";
            String nombreComercial= s != null && s.getNombreComercial() != null ? s.getNombreComercial() : razonSocial;
            String direccion      = s != null && s.getDireccionEstablecimiento() != null ?
                                    s.getDireccionEstablecimiento() : (s != null ? s.getDomicilioFiscal() : "-");
            String giro           = s != null ? s.getRubro() : "-";
            String area           = s != null && s.getAreaTotalM2() != null ? s.getAreaTotalM2() + " m2" : "-";
            String horario        = s != null && s.getHorarioAtencion() != null ? s.getHorarioAtencion() : "-";
            String expediente     = "EXP-" + String.format("%05d", licencia.getId()) + "-MPT";

            agregarFilaDatos(datos, "Razón Social:",        razonSocial,    fLabel, fValorBold);
            agregarFilaDatos(datos, "Doc. de Identidad:",   ruc,            fLabel, fValorBold);
            agregarFilaDatos(datos, "Representante Legal:", representante,  fLabel, fValorBold);
            agregarFilaDatos(datos, "Doc. de Identidad:",   dniRep,         fLabel, fValorBold);
            agregarFilaDatos(datos, "Nombre Comercial:",    nombreComercial,fLabel, fValorBold);
            agregarFilaDatos(datos, "Dirección:",           direccion,      fLabel, fValorBold);
            agregarFilaDatos(datos, "Giro:",                giro,           fLabel, fValorBold);
            agregarFilaDatos(datos, "Área:",                area,           fLabel, fValorBold);
            agregarFilaDatos(datos, "Horario de Atención:", horario,        fLabel, fValorBold);
            agregarFilaDatos(datos, "Visto el Expediente:", expediente,     fLabel, fValorBold);
            doc.add(datos);

            // Fecha y lugar
            String fechaEmision = licencia.getFechaEmision().format(FMT);
            Paragraph fecha = new Paragraph("Trujillo, " + fechaEmision, fNormal);
            fecha.setAlignment(Element.ALIGN_RIGHT);
            doc.add(fecha);
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            // ── Sello y firma ─────────────────────────────────────────────────
            PdfPTable firmaTable = new PdfPTable(1);
            firmaTable.setWidthPercentage(50);
            firmaTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell firmaCell = new PdfPCell();
            firmaCell.setBorder(Rectangle.NO_BORDER);
            firmaCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pFirmaInst = new Paragraph("MUNICIPALIDAD PROVINCIAL DE TRUJILLO", fSmall);
            pFirmaInst.setAlignment(Element.ALIGN_CENTER);
            firmaCell.addElement(pFirmaInst);

            Paragraph pSubGer = new Paragraph("Subgerencia de Licencias y Comercializaciones", fSmall);
            pSubGer.setAlignment(Element.ALIGN_CENTER);
            firmaCell.addElement(pSubGer);

            // Línea de firma
            Paragraph lineaFirma = new Paragraph("_________________________", fNormal);
            lineaFirma.setAlignment(Element.ALIGN_CENTER);
            lineaFirma.setSpacingBefore(30f);
            firmaCell.addElement(lineaFirma);

            Paragraph pCargoFirma = new Paragraph("Sub Gerente de Licencias", fSmall);
            pCargoFirma.setAlignment(Element.ALIGN_CENTER);
            firmaCell.addElement(pCargoFirma);

            firmaTable.addCell(firmaCell);
            doc.add(firmaTable);
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            // ── Línea separadora ──────────────────────────────────────────────
            doc.add(lineaAzul);
            doc.add(Chunk.NEWLINE);

            // ── Prohibiciones ─────────────────────────────────────────────────
            Paragraph titProhib = new Paragraph("PROHIBICIONES AL ESTABLECIMIENTO", fProhib);
            doc.add(titProhib);

            Font fProhibItem = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
            doc.add(new Paragraph("• Prohibido consumir bebidas alcohólicas dentro y fuera del local", fProhibItem));
            doc.add(new Paragraph("• Prohibido ocupar pasajes de circulación", fProhibItem));
            doc.add(Chunk.NEWLINE);

            Paragraph obligatorio = new Paragraph(
                "ES OBLIGATORIO QUE SE EXHIBA EN UN LUGAR VISIBLE DEL ESTABLECIMIENTO", fProhib);
            obligatorio.setAlignment(Element.ALIGN_CENTER);
            doc.add(obligatorio);

            // ── Vigencia ──────────────────────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            Font fVigencia = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.DARK_GRAY);
            Paragraph vigencia = new Paragraph(
                "Licencia válida hasta: " + licencia.getFechaVencimiento().format(FMT_CORTO) +
                " · N.° " + licencia.getNumeroLicencia(), fVigencia);
            vigencia.setAlignment(Element.ALIGN_CENTER);
            doc.add(vigencia);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    private static void agregarFilaDatos(PdfPTable tabla, String label, String valor,
                                          Font fLabel, Font fValor) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, fLabel));
        cLabel.setBorder(Rectangle.BOTTOM);
        cLabel.setBorderColor(new BaseColor(220, 220, 220));
        cLabel.setPadding(6);
        cLabel.setBackgroundColor(new BaseColor(245, 248, 252));

        PdfPCell cValor = new PdfPCell(new Phrase(valor != null ? valor : "-", fValor));
        cValor.setBorder(Rectangle.BOTTOM);
        cValor.setBorderColor(new BaseColor(220, 220, 220));
        cValor.setPadding(6);

        tabla.addCell(cLabel);
        tabla.addCell(cValor);
    }
}
