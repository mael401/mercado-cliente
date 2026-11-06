package com.mercadototal.cliente;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Decodifica fotos Base64 con reducción de resolución y caché limitada por memoria. */
public final class Img {

    // Límite defensivo. Firestore limita cada documento a 1 MiB; Base64 ocupa más que el binario.
    private static final int MAX_BASE64_CHARS = 900_000;
    private static final int MAX_DIMENSION = 1000;

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };

    private Img() { }

    public static Bitmap desdeBase64(String id, String b64) {
        if (b64 == null || b64.isEmpty() || b64.length() > MAX_BASE64_CHARS) return null;

        String clave = clave(id, b64);
        Bitmap almacenado = CACHE.get(clave);
        if (almacenado != null && !almacenado.isRecycled()) return almacenado;

        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calcularMuestra(bounds.outWidth, bounds.outHeight);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (bitmap != null) CACHE.put(clave, bitmap);
            return bitmap;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (OutOfMemoryError e) {
            // Una imagen anómala no debe cerrar la aplicación completa.
            return null;
        }
    }

    private static int calcularMuestra(int ancho, int alto) {
        int muestra = 1;
        while (ancho / muestra > MAX_DIMENSION || alto / muestra > MAX_DIMENSION) {
            muestra *= 2;
        }
        return muestra;
    }

    private static String clave(String id, String contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder((id == null ? "" : id) + ":");
            for (byte b : hash) sb.append(String.format(java.util.Locale.US, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            // Fallback que incluye el contenido completo para evitar colisiones por longitud.
            return (id == null ? "" : id) + ":" + contenido.hashCode() + ":" + contenido.length();
        }
    }
}
