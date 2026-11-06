package com.mercadototal.cliente;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
public class Carrito {
 public static final List<ItemCarrito> items=new ArrayList<>();
 public static boolean agregar(Producto p){
  double paso=p.paso();
  for(ItemCarrito i:items)if(i.productoId.equals(p.id)){i.existencia=p.existencia;if(i.cantidad+paso>p.existencia+0.000001)return false;i.cantidad=red(i.cantidad+paso);return true;}
  if(p.existencia+0.000001<paso)return false;
  ItemCarrito n=new ItemCarrito();n.productoId=p.id;n.nombre=p.nombre;n.precio=p.precio;n.cantidad=paso;n.existencia=p.existencia;n.paso=paso;n.unidadMedida=p.unidad();items.add(n);return true;
 }
 public static boolean sumarUno(int pos){ItemCarrito i=items.get(pos);if(i.cantidad+i.paso>i.existencia+0.000001)return false;i.cantidad=red(i.cantidad+i.paso);return true;}
 public static void quitarUno(int pos){ItemCarrito i=items.get(pos);i.cantidad=red(i.cantidad-i.paso);if(i.cantidad<=0.000001)items.remove(pos);}
 public static void quitarUnoPorId(String id){for(int k=0;k<items.size();k++)if(items.get(k).productoId.equals(id)){quitarUno(k);return;}}
 public static double cantidadDe(String id){for(ItemCarrito i:items)if(i.productoId.equals(id))return i.cantidad;return 0;}
 public static double total(){double t=0;for(ItemCarrito i:items)t+=i.precio*i.cantidad;return t;}
 public static double cantidad(){double n=0;for(ItemCarrito i:items)n+=i.cantidad;return n;}
 public static void vaciar(){items.clear();}
 public static String dinero(double v){return "$"+String.format(Locale.US,"%,.2f",v);}
 public static String cantidadTexto(double v){return String.format(Locale.US,"%.2f",v).replaceAll("\\.?0+$","");}
 private static double red(double v){return Math.round(v*10000d)/10000d;}
}