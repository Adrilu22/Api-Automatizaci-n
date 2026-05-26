package com.tiendamoda.model;

public record VentaRequest(String mes, int clienteId, double total, boolean descuentoAplicado) {}
