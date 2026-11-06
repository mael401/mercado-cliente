package com.mercadototal.cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Carrito {

    public static final List<ItemCarrito> items = new ArrayList<>();

    public static boolean agregar(Producto p) {
        for (ItemCarrito i : items) {
            if (i.productoId.equals(p.id)) {
                i.existencia = p.existencia;
                if (i.cantidad >= p.existencia) return false;
                i.cantidad++;
                return true;
            }
        }
        if (p.existencia < 1) return false;
        ItemCarrito n = new ItemCarrito();
        n.productoId = p.id;
        n.nombre = p.nombre;
        n.precio = p.precio;
        n.cantidad = 1;
        n.existencia = p.existencia;
        items.add(n);
        return true;
    }

    /** Suma una unidad a la línea del carrito (respeta las existencias). */
    public static boolean sumarUno(int pos) {
        ItemCarrito i = items.get(pos);
        if (i.cantidad >= i.existencia) return false;
        i.cantidad++;
        return true;
    }

    public static void quitarUno(int pos) {
        ItemCarrito i = items.get(pos);
        i.cantidad--;
        if (i.cantidad <= 0) items.remove(pos);
    }

    public static void quitarUnoPorId(String productoId) {
        for (int k = 0; k < items.size(); k++) {
            if (items.get(k).productoId.equals(productoId)) {
                quitarUno(k);
                return;
            }
        }
    }

    public static int cantidadDe(String productoId) {
        for (ItemCarrito i : items) {
            if (i.productoId.equals(productoId)) return i.cantidad;
        }
        return 0;
    }

    public static double total() {
        double t = 0;
        for (ItemCarrito i : items) t += i.precio * i.cantidad;
        return t;
    }

    public static int cantidad() {
        int c = 0;
        for (ItemCarrito i : items) c += i.cantidad;
        return c;
    }

    public static void vaciar() {
        items.clear();
    }

    public static String dinero(double v) {
        if (v == Math.floor(v)) return "$" + String.format(Locale.US, "%.0f", v);
        return "$" + String.format(Locale.US, "%.2f", v);
    }
}
