package com.example.Smarthome.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

/**
 * Заглушка для обработчика MQTT сообщений, когда MQTT не используется
 */
@Component
@Profile("!mqtt")
public class DummyMqttMessageHandler implements MessageHandler {
    
    private static final Logger log = LoggerFactory.getLogger(DummyMqttMessageHandler.class);
    
    @Override
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) throws MessagingException {
        // Заглушка не обрабатывает сообщения, так как MQTT отключен
        log.debug("MQTT отключен, сообщение игнорируется: {}", message);
    }
} 