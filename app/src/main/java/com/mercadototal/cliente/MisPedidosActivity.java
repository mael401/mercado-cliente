package com.mercadototal.cliente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class MisPedidosActivity extends AppCompatActivity {

    private final List<Pedido> pedidos = new ArrayList<>();
    private PedidoAdapter adapter;
    private ListenerRegistration registro;
    private TextView tvVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_pedidos);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvVacio = findViewById(R.id.tvVacio);
        RecyclerView rv = findViewById(R.id.rvPedidos);
        adapter = new PedidoAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        registro = Repo.misPedidos(new Repo.Callback<List<Pedido>>() {
            @Override
            public void ok(List<Pedido> r) {
                pedidos.clear();
                pedidos.addAll(r);
                adapter.notifyDataSetChanged();
                tvVacio.setVisibility(pedidos.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void error(String m) {
                tvVacio.setText("No se pudieron cargar tus pedidos: " + m);
                tvVacio.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registro != null) registro.remove();
    }

    private void cancelar(Pedido p) {
        new AlertDialog.Builder(this)
            .setTitle("Cancelar pedido")
            .setMessage("¿Seguro que quieres cancelar este pedido?")
            .setPositiveButton("Sí, cancelar", (d, w) ->
                Repo.cancelarPedido(p.id, new Repo.Callback<Void>() {
                    @Override
                    public void ok(Void r) {
                        Toast.makeText(MisPedidosActivity.this, "Pedido cancelado.",
                            Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void error(String m) {
                        Toast.makeText(MisPedidosActivity.this,
                            "No se pudo cancelar (¿ya fue confirmado?): " + m,
                            Toast.LENGTH_LONG).show();
                    }
                }))
            .setNegativeButton("No", null)
            .show();
    }

    class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            TextView fecha, chip, productos, direccion, total;
            Button cancelar;
            VH(View v) {
                super(v);
                fecha = v.findViewById(R.id.tvFecha);
                chip = v.findViewById(R.id.tvChip);
                productos = v.findViewById(R.id.tvProductos);
                direccion = v.findViewById(R.id.tvDireccion);
                total = v.findViewById(R.id.tvTotal);
                cancelar = v.findViewById(R.id.btnCancelar);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mi_pedido, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Pedido p = pedidos.get(pos);
            h.fecha.setText(Fmt.fecha(p.fecha));
            h.chip.setText(Fmt.estadoBonito(p.estado));
            h.chip.setTextColor(Fmt.colorEstado(p.estado));
            h.productos.setText(p.resumen("\n"));
            h.direccion.setText("Entrega en: " + (p.direccion == null ? "" : p.direccion));
            h.total.setText("Total: " + Carrito.dinero(p.total));

            boolean pendiente = "pendiente".equals(p.estado);
            h.cancelar.setVisibility(pendiente ? View.VISIBLE : View.GONE);
            h.cancelar.setOnClickListener(v -> cancelar(p));
        }

        @Override
        public int getItemCount() {
            return pedidos.size();
        }
    }
}
