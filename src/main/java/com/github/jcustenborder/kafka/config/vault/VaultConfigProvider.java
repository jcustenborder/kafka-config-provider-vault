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

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;
import com.github.jcustenborder.kafka.connect.utils.config.Description;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.provider.ConfigProvider;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Description("This config provider is used to retrieve configuration settings from a Hashicorp vault instance. " +
    "Config providers are generic and can be used in any application that utilized the Kafka AbstractConfig class. ")
public class VaultConfigProvider implements ConfigProvider {
  private static final Logger log = LoggerFactory.getLogger(VaultConfigProvider.class);
  VaultConfigProviderConfig config;
  Vault vault;


  @Override
  public ConfigData get(String path) {
    return get(path, Collections.emptySet());
  }

  @Override
  public ConfigData get(String path, Set<String> keys) {
    log.info("get() - path = '{}' keys = '{}'", path, keys);
    try {
      LogicalResponse logicalResponse = this.vault.withRetries(this.config.maxRetries, this.config.retryInterval)
          .logical()
          .read(path);
      RestResponse restResponse = logicalResponse.getRestResponse();
      if (restResponse.getStatus() == 200) {
        Predicate<Map.Entry<String, String>> filter = keys == null || keys.isEmpty() ?
            entry -> true : entry -> keys.contains(entry.getKey());
        Map<String, String> result = logicalResponse.getData()
            .entrySet()
            .stream()
            .filter(filter)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Long ttl = logicalResponse.getLeaseDuration();
        if (ttl == null || ttl <= 0) {
          ttl = config.minimumSecretTTL;
        }
        return new ConfigData(result, ttl);
      } else {
        throw new ConfigException(
            String.format(
                "Vault path '%s' was not found. Rest response details: { code: [%s], mimeType: [%s], response: [%s] }",
                path,
                restResponse.getStatus(),
                restResponse.getMimeType(),
                new String(restResponse.getBody())
            )
        );
      }
    } catch (VaultException e) {
      ConfigException configException = new ConfigException(
          String.format("Exception thrown reading from '%s'", path)
      );
      configException.initCause(e);
      throw configException;
    }
  }


  @Override
  public void close() throws IOException {

  }

  @Override
  public void configure(Map<String, ?> settings) {
    this.config = new VaultConfigProviderConfig(settings);

    VaultConfig config = this.config.createConfig();
    this.vault = new Vault(config);

    AuthHandlers.AuthHandler authHandler = AuthHandlers.getHandler(this.config.loginBy);
    AuthHandlers.AuthConfig authConfig;
    try {
      authConfig = authHandler.auth(this.config, this.vault);
    } catch (VaultException ex) {
      throw new ConnectException(
          "Exception while authenticating to Vault",
          ex
      );
    }
    log.trace("authConfig = {}", authConfig);
  }

  public static ConfigDef config() {
    return VaultConfigProviderConfig.config();
  }
}
