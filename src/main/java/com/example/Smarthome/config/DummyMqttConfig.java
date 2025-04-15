package com.example.Smarthome.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/**
 * Заглушка для конфигурации MQTT, когда MQTT не используется
 */
@Configuration
@Profile("!mqtt")
public class DummyMqttConfig {
    
    /**
     * Создает пустой канал сообщений для совместимости
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }
} 