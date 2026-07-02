package com.voltpay.app.ui.contacts;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.voltpay.app.R;
import com.voltpay.app.data.model.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private List<Contact> contacts = new ArrayList<>();
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
        void onContactLongClick(Contact contact, View view);
    }

    public ContactsAdapter(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void setContacts(List<Contact> newContacts) {
        this.contacts = newContacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.tvName.setText(contact.getName());
        holder.tvUpiId.setText(contact.getUpiId());

        if (contact.getLastPaidAt() > 0) {
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                    contact.getLastPaidAt(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS
            ).toString();
            holder.tvLastPaid.setText("Last paid: " + timeAgo);
            holder.tvLastPaid.setVisibility(View.VISIBLE);
        } else {
            holder.tvLastPaid.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onContactClick(contact));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onContactLongClick(contact, v);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvUpiId;
        TextView tvLastPaid;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvUpiId = itemView.findViewById(R.id.tvUpiId);
            tvLastPaid = itemView.findViewById(R.id.tvLastPaid);
        }
    }
}
