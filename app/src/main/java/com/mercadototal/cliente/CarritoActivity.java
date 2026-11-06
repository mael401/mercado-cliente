package com.mercadototal.cliente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.Map;

public class CarritoActivity extends AppCompatActivity {

    private Map<String, Object> perfil = new HashMap<>();
    private boolean perfilCargado = false;
    private CarritoAdapter adapter;
    private TextView tvTotal, tvVacio;
    private double tasaUSD = 0;
    private com.google.firebase.firestore.ListenerRegistration tasaRegistro;
    private TextInputEditText etDireccion;
    private Button btnEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrito);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTotal = findViewById(R.id.tvTotal);
        tvVacio = findViewById(R.id.tvVacio);
        etDireccion = findViewById(R.id.etDireccionEntrega);
        btnEnviar = findViewById(R.id.btnEnviar);
        RecyclerView rv = findViewById(R.id.rvCarrito);

        adapter = new CarritoAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnEnviar.setOnClickListener(v -> enviar());
        tasaRegistro = Repo.escucharTasaUSD(new Repo.Callback<Double>() {
            @Override public void ok(Double v) { tasaUSD = v == null ? 0 : v; refrescar(); }
            @Override public void error(String m) { }
        });
        refrescar();

        Repo.perfil(new Repo.Callback<Map<String, Object>>() {
            @Override
            public void ok(Map<String, Object> p) {
                perfil = p;
                perfilCargado = true;
                Object dir = p.get("direccion");
                if (dir != null) etDireccion.setText(dir.toString());
                refrescar();
            }
            @Override
            public void error(String m) {
                Toast.makeText(CarritoActivity.this,
                    "No se pudo cargar tu perfil: " + m, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override protected void onDestroy() { if (tasaRegistro != null) tasaRegistro.remove(); super.onDestroy(); }

    private void refrescar() {
        adapter.notifyDataSetChanged();
        boolean vacio = Carrito.items.isEmpty();
        tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        tvTotal.setText("Total: " + Carrito.dinero(Carrito.total()) + " CUP"\n            + (tasaUSD > 0 ? "\\nEquivalente: " + Carrito.dinero(Carrito.total()/tasaUSD) + " USD" : ""));
        btnEnviar.setEnabled(!vacio && perfilCargado);
    }

    private void enviar() {
        String dir = etDireccion.getText() == null ? "" : etDireccion.getText().toString().trim();
        if (dir.isEmpty()) {
            Toast.makeText(this, "Escribe la dirección de entrega.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Carrito.items.isEmpty()) return;

        btnEnviar.setEnabled(false);
        Repo.enviarPedido(perfil, dir, new Repo.Callback<Void>() {
            @Override
            public void ok(Void r) {
                Carrito.vaciar();
                Toast.makeText(CarritoActivity.this,
                    "¡Pedido enviado! Puedes seguirlo en «Mis pedidos».", Toast.LENGTH_LONG).show();
                finish();
            }
            @Override
            public void error(String m) {
                btnEnviar.setEnabled(true);
                Toast.makeText(CarritoActivity.this,
                    "No se pudo enviar el pedido: " + m, Toast.LENGTH_LONG).show();
            }
        });
    }

    class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            TextView nombre, subtotal, cant;
            ImageButton menos, mas;
            VH(View v) {
                super(v);
                nombre = v.findViewById(R.id.tvNombre);
                subtotal = v.findViewById(R.id.tvSubtotal);
                cant = v.findViewById(R.id.tvCant);
                menos = v.findViewById(R.id.btnMenos);
                mas = v.findViewById(R.id.btnMas);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrito, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ItemCarrito i = Carrito.items.get(pos);
            h.nombre.setText(i.nombre);
            h.subtotal.setText(Carrito.dinero(i.precio) + " c/u  ·  "
                + Carrito.dinero(i.precio * i.cantidad));
            h.cant.setText(Carrito.cantidadTexto(i.cantidad) + " " + i.unidadMedida);

            h.menos.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) {
                    Carrito.quitarUno(p);
                    refrescar();
                }
            });
            h.mas.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                if (Carrito.sumarUno(p)) {
                    refrescar();
                } else {
                    Toast.makeText(CarritoActivity.this,
                        "No hay más existencias de este producto.",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return Carrito.items.size();
        }
    }
}
