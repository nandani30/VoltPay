package com.voltpay.app.data.model;

import java.util.List;

public class ContactSyncRequest {
    private List<Contact> contacts;

    public ContactSyncRequest() {}

    public ContactSyncRequest(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }
}
