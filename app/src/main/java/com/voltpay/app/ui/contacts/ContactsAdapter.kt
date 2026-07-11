package com.voltpay.app.ui.contacts

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.voltpay.app.R
import com.voltpay.app.data.model.Contact

class ContactsAdapter(private val listener: OnContactClickListener) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private var contacts: List<Contact> = ArrayList()

    interface OnContactClickListener {
        fun onContactClick(contact: Contact)
        fun onContactLongClick(contact: Contact, view: View)
    }

    fun setContacts(newContacts: List<Contact>) {
        this.contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.tvName.text = contact.name
        holder.tvUpiId.text = contact.upiId

        if (contact.lastPaidAt > 0) {
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                contact.lastPaidAt,
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS
            ).toString()
            holder.tvLastPaid.text = "Last paid: $timeAgo"
            holder.tvLastPaid.visibility = View.VISIBLE
        } else {
            holder.tvLastPaid.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { listener.onContactClick(contact) }
        holder.itemView.setOnLongClickListener { v ->
            listener.onContactLongClick(contact, v)
            true
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvUpiId: TextView = itemView.findViewById(R.id.tvUpiId)
        val tvLastPaid: TextView = itemView.findViewById(R.id.tvLastPaid)
    }
}
