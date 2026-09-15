package com.skipadstube.app;

/** Local, in-memory diagnostics; never contains screen text or leaves the device. */
final class RuntimeStatus {
    static volatile boolean connected;
    static volatile String scan = "Servicio sin conectar";
    static volatile String lastAd = "Todavía no se ha detectado ningún anuncio";
    static volatile String audioError = "";
    static volatile String sound = "Sin probar";
    private RuntimeStatus() {}
}
