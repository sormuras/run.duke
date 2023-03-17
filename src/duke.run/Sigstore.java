import static java.lang.System.out;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Single-file Source-code Sigstore-support.
 *
 * <p>Sign: {@code java -Dsign Sigstore.java a.jar b.jar ...}
 *
 * <p>Verify: {@code java -Dverify -Dtrust-email=jim@abc.org,... Sigstore.java a.jar b.jar ...}
 */
public class Sigstore {
  public static void main(String... args) throws Exception {
    if (Boolean.getBoolean("sign")) sign(args);
    if (Boolean.getBoolean("verify")) verify(args);
  }

  public static void sign(String... args) throws Exception {
    out.println("Fetching certificates...");
    var fetcher = new CertFetcher();
    fetcher.fetch();

    var certificate = fetcher.certificates.get(0);
    var pem = buildPem(certificate);

    for (var arg : args) {
      var jar = Path.of(arg);
      out.println("Hashing " + jar);
      var sha256 = buildSha256(jar);
      var hex256 = HexFormat.of().formatHex(sha256);
      out.println("-> " + hex256);

      out.println("Signing " + jar);
      var data = Files.readAllBytes(jar);
      var signature = Signature.getInstance("SHA256withECDSA");
      signature.initSign(fetcher.privateKey);
      signature.update(data);
      var signed = signature.sign();
      out.println("-> " + Base64.getEncoder().encodeToString(signed));

      if (Boolean.getBoolean("verify")) {
        out.println("Verifying with " + pem);
        signature.initVerify(certificate);
        signature.update(data);
        var verified = signature.verify(signed);
        out.println("-> " + verified);
        if (!verified) throw new AssertionError();
      }

      var location = uploadHashedRekord(sha256, signed, pem.getBytes(StandardCharsets.UTF_8));
      out.println("Created entry : " + REKOR + location);
      out.println("Rekor Search -> https://rekor.tlog.dev/?hash=" + hex256);
    }
  }

  public static void verify(String... args) throws Exception {
    var pattern = Pattern.compile("\"(.+?)\"");
    var trusted = new TreeSet<String>();
    var emails = List.of(System.getProperty("trust-email", "").split(","));
    out.println("Trusted emails: " + emails);
    for (var email : emails) {
      var uuids = searchRekor("""
        {"email": "%s"}
        """.formatted(email));
      pattern
          .matcher(uuids)
          .results()
          .map(MatchResult::group)
          .map(string -> string.substring(1, string.length() - 1))
          .forEach(trusted::add);
    }
    out.println("Found " + trusted.size() + " trusted UUID(s)");
    for (var arg : args) {
      var jar = Path.of(arg);
      var sha256 = HexFormat.of().formatHex(buildSha256(jar));
      out.println(jar + " -> " + sha256);
      var hashed = new TreeSet<String>();
      var uuids =
          searchRekor(
              """
              {"hash": "sha256:%s"}
              """.formatted(sha256));
      pattern
          .matcher(uuids)
          .results()
          .map(MatchResult::group)
          .map(string -> string.substring(1, string.length() - 1))
          .forEach(hashed::add);
      for (var hash : hashed) {
        out.println(hash + " -> " + trusted.contains(hash));
      }
    }
  }

  private static class CertFetcher {
    PrivateKey privateKey;
    PublicKey publicKey;
    List<Certificate> certificates;

    void fetch() throws Exception {
      var requestUrl = System.getenv("ACTIONS_ID_TOKEN_REQUEST_URL");
      var requestToken = System.getenv("ACTIONS_ID_TOKEN_REQUEST_TOKEN");
      var extractSub = requestUrl != null && requestToken != null;

      var token = getToken();
      var claim = whoami(extractSub ? "sub" : "email", token);

      var keys = generateKeyPair("EC", "secp256r1");
      privateKey = keys.getPrivate();
      publicKey = keys.getPublic();
      var signedClaim = signClaim(claim);

      certificates = getCertificates(signedClaim, token);
    }

