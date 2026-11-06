package com.mercadototal.cliente;

public class Producto {
    public String id, nombre, categoria, foto, tipoExistencia, unidadMedida;
    public double precio, existencia;
    public boolean esPeso(){return "peso".equalsIgnoreCase(tipoExistencia);}
    public double paso(){return esPeso()?0.25d:1.0d;}
    public String unidad(){return unidadMedida==null||unidadMedida.trim().isEmpty()?(esPeso()?"kg":"unidad"):unidadMedida;}
    public String cantidadTexto(double n){return String.format(java.util.Locale.US,"%.2f",n).replaceAll("\\.?0+$","")+" "+unidad();}
}