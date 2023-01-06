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
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LookupResponse;
import com.github.jcustenborder.kafka.config.vault.VaultConfigProviderConfig.VaultLoginBy;
import com.google.common.base.Strings;
import org.apache.kafka.common.config.types.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.jcustenborder.kafka.config.vault.VaultConfigProviderConfig.ROLE_ID_CONFIG;
import static com.github.jcustenborder.kafka.config.vault.VaultConfigProviderConfig.SECRET_ID_CONFIG;

public class AuthHandlers {
  private static final Logger log = LoggerFactory.getLogger(AuthHandlers.class);

  static class AuthConfig {
    public final boolean isRenewable;
    public final VaultConfig updatedConfig;

    AuthConfig(boolean isRenewable, VaultConfig updatedConfig) {
      this.isRenewable = isRenewable;
      this.updatedConfig = updatedConfig;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", AuthConfig.class.getSimpleName() + "[", "]")
          .add("isRenewable=" + isRenewable)
          .toString();
    }
  }

  interface AuthHandler {
    VaultLoginBy[] supports();

    AuthConfig auth(VaultConfigProviderConfig config, Vault vault) throws VaultException;
  }

  static final Map<VaultLoginBy, AuthHandler> HANDLERS;

  public static AuthHandler getHandler(VaultLoginBy loginBy) {
    AuthHandler result = HANDLERS.get(loginBy);

    if (null == result) {
      throw new UnsupportedOperationException(
          String.format("'%s' does not have an AuthHandler defined", loginBy)
      );
    }

    return result;
  }

  static {
    AuthHandler[] handlers = new AuthHandler[] {
        new TokenAuthHandler(),
        new AppRoleAuthHandler()
    };
    Map<VaultLoginBy, AuthHandler> result = new LinkedHashMap<>();
    for (AuthHandler handler : handlers) {
      for (VaultLoginBy loginBy : handler.supports()) {
        AuthHandler previous = result.put(loginBy, handler);
        if (null != previous) {
          throw new IllegalStateException(
              String.format(
                  "'%s' is defined as supported by '%s' and '%s'. Only one can be supported.",
                  loginBy,
                  handler.getClass().getName(),
                  previous.getClass().getName()
              )
          );
        }
      }
    }
    HANDLERS = result;
  }


  static void dumpDebug(Object input) {
    if (!log.isTraceEnabled()) {
      return;
    }
    Map<String, Object> result = new TreeMap<>();
    Pattern methodPrefix = Pattern.compile("^(get|is)(.+)$");
    for (Method method : input.getClass().getMethods()) {
      if (method.getName().equals("getClass")) {
        continue;
      }
      Matcher matcher = methodPrefix.matcher(method.getName());
      if (matcher.matches()) {
        try {
          Object value = method.invoke(input);
          result.put(matcher.group(2), value);
        } catch (IllegalAccessException | InvocationTargetException e) {

        }
      }
    }
    log.trace("{}:{}", input.getClass().getSimpleName(), result);
  }


  static class TokenAuthHandler implements AuthHandler {
    @Override
    public VaultLoginBy[] supports() {
      return new VaultLoginBy[] {VaultLoginBy.Token};
    }

    @Override
    public AuthConfig auth(VaultConfigProviderConfig config, Vault vault) throws VaultException {
      LookupResponse lookupResponse = vault.auth().lookupSelf();

      dumpDebug(lookupResponse);
      log.info("Authenticated to Vault as {}: path: {}", lookupResponse.getDisplayName(), lookupResponse.getPath());
      return new AuthConfig(
          lookupResponse.isRenewable(),
          config.createConfig()
      );
    }
  }

  static class AppRoleAuthHandler implements AuthHandler {

    @Override
    public VaultLoginBy[] supports() {
      return new VaultLoginBy[] {VaultLoginBy.AppRole};
    }

    @Override
    public AuthConfig auth(VaultConfigProviderConfig config, Vault vault) throws VaultException {

      String roleId = config.getString(ROLE_ID_CONFIG);
      String erroMsg = "";

      if (Strings.isNullOrEmpty(roleId)) {
        erroMsg += String.format("\n   - `%s`", ROLE_ID_CONFIG);
      }

      Password secretId = config.getPassword(SECRET_ID_CONFIG);
      if (Strings.isNullOrEmpty(secretId.value())) {
        erroMsg += String.format("\n   - `%s`", SECRET_ID_CONFIG);
      }

      if (!erroMsg.isEmpty()) {
        String msg = String.format("you have specified `AppRole` as login method but the following fields are missing: [ %s]", erroMsg);
        throw new IllegalArgumentException(msg);
      }

      AuthResponse response = vault.auth().loginByAppRole(roleId, secretId.value());

      dumpDebug(response);
      log.info(
          "Authenticated to Vault using {} appId, the available policies are: [{}]",
          response.getAppId(),
          response.getAuthPolicies()
              .stream()
              .skip(1)
              .reduce(response.getAuthPolicies().get(0), (acc, role) -> acc + "," + role)
      );

      return new AuthConfig(
          response.isAuthRenewable(),
          config.createConfig(new EnvironmentLoader() {
            @Override
            public String loadVariable(String name) {

              if (name.equals("VAULT_TOKEN")) {
                return response.getAuthClientToken();
              } else {
                return super.loadVariable(name);
              }
            }
          })
      );
    }
  }
}
