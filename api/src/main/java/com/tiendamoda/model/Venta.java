package com.tiendamoda.model;

public record Venta(int id, String mes, int clienteId, double total, boolean descuentoAplicado) {}
