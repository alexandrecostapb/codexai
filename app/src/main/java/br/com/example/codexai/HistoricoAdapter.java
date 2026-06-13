package br.com.example.codexai;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.ViewHolder> {

    private ArrayList<Conversa> conversas;
    private OnConversaClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private int itemSelecionado = -1;

    public interface OnConversaClickListener {
        void onConversaClick(int position);
    }

    public interface OnItemLongClickListener {
        void onEditar(int position);
        void onExcluir(int position);
    }

    public HistoricoAdapter(
            ArrayList<Conversa> conversas,
            OnConversaClickListener clickListener,
            OnItemLongClickListener longClickListener
    ) {
        this.conversas = conversas;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemCount() {
        return conversas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tituloChat;
        ImageView btnEditar;
        ImageView btnExcluir;

        public ViewHolder(View itemView) {
            super(itemView);

            tituloChat = itemView.findViewById(R.id.tituloChat);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnExcluir = itemView.findViewById(R.id.btnExcluir);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(
            ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_historico,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    public void selecionarItem(int position){
        itemSelecionado = position;
        notifyDataSetChanged();
    }

    public void limparSelecao(){
        itemSelecionado = -1;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(
            ViewHolder holder,
            int position
    ) {

        holder.tituloChat.setText(
                conversas.get(position).getTitulo()
        );

        if(position == itemSelecionado){

            holder.btnEditar.setVisibility(View.VISIBLE);
            holder.btnExcluir.setVisibility(View.VISIBLE);

            holder.itemView.setBackgroundColor(
                    Color.parseColor("#1E3A5F")
            );

        }else{

            holder.btnEditar.setVisibility(View.GONE);
            holder.btnExcluir.setVisibility(View.GONE);

            holder.itemView.setBackgroundColor(
                    Color.TRANSPARENT
            );
        }

        holder.itemView.setOnClickListener(v -> {
            clickListener.onConversaClick(position);
        });

        holder.itemView.setOnLongClickListener(v -> {

            selecionarItem(position);

            return true;
        });

        holder.btnEditar.setOnClickListener(v -> {
            longClickListener.onEditar(position);
        });

        holder.btnExcluir.setOnClickListener(v -> {
            longClickListener.onExcluir(position);
        });

        holder.itemView.setOnClickListener(v -> {

            limparSelecao();

            clickListener.onConversaClick(position);
        });
    }
}