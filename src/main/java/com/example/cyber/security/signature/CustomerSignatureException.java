package com.example.cyber.security.signature;

public class CustomerSignatureException extends RuntimeException {
    public CustomerSignatureException() { super("A valid customer transaction signature is required"); }
}
