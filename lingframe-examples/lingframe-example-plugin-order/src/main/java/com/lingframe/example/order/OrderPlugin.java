package com.lingframe.example.order;

import com.lingframe.api.context.PluginContext;
import com.lingframe.api.plugin.LingPlugin;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderPlugin implements LingPlugin {

    @Override
    public void onStart(PluginContext context) {
        System.out.println("Order plugin started: " + context.getPluginId());
    }

    @Override
    public void onStop(PluginContext context) {
        System.out.println("Order plugin stopped: " + context.getPluginId());
    }
}