package org.datahive.security;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verifies Google Identity Services ID tokens against Google's published RSA signing keys. */
public final class GoogleIdTokenVerifier {
    private static final String CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final Map<String, java.security.PublicKey> KEY_CACHE = new ConcurrentHashMap<>();
    private static volatile Instant cacheExpiresAt = Instant.EPOCH;

    private GoogleIdTokenVerifier() { }

    public static GoogleIdentity verify(String token, String expectedAudience, String expectedNonce) {
        try {
            if (token == null || token.length() > 12_000 || expectedAudience == null || expectedAudience.isBlank()) return null;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String keyId = stringField(header, "kid");
            if (!"RS256".equals(stringField(header, "alg")) || keyId == null) return null;
            java.security.PublicKey key = googleKey(keyId);
            if (key == null) return null;
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(Base64.getUrlDecoder().decode(parts[2]))) return null;

            String issuer = stringField(claims, "iss");
            String audience = stringField(claims, "aud");
            String email = stringField(claims, "email");
            String name = stringField(claims, "name");
            String subject = stringField(claims, "sub");
            String nonce = stringField(claims, "nonce");
            long expiration = longField(claims, "exp");
            if (!("accounts.google.com".equals(issuer) || "https://accounts.google.com".equals(issuer)) ||
                    !expectedAudience.equals(audience) || !"true".equals(stringField(claims, "email_verified")) ||
                    expectedNonce == null || !expectedNonce.equals(nonce) || subject == null || subject.isBlank() ||
                    email == null || !email.matches("(?i)^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$") ||
                    expiration <= Instant.now().getEpochSecond() || expiration > Instant.now().plusSeconds(3600).getEpochSecond()) return null;
            return new GoogleIdentity(name == null ? "" : name, email.trim().toLowerCase(java.util.Locale.ROOT), subject);
        } catch (Exception exception) {
            return null;
        }
    }

    private static java.security.PublicKey googleKey(String keyId) throws Exception {
        if (Instant.now().isAfter(cacheExpiresAt)) refreshKeys(false);
        java.security.PublicKey cached = KEY_CACHE.get(keyId);
        if (cached != null) return cached;
        refreshKeys(true);
        return KEY_CACHE.get(keyId);
    }

    private static synchronized void refreshKeys(boolean force) throws Exception {
        if (!force && Instant.now().isBefore(cacheExpiresAt) && !KEY_CACHE.isEmpty()) return;
        HttpRequest request = HttpRequest.newBuilder(URI.create(CERTS_URL)).timeout(Duration.ofSeconds(8)).GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IllegalStateException("Google signing keys are unavailable");
        Map<String, java.security.PublicKey> refreshed = new ConcurrentHashMap<>();
        Matcher objects = Pattern.compile("\\{([^{}]+)}").matcher(response.body());
        while (objects.find()) {
            String item = objects.group(1);
            String keyId = stringField(item, "kid");
            String modulus = stringField(item, "n");
            String exponent = stringField(item, "e");
            if (keyId == null || modulus == null || exponent == null || !"RSA".equals(stringField(item, "kty"))) continue;
            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
            refreshed.put(keyId, KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e)));
        }
        if (refreshed.isEmpty()) throw new IllegalStateException("Google signing keys could not be read");
        KEY_CACHE.clear();
        KEY_CACHE.putAll(refreshed);
        cacheExpiresAt = Instant.now().plusSeconds(3600);
    }

    private static String stringField(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyStart = json.indexOf(marker);
        if (keyStart < 0) return null;
        int colon = json.indexOf(':', keyStart + marker.length());
        if (colon < 0) return null;
        int cursor = colon + 1;
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) cursor++;
        if (cursor >= json.length()) return null;
        if (json.charAt(cursor) == 't' && json.startsWith("true", cursor)) return "true";
        if (json.charAt(cursor) == 'f' && json.startsWith("false", cursor)) return "false";
        if (json.charAt(cursor) != '"') return null;
        StringBuilder value = new StringBuilder();
        for (int i = cursor + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '"') return value.toString();
            if (current != '\\') { value.append(current); continue; }
            if (++i >= json.length()) return null;
            char escaped = json.charAt(i);
            switch (escaped) {
                case '"', '\\', '/' -> value.append(escaped);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (i + 4 >= json.length()) return null;
                    try { value.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16)); }
                    catch (NumberFormatException exception) { return null; }
                    i += 4;
                }
                default -> { return null; }
            }
        }
        return null;
    }

    private static long longField(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyStart = json.indexOf(marker);
        if (keyStart < 0) return 0;
        int cursor = json.indexOf(':', keyStart + marker.length());
        if (cursor < 0) return 0;
        cursor++;
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) cursor++;
        int start = cursor;
        while (cursor < json.length() && Character.isDigit(json.charAt(cursor))) cursor++;
        if (start == cursor) return 0;
        try { return Long.parseLong(json.substring(start, cursor)); }
        catch (NumberFormatException exception) { return 0; }
    }
    public record GoogleIdentity(String name, String email, String subject) { }
}

