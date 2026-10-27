package br.com.example.clareai;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;

public class MensagemAdapter extends RecyclerView.Adapter<MensagemAdapter.ViewHolder> {

    public interface OnEditarListener {
        void onEditar(int mensagemId, String novoTexto);
    }

    private static final int TIPO_USUARIO = 1;
    private static final int TIPO_IA = 2;
    private final List<Mensagem> mensagens;
    private final Markwon markwon;
    private final OnEditarListener editarListener;

    public MensagemAdapter(List<Mensagem> mensagens, Context context, OnEditarListener editarListener){
        this.mensagens = mensagens;
        this.editarListener = editarListener;
        this.markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .build();// inicializa Markwon para Markdown
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView texto;
        ImageView imagem;
        public ViewHolder(View itemView){
            super(itemView);
            texto = itemView.findViewById(R.id.textoMensagem);
            imagem = itemView.findViewById(R.id.imagemMensagem);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mensagens.get(position).isUsuario() ? TIPO_USUARIO : TIPO_IA;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        int layout = viewType == TIPO_USUARIO ? R.layout.item_usuario : R.layout.item_ia;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    //para abrir o dialog de ediçao de mensagem
    private void abrirDialogEdicao(Context context, Mensagem msg){
        EditText editText = new EditText(context);
        editText.setText(msg.getTexto());
        editText.setTextColor(Color.WHITE);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.titulo_editar_mensagem)
                .setView(editText)
                .setPositiveButton(R.string.acao_salvar, (d, which) ->
                        editarListener.onEditar(msg.getId(), editText.getText().toString()))
                .setNegativeButton(R.string.acao_cancelar, null)
                .create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        int titleId = context.getResources().getIdentifier("alertTitle", "id", "android");
        if (titleId > 0) {
            TextView titleView = dialog.findViewById(titleId);
            if (titleView != null) {
                titleView.setTextColor(Color.WHITE);
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        Mensagem msg = mensagens.get(position);
        boolean temTexto = msg.getTexto() != null && !msg.getTexto().isEmpty();
        if (temTexto) {
            holder.texto.setVisibility(View.VISIBLE);
            markwon.setMarkdown(holder.texto, msg.getTexto());
        } else {
            holder.texto.setVisibility(View.GONE);
        }
        if (msg.getImagem() != null) {
            holder.imagem.setVisibility(View.VISIBLE);
            holder.imagem.setImageBitmap(msg.getImagem());
        } else {
            holder.imagem.setVisibility(View.GONE);
        }

        //para editar apenas a última mensagem do usuario, já respondida
        if (msg.isUsuario()) {
            boolean podeEditar = msg.getId() > 0
                    && position == mensagens.size() - 2
                    && !mensagens.get(mensagens.size() - 1).isUsuario()
                    && !mensagens.get(mensagens.size() - 1).isPendente();
            View.OnLongClickListener listener = podeEditar ? v -> {
                abrirDialogEdicao(v.getContext(), msg);
                return true;
            } : null;
            holder.texto.setOnLongClickListener(listener);
            holder.imagem.setOnLongClickListener(listener);
        }
    }

    @Override
    public int getItemCount(){
        return mensagens.size();
    }
}
