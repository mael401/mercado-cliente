package com.mercadototal.cliente;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Pedido {

    public static class Linea {
        public String nombre;
        public double cantidad;
    }

    public String id, direccion, estado;
    public double total;
    public Date fecha;
    public List<Linea> lineas = new ArrayList<>();

    public String resumen(String separador) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineas.size(); i++) {
            if (i > 0) sb.append(separador);
            sb.append(String.format(java.util.Locale.US, "%.2f", lineas.get(i).cantidad).replaceAll("\\.?0+$", "")).append(" x ").append(lineas.get(i).nombre);
        }
        return sb.toString();
    }
}
