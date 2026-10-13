package com.dailyenglish.app;

import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

public final class KeyVault {
    private static final String ALIAS = "daily-english-api";
    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        return (SecretKey) store.getKey(ALIAS, null);
    }
    public static void save(Context context, String value) throws Exception {
        if (!value.matches("sk-[A-Za-z0-9_-]{16,200}")) throw new IllegalArgumentException("请输入有效的 DeepSeek API 密钥");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        String encrypted = Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        boolean saved = context.getSharedPreferences("vault", Context.MODE_PRIVATE).edit()
                .putString("cipher", encrypted).putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)).commit();
        if (!saved) throw new IllegalStateException("密钥保存失败，请重试");
    }
    public static String read(Context context) throws Exception {
        var prefs = context.getSharedPreferences("vault", Context.MODE_PRIVATE);
        if (!prefs.contains("cipher")) return "";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(prefs.getString("cipher", ""), Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
    public static void clear(Context context) {
        context.getSharedPreferences("vault", Context.MODE_PRIVATE).edit().clear().commit();
    }
    public static boolean configured(Context context) {
        return context.getSharedPreferences("vault", Context.MODE_PRIVATE).contains("cipher");
    }
}
