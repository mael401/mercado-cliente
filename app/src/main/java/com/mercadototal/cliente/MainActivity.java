package com.mercadototal.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final List<Producto> productos = new ArrayList<>();
    private ProductoAdapter adapter;
    private Button btnCarrito;
    private TextView tvEstado, tvSaludo, tvDireccion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            salir();
            return;
        }

        setContentView(R.layout.activity_main);
        tvSaludo = findViewById(R.id.tvSaludo);
        tvDireccion = findViewById(R.id.tvDireccion);
        tvEstado = findViewById(R.id.tvEstado);
        btnCarrito = findViewById(R.id.btnCarrito);
        Button btnSalir = findViewById(R.id.btnSalir);
        RecyclerView rv = findViewById(R.id.rvProductos);

        adapter = new ProductoAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnCarrito.setOnClickListener(v ->
            startActivity(new Intent(this, CarritoActivity.class)));

        btnSalir.setOnClickListener(v -> {
            Carrito.vaciar();
            FirebaseAuth.getInstance().signOut();
            salir();
        });

        tvSaludo.setText("Hola");
        Repo.perfil(new Repo.Callback<Map<String, Object>>() {
            @Override
            public void ok(Map<String, Object> p) {
                if (p.get("nombre") != null) tvSaludo.setText("Hola, " + p.get("nombre"));
                if (p.get("direccion") != null)
                    tvDireccion.setText("Entrega en: " + p.get("direccion"));
            }
            @Override
            public void error(String m) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter == null) return;
        actualizarBotonCarrito();
        cargarProductos();
    }

    private void cargarProductos() {
        tvEstado.setText("Cargando productos...");
        tvEstado.setVisibility(View.VISIBLE);
        Repo.productos(new Repo.Callback<List<Producto>>() {
            @Override
            public void ok(List<Producto> r) {
                productos.clear();
                productos.addAll(r);
                adapter.notifyDataSetChanged();
                tvEstado.setText("No hay productos disponibles por ahora.");
                tvEstado.setVisibility(r.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void error(String m) {
                tvEstado.setText("No se pudo cargar el catálogo: " + m);
                tvEstado.setVisibility(View.VISIBLE);
            }
        });
    }

    private void actualizarBotonCarrito() {
        btnCarrito.setText("Ver carrito (" + Carrito.cantidad() + ")  "
            + Carrito.dinero(Carrito.total()));
    }

    private void salir() {
        Intent i = new Intent(this, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            TextView nombre, detalle;
            Button agregar;
            VH(View v) {
                super(v);
                nombre = v.findViewById(R.id.tvNombre);
                detalle = v.findViewById(R.id.tvDetalle);
                agregar = v.findViewById(R.id.btnAgregar);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Producto p = productos.get(pos);
            h.nombre.setText(p.nombre);
            h.detalle.setText(Carrito.dinero(p.precio) + "  ·  Disponibles: " + p.existencia);
            h.agregar.setOnClickListener(v -> {
                if (Carrito.agregar(p)) {
                    actualizarBotonCarrito();
                } else {
                    Toast.makeText(MainActivity.this,
                        "No hay más existencias de este producto.",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return productos.size();
        }
    }
}
