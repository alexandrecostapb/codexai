package br.com.example.clareai;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String ESTADO_FOTO = "caminho_foto";
    private static final float ALPHA_DESABILITADO = 0.4f;

    private ClareAIViewModel viewModel;
    private EditText promptEditText;
    private ImageButton enviarButton;
    private ImageView imagemPreview;
    private RecyclerView mensagemRecyclerView;
    private DrawerLayout drawerLayout;
    private MensagemAdapter mensagemAdapter;
    private HistoricoAdapter historicoAdapter;
    private File arquivoFoto;
    private int totalMensagensRenderizado;

    private final androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest> seletorGaleria =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) viewModel.carregarImagem(uri, null);
            });

    private final androidx.activity.result.ActivityResultLauncher<Uri> tirarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), sucesso -> {
                if (arquivoFoto == null) return;
                if (sucesso) {
                    viewModel.carregarImagem(Uri.fromFile(arquivoFoto), arquivoFoto);
                } else {
                    arquivoFoto.delete();
                }
                arquivoFoto = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            String caminho = savedInstanceState.getString(ESTADO_FOTO);
            if (caminho != null) arquivoFoto = new File(caminho);
        }
        viewModel = new ViewModelProvider(this).get(ClareAIViewModel.class);

        drawerLayout = findViewById(R.id.main);
        promptEditText = findViewById(R.id.promptEditText);
        enviarButton = findViewById(R.id.enviarButton);
        imagemPreview = findViewById(R.id.imagemPreview);
        mensagemRecyclerView = findViewById(R.id.mensagemRecyclerView);
        RecyclerView recyclerHistorico = findViewById(R.id.recyclerHistorico);

        promptEditText.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(ClareAIViewModel.MAX_CARACTERES)
        });

        ajustarInsets();

        historicoAdapter = new HistoricoAdapter(
                viewModel.getHistorico(),
                position -> {
                    viewModel.abrirConversa(viewModel.getHistorico().get(position));
                    drawerLayout.closeDrawer(GravityCompat.START);
                },
                new HistoricoAdapter.OnItemLongClickListener() {
                    @Override
                    public void onEditar(int position) {
                        dialogRenomear(viewModel.getHistorico().get(position));
                    }

                    @Override
                    public void onExcluir(int position) {
                        dialogExcluir(viewModel.getHistorico().get(position));
                    }
                }
        );
        recyclerHistorico.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistorico.setAdapter(historicoAdapter);

        mensagemAdapter = new MensagemAdapter(
                viewModel.getMensagens(), this, viewModel::editarUltimaMensagem);
        mensagemRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mensagemRecyclerView.setAdapter(mensagemAdapter);

        findViewById(R.id.btnHistorico).setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        findViewById(R.id.btnHistorico2).setOnClickListener(v ->
                drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.btnSobre).setOnClickListener(v ->
                startActivity(new Intent(this, Sobre.class)));
        findViewById(R.id.imagemButton).setOnClickListener(v -> escolherImagem());
        enviarButton.setOnClickListener(v -> {
            if (viewModel.enviar(promptEditText.getText().toString())) {
                promptEditText.setText("");
            }
        });
        ((MaterialButton) findViewById(R.id.btnNovoChat)).setOnClickListener(v -> {
            viewModel.novoChat();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        viewModel.getMudancas().observe(this, versao -> renderizar());
        viewModel.getAviso().observe(this, recurso -> {
            if (recurso != null) {
                Toast.makeText(this, recurso, Toast.LENGTH_LONG).show();
                viewModel.avisoLido();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (arquivoFoto != null) outState.putString(ESTADO_FOTO, arquivoFoto.getAbsolutePath());
    }

    private void ajustarInsets() {
        View telaChat = findViewById(R.id.tela_do_chat);
        View menu = findViewById(R.id.menu_do_historico);
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (view, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            telaChat.setPadding(barras.left, barras.top, barras.right,
                    Math.max(barras.bottom, teclado.bottom));
            menu.setPadding(0, barras.top, 0, barras.bottom);
            return insets;
        });
    }

    private void renderizar() {
        mensagemAdapter.notifyDataSetChanged();
        historicoAdapter.notifyDataSetChanged();

        boolean ocupado = viewModel.isEnviando();
        enviarButton.setEnabled(!ocupado);
        enviarButton.setAlpha(ocupado ? ALPHA_DESABILITADO : 1f);

        Bitmap imagem = viewModel.getImagemSelecionada();
        imagemPreview.setImageBitmap(imagem);
        imagemPreview.setVisibility(imagem != null ? View.VISIBLE : View.GONE);

        String devolvido = viewModel.consumirTextoDevolvido();
        if (devolvido != null) promptEditText.setText(devolvido);

        int total = viewModel.getMensagens().size();
        if (total != totalMensagensRenderizado && total > 0) {
            mensagemRecyclerView.post(() -> mensagemRecyclerView.smoothScrollToPosition(total - 1));
        }
        totalMensagensRenderizado = total;
    }

    private void escolherImagem() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_selecionar_imagem)
                .setItems(
                        new String[]{getString(R.string.opcao_camera), getString(R.string.opcao_galeria)},
                        (dialog, which) -> {
                            if (which == 0) {
                                abrirCamera();
                            } else {
                                seletorGaleria.launch(new PickVisualMediaRequest.Builder()
                                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                        .build());
                            }
                        })
                .show();
    }

    // ACTION_IMAGE_CAPTURE via FileProvider não exige a permissão CAMERA.
    private void abrirCamera() {
        try {
            File pasta = new File(getCacheDir(), "fotos");
            if (!pasta.exists() && !pasta.mkdirs()) throw new IOException("pasta de fotos");
            arquivoFoto = File.createTempFile("foto_", ".jpg", pasta);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", arquivoFoto);
            tirarFoto.launch(uri);
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(this, R.string.erro_imagem, Toast.LENGTH_SHORT).show();
        }
    }

    private void dialogRenomear(Conversa conversa) {
        View view = getLayoutInflater().inflate(R.layout.dialog_renomear, null);
        TextInputEditText editText = view.findViewById(R.id.editNomeChat);
        editText.setText(conversa.getTitulo());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_renomear_chat)
                .setView(view)
                .setPositiveButton(R.string.acao_salvar, (d, which) -> {
                    viewModel.renomear(conversa, String.valueOf(editText.getText()));
                    historicoAdapter.limparSelecao();
                })
                .setNegativeButton(R.string.acao_cancelar, null)
                .create();
        mostrarDialogEscuro(dialog);
    }

    private void dialogExcluir(Conversa conversa) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_excluir_conversa)
                .setMessage(R.string.mensagem_excluir_conversa)
                .setPositiveButton(R.string.acao_sim, (d, which) -> {
                    viewModel.excluir(conversa);
                    historicoAdapter.limparSelecao();
                })
                .setNegativeButton(R.string.acao_nao, null)
                .create();
        mostrarDialogEscuro(dialog);
    }

    private void mostrarDialogEscuro(AlertDialog dialog) {
        dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
    }
}
