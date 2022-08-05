package com.github.jcustenborder.kafka.config.vault;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.jcustenborder.docker.junit5.Compose;
import com.github.jcustenborder.docker.junit5.Port;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Compose(dockerComposePath = "src/test/resources/docker/dev-mode.yml", clusterHealthCheck = VaultClusterHealthCheck.class)
public class RoleBasedAuthIT {
  private String vaultUrl;
  private String roleId;
  private String secretId;
  private Vault vault;
  private final OkHttpClient client = new OkHttpClient();
  private final Gson gson = new Gson();

  private String vaultPath(String additionalPath) {
    return String.format("%s/%s", vaultUrl, additionalPath);
  }

  private void createRole() throws IOException {
    Request createRole = new Request.Builder()
        .url(vaultPath("v1/auth/approle/role/test-role/role-id"))
        .addHeader("X-Vault-Token", Constants.TOKEN)
        .get().build();

    Request createSecret = new Request.Builder()
        .url(vaultPath("v1/auth/approle/role/test-role/secret-id"))
        .addHeader("X-Vault-Token", Constants.TOKEN)
        .post(new FormBody.Builder().build()).build();

    Response roleResponse = client.newCall(createRole).execute();
    Response secretResponse = client.newCall(createSecret).execute();

    JsonObject parsedRoleResponse = gson.fromJson(
        Objects.requireNonNull(
            roleResponse.body()
        ).string(), JsonObject.class
    );

    JsonObject secretRoleResponse = gson.fromJson(
        Objects.requireNonNull(
            secretResponse.body()
        ).string(), JsonObject.class
    );

    roleId = parsedRoleResponse.getAsJsonObject("data")
        .get("role_id")
        .getAsString();

    secretId = secretRoleResponse.getAsJsonObject("data")
        .get("secret_id")
        .getAsString();
  }

  private void enableRoleAuth() throws IOException {
    Request enableAuth = new Request.Builder()
        .url(vaultPath("v1/sys/auth/approle"))
        .addHeader("X-Vault-Token", Constants.TOKEN)
        .post(
            new FormBody.Builder()
                .add("type", "approle")
                .build()
        ).build();


    client.newCall(enableAuth).execute();

    Request createPolicy = new Request.Builder()
        .url(vaultPath("v1/sys/policy/test-secrets"))
        .addHeader("X-Vault-Token", Constants.TOKEN)
        .post(
            new FormBody.Builder()
                .add("policy", "path \"secret/*\" { capabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"] }")
                .build()
        )
        .build();


    client.newCall(createPolicy).execute();

    Request createAppRole = new Request.Builder().url(vaultPath("v1/auth/approle/role/test-role"))
        .addHeader("X-Vault-Token", Constants.TOKEN)
        .post(
            new FormBody.Builder()
                .add("policies", "test-secrets")
                .build()
        )
        .build();

    client.newCall(createAppRole).execute();
  }

  @BeforeEach
  public void before(@Port(container = "vault", internalPort = 8200) InetSocketAddress address) throws VaultException, IOException {
    vaultUrl = String.format("http://%s:%s", address.getHostString(), address.getPort());
    // enabling role based auth and registering the role
    enableRoleAuth();
    createRole();

    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(VaultConfigProviderConfig.ADDRESS_CONFIG, vaultUrl);
    settings.put(VaultConfigProviderConfig.ROLE_ID_CONFIG, roleId);
    settings.put(VaultConfigProviderConfig.SECRET_ID_CONFIG, secretId);
    settings.put(VaultConfigProviderConfig.LOGIN_BY_CONFIG, "AppRole");
    VaultConfigProvider configProvider = new VaultConfigProvider();
    configProvider.configure(settings);

    SslConfig config = new SslConfig()
        .verify(false)
        .build();
    VaultConfig vaultConfig = new VaultConfig()
        .address(vaultUrl)
        .token(Constants.TOKEN)
        .prefixPath("staging/")
        .sslConfig(config)
        .build();
    this.vault = new Vault(vaultConfig);
  }


  @Test
  public void testWriting() throws VaultException {
    this.vault.auth().loginByAppRole(roleId, secretId);
    this.vault.logical().write("secret/data/my-secret", new HashMap<String, Object>() {
      {
        put("most-foundamental-question-of-the-universe", "the answer is 42");
      }
    });
  }

  @Test
  public void testReading() throws VaultException {
    this.vault.auth().loginByAppRole(roleId, secretId);
    this.vault.logical().write("secret/data/my-secret", new HashMap<String, Object>() {
      {
        put("most-foundamental-question-of-the-universe", "the answer is 42");
      }
    });
  }

}
