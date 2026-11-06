package com.mercadototal.cliente;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;

/** Convierte las fotos guardadas como texto (base64) en imágenes, con caché. */
public class Img {

    private static final LruCache<String, Bitmap> cache = new LruCache<>(24);

    public static Bitmap desdeBase64(String id, String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        String clave = id + ":" + b64.length();
        Bitmap b = cache.get(clave);
        if (b != null) return b;
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (b != null) cache.put(clave, b);
            return b;
        } catch (Exception e) {
            return null;
        }
    }
}
