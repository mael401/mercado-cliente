package com.mercadototal.cliente;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final List<Producto> todos = new ArrayList<>();
    private final List<Producto> visibles = new ArrayList<>();
    private ProductoAdapter adapter;
    private Button btnCarrito;
    private TextView tvEstado, tvSaludo, tvDireccion, tvTasa;
    private double tasaUSD = 0;
    private ListenerRegistration tasaRegistro;
    private EditText etBuscar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            salir();
            return;
        }

        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        tvSaludo = findViewById(R.id.tvSaludo);
        tvDireccion = findViewById(R.id.tvDireccion);
        tvTasa = findViewById(R.id.tvTasa);
        tasaRegistro = Repo.escucharTasaUSD(new Repo.Callback<Double>() {
            @Override public void ok(Double v) {
                tasaUSD = v == null ? 0 : v;
                tvTasa.setText(tasaUSD > 0 ? "Tasa del día: 1 USD = " + Carrito.dinero(tasaUSD) + " CUP" : "Tasa USD no configurada");
                if (adapter != null) adapter.notifyDataSetChanged();
                actualizarBotonCarrito();
            }
            @Override public void error(String m) { tvTasa.setText("No se pudo consultar la tasa USD"); }
        });
        tvEstado = findViewById(R.id.tvEstado);
        etBuscar = findViewById(R.id.etBuscar);
        btnCarrito = findViewById(R.id.btnCarrito);
        RecyclerView rv = findViewById(R.id.rvProductos);

        toolbar.inflateMenu(R.menu.menu_cliente);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.accion_mis_pedidos) {
                startActivity(new Intent(this, MisPedidosActivity.class));
                return true;
            }
            if (id == R.id.accion_clave) {
                Cuenta.cambiarPassword(this);
                return true;
            }
            if (id == R.id.accion_salir) {
                Carrito.vaciar();
                FirebaseAuth.getInstance().signOut();
                salir();
                return true;
            }
            return false;
        });

        adapter = new ProductoAdapter();
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(adapter);

        btnCarrito.setOnClickListener(v ->
            startActivity(new Intent(this, CarritoActivity.class)));

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                filtrar();
            }
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
        adapter.notifyDataSetChanged();
        cargarProductos();
    }

    private void cargarProductos() {
        if (todos.isEmpty()) {
            tvEstado.setText("Cargando productos...");
            tvEstado.setVisibility(View.VISIBLE);
        }
        Repo.productos(new Repo.Callback<List<Producto>>() {
            @Override
            public void ok(List<Producto> r) {
                todos.clear();
                todos.addAll(r);
                filtrar();
            }
            @Override
            public void error(String m) {
                tvEstado.setText("No se pudo cargar el catálogo: " + m);
                tvEstado.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filtrar() {
        String q = etBuscar.getText().toString().trim().toLowerCase(Locale.getDefault());
        visibles.clear();
        for (Producto p : todos) {
            String n = p.nombre == null ? "" : p.nombre.toLowerCase(Locale.getDefault());
            String c = p.categoria == null ? "" : p.categoria.toLowerCase(Locale.getDefault());
            if (q.isEmpty() || n.contains(q) || c.contains(q)) visibles.add(p);
        }
        adapter.notifyDataSetChanged();
        tvEstado.setText(todos.isEmpty()
            ? "No hay productos disponibles por ahora."
            : "No encontramos productos con esa búsqueda.");
        tvEstado.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void actualizarBotonCarrito() {
        double n = Carrito.cantidad();
        if (n == 0) {
            btnCarrito.setText("Tu carrito está vacío");
            btnCarrito.setEnabled(false);
        } else {
            btnCarrito.setText("Ver carrito  ·  " + n + (n == 1 ? " producto" : " productos")
                + "  ·  " + Carrito.dinero(Carrito.total()));
            btnCarrito.setEnabled(true);
        }
    }

    @Override protected void onDestroy() {\n        if (tasaRegistro != null) tasaRegistro.remove();\n        super.onDestroy();\n    }\n\n    private String precioConUSD(double cup) {\n        return Carrito.dinero(cup) + " CUP" + (tasaUSD > 0 ? "  ·  " + Carrito.dinero(cup / tasaUSD) + " USD" : "");\n    }\n\n    private void salir() {
        Intent i = new Intent(this, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            ImageView foto;
            TextView nombre, precio, disp, cant;
            Button agregar;
            View bloque;
            ImageButton menos, mas;
            VH(View v) {
                super(v);
                foto = v.findViewById(R.id.ivFoto);
                nombre = v.findViewById(R.id.tvNombre);
                precio = v.findViewById(R.id.tvPrecio);
                disp = v.findViewById(R.id.tvDisp);
                cant = v.findViewById(R.id.tvCant);
                agregar = v.findViewById(R.id.btnAgregar);
                bloque = v.findViewById(R.id.bloqueCantidad);
                menos = v.findViewById(R.id.btnMenos);
                mas = v.findViewById(R.id.btnMas);
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
            Producto p = visibles.get(pos);
            h.nombre.setText(p.nombre);
            h.precio.setText(precioConUSD(p.precio) + " / " + p.unidad());
            h.disp.setText("Disponibles: " + p.cantidadTexto(p.existencia));

            Bitmap bm = Img.desdeBase64(p.id, p.foto);
            if (bm != null) {
                h.foto.setImageBitmap(bm);
                h.foto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                h.foto.setImageResource(R.drawable.ic_image);
                h.foto.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }

            double c = Carrito.cantidadDe(p.id);
            h.agregar.setVisibility(c == 0 ? View.VISIBLE : View.GONE);
            h.bloque.setVisibility(c == 0 ? View.GONE : View.VISIBLE);
            h.cant.setText(Carrito.cantidadTexto(c));

            View.OnClickListener sumar = v -> {
                if (Carrito.agregar(p)) {
                    actualizarBotonCarrito();
                    refrescarFila(h);
                } else {
                    Toast.makeText(MainActivity.this,
                        "No hay más existencias de este producto.",
                        Toast.LENGTH_SHORT).show();
                }
            };
            h.agregar.setOnClickListener(sumar);
            h.mas.setOnClickListener(sumar);
            h.menos.setOnClickListener(v -> {
                Carrito.quitarUnoPorId(p.id);
                actualizarBotonCarrito();
                refrescarFila(h);
            });
        }

        private void refrescarFila(VH h) {
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
        }

        @Override
        public int getItemCount() {
            return visibles.size();
        }
    }
}
