package com.mercadototal.cliente;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Pedido {

    public static class Linea {
        public String nombre;
        public long cantidad;
    }

    public String id, direccion, estado;
    public double total;
    public Date fecha;
    public List<Linea> lineas = new ArrayList<>();

    public String resumen(String separador) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineas.size(); i++) {
            if (i > 0) sb.append(separador);
            sb.append(lineas.get(i).cantidad).append(" x ").append(lineas.get(i).nombre);
        }
        return sb.toString();
    }
}
