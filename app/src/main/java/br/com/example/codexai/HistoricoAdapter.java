package br.com.example.codexai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.ViewHolder> {

    private ArrayList<Conversa> conversas;
    private OnConversaClickListener listener;

    public interface OnConversaClickListener {
        void onConversaClick(int position);
    }

    public HistoricoAdapter(
            ArrayList<Conversa> conversas,
            OnConversaClickListener listener
    ) {
        this.conversas = conversas;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tituloChat;

        public ViewHolder(View itemView) {
            super(itemView);

            tituloChat =
                    itemView.findViewById(R.id.tituloChat);
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

    @Override
    public void onBindViewHolder(
            ViewHolder holder,
            int position
    ) {

        holder.tituloChat.setText(
                conversas.get(position).getTitulo()
        );

        holder.itemView.setOnClickListener(v -> {
            listener.onConversaClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return conversas.size();
    }
}