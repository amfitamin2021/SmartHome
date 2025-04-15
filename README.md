# SmartHome - Платформа управления умным домом

Платформа для управления и мониторинга различных устройств умного дома. Объединяет устройства разных производителей с различными протоколами связи (WiFi, ZigBee, MQTT и др.) в единую систему.

## Особенности

- 🏠 Управление локациями и комнатами
- 💡 Поддержка различных типов устройств (освещение, датчики, термостаты и др.)
- 🔌 Поддержка множества протоколов связи (MQTT, WiFi, ZigBee и др.)
- 📊 Интеграция с ThingsBoard для мониторинга и визуализации
- 🧪 Поддержка виртуальных устройств для тестирования
- 🔄 Автоматическое обнаружение устройств
- 🔒 Безопасное API для интеграции с другими системами

## Технологии

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- Spring Security
- PostgreSQL
- MQTT
- RESTful API

## Начало работы

### Предварительные требования

- JDK 17+
- PostgreSQL 12+
- Maven или Gradle
- MQTT Broker (опционально, например, Mosquitto)
- ThingsBoard (опционально)

### Установка и запуск

1. Клонируйте репозиторий
   ```bash
   git clone https://github.com/yourusername/smarthome.git
   cd smarthome
   ```

2. Настройте базу данных PostgreSQL
   ```sql
   CREATE DATABASE smarthome;
   CREATE USER smarthome WITH PASSWORD 'password';
   GRANT ALL PRIVILEGES ON DATABASE smarthome TO smarthome;
   ```

3. Настройте application.properties в соответствии с вашими настройками
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/smarthome
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   ```

4. Запустите приложение
   ```bash
   ./gradlew bootRun
   ```
   или
   ```bash
   ./mvnw spring-boot:run
   ```

5. Приложение будет доступно по адресу: `http://localhost:8080`

## API

### Устройства

- `GET /api/devices` - Получение списка всех устройств
- `GET /api/devices/{id}` - Получение устройства по ID
- `POST /api/devices` - Регистрация нового устройства
- `PUT /api/devices/{id}` - Обновление информации об устройстве
- `DELETE /api/devices/{id}` - Удаление устройства
- `POST /api/devices/{id}/command` - Отправка команды на устройство

### Локации и комнаты

- `GET /api/locations` - Получение списка всех локаций
- `GET /api/locations/{id}` - Получение локации по ID
- `POST /api/locations` - Создание новой локации
- `PUT /api/locations/{id}` - Обновление информации о локации
- `DELETE /api/locations/{id}` - Удаление локации
- `GET /api/locations/{id}/rooms` - Получение списка комнат в локации
- `POST /api/locations/{id}/rooms` - Добавление комнаты в локацию
- `PUT /api/locations/{id}/rooms/{roomId}` - Обновление информации о комнате
- `DELETE /api/locations/{id}/rooms/{roomId}` - Удаление комнаты

## Виртуальные устройства

Проект включает систему эмуляции устройств для тестирования без реальных физических устройств. При запуске приложения автоматически создаются тестовые виртуальные устройства различных типов.

### Типы виртуальных устройств:

- **Освещение** - управление светом, яркостью, цветом
- **Термостат** - контроль температуры
- **Датчики** - датчики движения, температуры, влажности
- **Умные розетки** - управление питанием и отслеживание энергопотребления

## Интеграция с ThingsBoard

Для мониторинга и визуализации данных с устройств используется интеграция с ThingsBoard. Система автоматически синхронизирует устройства и их телеметрию с ThingsBoard.

### Настройка ThingsBoard

1. Установите ThingsBoard согласно [официальной документации](https://thingsboard.io/docs/installation/)
2. Создайте учетную запись tenant
3. Настройте параметры подключения в `application.properties`

## Дополнительная информация

- Для добавления новых протоколов необходимо реализовать интерфейс `ProtocolAdapter`
- Для логирования используется SLF4J
- Для безопасности API рекомендуется настроить Spring Security (в демо-режиме отключено)

## Лицензия

MIT 