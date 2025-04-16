import axios from 'axios'

// Создаем экземпляр axios с базовыми настройками
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8086/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Интерцептор для запросов - добавляет авторизационный токен
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Интерцептор для ответов - обрабатывает ошибки
api.interceptors.response.use(
  response => response,
  error => {
    // Обработка 401 ошибки - перенаправление на страницу входа
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('authToken')
      window.location.href = '/login'
    }
    
    return Promise.reject(error)
  }
)

// API-методы для устройств
const devicesApi = {
  /**
   * Получить список всех устройств
   * @returns {Promise<Array>} - массив устройств
   */
  getDevices() {
    return api.get('/devices')
      .then(response => response.data)
  },

  /**
   * Получить устройство по ID
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<Object>} - данные устройства
   */
  getDevice(id) {
    return api.get(`/devices/${id}`)
      .then(response => response.data)
  },

  /**
   * Создать новое устройство
   * @param {Object} deviceData - данные устройства
   * @returns {Promise<Object>} - созданное устройство
   */
  createDevice(deviceData) {
    // Проверка обязательных полей перед отправкой на сервер
    if (!deviceData.name) {
      return Promise.reject(new Error('Имя устройства не может быть пустым'));
    }
    
    if (!deviceData.type) {
      return Promise.reject(new Error('Тип устройства должен быть указан'));
    }
    
    // Сохраняем уникальный ID для проверки дубликатов
    if (!deviceData.properties) {
      deviceData.properties = {};
    }
    
    if (!deviceData.properties.device_unique_id) {
      deviceData.properties.device_unique_id = 'device_' + Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
    }
    
    // Отправляем запрос на сервер
    return api.post('/devices', deviceData)
      .then(response => response.data)
      .catch(error => {
        // Специальная обработка ошибок при создании устройства
        if (error.response && error.response.status === 409) {
          throw new Error('Устройство с таким именем уже существует');
        }
        throw error;
      });
  },

  /**
   * Отправить команду устройству
   * @param {string|number} id - идентификатор устройства
   * @param {Object} commandData - данные команды
   * @returns {Promise<Object>} - результат выполнения команды
   */
  sendCommand(id, commandData) {
    return api.post(`/devices/${id}/command`, commandData)
      .then(response => response.data)
  },

  /**
   * Проверить состояние устройства
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<Object>} - состояние устройства
   */
  checkDeviceStatus(id) {
    return api.get(`/devices/${id}/status`)
      .then(response => response.data)
  },

  /**
   * Обновить устройство
   * @param {string|number} id - идентификатор устройства
   * @param {Object} deviceData - данные устройства
   * @returns {Promise<Object>} - обновленное устройство
   */
  updateDevice(id, deviceData) {
    return api.put(`/devices/${id}`, deviceData)
      .then(response => response.data)
  },

  /**
   * Удалить устройство
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<void>}
   */
  deleteDevice(id) {
    return api.delete(`/devices/${id}`)
      .then(response => response.data)
  },

  /**
   * Получить список доступных устройств из ThingsBoard
   * @returns {Promise<Array>} - массив доступных устройств
   */
  getAvailableDevices() {
    return api.get('/devices/available-devices')
      .then(response => response.data)
  },
  
  /**
   * Синхронизировать устройство с ThingsBoard
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<Object>} - результат синхронизации
   */
  syncThingsBoard(id) {
    return api.post(`/devices/${id}/sync-thingsboard`)
      .then(response => response.data)
  }
}

// API-методы для сценариев
const scenariosApi = {
  /**
   * Получить список всех сценариев
   * @returns {Promise<Array>} - массив сценариев
   */
  getScenarios() {
    return api.get('/scenarios')
      .then(response => response.data)
  },

  /**
   * Получить сценарий по ID
   * @param {string|number} id - идентификатор сценария
   * @returns {Promise<Object>} - данные сценария
   */
  getScenario(id) {
    return api.get(`/scenarios/${id}`)
      .then(response => response.data)
  },

  /**
   * Создать новый сценарий
   * @param {Object} scenarioData - данные сценария
   * @returns {Promise<Object>} - созданный сценарий
   */
  createScenario(scenarioData) {
    return api.post('/scenarios', scenarioData)
      .then(response => response.data)
  },

  /**
   * Обновить сценарий
   * @param {string|number} id - идентификатор сценария
   * @param {Object} scenarioData - данные сценария
   * @returns {Promise<Object>} - обновленный сценарий
   */
  updateScenario(id, scenarioData) {
    return api.put(`/scenarios/${id}`, scenarioData)
      .then(response => response.data)
  },

  /**
   * Удалить сценарий
   * @param {string|number} id - идентификатор сценария
   * @returns {Promise<void>}
   */
  deleteScenario(id) {
    return api.delete(`/scenarios/${id}`)
      .then(response => response.data)
  },

  /**
   * Запустить сценарий
   * @param {string|number} id - идентификатор сценария
   * @returns {Promise<Object>} - результат выполнения
   */
  runScenario(id) {
    return api.post(`/scenarios/${id}/run`)
      .then(response => response.data)
  }
}

// API-методы для уведомлений
const notificationsApi = {
  /**
   * Получить список уведомлений
   * @param {string} status - статус уведомлений (all, read, unread)
   * @returns {Promise<Array>} - массив уведомлений
   */
  getNotifications(status = 'all') {
    return api.get(`/notifications?status=${status}`)
      .then(response => response.data)
  },

  /**
   * Пометить уведомление как прочитанное
   * @param {string|number} id - идентификатор уведомления
   * @returns {Promise<Object>} - обновленное уведомление
   */
  markAsRead(id) {
    return api.put(`/notifications/${id}/read`)
      .then(response => response.data)
  },

  /**
   * Удалить уведомление
   * @param {string|number} id - идентификатор уведомления
   * @returns {Promise<void>}
   */
  deleteNotification(id) {
    return api.delete(`/notifications/${id}`)
      .then(response => response.data)
  }
}

