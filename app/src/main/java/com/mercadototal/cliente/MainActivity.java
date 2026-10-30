package com.mercadototal.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            salir();
            return;
        }

        setContentView(R.layout.activity_main);
        TextView tvSaludo = findViewById(R.id.tvSaludo);
        TextView tvDireccion = findViewById(R.id.tvDireccion);
        Button btnSalir = findViewById(R.id.btnSalir);

        tvSaludo.setText("Hola");
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(d -> {
                if (d.exists()) {
                    tvSaludo.setText("Hola, " + d.getString("nombre"));
                    tvDireccion.setText("Entrega en: " + d.getString("direccion"));
                }
            });

        btnSalir.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            salir();
        });
    }

    private void salir() {
        Intent i = new Intent(this, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}