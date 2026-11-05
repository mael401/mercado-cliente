package com.mercadototal.cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Carrito {

    public static final List<ItemCarrito> items = new ArrayList<>();

    public static boolean agregar(Producto p) {
        for (ItemCarrito i : items) {
            if (i.productoId.equals(p.id)) {
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
        items.add(n);
        return true;
    }

    public static void quitarUno(int pos) {
        ItemCarrito i = items.get(pos);
        i.cantidad--;
        if (i.cantidad <= 0) items.remove(pos);
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