    String getToken() throws Exception {
      // headless (on GitHub)?
      var requestUrl = System.getenv("ACTIONS_ID_TOKEN_REQUEST_URL");
      var requestToken = System.getenv("ACTIONS_ID_TOKEN_REQUEST_TOKEN");
      if (requestUrl != null && requestToken != null) {
        return getToken(requestUrl, requestToken);
      }
      // do the browser/server login dance
      HttpServer server = null;
      try {
        var future = new CompletableFuture<String>();
        server = startServer(future);
        var verifier = generateVerifier();
        authenticate(server.getAddress(), verifier);
        // how long shall we wait?
        var code = future.get();
        return getToken(server.getAddress(), verifier, code);
      } finally {
        if (server != null) server.stop(0);
      }
    }

    KeyPair generateKeyPair(String signingAlgorithm, String signingAlgorithmSpec) throws Exception {
      var kpg = KeyPairGenerator.getInstance(signingAlgorithm);
      if (signingAlgorithmSpec != null) {
        var aps = new ECGenParameterSpec(signingAlgorithmSpec);
        kpg.initialize(aps, new SecureRandom());
      }
      return kpg.generateKeyPair();
    }

    String whoami(String what, String token) {
      var parts = token.split("\\.");
      var payload = new String(Base64.getUrlDecoder().decode(parts[1]));
      return getValue(payload, what);
    }

    HttpServer startServer(CompletableFuture<String> cf) throws Exception {
      var handler = new MyHttpHandler(cf);
      var server = HttpServer.create(new InetSocketAddress(0), 0);
      server.createContext("/", handler);
      server.start();
      return server;
    }

    static class MyHttpHandler implements HttpHandler {
      private final CompletableFuture<String> future;

      public MyHttpHandler(CompletableFuture<String> future) {
        this.future = future;
      }

      public void handle(HttpExchange exchange) throws IOException {
        var uri = exchange.getRequestURI().toString();
        int from = uri.indexOf("code=");
        int to = uri.indexOf("&", from);
        future.complete(uri.substring(from + 5, to));
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().write("OK".getBytes());
        exchange.close();
      }
    }

    String generateVerifier() {
      var random = new SecureRandom();
      var secret = new byte[32];
      random.nextBytes(secret);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    void authenticate(InetSocketAddress callbackAddress, String verifier) throws Exception {
      var bytes = verifier.getBytes();
      var md = MessageDigest.getInstance("SHA-256");
      md.update(bytes, 0, bytes.length);
      var digest = md.digest();
      var challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
      var challengeMethod = "S256";
      var uri =
          new URI(
              "https://oauth2.sigstore.dev/auth/auth?client_id=sigstore&code_challenge="
                  + challenge
                  + "&code_challenge_method="
                  + challengeMethod
                  + "&redirect_uri="
                  + "http://localhost:"
                  + callbackAddress.getPort()
                  + "&response_type=code&scope=openid%20email");

      out.println(uri);
      if (Desktop.isDesktopSupported()) {
        var desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
          desktop.browse(uri);
        }
      }
    }

    String getToken(String url, String bearer) throws Exception {
      var builder = HttpClient.newBuilder();
      var hc = builder.build();
      var request = HttpRequest.newBuilder(URI.create(url + "&audience=sigstore"));
      request.header("Authorization", "bearer " + bearer).method("GET", BodyPublishers.noBody());
      var response = hc.send(request.build(), BodyHandlers.ofString());
      return getValue(response.body(), "value");
    }

    String getToken(InetSocketAddress callbackAddress, String verifier, String code)
        throws Exception {
      var auth = new URI("https://oauth2.sigstore.dev/auth/token");
      var request = HttpRequest.newBuilder().uri(auth);
      String payload =
          "code="
              + code
              + "&grant_type=authorization_code"
              + "&redirect_uri="
              + URLEncoder.encode(
                  "http://localhost:" + callbackAddress.getPort(), StandardCharsets.UTF_8)
              + "&scope=openid%20email&client_id=sigstore"
              + "&code_verifier="
              + verifier;
      request
          .header("Accept", "application/json")
          .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
          .method("POST", BodyPublishers.ofString(payload));
      var response = HTTP.send(request.build(), BodyHandlers.ofString());
      var body = response.body();
      return getValue(body, "id_token");
    }

