<template>
  <div class="p-6">
    <div class="mb-6">
      <h1 class="text-2xl font-bold">Безопасность</h1>
      <p class="text-gray-500">Управление системой безопасности и мониторинг</p>
    </div>

    <!-- Вкладки для подразделов безопасности -->
    <div class="mb-6 border-b">
      <div class="flex gap-6">
        <router-link 
          to="/security/cameras" 
          class="pb-3 px-1 border-b-2 transition-colors"
          :class="isActive('cameras') ? 'border-blue-500 text-blue-600' : 'border-transparent hover:border-gray-300'"
        >
          <i class="fas fa-video mr-2"></i>Камеры
        </router-link>
        <router-link 
          to="/security/sensors" 
          class="pb-3 px-1 border-b-2 transition-colors"
          :class="isActive('sensors') ? 'border-blue-500 text-blue-600' : 'border-transparent hover:border-gray-300'"
        >
          <i class="fas fa-bullseye mr-2"></i>Датчики
        </router-link>
        <router-link 
          to="/security/locks" 
          class="pb-3 px-1 border-b-2 transition-colors"
          :class="isActive('locks') ? 'border-blue-500 text-blue-600' : 'border-transparent hover:border-gray-300'"
        >
          <i class="fas fa-lock mr-2"></i>Замки
        </router-link>
        <router-link 
          to="/security/alarms" 
          class="pb-3 px-1 border-b-2 transition-colors"
          :class="isActive('alarms') ? 'border-blue-500 text-blue-600' : 'border-transparent hover:border-gray-300'"
        >
          <i class="fas fa-bell mr-2"></i>Сигнализация
        </router-link>
        <router-link 
          to="/security/history" 
          class="pb-3 px-1 border-b-2 transition-colors"
          :class="isActive('history') ? 'border-blue-500 text-blue-600' : 'border-transparent hover:border-gray-300'"
        >
          <i class="fas fa-history mr-2"></i>История
        </router-link>
      </div>
    </div>

    <!-- Панель статуса безопасности -->
    <div v-if="isRootRoute" class="mb-6 p-4 bg-white rounded-xl shadow-sm">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div 
          class="p-4 rounded-lg text-center"
          :class="securityStatus.system === 'armed' ? 'bg-green-50 text-green-700' : 'bg-gray-50'"
        >
          <div class="text-xl mb-1">
            <i class="fas fa-shield-alt"></i>
          </div>
          <h3 class="font-semibold">Система</h3>
          <p>{{ securityStatus.system === 'armed' ? 'Активна' : 'Отключена' }}</p>
        </div>
        <div 
          class="p-4 rounded-lg text-center"
          :class="securityStatus.doorsWindows === 'secured' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'"
        >
          <div class="text-xl mb-1">
            <i class="fas fa-door-closed"></i>
          </div>
          <h3 class="font-semibold">Двери и окна</h3>
          <p>{{ securityStatus.doorsWindows === 'secured' ? 'Закрыты' : 'Открыты' }}</p>
        </div>
        <div 
          class="p-4 rounded-lg text-center"
          :class="securityStatus.activeCameras > 0 ? 'bg-green-50 text-green-700' : 'bg-gray-50'"
        >
          <div class="text-xl mb-1">
            <i class="fas fa-video"></i>
          </div>
          <h3 class="font-semibold">Камеры</h3>
          <p>{{ securityStatus.activeCameras }} из {{ securityStatus.totalCameras }} активны</p>
        </div>
        <div 
          class="p-4 rounded-lg text-center"
          :class="securityStatus.motion === 'detected' ? 'bg-yellow-50 text-yellow-700' : 'bg-green-50 text-green-700'"
        >
          <div class="text-xl mb-1">
            <i class="fas fa-running"></i>
          </div>
          <h3 class="font-semibold">Движение</h3>
          <p>{{ securityStatus.motion === 'detected' ? 'Обнаружено' : 'Не обнаружено' }}</p>
        </div>
      </div>
      
      <!-- Управление режимом охраны -->
      <div class="mt-6 p-4 bg-gray-50 rounded-lg">
        <div class="flex justify-between items-center">
          <div>
            <h3 class="font-semibold text-lg">Режим охраны</h3>
            <p class="text-gray-600">{{ securityStatus.system === 'armed' ? 'Система находится под охраной' : 'Охрана отключена' }}</p>
          </div>
          <div class="flex gap-3">
            <button 
              class="px-4 py-2 rounded-lg"
              :class="securityStatus.system === 'armed' ? 'bg-gray-200 text-gray-700' : 'bg-green-600 text-white'"
              @click="toggleSecuritySystem"
              :disabled="securityStatus.system === 'armed'"
            >
              <i class="fas fa-shield-alt mr-2"></i>Включить
            </button>
            <button 
              class="px-4 py-2 rounded-lg"
              :class="securityStatus.system === 'disarmed' ? 'bg-gray-200 text-gray-700' : 'bg-red-600 text-white'"
              @click="toggleSecuritySystem"
              :disabled="securityStatus.system === 'disarmed'"
            >
              <i class="fas fa-shield-alt mr-2"></i>Отключить
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Содержимое вложенных маршрутов -->
    <router-view></router-view>
    
    <!-- Плитки с датчиками безопасности -->
    <div v-if="isRootRoute" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-6">
      <div 
        v-for="sensor in securitySensors" 
        :key="sensor.id"
        class="bg-white p-4 rounded-xl shadow-sm"
        :class="{ 'border-l-4 border-red-500': sensor.status === 'triggered' }"
      >
        <div class="flex justify-between items-start mb-4">
          <div>
            <h3 class="font-semibold">{{ sensor.name }}</h3>
            <p class="text-gray-600">{{ sensor.location }}</p>
          </div>
          <div class="flex gap-2">
            <div 
              class="h-4 w-4 rounded-full mt-1"
              :class="sensor.status === 'ok' ? 'bg-green-500' : sensor.status === 'triggered' ? 'bg-red-500' : 'bg-yellow-500'"
            ></div>
          </div>
        </div>
        <div class="flex justify-between items-center">
          <div class="text-sm text-gray-500">
            <i :class="getSensorIcon(sensor.type)"></i>
            {{ getSensorTypeText(sensor.type) }}
          </div>
          <div class="text-sm">
            {{ getSensorStatusText(sensor.status) }}
          </div>
        </div>
      </div>
    </div>
    
    <!-- Последние события безопасности -->
    <div v-if="isRootRoute" class="mt-6">
      <h2 class="text-xl font-semibold mb-4">Последние события</h2>
      <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-4 py-3 text-left">Время</th>
                <th class="px-4 py-3 text-left">Устройство</th>
                <th class="px-4 py-3 text-left">Событие</th>
                <th class="px-4 py-3 text-left">Статус</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="event in securityEvents" :key="event.id" class="border-t">
                <td class="px-4 py-3">{{ formatDate(event.timestamp) }}</td>
                <td class="px-4 py-3">{{ event.device }}</td>
                <td class="px-4 py-3">{{ event.event }}</td>
                <td class="px-4 py-3">
                  <span 
                    class="px-2 py-1 text-xs rounded-full"
                    :class="getEventStatusClass(event.status)"
                  >
                    {{ event.status }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

export default defineComponent({
  name: 'SecurityView',
  
  setup() {
    const route = useRoute()
    
    // Проверка, находимся ли мы на корневом маршруте /security
    const isRootRoute = computed(() => {
      return route.path === '/security'
    })
    
    // Определение активной вкладки
    const isActive = (routeName) => {
      return route.path.includes(`/security/${routeName}`)
    }
    
    // Статус системы безопасности
    const securityStatus = ref({
      system: 'armed', // armed или disarmed
      doorsWindows: 'secured', // secured или open
      activeCameras: 4,
      totalCameras: 5,
      motion: 'none' // none или detected
    })
    
    // Переключение системы безопасности
    const toggleSecuritySystem = () => {
      securityStatus.value.system = securityStatus.value.system === 'armed' ? 'disarmed' : 'armed'
    }
    
    // Датчики безопасности
    const securitySensors = ref([
      {
        id: 1,
        name: 'Датчик движения',
        location: 'Гостиная',
        type: 'motion',
        status: 'ok'
      },
      {
        id: 2,
        name: 'Дверной сенсор',
        location: 'Входная дверь',
        type: 'door',
        status: 'ok'
      },
      {
        id: 3,
        name: 'Датчик движения',
        location: 'Коридор',
        type: 'motion',
        status: 'triggered'
      },
      {
        id: 4,
        name: 'Датчик окна',
        location: 'Кухня',
        type: 'window',
        status: 'ok'
      },
      {
        id: 5,
        name: 'Датчик дыма',
        location: 'Кухня',
        type: 'smoke',
        status: 'ok'
      },
      {
        id: 6,
        name: 'Датчик протечки',
        location: 'Ванная комната',
        type: 'water',
        status: 'warning'
      }
    ])
    
    // События безопасности
    const securityEvents = ref([
      {
        id: 1,
        timestamp: new Date(Date.now() - 1800000),
        device: 'Датчик движения (Коридор)',
        event: 'Обнаружено движение',
        status: 'Тревога'
      },
      {
        id: 2,
        timestamp: new Date(Date.now() - 3600000),
        device: 'Система безопасности',
        event: 'Активация охранной системы',
        status: 'Информация'
      },
      {
        id: 3,
        timestamp: new Date(Date.now() - 7200000),
        device: 'Входная дверь',
        event: 'Дверь открыта',
        status: 'Предупреждение'
      },
      {
        id: 4,
        timestamp: new Date(Date.now() - 86400000),
        device: 'Датчик протечки (Ванная)',
        event: 'Низкий заряд батареи',
        status: 'Предупреждение'
      },
      {
        id: 5,
        timestamp: new Date(Date.now() - 172800000),
        device: 'Система безопасности',
        event: 'Обновление прошивки',
        status: 'Информация'
      }
    ])
    
    // Получение иконки для типа датчика
    const getSensorIcon = (type) => {
      switch (type) {
        case 'motion':
          return 'fas fa-running mr-1'
        case 'door':
          return 'fas fa-door-closed mr-1'
        case 'window':
          return 'fas fa-window-maximize mr-1'
        case 'smoke':
          return 'fas fa-smoke mr-1'
        case 'water':
          return 'fas fa-tint mr-1'
        default:
          return 'fas fa-sensor mr-1'
      }
    }
    
    // Получение названия типа датчика
    const getSensorTypeText = (type) => {
      switch (type) {
        case 'motion':
          return 'Датчик движения'
        case 'door':
          return 'Датчик двери'
        case 'window':
          return 'Датчик окна'
        case 'smoke':
          return 'Датчик дыма'
        case 'water':
          return 'Датчик протечки'
        default:
          return 'Датчик'
      }
    }
    
    // Получение текста статуса датчика
    const getSensorStatusText = (status) => {
      switch (status) {
        case 'ok':
          return 'В порядке'
        case 'triggered':
          return 'Срабатывание'
        case 'warning':
          return 'Требует внимания'
        default:
          return 'Неизвестно'
      }
    }
    
    // Получение класса для статуса события
    const getEventStatusClass = (status) => {
      switch (status) {
        case 'Тревога':
          return 'bg-red-100 text-red-800'
        case 'Предупреждение':
          return 'bg-yellow-100 text-yellow-800'
        case 'Информация':
          return 'bg-blue-100 text-blue-800'
        default:
          return 'bg-gray-100 text-gray-800'
      }
    }
    
    // Форматирование даты
    const formatDate = (date) => {
      return new Date(date).toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    onMounted(() => {
      // В реальном приложении здесь была бы загрузка данных
    })
    
    return {
      isRootRoute,
      isActive,
      securityStatus,
      securitySensors,
      securityEvents,
      toggleSecuritySystem,
      getSensorIcon,
      getSensorTypeText,
      getSensorStatusText,
      getEventStatusClass,
      formatDate
    }
  }
})
</script> 