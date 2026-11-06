package com.mercadototal.cliente;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Repo {

    public interface Callback<T> {
        void ok(T resultado);
        void error(String mensaje);
    }

    private static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    private static String uid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static void perfil(Callback<Map<String, Object>> cb) {
        db().collection("users").document(uid()).get()
            .addOnSuccessListener(d -> {
                Map<String, Object> m = d.exists() ? d.getData() : new HashMap<>();
                cb.ok(m);
            })
            .addOnFailureListener(e -> cb.error(e.getMessage()));
    }

    public static void productos(Callback<List<Producto>> cb) {
        db().collection("productos").whereEqualTo("activo", true).get()
            .addOnSuccessListener(q -> {
                List<Producto> lista = new ArrayList<>();
                for (DocumentSnapshot d : q.getDocuments()) {
                    try {
                        Producto p = new Producto();
                        p.id = d.getId();
                        p.nombre = d.getString("nombre");
                        p.categoria = d.getString("categoria");
                        p.foto = d.getString("foto");
                        Double precio = d.getDouble("precio");
                        Long ex = d.getLong("existencia");
                        p.precio = precio == null ? 0 : precio;
                        p.existencia = ex == null ? 0 : ex;
                        if (p.nombre != null && p.existencia > 0) lista.add(p);
                    } catch (Exception ignorado) {
                        // producto mal formado: se omite
                    }
                }
                Collections.sort(lista, (a, b) -> a.nombre.compareToIgnoreCase(b.nombre));
                cb.ok(lista);
            })
            .addOnFailureListener(e -> cb.error(e.getMessage()));
    }

    public static void enviarPedido(Map<String, Object> perfil, String direccion,
                                    Callback<Void> cb) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemCarrito i : Carrito.items) {
            Map<String, Object> m = new HashMap<>();
            m.put("productoId", i.productoId);
            m.put("nombre", i.nombre);
            m.put("precio", i.precio);
            m.put("cantidad", i.cantidad);
            items.add(m);
        }

        Map<String, Object> p = new HashMap<>();
        p.put("clienteId", uid());
        p.put("clienteNombre", perfil.get("nombre"));
        p.put("telefono", perfil.get("telefono"));
        p.put("direccion", direccion);
        p.put("items", items);
        p.put("total", Carrito.total());
        p.put("estado", "pendiente");
        p.put("fecha", FieldValue.serverTimestamp());

        db().collection("pedidos").add(p)
            .addOnSuccessListener(r -> cb.ok(null))
            .addOnFailureListener(e -> cb.error(e.getMessage()));
    }

    /** Pedidos del cliente en tiempo real (el más reciente primero). */
    @SuppressWarnings("unchecked")
    public static ListenerRegistration misPedidos(Callback<List<Pedido>> cb) {
        return db().collection("pedidos")
            .whereEqualTo("clienteId", uid())
            .limit(50)
            .addSnapshotListener((snap, e) -> {
                if (e != null) {
                    cb.error(e.getMessage());
                    return;
                }
                if (snap == null) return;
                List<Pedido> lista = new ArrayList<>();
                for (DocumentSnapshot d : snap.getDocuments()) {
                    try {
                        Pedido p = new Pedido();
                        p.id = d.getId();
                        p.direccion = d.getString("direccion");
                        p.estado = d.getString("estado");
                        Double t = d.getDouble("total");
                        p.total = t == null ? 0 : t;
                        Timestamp ts = d.getTimestamp("fecha");
                        p.fecha = ts == null ? null : ts.toDate();
                        List<Map<String, Object>> its = (List<Map<String, Object>>) d.get("items");
                        if (its != null) {
                            for (Map<String, Object> m : its) {
                                Pedido.Linea l = new Pedido.Linea();
                                l.nombre = String.valueOf(m.get("nombre"));
                                Object c = m.get("cantidad");
                                l.cantidad = c instanceof Number ? ((Number) c).longValue() : 0;
                                p.lineas.add(l);
                            }
                        }
                        if (p.estado == null) p.estado = "pendiente";
                        lista.add(p);
                    } catch (Exception ignorado) {
                        // pedido mal formado: se omite
                    }
                }
                Collections.sort(lista, (a, b) -> {
                    long fa = a.fecha == null ? Long.MAX_VALUE : a.fecha.getTime();
                    long fb = b.fecha == null ? Long.MAX_VALUE : b.fecha.getTime();
                    return Long.compare(fb, fa);
                });
                cb.ok(lista);
            });
    }

    /** El cliente solo puede cancelar mientras el pedido está pendiente. */
    public static void cancelarPedido(String pedidoId, Callback<Void> cb) {
        db().collection("pedidos").document(pedidoId).update("estado", "cancelado")
            .addOnSuccessListener(r -> cb.ok(null))
            .addOnFailureListener(e -> cb.error(e.getMessage()));
    }
}
