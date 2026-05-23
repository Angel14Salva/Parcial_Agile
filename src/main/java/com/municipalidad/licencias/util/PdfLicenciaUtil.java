package com.municipalidad.licencias.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.municipalidad.licencias.model.Licencia;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

public class PdfLicenciaUtil {

    private static final BaseColor AZUL_MUNICIPAL = new BaseColor(29, 53, 87);
    private static final BaseColor VERDE_VIGENTE  = new BaseColor(29, 158, 117);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static byte[] generar(Licencia licencia) {
        try {
            Document doc = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Encabezado
            Font fuenteTitulo = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.WHITE);
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell celdaHeader = new PdfPCell(new Phrase("MUNICIPALIDAD\nLICENCIA DE FUNCIONAMIENTO", fuenteTitulo));
            celdaHeader.setBackgroundColor(AZUL_MUNICIPAL);
            celdaHeader.setPadding(20);
            celdaHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaHeader.setBorder(Rectangle.NO_BORDER);
            header.addCell(celdaHeader);
            doc.add(header);

            doc.add(Chunk.NEWLINE);

            // Número de licencia destacado
            Font fuenteNumero = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, VERDE_VIGENTE);
            Paragraph numPar = new Paragraph("N.º " + licencia.getNumeroLicencia(), fuenteNumero);
            numPar.setAlignment(Element.ALIGN_CENTER);
            doc.add(numPar);

            doc.add(Chunk.NEWLINE);

            // Datos del negocio
            Solicitud s = licencia.getSolicitud();
            Font fuenteLabel = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, AZUL_MUNICIPAL);
            Font fuenteValor = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);

            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{35, 65});
            tabla.setSpacingBefore(10);

            agregarFila(tabla, "Razón social:",
                s != null ? s.getRazonSocial() : "-", fuenteLabel, fuenteValor);
            agregarFila(tabla, "Domicilio fiscal:",
                s != null ? s.getDomicilioFiscal() : "-", fuenteLabel, fuenteValor);
            agregarFila(tabla, "Rubro:",
                s != null ? s.getRubro() : "-", fuenteLabel, fuenteValor);
            agregarFila(tabla, "Fecha de emisión:",
                licencia.getFechaEmision().format(FMT), fuenteLabel, fuenteValor);
            agregarFila(tabla, "Válida hasta:",
                licencia.getFechaVencimiento().format(FMT), fuenteLabel, fuenteValor);
            agregarFila(tabla, "Estado:",
                licencia.getEstado().name(), fuenteLabel, fuenteValor);

            doc.add(tabla);

            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            // Nota legal
            Font fuenteNota = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
            Paragraph nota = new Paragraph(
                "Esta licencia debe estar colocada en un lugar visible dentro del establecimiento. " +
                "La municipalidad se reserva el derecho de realizar inspecciones de fiscalización " +
                "en cualquier momento para verificar el cumplimiento de las condiciones declaradas.",
                fuenteNota);
            nota.setAlignment(Element.ALIGN_JUSTIFIED);
            doc.add(nota);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de licencia: " + e.getMessage(), e);
        }
    }

    private static void agregarFila(PdfPTable tabla, String label, String valor,
                                     Font fLabel, Font fValor) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, fLabel));
        cLabel.setBorder(Rectangle.BOTTOM);
        cLabel.setPadding(6);
        cLabel.setBorderColor(BaseColor.LIGHT_GRAY);

        PdfPCell cValor = new PdfPCell(new Phrase(valor, fValor));
        cValor.setBorder(Rectangle.BOTTOM);
        cValor.setPadding(6);
        cValor.setBorderColor(BaseColor.LIGHT_GRAY);

        tabla.addCell(cLabel);
        tabla.addCell(cValor);
    }

    // Clase interna para evitar import circular
    static class Solicitud {
        String getRazonSocial() { return ""; }
        String getDomicilioFiscal() { return ""; }
        String getRubro() { return ""; }
    }
}
