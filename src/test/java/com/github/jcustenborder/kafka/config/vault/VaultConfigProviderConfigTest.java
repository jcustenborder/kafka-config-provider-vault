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

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.VaultConfig;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VaultConfigProviderConfigTest {
  private static final Logger log = LoggerFactory.getLogger(VaultConfigProviderConfigTest.class);
  @Test
  public void addressNotSet() {
    Map<String, String> settings = new LinkedHashMap<>();
    EnvironmentLoader environmentLoader = MockEnvironment.of();
    VaultConfigProviderConfig config = new VaultConfigProviderConfig(settings);
    assertThrows(ConfigException.class, ()->{
      VaultConfig vaultConfig = config.createConfig(environmentLoader);
    });


  }

  @Test
  public void addressSet() {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(VaultConfigProviderConfig.ADDRESS_CONFIG, "https://vault.example.com");
    VaultConfigProviderConfig config = new VaultConfigProviderConfig(settings);
    VaultConfig vaultConfig = config.createConfig();
    assertNotNull(vaultConfig);
    assertEquals("https://vault.example.com", vaultConfig.getAddress());
  }

  @Test
  public void addressEnvironmentVariable() {
    Map<String, String> settings = new LinkedHashMap<>();
    VaultConfigProviderConfig config = new VaultConfigProviderConfig(settings);
    EnvironmentLoader environmentLoader = MockEnvironment.of("VAULT_ADDR", "https://vault.example.com");
    VaultConfig vaultConfig = config.createConfig(environmentLoader);
    assertNotNull(vaultConfig);
    assertEquals("https://vault.example.com", vaultConfig.getAddress());
  }

  @Test
  public void tokenSet() {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(VaultConfigProviderConfig.ADDRESS_CONFIG, "https://vault.example.com");
    settings.put(VaultConfigProviderConfig.TOKEN_CONFIG, Constants.TOKEN);
    VaultConfigProviderConfig config = new VaultConfigProviderConfig(settings);
    VaultConfig vaultConfig = config.createConfig();
    assertNotNull(vaultConfig);
    assertEquals(Constants.TOKEN, vaultConfig.getToken());
  }
  @Test
  public void tokenNotSet() {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(VaultConfigProviderConfig.ADDRESS_CONFIG, "https://vault.example.com");
    VaultConfigProviderConfig config = new VaultConfigProviderConfig(settings);
    EnvironmentLoader environmentLoader = MockEnvironment.of();
    VaultConfig vaultConfig = config.createConfig(environmentLoader);
    assertNotNull(vaultConfig);
  }

  @Test
  public void tokenEnvironmentVariable() {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(VaultConfigProviderConfig.ADDRESS_CONFIG, "https://vault.example.com");
    VaultConfigProviderConfig config = new VaultConfigProviderConfig(settings);
    EnvironmentLoader environmentLoader = MockEnvironment.of("VAULT_TOKEN", Constants.TOKEN);
    VaultConfig vaultConfig = config.createConfig(environmentLoader);
    assertNotNull(vaultConfig);
    assertEquals(Constants.TOKEN, vaultConfig.getToken());
  }

  static class MockEnvironment extends EnvironmentLoader {
    private final Map<String, String> values;


    MockEnvironment(Map<String, String> values) {
      this.values = values;
    }

    @Override
    public String loadVariable(String name) {
      return this.values.get(name);
    }

    public static MockEnvironment of() {
      Map<String, String> result = new LinkedHashMap<>();
      return new MockEnvironment(result);
    }

    public static MockEnvironment of(String key, String value) {
      Map<String, String> result = new LinkedHashMap<>();
      result.put(key, value);
      return new MockEnvironment(result);
    }

    public static MockEnvironment of(String key0, String value0, String key1, String value1) {
      Map<String, String> result = new LinkedHashMap<>();
      result.put(key0, value0);
      result.put(key1, value1);
      return new MockEnvironment(result);
    }
  }
}
