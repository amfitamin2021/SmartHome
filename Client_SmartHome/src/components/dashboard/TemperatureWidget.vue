<template>
  <div class="bg-white p-6 rounded-xl shadow-sm">
    <div class="flex justify-between items-center mb-6">
      <h3 class="text-lg font-semibold">Температура</h3>
      <i class="text-blue-600 fas fa-temperature-half"></i>
    </div>
    <div class="flex items-center justify-between">
      <div>
        <p class="text-3xl font-bold">{{ currentTemperature }}°C</p>
        <p class="text-gray-500">Целевая: {{ targetTemperature }}°C</p>
      </div>
      <div class="flex gap-2">
        <button 
          class="p-3 rounded-full bg-blue-50 text-blue-600"
          @click="decreaseTemperature"
          :disabled="targetTemperature <= minTemperature"
        >
          <i class="fas fa-minus"></i>
        </button>
        <button 
          class="p-3 rounded-full bg-blue-50 text-blue-600"
          @click="increaseTemperature"
          :disabled="targetTemperature >= maxTemperature"
        >
          <i class="fas fa-plus"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TemperatureWidget',
  data() {
    return {
      currentTemperature: 23,
      targetTemperature: 22,
      minTemperature: 16,
      maxTemperature: 30,
      temperatureStep: 0.5
    }
  },
  methods: {
    increaseTemperature() {
      if (this.targetTemperature < this.maxTemperature) {
        this.targetTemperature = parseFloat((this.targetTemperature + this.temperatureStep).toFixed(1))
        this.updateTargetTemperature()
      }
    },
    decreaseTemperature() {
      if (this.targetTemperature > this.minTemperature) {
        this.targetTemperature = parseFloat((this.targetTemperature - this.temperatureStep).toFixed(1))
        this.updateTargetTemperature()
      }
    },
    updateTargetTemperature() {
      // Здесь будет вызов API для обновления целевой температуры
      console.log(`Установка целевой температуры: ${this.targetTemperature}°C`)
    },
    fetchTemperatureData() {
      // Здесь будет загрузка данных о температуре
      // В реальном приложении это будет вызов API
      console.log('Загрузка данных о температуре')
      
      // Симуляция получения данных
      setTimeout(() => {
        this.currentTemperature = 23
        this.targetTemperature = 22
      }, 1000)
    }
  },
  mounted() {
    this.fetchTemperatureData()
    
    // В реальном приложении здесь можно настроить регулярное обновление данных
    // setInterval(this.fetchTemperatureData, 60000) // обновление каждую минуту
  }
}
</script> 