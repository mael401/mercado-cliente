package com.mercadototal.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean modoRegistro = false;

    private View bloqueRegistro;
    private TextInputEditText etNombre, etTelefono, etDireccion, etEmail, etPassword;
    private Button btnAccion;
    private TextView tvTitulo, tvCambiar, tvOlvide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            irAInicio();
            return;
        }

        setContentView(R.layout.activity_auth);
        bloqueRegistro = findViewById(R.id.bloqueRegistro);
        etNombre = findViewById(R.id.etNombre);
        etTelefono = findViewById(R.id.etTelefono);
        etDireccion = findViewById(R.id.etDireccion);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnAccion = findViewById(R.id.btnAccion);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvCambiar = findViewById(R.id.tvCambiar);
        tvOlvide = findViewById(R.id.tvOlvide);

        btnAccion.setOnClickListener(v -> {
            if (modoRegistro) registrar(); else iniciar();
        });
        tvCambiar.setOnClickListener(v -> {
            modoRegistro = !modoRegistro;
            actualizarModo();
        });
        tvOlvide.setOnClickListener(v -> olvidePassword());
        actualizarModo();
    }

    private void actualizarModo() {
        if (modoRegistro) {
            tvTitulo.setText("Crear cuenta");
            btnAccion.setText("Registrarme");
            tvCambiar.setText("¿Ya tienes cuenta? Inicia sesión");
            tvOlvide.setVisibility(View.GONE);
            bloqueRegistro.setVisibility(View.VISIBLE);
        } else {
            tvTitulo.setText("Iniciar sesión");
            btnAccion.setText("Entrar");
            tvCambiar.setText("¿No tienes cuenta? Regístrate");
            tvOlvide.setVisibility(View.VISIBLE);
            bloqueRegistro.setVisibility(View.GONE);
        }
    }

    private String texto(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void iniciar() {
        String email = texto(etEmail);
        String pass = texto(etPassword);
        if (email.isEmpty() || pass.isEmpty()) {
            aviso("Escribe tu correo y contraseña.");
            return;
        }
        btnAccion.setEnabled(false);
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener(r -> irAInicio())
            .addOnFailureListener(e -> {
                btnAccion.setEnabled(true);
                aviso(mensajeError(e));
            });
    }

    private void registrar() {
        String nombre = texto(etNombre);
        String telefono = texto(etTelefono);
        String direccion = texto(etDireccion);
        String email = texto(etEmail);
        String pass = texto(etPassword);

        if (nombre.isEmpty() || telefono.isEmpty() || direccion.isEmpty()) {
            aviso("Completa nombre, teléfono y dirección.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            aviso("El correo no es válido.");
            return;
        }
        if (pass.length() < 6) {
            aviso("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        btnAccion.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener(r -> {
                Map<String, Object> u = new HashMap<>();
                u.put("nombre", nombre);
                u.put("telefono", telefono);
                u.put("direccion", direccion);
                u.put("email", email);
                u.put("rol", "cliente");
                u.put("activo", true);
                u.put("creado", FieldValue.serverTimestamp());

                db.collection("users").document(r.getUser().getUid()).set(u)
                    .addOnSuccessListener(x -> irAInicio())
                    .addOnFailureListener(e -> {
                        r.getUser().delete();
                        btnAccion.setEnabled(true);
                        aviso("No se pudo guardar tu perfil: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                btnAccion.setEnabled(true);
                aviso(mensajeError(e));
            });
    }

    private void olvidePassword() {
        EditText entrada = new EditText(this);
        entrada.setHint("Correo registrado");
        entrada.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        entrada.setText(texto(etEmail));

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout caja = new FrameLayout(this);
        caja.setPadding(pad, 8, pad, 0);
        caja.addView(entrada);

        new AlertDialog.Builder(this)
            .setTitle("Restablecer contraseña")
            .setMessage("Te enviaremos un enlace a tu correo para crear una contraseña nueva.")
            .setView(caja)
            .setPositiveButton("Enviar", (d, w) -> enviarReset(entrada.getText().toString().trim()))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void enviarReset(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            aviso("Escribe un correo válido.");
            return;
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener(x -> aviso(
                "Listo. Si el correo está registrado, recibirás un enlace para restablecer tu contraseña. Revisa también la carpeta de spam."))
            .addOnFailureListener(e -> aviso(mensajeError(e)));
    }

    private String mensajeError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException)
            return "Ese correo ya está registrado.";
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException)
            return "Correo o contraseña incorrectos.";
        if (e instanceof FirebaseNetworkException)
            return "Sin conexión a internet.";
        return "Error: " + e.getMessage();
    }

    private void aviso(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }

    private void irAInicio() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
