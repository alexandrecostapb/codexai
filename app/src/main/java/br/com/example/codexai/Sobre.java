package br.com.example.codexai;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Sobre extends AppCompatActivity {
    private ImageButton btnHomeVoltar;
    private void abrirNavegador(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sobre);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnHomeVoltar = findViewById(R.id.btnVoltar);
        getWindow().setStatusBarColor(Color.parseColor("#0A111A"));
        getWindow().setNavigationBarColor(Color.parseColor("#344468"));

        btnHomeVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        //Arlindo
        findViewById(R.id.linkGitArlindo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirNavegador("https://github.com/arlindofranklin1");
            }
        });
        findViewById(R.id.linkLinkedinArlindo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirNavegador("https://www.linkedin.com/in/arlindo-franklim-62b6a3409?utm_source=share_via&utm_content=profile&utm_medium=member_android");
            }
        });

        //Iasmim
        findViewById(R.id.linkGitIasmim).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirNavegador("https://github.com/iasmimanahi");
            }
        });
        findViewById(R.id.linkLinkedinIasmim).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirNavegador("https://www.linkedin.com/in/iasmim-anah%C3%AD-14a25240a/");
            }
        });

        //Kauã
        findViewById(R.id.linkGitKaua).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirNavegador("https://github.com/kauazinn2109");
            }
        });
        findViewById(R.id.linkLinkedinKaua).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirNavegador("https://www.linkedin.com/in/kau%C3%A3-lucas-a98693409/");
            }
        });

        //Alexandre
        findViewById(R.id.linkGitAlexandre).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirNavegador("https://github.com/alexandrecostapb");
            }
        });
        findViewById(R.id.linkLinkedinAlexandre).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirNavegador("https://www.linkedin.com/in/alexandrecostapb/");
            }
        });
    }
}