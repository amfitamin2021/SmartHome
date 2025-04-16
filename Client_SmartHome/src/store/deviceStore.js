import { defineStore } from 'pinia'
import api from '@/services/api'

export const useDeviceStore = defineStore('devices', {
  state: () => ({
    devices: [],
    loading: false,
    error: null,
  }),
  
  getters: {
    getDeviceById: (state) => (id) => {
      return state.devices.find(device => device.id === id)
    },
    
    getDevicesByRoom: (state) => (roomId) => {
      if (roomId === 'all') return state.devices
      return state.devices.filter(device => device.roomId === roomId)
    },
    
    getDevicesByType: (state) => (type) => {
      return state.devices.filter(device => device.type === type)
    },
    
    getFilteredDevices: (state) => (roomId, searchQuery) => {
      return state.devices.filter(device => {
        // Фильтрация по комнате
        const roomMatch = roomId === 'all' || device.roomId === roomId
        
        // Фильтрация по поисковому запросу
        const query = searchQuery.toLowerCase()
        const searchMatch = !query || 
          device.name.toLowerCase().includes(query) ||
          device.type.toLowerCase().includes(query) ||
          device.room.toLowerCase().includes(query)
        
        return roomMatch && searchMatch
      })
    }
  },
  
  actions: {
    async fetchDevices() {
      try {
        this.loading = true
        this.error = null
        
        const response = await api.devices.getDevices()
        this.devices = response.map(device => this.mapDeviceFromBackend(device))
        
        return this.devices
      } catch (error) {
        console.error('Ошибка при получении устройств:', error)
        this.error = 'Не удалось загрузить устройства. Пожалуйста, попробуйте позже.'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // Маппинг данных устройства с бэкенда в формат фронтенда
    mapDeviceFromBackend(backendDevice) {
      const properties = backendDevice.properties || {}
      
      // Преобразуем значения из строк в нужные типы данных
      const active = properties.attr_server_active === 'true' && properties.tb_power !== 'off'
      const brightness = parseInt(properties.brightness || properties.tb_brightness || '0')
      const color = properties.color ? `#${properties.color}` : (properties.tb_color ? `#${properties.tb_color}` : '#FFFFFF')
      const online = backendDevice.status !== 'OFFLINE'
      const isVirtual = backendDevice.protocol === 'VIRTUAL'
      
      return {
        id: backendDevice.id,
        name: backendDevice.name,
        type: backendDevice.type.toLowerCase(),
        category: backendDevice.category || null,
        subType: backendDevice.subType || null,
        room: backendDevice.roomName || 'Неназначено',
        roomId: backendDevice.roomId,
        active: active,
        brightness: brightness,
        color: color,
        online: online,
        isVirtual: isVirtual,  // Добавляем флаг виртуального устройства
        canControl: online || isVirtual, // Можно управлять устройством, если оно онлайн или виртуальное
        hasColor: backendDevice.type === 'LIGHT' || 
                 (backendDevice.category === 'LIGHTING' && 
                  (backendDevice.subType === 'SMART_BULB' || backendDevice.subType === 'LED_STRIP')),
        protocol: backendDevice.protocol,
        // Сохраняем оригинальные свойства для отправки на сервер
        rawProperties: properties,
        manufacturer: backendDevice.manufacturer,
        model: backendDevice.model,
        // Добавляем информацию о связи с ThingsBoard
        thingsboardId: backendDevice.thingsboardId || null,
        thingsboardToken: backendDevice.thingsboardToken || null
      }
    },
    
    // Преобразует данные для отправки на бэкенд
    mapDeviceToBackend(frontendData) {
      // Преобразуем состояние для бэкенда
      const backendData = {}
      
      if (frontendData.active !== undefined) {
        // Используем attr_server_active для всех устройств
        backendData.attr_server_active = frontendData.active.toString()
        
        // Для обратной совместимости добавляем tb_power
        backendData.tb_power = frontendData.active ? 'on' : 'off'
      }
      
      if (frontendData.brightness !== undefined) {
        // Для яркости добавляем оба варианта параметра
        backendData.brightness = frontendData.brightness.toString()
        backendData.tb_brightness = frontendData.brightness.toString()
      }
      
      if (frontendData.color !== undefined) {
        // Убираем # из цвета для бэкенда и добавляем оба варианта параметра
        const colorValue = frontendData.color.replace('#', '')
        backendData.color = colorValue
        backendData.tb_color = colorValue
      }
      
      console.log('Данные для отправки на бэкенд:', backendData)
      return backendData
    },
    
    async addDevice(deviceData) {
      try {
        // Не показываем индикатор загрузки, чтобы избежать эффекта зависания
        this.error = null
        
        // Отправляем данные на сервер
        const newDevice = await api.devices.createDevice(deviceData)
        console.log('Устройство успешно создано на сервере:', newDevice)
        
        // Проверяем, нет ли уже такого устройства в списке
        const existingDeviceIndex = this.devices.findIndex(device => device.id === newDevice.id)
        
        if (existingDeviceIndex !== -1) {
          // Если устройство уже есть, обновляем его данные
          console.log('Устройство уже существует, обновляем данные:', newDevice.id)
          this.devices[existingDeviceIndex] = this.mapDeviceFromBackend(newDevice)
        } else {
          // Иначе добавляем новое устройство
          console.log('Добавляем новое устройство в список:', newDevice.id)
          this.devices.push(this.mapDeviceFromBackend(newDevice))
        }
        
        return newDevice
      } catch (error) {
        console.error('Ошибка при добавлении устройства:', error)
        this.error = 'Не удалось добавить устройство. Пожалуйста, попробуйте позже.'
        throw error
      }
    },
    
    async updateDevice(id, deviceData) {
      try {
        // Не показываем индикатор загрузки при обновлении
        this.error = null
        
        console.log('Обновление устройства:', id, deviceData)
        
        // Если deviceData содержит весь объект устройства, используем его напрямую
        if (deviceData.id && deviceData.name && deviceData.type) {
          const index = this.devices.findIndex(device => device.id === id)
          if (index !== -1) {
            this.devices[index] = this.mapDeviceFromBackend(deviceData)
            console.log('Устройство полностью обновлено:', this.devices[index])
            return this.devices[index]
          }
        }
        
        // Для частичного обновления попробуем использовать API
        try {
          // Отправляем данные на сервер
          const updatedDevice = await api.devices.updateState(id, deviceData)
          
          const index = this.devices.findIndex(device => device.id === id)
          if (index !== -1) {
            // Объединяем существующие данные с полученными из ответа
            this.devices[index] = { 
              ...this.devices[index], 
              ...this.mapDeviceFromBackend(updatedDevice)
            }
            console.log('Устройство частично обновлено через API:', this.devices[index])
            return this.devices[index]
          }
        } catch (error) {
          console.error('Ошибка при обновлении через API:', error)
          
          // В случае ошибки API, обновим локальные данные на основе переданных параметров
          const index = this.devices.findIndex(device => device.id === id)
          if (index !== -1) {
            // Только обновляем конкретные поля, которые были в deviceData
            if (deviceData.active !== undefined) {
              this.devices[index].active = deviceData.active === true || deviceData.active === 'true'
            }
            
            if (deviceData.brightness !== undefined) {
              this.devices[index].brightness = parseInt(deviceData.brightness)
            }
            
            if (deviceData.color !== undefined) {
              this.devices[index].color = deviceData.color.startsWith('#') 
                ? deviceData.color 
                : `#${deviceData.color}`
            }
            
            console.log('Устройство обновлено локально:', this.devices[index])
            return this.devices[index]
          }
        }
        
        throw new Error('Устройство не найдено')
      } catch (error) {
        console.error('Ошибка при обновлении устройства:', error)
        this.error = 'Не удалось обновить устройство. Пожалуйста, попробуйте позже.'
        throw error
      }
    },
    
    async deleteDevice(id) {
      try {
        // Не показываем индикатор загрузки при удалении
        this.error = null
        
        // Отправляем запрос на сервер
        await api.devices.deleteDevice(id)
        
        const index = this.devices.findIndex(device => device.id === id)
        if (index !== -1) {
          this.devices.splice(index, 1)
          return true
        }
        
        return false
      } catch (error) {
        console.error('Ошибка при удалении устройства:', error)
        this.error = 'Не удалось удалить устройство. Пожалуйста, попробуйте позже.'
        throw error
      }
    },
    
    async sendCommand(id, command, parameters = {}) {
      try {
        const device = this.getDeviceById(id)
        
        if (!device) {
          throw new Error(`Устройство с ID ${id} не найдено`)
        }
        
        // Проверяем, можно ли управлять устройством
        if (!device.canControl) {
          throw new Error(`Устройство находится в состоянии OFFLINE и не может быть виртуальным`)
        }
        
        console.log(`Отправка команды ${command} на устройство ${id} с параметрами:`, parameters)
        
        // Отправляем запрос
        const response = await api.devices.sendCommand(id, { 
          command, 
          parameters 
        })
        
        console.log('Ответ от сервера:', response)
        
        // Если сервер вернул обновленные свойства, обновляем их локально
        if (response && response.properties) {
          const deviceIndex = this.devices.findIndex(d => d.id === id)
          if (deviceIndex !== -1) {
            // Обновляем свойства
            this.devices[deviceIndex].rawProperties = { ...this.devices[deviceIndex].rawProperties, ...response.properties }
            
            // Обновляем вычисляемые свойства (активность, яркость, цвет и т.д.)
            if (response.properties.tb_power) {
              this.devices[deviceIndex].active = response.properties.tb_power === 'on'
            }
            
            if (response.properties.tb_brightness) {
              this.devices[deviceIndex].brightness = parseInt(response.properties.tb_brightness)
            }
            
            if (response.properties.tb_color) {
              this.devices[deviceIndex].color = `#${response.properties.tb_color}`
            }
          }
        }
        
        return response
      } catch (error) {
        console.error(`Ошибка при отправке команды ${command} на устройство ${id}:`, error)
        
        // Используем ошибку с сервера, если она есть
        if (error.response && error.response.data && error.response.data.message) {
          throw new Error(error.response.data.message)
        }
        
        throw error
      }
    },
    
    // Методы для работы с умной лампой
    
    // Включить/выключить устройство
    async toggleDevice(id, active) {
      try {
        const device = this.getDeviceById(id)
        if (!device) throw new Error('Устройство не найдено')
        
        if (!device.canControl) {
          throw new Error('Устройство не может быть контролируемо (OFFLINE)')
        }
        
        // Формируем данные для бэкенда
        const backendData = { 
          tb_power: active ? 'on' : 'off',
          attr_server_active: active.toString()
        }
        
        // Для датчика влажности устанавливаем дополнительные параметры
        if (device.category === 'CLIMATE' && device.subType === 'HUMIDITY_SENSOR') {
          // Обновляем время последнего обновления
          backendData.tb_last_updated = new Date().toISOString()
        }
        
        // Отправляем команду на сервер
        await this.sendCommand(id, 'setState', backendData)
        
        // Обновляем устройство локально
        device.active = active
        
        return true
      } catch (error) {
        console.error('Ошибка при переключении устройства:', error)
        throw error
      }
    },
    
    // Изменить яркость лампы
    async setBrightness(id, brightness) {
      try {
        const device = this.getDeviceById(id)
        if (!device) throw new Error('Устройство не найдено')
        
        if (!device.canControl) {
          throw new Error('Устройство не может быть контролируемо (OFFLINE)')
        }
        
        // Формируем данные для бэкенда
        const backendData = { 
          tb_brightness: brightness.toString(),
          brightness: brightness.toString() // Добавляем стандартный ключ brightness
        }
        
        // Отправляем команду на сервер
        await this.sendCommand(id, 'setState', backendData)
        
        // Обновляем устройство локально
        device.brightness = brightness
        
        return true
      } catch (error) {
        console.error('Ошибка при изменении яркости:', error)
        throw error
      }
    },
    
    // Изменить цвет лампы
    async setLightColor(id, color) {
      try {
        const device = this.getDeviceById(id)
        if (!device) throw new Error('Устройство не найдено')
        
        if (!device.canControl) {
          throw new Error('Устройство не может быть контролируемо (OFFLINE)')
        }
        
        // Удаляем решетку из цвета если она есть
        if (color.startsWith('#')) {
          color = color.substring(1)
        }
        
        // Формируем данные для бэкенда
        const backendData = { 
          tb_color: color,
          color: color // Добавляем стандартный ключ color
        }
        
        // Отправляем команду на сервер
        await this.sendCommand(id, 'setState', backendData)
        
        // Обновляем устройство локально
        device.color = `#${color}`
        
        return true
      } catch (error) {
        console.error('Ошибка при изменении цвета:', error)
        throw error
      }
    },
    
    // Обновить цвет и яркость лампы одновременно
    async updateLightProperties(id, { color, brightness }) {
      try {
        const device = this.getDeviceById(id)
        if (!device) throw new Error('Устройство не найдено')
        
        if (!device.canControl) {
          throw new Error('Устройство не может быть контролируемо (OFFLINE)')
        }
        
        // Подготавливаем данные
        const backendData = {
          // Устанавливаем устройство в активное состояние
          attr_server_active: 'true',
          tb_power: 'on'
        }
        
        // Удаляем решетку из цвета если она есть
        if (color) {
          if (color.startsWith('#')) {
            color = color.substring(1)
          }
          backendData.tb_color = color
          backendData.color = color // Добавляем стандартный ключ color
        }
        
        // Добавляем яркость
        if (brightness !== undefined) {
          backendData.tb_brightness = brightness.toString()
          backendData.brightness = brightness.toString() // Добавляем стандартный ключ brightness
        }
        
        // Отправляем команду на сервер
        await this.sendCommand(id, 'setState', backendData)
        
        // Обновляем устройство локально
        if (color) {
          device.color = `#${color}`
        }
        
        if (brightness !== undefined) {
          device.brightness = brightness
        }
        
        // Убедимся, что устройство отмечено как активное
        device.active = true
        
        return true
      } catch (error) {
        console.error('Ошибка при обновлении свойств лампы:', error)
        throw error
      }
    },
    
    // Получение доступных устройств из ThingsBoard
    async getAvailableDevices() {
      try {
        return await api.devices.getAvailableDevices()
      } catch (error) {
        console.error('Ошибка при получении доступных устройств:', error)
        throw error
      }
    },
    
    // Обновление данных датчика влажности
    async updateHumiditySensorData(id, { humidity, battery }) {
      try {
        const device = this.getDeviceById(id)
        if (!device) throw new Error('Устройство не найдено')
        
        if (device.category !== 'CLIMATE' || device.subType !== 'HUMIDITY_SENSOR') {
          throw new Error('Устройство не является датчиком влажности')
        }
        
        // Формируем данные для бэкенда
        const backendData = {
          // Добавляем значение влажности для телеметрии ThingsBoard (без префикса 'tb_')
          humidity: humidity.toString(),
          battery: battery.toString(),
          // Также обновляем свойства устройства
          tb_humidity: humidity.toString(),
          tb_battery: battery.toString(),
          tb_last_updated: new Date().toISOString()
        }
        
        // Отправляем команду на сервер
        await this.sendCommand(id, 'updateTelemetry', backendData)
        
        // Обновляем устройство локально
        if (device.rawProperties) {
          device.rawProperties.tb_humidity = humidity.toString()
          device.rawProperties.tb_battery = battery.toString()
          device.rawProperties.tb_last_updated = backendData.tb_last_updated
        }
        
        return true
      } catch (error) {
        console.error('Ошибка при обновлении данных датчика влажности:', error)
        throw error
      }
    }
  }
}) 