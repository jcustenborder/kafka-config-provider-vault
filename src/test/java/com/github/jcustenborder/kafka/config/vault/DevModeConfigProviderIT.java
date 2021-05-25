/**
 * Copyright © 2021 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.config.vault;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.jcustenborder.docker.junit5.Compose;
import com.github.jcustenborder.docker.junit5.Port;
import com.google.common.collect.ImmutableSet;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Compose(dockerComposePath = "src/test/resources/docker/dev-mode.yml", clusterHealthCheck = VaultClusterHealthCheck.class)
public class DevModeConfigProviderIT extends VaultConfigProviderIT {

  @BeforeEach
  public void before(@Port(container = "vault", internalPort = 8200) InetSocketAddress address) throws VaultException {
    Map<String, String> settings = new LinkedHashMap<>();
    final String vaultUrl = String.format("http://%s:%s", address.getHostString(), address.getPort());
    settings.put(VaultConfigProviderConfig.ADDRESS_CONFIG, vaultUrl);
    settings.put(VaultConfigProviderConfig.TOKEN_CONFIG, Constants.TOKEN);
    this.configProvider = new VaultConfigProvider();
    this.configProvider.configure(settings);

    SslConfig config = new SslConfig()
        .verify(false)
        .build();
    VaultConfig vaultConfig = new VaultConfig()
        .address(vaultUrl)
        .token(Constants.TOKEN)
        .sslConfig(config)
        .build();
    this.vault = new Vault(vaultConfig);
  }


  @Test
  public void missing() {
    assertThrows(ConfigException.class, () -> {
      this.configProvider.get("secret/missing");
    });
  }

  @Test
  public void valid() throws VaultException {
    final Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("one", "1");
    expected.put("two", "2");
    expected.put("three", "3");
    expected.put("four", "4");
    expected.put("five", "5");
    final String path = "secret/valid";
    this.vault.logical().write(path, expected);
    ConfigData configData = this.configProvider.get(path);
    assertNotNull(configData);
    assertEquals(expected, configData.data());
  }

  @Test
  public void selected() throws VaultException {
    final Map<String, Object> input = new LinkedHashMap<>();
    input.put("one", "1");
    input.put("two", "2");
    input.put("three", "3");
    input.put("four", "4");
    input.put("five", "5");
    final Set<String> keys = ImmutableSet.of("one", "three", "five");
    final Map<String, Object> expected = input.entrySet()
        .stream()
        .filter(e -> keys.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    final String path = "secret/selected";
    this.vault.logical().write(path, input);
    ConfigData configData = this.configProvider.get(path, keys);
    assertNotNull(configData);
    assertEquals(expected, configData.data());
  }

}