    public byte[] signClaim(String proof) throws Exception {
      var signature = Signature.getInstance("SHA256withECDSA");
      signature.initSign(privateKey);
      signature.update(proof.getBytes(StandardCharsets.UTF_8));
      return signature.sign();
    }

    public List<Certificate> getCertificates(byte[] claimValue, String token) throws Exception {
      var request = HttpRequest.newBuilder().uri(new URI(FULCIO + "/api/v1/signingCert"));
      var json =
          """
          {
            "signedEmailAddress": "%s",
            "publicKey": {
              "algorithm": "ecdsa",
              "content": "%s"
             }
          }
          """
              .formatted(
                  Base64.getEncoder().encodeToString(claimValue),
                  Base64.getEncoder().encodeToString(publicKey.getEncoded()));
      out.println(json);
      request
          .header("Accept", "application/pem-certificate-chain")
          .header("Authorization", "Bearer " + token)
          .header("Content-type", "application/json")
          .method("POST", BodyPublishers.ofString(json));

      var response = HTTP.send(request.build(), BodyHandlers.ofString());
      if (response.statusCode() != 201) {
        throw new IOException("Fulcio @ '%s' : %s".formatted(FULCIO, response.statusCode()));
      }
      var factory = CertificateFactory.getInstance("X.509");
      var stream = new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8));
      return List.copyOf(factory.generateCertificates(stream));
    }

    // Here be a dragon or a JSON parser
    static String getValue(String body, String key) {
      var header = "\"" + key + "\":\"";
      int from = body.indexOf(header);
      int to = body.indexOf("\"", from + header.length());
      return body.substring(from + header.length(), to);
    }
  }

  private static String buildPem(Certificate... certificates) throws Exception {
    var pem = new StringJoiner("");
    var encoder = Base64.getMimeEncoder(64, new byte[] {'\n'});
    for (var certificate : certificates) {
      pem.add(
          """
          -----BEGIN CERTIFICATE-----
          %s
          -----END CERTIFICATE-----
          """
              .formatted(encoder.encodeToString(certificate.getEncoded())));
    }
    return pem.toString();
  }

  private static byte[] buildSha256(Path path) throws Exception {
    if (Files.notExists(path)) throw new RuntimeException("File not found: " + path);
    var md = MessageDigest.getInstance("SHA-256");
    try (var source = new BufferedInputStream(new FileInputStream(path.toFile()));
        var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
      source.transferTo(target);
    }
    return md.digest();
  }

  private static String uploadHashedRekord(byte[] sha256, byte[] signature, byte[] certificate)
      throws Exception {
    var base64 = Base64.getEncoder();
    // https://github.com/sigstore/rekor/tree/main/pkg/types/hashedrekord/v0.0.1
    var payload =
        """
        {
          "apiVersion": "0.0.1",
          "kind": "hashedrekord",
          "spec": {
            "data": {
              "hash": {
                "algorithm": "sha256",
                "value": "%s"
              }
            },
            "signature": {
              "content": "%s",
              "publicKey": {
                "content": "%s"
              }
            }
          }
        }
        """
            .formatted(
                HexFormat.of().formatHex(sha256),
                base64.encodeToString(signature),
                base64.encodeToString(certificate));

    var request = HttpRequest.newBuilder().uri(new URI(REKOR + "/api/v1/log/entries"));
    request
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .method("POST", BodyPublishers.ofString(payload));
    var response = HTTP.send(request.build(), BodyHandlers.ofString());
    if (response.statusCode() != 201) {
      throw new IOException("bad response from rekor: " + response + "\n" + response.body());
    }
    return response.headers().firstValue("location").orElseThrow();
  }

  private static String searchRekor(String query) throws Exception {
    var request = HttpRequest.newBuilder().uri(new URI(REKOR + "/api/v1/index/retrieve"));
    request
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .method("POST", BodyPublishers.ofString(query));
    var response = HTTP.send(request.build(), BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("bad response from rekor: " + response + "\n" + response.body());
    }
    return response.body();
  }

  static final HttpClient HTTP = HttpClient.newHttpClient();

  static final String FULCIO = "https://fulcio.sigstore.dev";

  static final String REKOR = "https://rekor.sigstore.dev";
}
