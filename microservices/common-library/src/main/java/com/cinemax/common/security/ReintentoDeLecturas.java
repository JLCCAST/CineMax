package com.cinemax.common.security;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;

// Reintenta solo los GET que no pudieron conectar. Un POST nunca se repite (descontaría dos
// veces), y un servicio lento tampoco: reintentar solo alargaría la espera.
public class ReintentoDeLecturas implements Retryer {

    private static final int INTENTOS = 2;
    private static final long PAUSA_MS = 150;

    private int intento = 1;

    @Override
    public void continueOrPropagate(RetryableException e) {
        if (e.method() != Request.HttpMethod.GET || !fallaDeConexion(e) || intento >= INTENTOS) {
            throw e;
        }
        intento++;
        try {
            Thread.sleep(PAUSA_MS);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public Retryer clone() {
        return new ReintentoDeLecturas();
    }

    private static boolean fallaDeConexion(RetryableException e) {
        Throwable causa = e.getCause();
        return causa instanceof ConnectException || causa instanceof NoRouteToHostException
                || causa instanceof UnknownHostException;
    }
}
