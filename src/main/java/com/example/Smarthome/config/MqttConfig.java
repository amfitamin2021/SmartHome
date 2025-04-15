package com.example.Smarthome.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
@IntegrationComponentScan
@Profile("mqtt")
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;
    
    @Value("${mqtt.topics.state}")
    private String stateTopic;
    
    @Value("${mqtt.topics.discovery}")
    private String discoveryTopic;
    
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { brokerUrl });
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        // Устанавливаем таймаут соединения в 5 секунд
        options.setConnectionTimeout(5);
        factory.setConnectionOptions(options);
        return factory;
    }
    
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }
    
    @Bean
    public MessageProducer inboundMqttAdapter() {
        // Подписываемся на топики состояния и обнаружения устройств
        MqttPahoMessageDrivenChannelAdapter adapter = 
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-inbound", mqttClientFactory(), 
                        stateTopic, discoveryTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }
    
    @Bean
    public MqttClient mqttClient(MqttPahoClientFactory mqttClientFactory) throws MqttException {
        MqttClient client = new MqttClient(
                brokerUrl, 
                clientId + "-outbound",
                null);
        try {
            client.connect(mqttClientFactory.getConnectionOptions());
        } catch (MqttException e) {
            // Логируем ошибку, но не бросаем исключение
            System.err.println("Не удалось подключиться к MQTT-брокеру: " + e.getMessage());
        }
        return client;
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
} 