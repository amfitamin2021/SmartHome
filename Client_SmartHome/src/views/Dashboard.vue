<template>
  <div class="p-2">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-4">
      <!-- Температура -->
      <div class="bg-white p-4 rounded-lg shadow-sm">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-medium flex items-center">
            <i class="fas fa-thermometer-half text-blue-500 mr-2"></i>
            Температура
          </h2>
        </div>
        <div class="flex justify-between items-center">
          <div>
            <p class="text-4xl font-bold">{{ temperature.current }}°C</p>
            <p class="text-gray-500">Целевая: {{ temperature.target }}°C</p>
          </div>
          <div class="flex gap-2">
            <button 
              class="h-12 w-12 rounded-full bg-gray-100 flex items-center justify-center" 
              @click="decreaseTemperature"
            >
              <i class="fas fa-minus text-blue-500"></i>
            </button>
            <button 
              class="h-12 w-12 rounded-full bg-gray-100 flex items-center justify-center"
              @click="increaseTemperature"
            >
              <i class="fas fa-plus text-blue-500"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Освещение -->
      <div class="bg-white p-4 rounded-lg shadow-sm">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-medium flex items-center">
            <i class="fas fa-lightbulb text-yellow-500 mr-2"></i>
            Освещение
          </h2>
        </div>
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <span>Гостиная</span>
            <label class="relative inline-flex items-center cursor-pointer">
              <input 
                type="checkbox" 
                v-model="lights.livingRoom" 
                @change="toggleLight('livingRoom')" 
                class="sr-only peer"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
          <div class="flex items-center justify-between">
            <span>Кухня</span>
            <label class="relative inline-flex items-center cursor-pointer">
              <input 
                type="checkbox" 
                v-model="lights.kitchen" 
                @change="toggleLight('kitchen')" 
                class="sr-only peer"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
          <div class="flex items-center justify-between">
            <span>Спальня</span>
            <label class="relative inline-flex items-center cursor-pointer">
              <input 
                type="checkbox" 
                v-model="lights.bedroom" 
                @change="toggleLight('bedroom')" 
                class="sr-only peer"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
          <div class="pt-2">
            <div class="flex items-center justify-between mb-1">
              <span>Яркость</span>
              <span class="text-sm text-gray-500">{{ lights.brightness }}%</span>
            </div>
            <input 
              type="range" 
              min="0" 
              max="100" 
              v-model="lights.brightness" 
              @change="updateBrightness(lights.brightness)"
              class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            >
          </div>
        </div>
      </div>

      <!-- Безопасность -->
      <div class="bg-white p-4 rounded-lg shadow-sm">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-medium flex items-center">
            <i class="fas fa-shield-alt text-green-500 mr-2"></i>
            Безопасность
          </h2>
        </div>
        <div class="mb-4">
          <div class="bg-green-50 p-2 rounded-md flex items-center mb-4" v-if="security.active">
            <i class="fas fa-check-circle text-green-500 mr-2"></i>
            <span class="text-green-700">Система активна</span>
          </div>
          <div class="bg-gray-50 p-2 rounded-md flex items-center mb-4" v-else>
            <i class="fas fa-info-circle text-gray-500 mr-2"></i>
            <span class="text-gray-700">Система отключена</span>
          </div>
          <div class="grid grid-cols-2 gap-3 mb-4">
            <router-link to="/security/cameras" class="flex flex-col items-center p-3 bg-gray-50 rounded-lg hover:bg-gray-100">
              <i class="fas fa-video text-gray-600 mb-1"></i>
              <span class="text-sm">Камеры</span>
            </router-link>
            <router-link to="/security/locks" class="flex flex-col items-center p-3 bg-gray-50 rounded-lg hover:bg-gray-100">
              <i class="fas fa-lock text-gray-600 mb-1"></i>
              <span class="text-sm">Замки</span>
            </router-link>
          </div>
          <div class="flex items-center justify-between">
            <span>Режим охраны</span>
            <label class="relative inline-flex items-center cursor-pointer">
              <input 
                type="checkbox" 
                v-model="security.guardMode" 
                @change="toggleSecurityMode" 
                class="sr-only peer"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- Энергопотребление -->
    <div class="bg-white p-4 rounded-lg shadow-sm mb-4">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-lg font-medium">Энергопотребление</h2>
        <div class="flex space-x-2 text-sm">
          <button 
            class="px-3 py-1 rounded-md" 
            :class="energyPeriod === 'day' ? 'bg-blue-100 text-blue-600' : ''"
            @click="setEnergyPeriod('day')"
          >
            День
          </button>
          <button 
            class="px-3 py-1 rounded-md" 
            :class="energyPeriod === 'week' ? 'bg-blue-100 text-blue-600' : ''"
            @click="setEnergyPeriod('week')"
          >
            Неделя
          </button>
          <button 
            class="px-3 py-1 rounded-md" 
            :class="energyPeriod === 'month' ? 'bg-blue-100 text-blue-600' : ''"
            @click="setEnergyPeriod('month')"
          >
            Месяц
          </button>
        </div>
      </div>
      <div class="h-64 bg-gray-50 rounded-lg mb-4 flex items-center justify-center">
        <!-- Здесь будет график энергопотребления -->
        <p class="text-gray-400">График энергопотребления ({{ getEnergyPeriodText }})</p>
      </div>
      <div class="flex justify-between items-center text-sm">
        <div>
          <div class="text-gray-500">Сегодня:</div>
          <div class="font-medium">{{ energy.today }} кВт·ч</div>
          <div class="text-gray-500">За день: {{ energy.forDay }} кВт·ч</div>
        </div>
        <div class="text-right">
          <div class="text-gray-500">Стоимость сегодня:</div>
          <div class="font-medium">{{ energy.cost }} ₽</div>
          <div class="text-gray-500">Прогноз на месяц: {{ energy.forecast }} ₽</div>
        </div>
      </div>
    </div>

    <!-- Уведомления -->
    <div class="bg-white p-4 rounded-lg shadow-sm">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-lg font-medium">Уведомления</h2>
        <span class="px-2 py-1 bg-red-100 text-red-600 rounded-full text-xs">{{ getNewNotificationsCount }} новых</span>
      </div>
      <div class="space-y-3">
        <div 
          v-for="notification in notifications" 
          :key="notification.id" 
          :class="{
            'bg-red-50 border-l-4 border-red-500': notification.type === 'danger',
            'bg-yellow-50 border-l-4 border-yellow-500': notification.type === 'warning',
            'bg-green-50 border-l-4 border-green-500': notification.type === 'success'
          }"
          class="p-3 rounded-lg"
        >
          <div class="flex items-start">
            <i 
              :class="{
                'fas fa-exclamation-circle text-red-500': notification.type === 'danger',
                'fas fa-exclamation-triangle text-yellow-500': notification.type === 'warning',
                'fas fa-check-circle text-green-500': notification.type === 'success'
              }"
              class="mr-2 mt-0.5"
            ></i>
            <div>
              <p class="font-medium">{{ notification.message }}</p>
            </div>
          </div>
        </div>
        <div class="text-center mt-4">
          <button @click="viewAllNotifications" class="text-blue-500 hover:underline text-sm">Посмотреть все уведомления</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      temperature: {
        current: 23,
        target: 22
      },
      lights: {
        livingRoom: true,
        kitchen: false,
        bedroom: false,
        brightness: 80
      },
      security: {
        active: true,
        guardMode: true
      },
      energy: {
        today: 3.2,
        forDay: 3.2,
        cost: 25.60,
        forecast: 780
      },
      energyPeriod: 'day', // 'day', 'week', 'month'
      notifications: [
        {
          id: 1,
          type: 'danger', 
          icon: 'exclamation-circle',
          message: 'Обнаружено движение (Гараж)',
          isNew: true
        },
        {
          id: 2,
          type: 'warning',
          icon: 'exclamation-triangle',
          message: 'Низкий заряд батареи (Датчик)',
          isNew: true
        },
        {
          id: 3, 
          type: 'success',
          icon: 'check-circle',
          message: 'Завершено обновление ПО',
          isNew: false
        }
      ]
    }
  },
  computed: {
    getEnergyPeriodText() {
      switch(this.energyPeriod) {
        case 'day': return 'за день';
        case 'week': return 'за неделю';
        case 'month': return 'за месяц';
        default: return 'за день';
      }
    },
    getNewNotificationsCount() {
      return this.notifications.filter(n => n.isNew).length;
    }
  },
  methods: {
    increaseTemperature() {
      if (this.temperature.target < 30) {
        this.temperature.target += 0.5;
        // В реальном приложении здесь был бы запрос к API
        console.log(`Установка целевой температуры: ${this.temperature.target}°C`);
      }
    },
    decreaseTemperature() {
      if (this.temperature.target > 16) {
        this.temperature.target -= 0.5;
        // В реальном приложении здесь был бы запрос к API
        console.log(`Установка целевой температуры: ${this.temperature.target}°C`);
      }
    },
    toggleLight(room) {
      // В реальном приложении здесь был бы запрос к API
      console.log(`${room}: ${this.lights[room] ? 'включен' : 'выключен'}`);
    },
    updateBrightness(value) {
      // В реальном приложении здесь был бы запрос к API
      console.log(`Установка яркости: ${value}%`);
    },
    toggleSecurityMode() {
      this.security.active = this.security.guardMode;
      // В реальном приложении здесь был бы запрос к API
      console.log(`Режим охраны: ${this.security.guardMode ? 'включен' : 'выключен'}`);
    },
    setEnergyPeriod(period) {
      this.energyPeriod = period;
      // В реальном приложении здесь был бы запрос к API для получения данных за выбранный период
      console.log(`Выбран период: ${period}`);
    },
    viewAllNotifications() {
      this.$router.push('/notifications');
    },
    markAllNotificationsAsRead() {
      this.notifications.forEach(n => n.isNew = false);
      // В реальном приложении здесь был бы запрос к API
      console.log('Все уведомления отмечены как прочитанные');
    }
  }
}
</script> 