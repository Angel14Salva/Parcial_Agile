package com.municipalidad.licencias.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Convierte un monto en soles peruanos a su representación en letras (formato factura SUNAT). */
public class NumeroALetrasUtil {

    private static final String[] UNIDADES = {
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
        "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };
    private static final String[] DECENAS = {
        "", "", "VEINTI", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    public static String convertir(BigDecimal monto) {
        BigDecimal m = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = m.longValue();
        int centimos = m.subtract(BigDecimal.valueOf(entero)).movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP).intValue();
        String letras = entero == 0 ? "CERO" : convertirEntero(entero);
        return "SON: " + letras + " Y " + String.format("%02d", centimos) + "/100 SOLES";
    }

    private static String convertirEntero(long n) {
        if (n == 0) return "";
        if (n == 100) return "CIEN";
        if (n < 21) return UNIDADES[(int) n];
        if (n < 30) return "VEINTI" + UNIDADES[(int) (n - 20)];
        if (n < 100) {
            int d = (int) (n / 10), u = (int) (n % 10);
            return DECENAS[d] + (u > 0 ? " Y " + UNIDADES[u] : "");
        }
        if (n < 1000) {
            int c = (int) (n / 100);
            long resto = n % 100;
            return CENTENAS[c] + (resto > 0 ? " " + convertirEntero(resto) : "");
        }
        if (n < 1_000_000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String milesStr = miles == 1 ? "MIL" : convertirEntero(miles) + " MIL";
            return milesStr + (resto > 0 ? " " + convertirEntero(resto) : "");
        }
        long millones = n / 1_000_000;
        long resto = n % 1_000_000;
        String millonesStr = millones == 1 ? "UN MILLON" : convertirEntero(millones) + " MILLONES";
        return millonesStr + (resto > 0 ? " " + convertirEntero(resto) : "");
    }
}