// API-методы для пользователя
const userApi = {
  /**
   * Получить информацию о текущем пользователе
   * @returns {Promise<Object>} - данные пользователя
   */
  getCurrentUser() {
    return api.get('/user')
      .then(response => response.data)
  },

  /**
   * Обновить данные пользователя
   * @param {Object} userData - данные пользователя
   * @returns {Promise<Object>} - обновленные данные пользователя
   */
  updateUser(userData) {
    return api.put('/user', userData)
      .then(response => response.data)
  },

  /**
   * Вход пользователя
   * @param {Object} credentials - учетные данные
   * @returns {Promise<Object>} - токен и данные пользователя
   */
  login(credentials) {
    return api.post('/auth/login', credentials)
      .then(response => response.data)
  },

  /**
   * Выход пользователя
   * @returns {Promise<void>}
   */
  logout() {
    return api.post('/auth/logout')
      .then(response => response.data)
  },

  /**
   * Регистрация нового пользователя
   * @param {Object} userData - данные пользователя
   * @returns {Promise<Object>} - созданный пользователь
   */
  register(userData) {
    return api.post('/auth/register', userData)
      .then(response => response.data)
  }
}

// API-методы для статистики
const statsApi = {
  /**
   * Получить данные по потреблению энергии
   * @param {string} period - период (day, week, month)
   * @returns {Promise<Object>} - данные по потреблению
   */
  getEnergyConsumption(period = 'day') {
    return api.get(`/stats/energy/${period}`)
      .then(response => response.data)
  },

  /**
   * Получить историю активности устройств
   * @param {Object} params - параметры запроса (deviceId, startDate, endDate)
   * @returns {Promise<Array>} - история активности
   */
  getDeviceHistory(params = {}) {
    return api.get('/stats/device-history', { params })
      .then(response => response.data)
  },

  /**
   * Получить сводную статистику системы
   * @returns {Promise<Object>} - сводная статистика
   */
  getDashboardStats() {
    return api.get('/stats/dashboard')
      .then(response => response.data)
  },

  /**
   * Получить статистику потребления
   * @param {string} period - период (day, week, month, year)
   * @returns {Promise<Object>} - статистика
   */
  getConsumptionStats(period = 'day') {
    return api.get(`/statistics/consumption?period=${period}`)
      .then(response => response.data)
  },

  /**
   * Получить статистику использования устройств
   * @returns {Promise<Object>} - статистика
   */
  getDeviceUsageStats() {
    return api.get('/statistics/devices/usage')
      .then(response => response.data)
  }
}

// API-методы для локаций (комнат)
const locationsApi = {
  /**
   * Получить список всех локаций
   * @returns {Promise<Array>} - список локаций
   */
  getLocations() {
    return api.get('/locations')
      .then(response => response.data)
  },

  /**
   * Получить локацию по ID
   * @param {string|number} id - идентификатор локации
   * @returns {Promise<Object>} - данные локации
   */
  getLocation(id) {
    return api.get(`/locations/${id}`)
      .then(response => response.data)
  },

  /**
   * Создать новую локацию
   * @param {Object} locationData - данные локации
   * @returns {Promise<Object>} - созданная локация
   */
  createLocation(locationData) {
    return api.post('/locations', locationData)
      .then(response => response.data)
  },

  /**
   * Обновить локацию
   * @param {string|number} id - идентификатор локации
   * @param {Object} locationData - новые данные
   * @returns {Promise<Object>} - обновленная локация
   */
  updateLocation(id, locationData) {
    return api.put(`/locations/${id}`, locationData)
      .then(response => response.data)
  },

  /**
   * Удалить локацию
   * @param {string|number} id - идентификатор локации
   * @returns {Promise<void>}
   */
  deleteLocation(id) {
    return api.delete(`/locations/${id}`)
      .then(response => response.data)
  },
  
  /**
   * Получить комнаты для локации
   * @param {string|number} locationId - ID локации
   * @returns {Promise<Array>} - список комнат
   */
  getRooms(locationId) {
    return api.get(`/locations/${locationId}/rooms`)
      .then(response => response.data)
  },
  
  /**
   * Создать комнату в локации
   * @param {string|number} locationId - ID локации
   * @param {Object} roomData - данные комнаты
   * @returns {Promise<Object>} - созданная комната
   */
  createRoom(locationId, roomData) {
    return api.post(`/locations/${locationId}/rooms`, roomData)
      .then(response => response.data)
  },
  
  /**
   * Обновить комнату
   * @param {string|number} locationId - ID локации
   * @param {string|number} roomId - ID комнаты
   * @param {Object} roomData - новые данные
   * @returns {Promise<Object>} - обновленная комната
   */
  updateRoom(locationId, roomId, roomData) {
    return api.put(`/locations/${locationId}/rooms/${roomId}`, roomData)
      .then(response => response.data)
  },
  
  /**
   * Удалить комнату
   * @param {string|number} locationId - ID локации
   * @param {string|number} roomId - ID комнаты
   * @returns {Promise<void>}
   */
  deleteRoom(locationId, roomId) {
    return api.delete(`/locations/${locationId}/rooms/${roomId}`)
      .then(response => response.data)
  }
}

// Объединяем все API-методы в один объект
export default {
  devices: devicesApi,
  scenarios: scenariosApi,
  notifications: notificationsApi,
  user: userApi,
  stats: statsApi,
  locations: locationsApi
}

// Отдельные экспорты для прямого импорта
export { devicesApi, scenariosApi, notificationsApi, userApi, statsApi, locationsApi }