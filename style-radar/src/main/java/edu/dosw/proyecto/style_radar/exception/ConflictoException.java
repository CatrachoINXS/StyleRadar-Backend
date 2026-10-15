package edu.dosw.proyecto.style_radar.exception;

/** Conflicto con el estado actual de los datos (HTTP 409), por ejemplo un duplicado. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String message) {
        super(message);
    }
}
