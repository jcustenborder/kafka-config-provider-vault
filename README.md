# Introduction
[Documentation](https://jcustenborder.github.io/kafka-connect-documentation/projects/kafka-config-provider-vault) | [Download from the Confluent Hub](https://www.confluent.io/hub/jcustenborder/kafka-config-provider-vault)

This plugin provides integration with Hashicorp Vault.

# Installation

## Confluent Hub

The following command can be used to install the plugin directly from the Confluent Hub using the
[Confluent Hub Client](https://docs.confluent.io/current/connect/managing/confluent-hub/client.html).

```bash
confluent-hub install jcustenborder/kafka-config-provider-vault:latest
```

## Manually

The zip file that is deployed to the [Confluent Hub](https://www.confluent.io/hub/jcustenborder/kafka-config-provider-vault) is available under
`target/components/packages/`. You can manually extract this zip file which includes all dependencies. All the dependencies
that are required to deploy the plugin are under `target/kafka-connect-target` as well. Make sure that you include all the dependencies that are required
to run the plugin.

1. Create a directory under the `plugin.path` on your Connect worker.
2. Copy all of the dependencies under the newly created subdirectory.
3. Restart the Connect worker.

# Converters

## [VaultConfigProvider](https://jcustenborder.github.io/kafka-connect-documentation/projects/kafka-config-provider-vault/configProviders/VaultConfigProvider.html)

```
com.github.jcustenborder.kafka.config.vault.VaultConfigProvider
```
This config provider is used to retrieve configuration settings from a Hashicorp vault instance. Config providers are generic and can be used in any application that utilized the Kafka AbstractConfig class. 






# Development

## Building the source

```bash
mvn clean package
```

## Contributions

Contributions are always welcomed! Before you start any development please create an issue and
start a discussion. Create a pull request against your newly created issue and we're happy to see
if we can merge your pull request. First and foremost any time you're adding code to the code base
you need to include test coverage. Make sure that you run `mvn clean package` before submitting your
pull to ensure that all of the tests, checkstyle rules, and the package can be successfully built.