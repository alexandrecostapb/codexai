package br.com.example.codexai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import io.noties.markwon.Markwon;
import android.content.Context;


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
    }

    @Override
    public int getItemCount(){
        return mensagens.size();
    }
}