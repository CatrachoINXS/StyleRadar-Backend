package edu.dosw.proyecto.style_radar.exception;

public class SuscripcionActivaDuplicadaException extends RuntimeException {
    public SuscripcionActivaDuplicadaException() {
        super("El almacén ya tiene una suscripción vigente activa");
    }
}
