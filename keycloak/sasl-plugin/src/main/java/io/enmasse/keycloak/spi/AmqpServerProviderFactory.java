package io.enmasse.keycloak.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

public interface AmqpServerProviderFactory extends Provider, ProviderFactory<AmqpServerProviderFactory> {
}
