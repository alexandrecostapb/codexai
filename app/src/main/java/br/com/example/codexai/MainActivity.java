package br.com.example.codexai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import androidx.core.content.FileProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    private ImageButton btnHistorico;
    private ImageButton btnHistorico2;
    private ImageButton btnSobre;
    private LinearLayout menuHistorico;
    private ImageButton enviarButton;
    private ImageButton imagemButton;
    private MaterialButton btnNovoChat;
    private EditText promptEditText;
    private ImageView imagemPreview;
    private RecyclerView mensagemRecyclerView;
    private ArrayList<Conversa> historicoConversas;
    private Conversa conversaAtual;
    private MensagemAdapter mensagemAdapter;
    private ArrayList<Mensagem> mensagens;
    private OpenRouterUtil openRouterUtil;
    private RecyclerView recyclerHistorico;
    private HistoricoAdapter historicoAdapter;
    private Bitmap imagemSelecionada;
    private DrawerLayout drawerLayout;
    private String caminhoFoto;
    private BancoDeDados bancoDeDados;
    private static final int CAMERA_REQUEST = 1;
    private static final int GALERIA_REQUEST = 2;
    private static final int CAMERA_PERMISSION = 100;
    private static final int LIMITE_CHATS = 5;
    private boolean enviando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 2. LINK DAS VIEWS PRIMEIRO

        btnHistorico = findViewById(R.id.btnHistorico);
        btnHistorico2 = findViewById(R.id.btnHistorico2);
        btnSobre = findViewById(R.id.btnSobre);
        menuHistorico = findViewById(R.id.menu_do_historico);
        enviarButton = findViewById(R.id.enviarButton);
        imagemButton = findViewById(R.id.imagemButton);
        promptEditText = findViewById(R.id.promptEditText);
        imagemPreview = findViewById(R.id.imagemPreview);
        btnNovoChat = findViewById(R.id.btnNovoChat);
        recyclerHistorico = findViewById(R.id.recyclerHistorico);
        drawerLayout = findViewById(R.id.main);
        mensagemRecyclerView = findViewById(R.id.mensagemRecyclerView);

        getWindow().setStatusBarColor(
                Color.parseColor("#0A111A")
        );

        getWindow().setNavigationBarColor(
                Color.parseColor("#344468")
        );

        bancoDeDados = BancoDeDados.getDatabase(this);

        // 3. LISTAS (OBRIGATÓRIO ANTES DOS ADAPTERS)
        historicoConversas =
                new ArrayList<>(
                        bancoDeDados
                                .conversaDao()
                                .listarTodas()
                );
        mensagens = new ArrayList<>();
        conversaAtual = null;

        // 4. RECYCLER HISTÓRICO
        recyclerHistorico.setLayoutManager(new LinearLayoutManager(this));

        historicoAdapter = new HistoricoAdapter(
                historicoConversas,
                position -> abrirConversa(
                        historicoConversas.get(position)
                ),
                new HistoricoAdapter.OnItemLongClickListener() {

                    @Override
                    public void onEditar(int position) {
                        View view = getLayoutInflater()
                                .inflate(R.layout.dialog_renomear, null);
                        TextInputEditText editText =
                                view.findViewById(R.id.editNomeChat);
                        editText.setText(
                                historicoConversas.get(position).getTitulo()
                        );
                        AlertDialog dialog = new AlertDialog.Builder(
                                MainActivity.this
                        )
                                .setTitle("Renomear chat")
                                .setView(view)
                                .setPositiveButton("Salvar", (d, which) -> {
                                    String novoNome =
                                            editText.getText()
                                                    .toString()
                                                    .trim();
                                    if (!novoNome.isEmpty()) {
                                        Conversa conversa =
                                                historicoConversas.get(position);
                                        conversa.setTitulo(novoNome);
                                        bancoDeDados
                                                .conversaDao()
                                                .atualizar(conversa);
                                        historicoAdapter.notifyItemChanged(position);
                                        historicoAdapter.limparSelecao();
                                    }
                                })
                                .setNegativeButton("Cancelar", null)
                                .create();
                        dialog.show();
                        dialog.getWindow().setBackgroundDrawableResource(
                                R.drawable.dialog_background
                        );
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(Color.parseColor("#FFFFFF"));
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(Color.parseColor("#FFFFFF"));
                    }

                    @Override
                    public void onExcluir(int position) {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Excluir conversa")
                                .setMessage("Deseja realmente excluir?")
                                .setPositiveButton("Sim", (d, which) -> {
                                    Conversa conversa = historicoConversas.get(position);

                                    bancoDeDados
                                            .conversaDao()
                                            .remover(conversa);

                                    historicoConversas.remove(position);
                                    historicoAdapter.notifyItemRemoved(position);
                                    historicoAdapter.limparSelecao();
                                })
                                .setNegativeButton("Não", null)
                                .create();
                        dialog.show();
                        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(Color.parseColor("#FFFFFF"));
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(Color.parseColor("#FFFFFF"));
                    }
                }
        );

        recyclerHistorico.setAdapter(historicoAdapter);

        // 5. CHAT RECYCLER
        String mensagemInicial =
                "Olá, eu sou o ClareAI!\n" +
                        "Uma IA criada para te ajudar a aprender programação de forma simples.\n" +
                        "Pode me perguntar qualquer coisa, programador!";

        mensagens.add(new Mensagem(mensagemInicial, false));
        mensagemAdapter = new MensagemAdapter(mensagens, this);
        mensagemRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mensagemRecyclerView.setAdapter(mensagemAdapter);

        // 6. OBJETO IA
        openRouterUtil = new OpenRouterUtil();

        // 7. LISTENERS
        btnHistorico.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        btnHistorico2.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        btnSobre.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Sobre.class);
            startActivity(intent);
        });

        imagemButton.setOnClickListener(v -> escolherImagem());
        enviarButton.setOnClickListener(v -> enviarMensagem());
        btnNovoChat.setOnClickListener(v -> {
            if (conversaAtual == null && atingiuLimiteChats()) {
                Toast.makeText(
                        this,
                        "Exclua um chat ou converse em um já criado.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            prepararNovoChat();
        });
    }


    private void atualizarHistorico() {
        historicoAdapter.notifyDataSetChanged();
        //salvarConversas();
    }

    private void rolarParaBaixo() {
        mensagemRecyclerView.post(() -> {
            if (mensagemAdapter.getItemCount() > 0) {
                mensagemRecyclerView.smoothScrollToPosition(
                        mensagemAdapter.getItemCount() - 1
                );
            }
        });
    }

    private void escolherImagem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecionar imagem");
        builder.setItems(
                new String[]{"Câmera", "Galeria"},
                (dialog, which) -> {
                    if (which == 0) {
                        abrirCamera();
                    } else {
                        Intent galeriaIntent = new Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        );
                        startActivityForResult(
                                galeriaIntent,
                                GALERIA_REQUEST
                        );
                    }
                }
        );
        builder.show();
    }

    private void mostrarMenuChat(int position) {
        String[] opcoes = {
                "Renomear",
                "Excluir"
        };
        new AlertDialog.Builder(this)
                .setTitle(historicoConversas.get(position).getTitulo())
                .setItems(opcoes, (dialog, which) -> {
                    if(which == 0){
                        // renomear
                    }
                    if(which == 1){
                        // excluir
                    }
                })
                .show();
    }

    private Bitmap reduzirImagem(Bitmap original) {
        int larguraOriginal = original.getWidth();
        int alturaOriginal = original.getHeight();
        int novaLargura;
        int novaAltura;
        if (larguraOriginal >= alturaOriginal) {
            novaLargura = 768;
            novaAltura = (alturaOriginal * 768) / larguraOriginal;
        } else {
            novaAltura = 768;
            novaLargura = (larguraOriginal * 768) / alturaOriginal;
        }
        return Bitmap.createScaledBitmap(
                original,
                novaLargura,
                novaAltura,
                true
        );
    }

    private String bitmapParaBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private File criarArquivoImagem() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nomeArquivo = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                nomeArquivo,
                ".jpg",
                storageDir
        );
        caminhoFoto = image.getAbsolutePath();
        return image;
    }

    private void abrirCamera() {
        Toast.makeText(this, "Abrindo câmera", Toast.LENGTH_SHORT).show();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            return;
        }
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File fotoArquivo = null;
            try {
                fotoArquivo = criarArquivoImagem();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fotoArquivo != null) {
                Uri fotoURI = FileProvider.getUriForFile(
                        this,
                        "br.com.example.codexai.fileprovider",
                        fotoArquivo
                );
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fotoURI);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        }
    }

    private void novoChat() {
        Conversa conversa =
                new Conversa("");
        long id =
                bancoDeDados
                        .conversaDao()
                        .inserir(conversa);
        conversa.setId((int) id);
        conversa.setTitulo(
                "Chat " + id
        );
        bancoDeDados
                .conversaDao()
                .atualizar(conversa);
        conversaAtual = conversa;
        historicoConversas.add(0, conversa);
        historicoAdapter.notifyDataSetChanged();
        mensagens = new ArrayList<>();
        mensagemAdapter = new MensagemAdapter(
                mensagens,
                this
        );
        mensagemRecyclerView.setAdapter(
                mensagemAdapter
        );
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void prepararNovoChat(){
        conversaAtual = null;
        mensagens = new ArrayList<>();
        mensagemAdapter = new MensagemAdapter(
                mensagens,
                this
        );
        mensagemRecyclerView.setAdapter(
                mensagemAdapter
        );
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private boolean atingiuLimiteChats() {
        return bancoDeDados.conversaDao().listarTodas().size() >= LIMITE_CHATS;
    }

    public void abrirConversa(Conversa conversa) {
        conversaAtual = conversa;
        mensagens = new ArrayList<>();
        List<MensagemEntity> listaBanco =
                bancoDeDados
                        .mensagemDao()
                        .buscarMensagens(
                                conversaAtual.getId()
                        );
        for(MensagemEntity msg : listaBanco){
            if(msg.getImagemBase64() != null){
                byte[] bytes =
                        Base64.decode(
                                msg.getImagemBase64(),
                                Base64.DEFAULT
                        );
                Bitmap bitmap =
                        BitmapFactory.decodeByteArray(
                                bytes,
                                0,
                                bytes.length
                        );
                Mensagem mensagem = new Mensagem(
                        bitmap,
                        msg.getTexto(),
                        msg.isUsuario()
                );

                mensagem.setId(msg.getId());

                mensagens.add(mensagem);
            }else{
                Mensagem mensagem = new Mensagem(
                        msg.getTexto(),
                        msg.isUsuario()
                );

                mensagem.setId(msg.getId());

                mensagens.add(mensagem);
            }
        }
        mensagemAdapter = new MensagemAdapter(
                mensagens,
                MainActivity.this
        );
        mensagemRecyclerView.setAdapter(
                mensagemAdapter
        );
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        try {
            if (requestCode == CAMERA_REQUEST) {
                File imgFile = new File(caminhoFoto);

                if (imgFile.exists()) {
                    imagemSelecionada = MediaStore.Images.Media.getBitmap(
                            getContentResolver(),
                            Uri.fromFile(imgFile)
                    );
                }
            } else if (requestCode == GALERIA_REQUEST) {
                Uri uri = data.getData();
                if (uri != null) {
                    imagemSelecionada = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), uri);
                }
            }
            if (imagemSelecionada != null) {
                imagemPreview.setImageBitmap(imagemSelecionada);
                imagemPreview.setVisibility(ImageView.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarMensagem() {
        String textoDigitado = promptEditText.getText().toString().trim();
        if (textoDigitado.isEmpty() && imagemSelecionada == null) {
            Toast.makeText(this, "Digite algo ou selecione uma imagem", Toast.LENGTH_SHORT).show();
            return;
        }
        if (conversaAtual == null) {
            if (atingiuLimiteChats()) {
                Toast.makeText(
                        this,
                        "Você atingiu o limite de 5 chats. Exclua um chat ou converse em um já criado.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            novoChat();
        }
        if (conversaAtual == null) {
            novoChat();
        }
        if (imagemSelecionada != null) {
            mensagens.add(new Mensagem(imagemSelecionada, textoDigitado, true));
        } else {
            mensagens.add(new Mensagem(textoDigitado, true));
        }
        Bitmap imagemReduzida = null;
        String imagemBase64 = null;
        if(imagemSelecionada != null){
            imagemReduzida = reduzirImagem(imagemSelecionada);
            imagemBase64 = bitmapParaBase64(imagemReduzida);
        }
        MensagemEntity mensagemEntity =
                new MensagemEntity(
                        conversaAtual.getId(),
                        textoDigitado,
                        true,
                        imagemBase64
                );
        bancoDeDados
                .mensagemDao()
                .inserir(mensagemEntity);
        mensagemAdapter.notifyItemInserted(mensagens.size() - 1);
        rolarParaBaixo();
        enviarParaIA(textoDigitado, imagemReduzida);
        imagemPreview.setVisibility(View.GONE);
        imagemSelecionada = null;
        promptEditText.setText("");
    }
    //metodo editar a ultima mensagem
    public void editarUltimaMensagem(int idMensagemUsuario, String novoTexto) {

        // Atualiza a mensagem do usuário no banco
        bancoDeDados.mensagemDao().atualizarMensagem(idMensagemUsuario, novoTexto);

        // Atualiza a mensagem na lista
        for (Mensagem m : mensagens) {
            if (m.getId() == idMensagemUsuario) {
                m.setTexto(novoTexto);
                break;
            }
        }

        // Remove a última resposta da IA da lista E do banco
        if (mensagens.size() > 0) {
            Mensagem ultima = mensagens.get(mensagens.size() - 1);

            if (!ultima.isUsuario()) {

                // Remove do banco
                bancoDeDados.mensagemDao().removerPorId(ultima.getId());

                // Remove da lista
                mensagens.remove(mensagens.size() - 1);
                mensagemAdapter.notifyItemRemoved(mensagens.size());
            }
        }

        mensagemAdapter.notifyDataSetChanged();

        // Gera uma nova resposta da IA
        enviarParaIA(novoTexto, null);
    }

    private void enviarParaIA(String texto, Bitmap imagem) {
        if (enviando) return;
        enviando = true;
        mensagens.add(new Mensagem("Pensando...", false));
        mensagemAdapter.notifyItemInserted(mensagens.size() - 1);
        rolarParaBaixo();
        String base64Imagem = bitmapParaBase64(imagem);
        android.util.Log.d(
                "IMG_TEST",
                base64Imagem == null
                        ? "Imagem NULL"
                        : "Imagem recebida. Tamanho: " + base64Imagem.length()
        );
        ArrayList<Mensagem> listaParaIA = new ArrayList<>();
        int contador = 0;
        for (int i = mensagens.size() - 1; i >= 0; i--) {
            Mensagem m = mensagens.get(i);
            if (m.getTexto() != null && m.getTexto().equals("Pensando...")) {
                continue;
            }
            listaParaIA.add(0, m);
            contador++;
            if (contador == 5) {
                break;
            }
        }

        openRouterUtil.enviarMensagem(listaParaIA, base64Imagem, new OpenRouterUtil.PoeCallback() {
            @Override
            public void onResponse(String resposta) {
                runOnUiThread(() -> {

                    mensagens.set(mensagens.size() - 1, new Mensagem(resposta, false));
                    MensagemEntity msgIA =
                            new MensagemEntity(
                                    conversaAtual.getId(),
                                    resposta,
                                    false,
                                    null
                            );
                    bancoDeDados
                            .mensagemDao()
                            .inserir(msgIA);
                    mensagemAdapter.notifyItemChanged(mensagens.size() - 1);
                    rolarParaBaixo();
                    enviando = false;
                });
            }

            @Override
            public void onError(String erro) {
                runOnUiThread(() -> {
                    mensagens.set(mensagens.size() - 1, new Mensagem("Erro: " + erro, false));
                    mensagemAdapter.notifyItemChanged(mensagens.size() - 1);
                    rolarParaBaixo();
                    enviando = false;
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}