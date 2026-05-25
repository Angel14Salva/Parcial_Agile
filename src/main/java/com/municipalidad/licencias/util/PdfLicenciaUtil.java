package com.municipalidad.licencias.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.municipalidad.licencias.model.Licencia;
import com.municipalidad.licencias.model.Solicitud;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PdfLicenciaUtil {

    private static final BaseColor AZUL_OSCURO = new BaseColor(0, 40, 100);
    private static final BaseColor AZUL_MEDIO  = new BaseColor(0, 70, 150);
    private static final DateTimeFormatter FMT_LARGO = DateTimeFormatter.ofPattern(
        "dd 'de' MMMM 'del' yyyy", new Locale("es", "PE"));

    public static byte[] generar(Licencia licencia) {
        try {
            Document doc = new Document(PageSize.A4, 55, 55, 45, 45);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            Solicitud s = licencia.getSolicitud();
            PdfContentByte canvas = writer.getDirectContentUnder();
            float W = PageSize.A4.getWidth();
            float H = PageSize.A4.getHeight();

            // Fondo azul superior curvo
            canvas.setColorFill(AZUL_OSCURO);
            canvas.moveTo(0, H);
            canvas.lineTo(W, H);
            canvas.lineTo(W, H - 175);
            canvas.curveTo(W * 0.7f, H - 140, W * 0.3f, H - 195, 0, H - 165);
            canvas.closePath();
            canvas.fill();

            // Fondo azul inferior curvo
            canvas.setColorFill(AZUL_OSCURO);
            canvas.moveTo(0, 0);
            canvas.lineTo(W, 0);
            canvas.lineTo(W, 80);
            canvas.curveTo(W * 0.7f, 50, W * 0.3f, 100, 0, 70);
            canvas.closePath();
            canvas.fill();

            // Linea curva azul medio superior
            canvas.setColorStroke(AZUL_MEDIO);
            canvas.setLineWidth(8);
            canvas.moveTo(0, H - 168);
            canvas.curveTo(W * 0.3f, H - 198, W * 0.7f, H - 143, W, H - 178);
            canvas.stroke();

            // Linea curva azul medio inferior
            canvas.moveTo(0, 73);
            canvas.curveTo(W * 0.3f, 103, W * 0.7f, 53, W, 83);
            canvas.stroke();

            // Fuentes
            Font fMunicipio = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD,   BaseColor.WHITE);
            Font fTitulo    = new Font(Font.FontFamily.TIMES_ROMAN, 13, Font.BOLD,   BaseColor.WHITE);
            Font fNroLey    = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.BOLD,   BaseColor.WHITE);
            Font fNormal    = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL, BaseColor.BLACK);
            Font fBold      = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.BOLD,   BaseColor.BLACK);
            Font fLabel     = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL, BaseColor.BLACK);
            Font fValor     = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.BOLD,   BaseColor.BLACK);
            Font fItalic    = new Font(Font.FontFamily.TIMES_ROMAN,  9, Font.ITALIC, BaseColor.BLACK);
            Font fSmall     = new Font(Font.FontFamily.TIMES_ROMAN,  8, Font.NORMAL, BaseColor.BLACK);
            Font fSmallBold = new Font(Font.FontFamily.TIMES_ROMAN,  8, Font.BOLD,   BaseColor.BLACK);
            Font fProhib    = new Font(Font.FontFamily.TIMES_ROMAN,  9, Font.BOLD,   BaseColor.BLACK);
            Font fOblig     = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.BOLD,   BaseColor.BLACK);

            // Escudo en zona azul superior
            try {
                InputStream escudoStream = PdfLicenciaUtil.class
                    .getResourceAsStream("/static/img/escudo_trujillo.png");
                if (escudoStream != null) {
                    Image escudo = Image.getInstance(escudoStream.readAllBytes());
                    escudo.scaleToFit(85, 95);
                    escudo.setAbsolutePosition(48, H - 165);
                    writer.getDirectContent().addImage(escudo);
                }
            } catch (Exception ignored) {}

            // Titulo en zona azul
            PdfPTable headerTxt = new PdfPTable(1);
            headerTxt.setTotalWidth(370);
            headerTxt.setLockedWidth(true);

            PdfPCell c1 = cellNoBorder(new Phrase("MUNICIPALIDAD PROVINCIAL DE TRUJILLO", fMunicipio));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER); c1.setPaddingBottom(4);
            headerTxt.addCell(c1);

            PdfPCell c2 = cellNoBorder(new Phrase("LICENCIA DE FUNCIONAMIENTO", fTitulo));
            c2.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerTxt.addCell(c2);

            PdfPCell c3 = cellNoBorder(new Phrase("Nro. " + licencia.getNumeroLicencia(), fNroLey));
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerTxt.addCell(c3);

            PdfPCell c4 = cellNoBorder(new Phrase("Ley Nro. 28976", fNroLey));
            c4.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerTxt.addCell(c4);

            headerTxt.writeSelectedRows(0, -1, 155, H - 20, writer.getDirectContent());

            // Espacio para saltar zona azul
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 68)));
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 5)));

            // Texto facultades
            Paragraph facultades = new Paragraph(
                "En uso de las Facultades conferidas mediante Resoluci\u00f3n Gerencial N\u00b0 1261-213-MPT-GDEL, la " +
                "Ordenanza Municipal Nro. 014-2018-MPT y la Ley Org\u00e1nica de Municipalidades.", fItalic);
            facultades.setAlignment(Element.ALIGN_JUSTIFIED);
            doc.add(facultades);
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 5)));

            // CONCEDE A
            doc.add(new Paragraph("CONCEDE A:", fBold));
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 4)));

            // Datos
            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new float[]{33, 67});

            String razonSocial   = safe(s != null ? s.getRazonSocial() : null);
            String ruc           = s != null && s.getRuc() != null ? "RUC: " + s.getRuc() : "-";
            String representante = safe(s != null ? s.getNombreRepresentante() : null);
            String dniRep        = s != null && s.getDniRepresentante() != null ? "DNI: " + s.getDniRepresentante() : "-";
            String nomComercial  = safe(s != null && s.getNombreComercial() != null ? s.getNombreComercial() : (s != null ? s.getRazonSocial() : null));
            String direccion     = safe(s != null && s.getDireccionEstablecimiento() != null ? s.getDireccionEstablecimiento() : (s != null ? s.getDomicilioFiscal() : null));
            String giro          = "\"" + safe(s != null ? s.getRubro() : null) + "\"";
            String area          = s != null && s.getAreaTotalM2() != null ? s.getAreaTotalM2() + " m2" : "-";
            String horario       = safe(s != null ? s.getHorarioAtencion() : null);
            String expediente    = String.format("%05d", licencia.getId()) + " \u2013 " + licencia.getFechaEmision().getYear() + " \u2013 MPT - 1";

            agregarFila(datos, "Raz\u00f3n Social:",       razonSocial,   fLabel, fValor);
            agregarFila(datos, "Doc. de Identidad:",        ruc,           fLabel, fValor);
            agregarFila(datos, "Representante Legal:",      representante, fLabel, fValor);
            agregarFila(datos, "Doc. de Identidad:",        dniRep,        fLabel, fValor);
            agregarFila(datos, "Nombre Comercial:",         nomComercial,  fLabel, fValor);
            agregarFila(datos, "Direcci\u00f3n:",          direccion,     fLabel, fValor);
            agregarFila(datos, "Giro:",                     giro,          fLabel, fValor);
            agregarFila(datos, "Zonificaci\u00f3n:",       "\"ZRE\"",   fLabel, fValor);
            agregarFila(datos, "\u00c1rea:",               area,          fLabel, fValor);
            agregarFila(datos, "Horario de Atenci\u00f3n:", horario,      fLabel, fValor);
            agregarFila(datos, "Visto el Expediente:",      expediente,    fLabel, fValor);
            doc.add(datos);
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 6)));

            // Fecha
            String fechaStr = "Trujillo, " + licencia.getFechaEmision().format(FMT_LARGO);
            Paragraph fecha = new Paragraph(fechaStr, fNormal);
            fecha.setAlignment(Element.ALIGN_RIGHT);
            doc.add(fecha);
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 8)));

            // Firma
            PdfPTable firmaTable = new PdfPTable(1);
            firmaTable.setWidthPercentage(55);
            firmaTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell fc = new PdfPCell();
            fc.setBorder(Rectangle.NO_BORDER);
            fc.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pInst = new Paragraph("MUNICIPALIDAD PROVINCIAL DE TRUJILLO", fSmallBold);
            pInst.setAlignment(Element.ALIGN_CENTER); fc.addElement(pInst);

            Paragraph pSubG = new Paragraph("Subgerencia de Licencias y Comercializaciones", fSmall);
            pSubG.setAlignment(Element.ALIGN_CENTER); fc.addElement(pSubG);

            try {
                InputStream es2 = PdfLicenciaUtil.class.getResourceAsStream("/static/img/escudo_trujillo.png");
                if (es2 != null) {
                    Image ef = Image.getInstance(es2.readAllBytes());
                    ef.scaleToFit(35, 40);
                    ef.setAlignment(Element.ALIGN_CENTER);
                    fc.addElement(ef);
                }
            } catch (Exception ignored) {}

            Paragraph lineaFirma = new Paragraph("_________________________", fNormal);
            lineaFirma.setAlignment(Element.ALIGN_CENTER);
            lineaFirma.setSpacingBefore(18f);
            fc.addElement(lineaFirma);

            Paragraph pNombreFuncionario = new Paragraph("Abog. Manuel Ernesto Garcia Blas", fSmall);
            pNombreFuncionario.setAlignment(Element.ALIGN_CENTER);
            fc.addElement(pNombreFuncionario);

            Paragraph pCargoFirma = new Paragraph("Sub Gerente", fSmall);
            pCargoFirma.setAlignment(Element.ALIGN_CENTER);
            fc.addElement(pCargoFirma);

            firmaTable.addCell(fc);
            doc.add(firmaTable);
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 10)));

            // Prohibiciones segun rubro
            doc.add(new Paragraph("PROHIBICIONES AL ESTABLECIMIENTO", fProhib));
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 3)));
            Font fPI = new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);

            String rubroLower = giro.toLowerCase();
            boolean esNocturno = rubroLower.contains("bar") || rubroLower.contains("cantina") ||
                rubroLower.contains("discoteca") || rubroLower.contains("club nocturno") ||
                rubroLower.contains("licor") || rubroLower.contains("alcohol") ||
                rubroLower.contains("karaoke") || rubroLower.contains("pub");
            boolean esAlimentos = rubroLower.contains("restaurante") || rubroLower.contains("comida") ||
                rubroLower.contains("bodega") || rubroLower.contains("panaderia") ||
                rubroLower.contains("pasteleria") || rubroLower.contains("mercado");

            if (esNocturno) {
                doc.add(new Paragraph("\u2022 Prohibido el ingreso de menores de edad", fPI));
                doc.add(new Paragraph("\u2022 Prohibido exceder los decibeles permitidos por la normativa municipal", fPI));
                doc.add(new Paragraph("\u2022 Prohibido funcionar fuera del horario autorizado", fPI));
                doc.add(new Paragraph("\u2022 Prohibido ocupar pasajes y v\u00edas p\u00fablicas", fPI));
            } else if (esAlimentos) {
                doc.add(new Paragraph("\u2022 Prohibido expender productos vencidos o en mal estado", fPI));
                doc.add(new Paragraph("\u2022 Prohibido ocupar pasajes de circulaci\u00f3n", fPI));
                doc.add(new Paragraph("\u2022 Prohibido el funcionamiento sin los certificados sanitarios vigentes", fPI));
            } else {
                doc.add(new Paragraph("\u2022 Prohibido consumir bebidas alcoh\u00f3licas dentro y fuera del local", fPI));
                doc.add(new Paragraph("\u2022 Prohibido ocupar pasajes de circulaci\u00f3n", fPI));
            }
            doc.add(new Paragraph("\u2022 Prohibido realizar actividades distintas a las autorizadas en esta licencia", fPI));
            doc.add(new Paragraph(" ", new Font(Font.FontFamily.TIMES_ROMAN, 5)));

            // Obligatorio
            Paragraph oblig = new Paragraph(
                "ES OBLIGATORIO QUE SE EXHIBA EN UN LUGAR VISIBLE DEL ESTABLECIMIENTO", fOblig);
            oblig.setAlignment(Element.ALIGN_CENTER);
            doc.add(oblig);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    private static void agregarFila(PdfPTable t, String label, String valor, Font fL, Font fV) {
        PdfPCell cL = new PdfPCell(new Phrase(label, fL));
        cL.setBorder(Rectangle.NO_BORDER); cL.setPaddingTop(5); cL.setPaddingBottom(5); cL.setPaddingLeft(2);
        PdfPCell cV = new PdfPCell(new Phrase(valor != null ? valor : "-", fV));
        cV.setBorder(Rectangle.NO_BORDER); cV.setPaddingTop(5); cV.setPaddingBottom(5);
        t.addCell(cL); t.addCell(cV);
    }

    private static PdfPCell cellNoBorder(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(2);
        return cell;
    }

    private static String safe(String val) {
        return val != null && !val.isBlank() ? val : "-";
    }
}
