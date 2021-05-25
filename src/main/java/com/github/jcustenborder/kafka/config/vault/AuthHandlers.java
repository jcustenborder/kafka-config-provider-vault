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

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LookupResponse;
import com.github.jcustenborder.kafka.config.vault.VaultConfigProviderConfig.VaultLoginBy;
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

public class AuthHandlers {
  private static final Logger log = LoggerFactory.getLogger(AuthHandlers.class);

  static class AuthConfig {
    public final boolean isRenewable;

    AuthConfig(boolean isRenewable) {
      this.isRenewable = isRenewable;
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
    AuthHandler[] handlers = new AuthHandler[]{
        new TokenAuthHandler()
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
      return new VaultLoginBy[]{VaultLoginBy.Token};
    }

    @Override
    public AuthConfig auth(VaultConfigProviderConfig config, Vault vault) throws VaultException {
      LookupResponse lookupResponse = vault.auth().lookupSelf();

      dumpDebug(lookupResponse);
      log.info("Authenticated to Vault as {}: path: {}", lookupResponse.getDisplayName(), lookupResponse.getPath());
      return new AuthConfig(
          lookupResponse.isRenewable()
      );
    }
  }

//  static class AppRoleAuthHandler implements AuthHandler {
//
//    @Override
//    public VaultLoginBy[] supports() {
//      return new VaultLoginBy[]{VaultLoginBy.AppRole};
//    }
//
//    @Override
//    public AuthConfig auth(VaultConfigProviderConfig config, Vault vault) throws VaultException {
//

//
//      return null;
//    }
//  }
}
