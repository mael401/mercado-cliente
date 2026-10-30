package com.mercadototal.cliente;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setText("Mercado Cliente\nBase lista");
        t.setTextSize(24);
        t.setPadding(48, 96, 48, 48);
        setContentView(t);
    }
}