/**
 * Copyright © 2021 Jeremy Custenborder (jcustenborder@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.jcustenborder.kafka.connect.utils.config.ConfigKeyBuilder;
import com.github.jcustenborder.kafka.connect.utils.config.ConfigUtils;
import com.github.jcustenborder.kafka.connect.utils.config.Description;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.types.Password;

import java.util.HashMap;
import java.util.Map;

class VaultConfigProviderConfig extends AbstractConfig {
  public static final String MAX_RETRIES_CONFIG = "vault.max.retries";
  static final String MAX_RETRIES_DOC = "The number of times that API operations will be retried when a failure occurs.";
  public static final String MAX_RETRY_INTERVAL_CONFIG = "vault.retry.interval.ms";
  static final String MAX_RETRY_INTERVAL_DOC = "The number of milliseconds that the driver will wait in between retries.";
  public static final String ADDRESS_CONFIG = "vault.address";
  static final String ADDRESS_DOC = "Sets the address (URL) of the Vault server instance to which API calls should be sent. " +
      "If no address is explicitly set, the object will look to the `VAULT_ADDR` If you do not supply it explicitly AND no " +
      "environment variable value is found, then initialization may fail.";

  public static final String PREFIX_CONFIG = "vault.prefix";
  static final String PREFIX_DOC = "Sets a prefix that will be added to all paths. For example you can use `staging` or `production` " +
      "and all of the calls to vault will be prefixed with that path. This allows the same configuration settings to be used across " +
      "multiple environments.";
  public static final String NAMESPACE_CONFIG = "vault.namespace";
  static final String NAMESPACE_DOC = "Sets a global namespace to the Vault server instance, if desired.";
  public static final String TOKEN_CONFIG = "vault.token";
  static final String TOKEN_DOC = "Sets the token used to access Vault. If no token is explicitly set " +
      "then the `VAULT_TOKEN` environment variable will be used. ";

  public static final String ROLE_ID_CONFIG = "vault.role.id";
  static final String ROLE_ID_DOC = "Sets the role id to access Vault. This configuration require you to set also the `vault.secret.id` configuration";

  public static final String SECRET_ID_CONFIG = "vault.secret.id";
  static final String SECRET_ID_DOC = "Sets the secret id to access Vault. This configuration require you to set also the `vault.role.id` configuration";

  public static final String LOGIN_BY_CONFIG = "vault.login.by";
  static final String LOGIN_BY_DOC = "The login method to use. " + ConfigUtils.enumDescription(VaultLoginBy.class);

  public static final String MIN_TTL_MS_CONFIG = "vault.secret.minimum.ttl.ms";
  static final String MIN_TTL_MS_DOC = "The minimum amount of time that a secret should be used. " +
      "If a secret does not have a TTL associated with it, this setting allows you to override how often " +
      "the config provider will check for updated secrets.";

  public static final String SSL_VERIFY_ENABLED_CONFIG = "vault.ssl.verify.enabled";
  static final String SSL_VERIFY_ENABLED_DOC = "Flag to determine if the configProvider should verify the SSL Certificate " +
      "of the Vault server. Outside of development this should never be enabled.";

  public final int maxRetries;
  public final int retryInterval;
  public final boolean sslVerifyEnabled;
  public final VaultLoginBy loginBy;
  public final long minimumSecretTTL;

  public VaultConfigProviderConfig(Map<String, ?> settings) {
    super(config(), settings);
    this.maxRetries = getInt(MAX_RETRIES_CONFIG);
    this.retryInterval = getInt(MAX_RETRY_INTERVAL_CONFIG);
    this.sslVerifyEnabled = getBoolean(SSL_VERIFY_ENABLED_CONFIG);
    this.loginBy = ConfigUtils.getEnum(VaultLoginBy.class, this, LOGIN_BY_CONFIG);
    this.minimumSecretTTL = getLong(MIN_TTL_MS_CONFIG);
  }

  public static ConfigDef config() {
    return new ConfigDef()
        .define(
            ConfigKeyBuilder.of(ADDRESS_CONFIG, ConfigDef.Type.STRING)
                .documentation(ADDRESS_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        )
        .define(
            ConfigKeyBuilder.of(LOGIN_BY_CONFIG, ConfigDef.Type.STRING)
                .documentation(LOGIN_BY_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue(VaultLoginBy.Token.name())
                .build()
        )
        .define(
            ConfigKeyBuilder.of(TOKEN_CONFIG, ConfigDef.Type.PASSWORD)
                .documentation(TOKEN_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        )
        .define(
            ConfigKeyBuilder.of(ROLE_ID_CONFIG, ConfigDef.Type.STRING)
                .documentation(ROLE_ID_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        )
        .define(
            ConfigKeyBuilder.of(SECRET_ID_CONFIG, ConfigDef.Type.PASSWORD)
                .documentation(SECRET_ID_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        )
        .define(
            ConfigKeyBuilder.of(NAMESPACE_CONFIG, ConfigDef.Type.STRING)
                .documentation(NAMESPACE_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue("")
                .build()
        )
        .define(
            ConfigKeyBuilder.of(PREFIX_CONFIG, ConfigDef.Type.STRING)
                .documentation(PREFIX_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue("")
                .build()
        ).define(
            ConfigKeyBuilder.of(MAX_RETRIES_CONFIG, ConfigDef.Type.INT)
                .documentation(MAX_RETRIES_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue(5)
                .build()
        )
        .define(
            ConfigKeyBuilder.of(MAX_RETRY_INTERVAL_CONFIG, ConfigDef.Type.INT)
                .documentation(MAX_RETRY_INTERVAL_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue(2000)
                .build()
        ).define(
            ConfigKeyBuilder.of(SSL_VERIFY_ENABLED_CONFIG, ConfigDef.Type.BOOLEAN)
                .documentation(SSL_VERIFY_ENABLED_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue(true)
                .build()
        ).define(
            ConfigKeyBuilder.of(MIN_TTL_MS_CONFIG, ConfigDef.Type.LONG)
                .documentation(MIN_TTL_MS_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue(1000L)
                .validator(ConfigDef.Range.atLeast(1000L))
                .build()
        );
  }

  private HashMap<String, Function<VaultConfig, VaultConfig>> loginTranformer = new HashMap<>();

  /**
   * Method is used to create a VaultConfig
   *
   * @return
   */
  public VaultConfig createConfig() {
    return createConfig(null);
  }

  /**
   * Method is used to create a VaultConfig.
   *
   * @param environmentLoader Used for configuration testing. Null most of the time
   * @return
   */
  VaultConfig createConfig(EnvironmentLoader environmentLoader) {
    SslConfig sslConfig = new SslConfig()
        .verify(this.sslVerifyEnabled);

    VaultConfig result = new VaultConfig();
    if (null != environmentLoader) {
      result = result.environmentLoader(environmentLoader);
    }

    try {
      result = result.sslConfig(sslConfig.build());
    } catch (VaultException e) {
      ConfigException configException = new ConfigException("Exception thrown while configuring ssl");
      configException.initCause(e);
      throw configException;
    }

    String address = getString(ADDRESS_CONFIG);
    if (!Strings.isNullOrEmpty(address)) {
      result = result.address(address);
    }
    Password token = getPassword(TOKEN_CONFIG);
    if (!Strings.isNullOrEmpty(token.value())) {
      result = result.token(token.value());
    }
    String prefix = getString(PREFIX_CONFIG);
    if (!Strings.isNullOrEmpty(prefix)) {
      result = result.prefixPath(prefix);
    }
    String namespace = getString(NAMESPACE_CONFIG);
    if (!Strings.isNullOrEmpty(namespace)) {
      try {
        result = result.nameSpace(namespace);
      } catch (VaultException e) {
        ConfigException configException = new ConfigException(NAMESPACE_CONFIG, namespace, "Exception thrown setting namespace");
        configException.initCause(e);
        throw configException;
      }
    }

    try {
      result = result.build();
    } catch (VaultException e) {
      ConfigException configException = new ConfigException("Exception thrown while configuring vault");
      configException.initCause(e);
      throw configException;
    }

    return result;
  }

  public enum VaultLoginBy {
    @Description("Authentication via the `token\n" + "<https://www.vaultproject.io/docs/auth/token>`_. endpoint.")
    Token,
    @Description("Authentication via the `role_id and secret_id \n" + "<https://www.vaultproject.io/docs/auth/role_id>`_. endpoint.")
    AppRole

//    @Description("")
//    UserPass,
//    LDAP,
//    AwsEc2,
//    AwsIam,
//    Github,
//    Jwt,
//    GCP,
//    Kubernetes,
//    ByCert,
  }

}
