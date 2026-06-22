package br.com.example.codexai;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import io.noties.markwon.Markwon;
import android.content.Context;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MensagemAdapter extends RecyclerView.Adapter<MensagemAdapter.ViewHolder> {
    private ArrayList<Mensagem> mensagens;
    private static final int TIPO_USUARIO = 1;
    private static final int TIPO_IA = 2;
    private final Markwon markwon;
    private final Context context;

    public MensagemAdapter(ArrayList<Mensagem> mensagens, Context context){
        this.mensagens = mensagens;
        this.context = context;
        this.markwon = Markwon.create(context); // inicializa Markwon para Markdown
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView texto;
        ImageView imagem;
        public ViewHolder(View itemView){
            super(itemView);
            texto = itemView.findViewById(R.id.textoMensagem);
            imagem = itemView.findViewById(R.id.imagemMensagem); //fazer a linkagem dos componentes depois
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mensagens.get(position).isUsuario() ? TIPO_USUARIO : TIPO_IA;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view;
        if(viewType == TIPO_USUARIO){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_usuario,parent,false); //definir depois o componente
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ia,parent,false); //definir depois o componete
        }
        return new ViewHolder(view);
    }

    //para abrir o dialog de ediçao de mensagem
    private void abrirDialogEdicao(Mensagem msg, int position){
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        EditText editText = new EditText(context);
        editText.setText(msg.getTexto());
        editText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        builder.setTitle("Editar mensagem");
        builder.setView(editText);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String novoTexto = editText.getText().toString();
            msg.setTexto(novoTexto);
            notifyItemChanged(position);
            ((MainActivity) context)
                    .editarUltimaMensagem(
                            msg.getId(),
                            novoTexto
                    );
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        int titleId = context.getResources().getIdentifier("alertTitle", "id", "android");
        if (titleId > 0) {
            TextView titleView = dialog.findViewById(titleId);
            if (titleView != null) {
                titleView.setTextColor(android.graphics.Color.parseColor("#FFFFFF")); // Título branco!
            }
        }
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        Mensagem msg = mensagens.get(position);
        if(msg.getImagem() != null){
            holder.imagem.setVisibility(View.VISIBLE);
            if(msg.getTexto() != null && !msg.getTexto().isEmpty()){
                holder.texto.setVisibility(View.VISIBLE);
                markwon.setMarkdown(holder.texto, msg.getTexto());
            } else {
                holder.texto.setVisibility(View.GONE);
            }
            holder.imagem.setImageBitmap(msg.getImagem());
        } else {
            holder.texto.setVisibility(View.VISIBLE);
            holder.imagem.setVisibility(View.GONE);
            markwon.setMarkdown(holder.texto, msg.getTexto());
        }

        //para editar apenas mensagem do usuario
        if(msg.isUsuario()){
            boolean ultimaMensagemUsuario = true;
            for(int i = position + 1; i < mensagens.size(); i++){
                if(mensagens.get(i).isUsuario()){
                    ultimaMensagemUsuario = false;
                    break;
                }
            }

            if(ultimaMensagemUsuario){
                holder.texto.setOnLongClickListener(v -> {
                    abrirDialogEdicao(msg, position);
                    return true;
                });
                holder.imagem.setOnLongClickListener(v -> {
                    abrirDialogEdicao(msg, position);
                    return true;
                });
            } else {
                holder.texto.setOnLongClickListener(null);
                holder.imagem.setOnLongClickListener(null);
            }
        }
    }
    @Override
    public int getItemCount(){
        return mensagens.size();
    }
}