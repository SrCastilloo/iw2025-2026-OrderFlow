package es.uca.orderflow.business.services;

public class PaymentRequest {
    private String address;
    private String opaqueToken; // token/nonce del PSP externo

    public PaymentRequest withAddress(String a){ this.address=a; return this; }
    public PaymentRequest withOpaqueToken(String t){ this.opaqueToken=t; return this; }

    public String address() { return address; }
    public String opaqueToken() { return opaqueToken; }
}
