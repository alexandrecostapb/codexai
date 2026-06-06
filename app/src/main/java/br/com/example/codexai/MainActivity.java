package br.com.example.codexai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.mlkit.vision.text.Text;
import java.io.File;
import java.io.IOException;
import androidx.core.content.FileProvider;
import java.text.SimpleDateFormat;
import java.util.Date;


import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;


import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button btnHistorico;
    private LinearLayout menuHistorico;
    private Button enviarButton;
    private Button imagemButton;

    private Button btnNovoChat;

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


    private static final int CAMERA_REQUEST = 1;
    private static final int GALERIA_REQUEST = 2;
    private static final int CAMERA_PERMISSION = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //depois definir a activity aqui



        // =========================
        // 2. LINK DAS VIEWS PRIMEIRO
        // =========================
        btnHistorico = findViewById(R.id.btnHistorico);
        menuHistorico = findViewById(R.id.menu_do_historico);
        enviarButton = findViewById(R.id.enviarButton);
        imagemButton = findViewById(R.id.imagemButton);

        promptEditText = findViewById(R.id.promptEditText);
        imagemPreview = findViewById(R.id.imagemPreview);

        btnNovoChat = findViewById(R.id.btnNovoChat);
        recyclerHistorico = findViewById(R.id.recyclerHistorico);
        drawerLayout = findViewById(R.id.main);

        mensagemRecyclerView = findViewById(R.id.mensagemRecyclerView);

        // =========================
        // 3. LISTAS (OBRIGATÓRIO ANTES DOS ADAPTERS)
        // =========================
        historicoConversas = carregarConversas();
        mensagens = new ArrayList<>();
        conversaAtual = null;

        // =========================
        // 4. RECYCLER HISTÓRICO
        // =========================
        recyclerHistorico.setLayoutManager(new LinearLayoutManager(this));

        historicoAdapter = new HistoricoAdapter(
                historicoConversas,
                position -> abrirConversa(
                        historicoConversas.get(position)
                )
        );

        recyclerHistorico.setAdapter(historicoAdapter);

        // =========================
        // 5. CHAT RECYCLER
        // =========================
        mensagemAdapter = new MensagemAdapter(mensagens, this);
        mensagemRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mensagemRecyclerView.setAdapter(mensagemAdapter);

        // =========================
        // 6. OBJETO IA
        // =========================
        openRouterUtil = new OpenRouterUtil();

        // =========================
        // 7. LISTENERS
        // =========================
        btnHistorico.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        imagemButton.setOnClickListener(v -> escolherImagem());

        enviarButton.setOnClickListener(v -> enviarMensagem());

        btnNovoChat.setOnClickListener(v -> novoChat());
    }


    private void atualizarHistorico() {
        historicoAdapter.notifyDataSetChanged();
        salvarConversas();
    }

    private void salvarConversas() {

        try {

            JSONArray conversasArray = new JSONArray();

            for (Conversa conversa : historicoConversas) {

                JSONObject conversaObj = new JSONObject();

                conversaObj.put(
                        "titulo",
                        conversa.getTitulo()
                );

                JSONArray mensagensArray =
                        new JSONArray();

                for (Mensagem msg :
                        conversa.getMensagens()) {

                    JSONObject msgObj =
                            new JSONObject();

                    msgObj.put(
                            "texto",
                            msg.getTexto()
                    );

                    msgObj.put(
                            "usuario",
                            msg.isUsuario()
                    );

                    mensagensArray.put(msgObj);
                }

                conversaObj.put(
                        "mensagens",
                        mensagensArray
                );

                conversasArray.put(conversaObj);
            }

            getSharedPreferences(
                    "CHATS",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            "historico",
                            conversasArray.toString()
                    )
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Conversa> carregarConversas() {

        ArrayList<Conversa> lista =
                new ArrayList<>();

        try {

            String json =
                    getSharedPreferences(
                            "CHATS",
                            MODE_PRIVATE
                    )
                            .getString(
                                    "historico",
                                    ""
                            );

            if (json.isEmpty()) {
                return lista;
            }

            JSONArray conversasArray =
                    new JSONArray(json);

            for (int i = 0;
                 i < conversasArray.length();
                 i++) {

                JSONObject conversaObj =
                        conversasArray.getJSONObject(i);

                Conversa conversa =
                        new Conversa(
                                conversaObj.getString(
                                        "titulo"
                                )
                        );

                JSONArray mensagensArray =
                        conversaObj.getJSONArray(
                                "mensagens"
                        );

                for (int j = 0;
                     j < mensagensArray.length();
                     j++) {

                    JSONObject msgObj =
                            mensagensArray.getJSONObject(j);

                    conversa.getMensagens().add(
                            new Mensagem(
                                    msgObj.getString("texto"),
                                    msgObj.getBoolean("usuario")
                            )
                    );
                }

                lista.add(conversa);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }



    private void escolherImagem(){


        String[] opcoes = {"Câmera","Galeria"};


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecionar imagem");


        builder.setItems(opcoes,(dialog,i)->{


            if(i==0){


                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {


                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION);


                } else {


                    abrirCamera();


                }


            }else{


                Intent galeria = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);


                startActivityForResult(galeria,GALERIA_REQUEST);


            }


        });


        builder.show();
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


    private void abrirCamera(){


        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


        if(cameraIntent.resolveActivity(getPackageManager()) != null){


            File fotoArquivo = null;


            try{
                fotoArquivo = criarArquivoImagem();
            }catch(IOException e){
                e.printStackTrace();
            }


            if(fotoArquivo != null){


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


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if(requestCode == CAMERA_PERMISSION){


            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){


                abrirCamera();


            }else{


                Toast.makeText(this,"Permissão da câmera negada",Toast.LENGTH_SHORT).show();


            }


        }
    }



    private void novoChat() {

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

    public void abrirConversa(Conversa conversa){

        conversaAtual = conversa;

        mensagens = conversaAtual.getMensagens();

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


        if(resultCode != RESULT_OK) return;


        try {


            if(requestCode == CAMERA_REQUEST){


                File imgFile = new File(caminhoFoto);


                if(imgFile.exists()){


                    imagemSelecionada = MediaStore.Images.Media.getBitmap(
                            getContentResolver(),
                            Uri.fromFile(imgFile)
                    );


                }
            }


            else if(requestCode == GALERIA_REQUEST){


                Uri uri = data.getData();


                if(uri != null){


                    imagemSelecionada = MediaStore.Images.Media.getBitmap(
                            getContentResolver(),uri);


                }


            }


            if(imagemSelecionada != null){


                imagemPreview.setImageBitmap(imagemSelecionada);
                imagemPreview.setVisibility(ImageView.VISIBLE);


            }


        }catch(Exception e){


            e.printStackTrace();
            Toast.makeText(this,"Erro ao carregar imagem",Toast.LENGTH_SHORT).show();


        }
    }


    private void enviarMensagem() {
        String textoDigitado = promptEditText.getText().toString().trim();


        if (textoDigitado.isEmpty() && imagemSelecionada == null) {
            Toast.makeText(this, "Digite algo ou selecione uma imagem", Toast.LENGTH_SHORT).show();
            return;
        }


        if (imagemSelecionada != null) {
            // OCR da imagem antes de adicionar na tela
            reconhecerTextoImagem(imagemSelecionada, textoDigitado);
            return;
        }


// adiciona mensagem
        if (conversaAtual == null) {

            conversaAtual = new Conversa(
                    "Chat " + (historicoConversas.size() + 1)
            );

            historicoConversas.add(0, conversaAtual);

            mensagens = conversaAtual.getMensagens();

            mensagemAdapter = new MensagemAdapter(
                    mensagens,
                    this
            );

            mensagemRecyclerView.setAdapter(
                    mensagemAdapter
            );
        }

        conversaAtual.getMensagens().add(
                new Mensagem(textoDigitado, true)
        );

        mensagemAdapter.notifyItemInserted(
                conversaAtual.getMensagens().size() - 1
        );
        atualizarHistorico();
        salvarConversas();
        historicoAdapter.notifyDataSetChanged();
        mensagemAdapter.notifyItemInserted(mensagens.size() - 1);
        mensagemRecyclerView.scrollToPosition(mensagens.size() - 1);


        enviarParaIA(textoDigitado);
        promptEditText.setText("");
    }


    private void reconhecerTextoImagem(Bitmap bitmap, String textoDigitado) {
        bitmap = Bitmap.createScaledBitmap(bitmap, 1920, 1920, true);
        InputImage image = InputImage.fromBitmap(bitmap, 0);


        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(visionText -> {


                    StringBuilder textoFinal = new StringBuilder();
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            textoFinal.append(line.getText()).append("\n");
                        }
                    }


                    String textoExtraido = textoFinal.toString().trim();


                    // Combina texto digitado + OCR
                    String mensagemFinal = textoDigitado;
                    if (!textoExtraido.isEmpty()) {
                        mensagemFinal += "\n\n" + textoExtraido;
                    }

                    if (conversaAtual == null) {

                        conversaAtual = new Conversa(
                                "Chat " + (historicoConversas.size() + 1)
                        );

                        historicoConversas.add(0, conversaAtual);

                        mensagens = conversaAtual.getMensagens();
                        historicoAdapter.notifyDataSetChanged();
                        salvarConversas();
                    }


                    // Adiciona a mensagem combinada na tela
                    mensagens.add(new Mensagem(imagemSelecionada, mensagemFinal, true));
                    salvarConversas();
                    mensagemAdapter.notifyItemInserted(mensagens.size() - 1);
                    mensagemRecyclerView.scrollToPosition(mensagens.size() - 1);


                    // Envia para a IA
                    enviarParaIA(mensagemFinal);


                    imagemPreview.setVisibility(View.GONE);
                    imagemSelecionada = null;


                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao ler imagem", Toast.LENGTH_SHORT).show();
                    imagemPreview.setVisibility(View.GONE);
                    imagemSelecionada = null;
                });
    }




    private void enviarParaIA(String texto){
        // adiciona mensagem de "pensando" depois da última mensagem já adicionada
        mensagens.add(new Mensagem("⏳ Pensando...", false));
        mensagemAdapter.notifyItemInserted(mensagens.size() - 1);
        mensagemRecyclerView.scrollToPosition(mensagens.size() - 1);


        openRouterUtil.enviarMensagem(mensagens, new OpenRouterUtil.PoeCallback() {
            @Override
            public void onResponse(String resposta) {
                runOnUiThread(() -> {
                    // substitui a mensagem "pensando" pelo resultado
                    mensagens.set(mensagens.size() - 1, new Mensagem(resposta, false));
                    salvarConversas();
                    mensagemAdapter.notifyItemChanged(mensagens.size() - 1);
                    mensagemRecyclerView.scrollToPosition(mensagens.size() - 1);
                });
            }


            @Override
            public void onError(String erro) {
                runOnUiThread(() -> {
                    mensagens.set(mensagens.size() - 1, new Mensagem("Erro: " + erro, false));
                    mensagemAdapter.notifyItemChanged(mensagens.size() - 1);
                    mensagemRecyclerView.scrollToPosition(mensagens.size() - 1);
                });
            }
        });


        promptEditText.setText("");
    }

    //teste
}

