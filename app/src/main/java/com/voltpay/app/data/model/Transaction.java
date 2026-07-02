package com.voltpay.app.data.model;

public class Transaction {
    private long id;
    private double amount;
    private String type; // DEBIT or CREDIT
    private String upiRef;
    private String counterparty;
    private long timestamp;
    private String rawSms;
    private String source; // SMS or MANUAL

    public Transaction() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUpiRef() { return upiRef; }
    public void setUpiRef(String upiRef) { this.upiRef = upiRef; }

    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getRawSms() { return rawSms; }
    public void setRawSms(String rawSms) { this.rawSms = rawSms; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
