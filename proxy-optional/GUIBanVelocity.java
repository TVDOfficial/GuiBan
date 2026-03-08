package com.enterprise.guiban.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(id = "guiban", name = "GUIBan", version = "2.0", authors = {"Mathew Pittard"})
public class GUIBanVelocity {
    private final Logger logger;

    @Inject
    public GUIBanVelocity(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GUIBan Proxy (Velocity) Enabled!");
    }
}
