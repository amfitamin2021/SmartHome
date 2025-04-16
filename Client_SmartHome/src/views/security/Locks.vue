<template>
  <div>
    <div class="mb-6 flex justify-between items-center">
      <h2 class="text-xl font-semibold">Умные замки</h2>
      <div class="flex gap-3">
        <button class="px-4 py-2 bg-blue-600 text-white rounded-lg">
          <i class="fas fa-plus mr-2"></i>Добавить замок
        </button>
      </div>
    </div>

    <!-- Сетка замков -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="lock in locks" :key="lock.id" class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="p-4">
          <div class="flex justify-between items-start mb-4">
            <div>
              <h3 class="font-semibold">{{ lock.name }}</h3>
              <p class="text-sm text-gray-600">{{ lock.location }}</p>
            </div>
            <div class="flex items-center">
              <span class="inline-block h-2 w-2 rounded-full mr-1" :class="lock.status === 'online' ? 'bg-green-500' : 'bg-gray-400'"></span>
              <span class="text-xs text-gray-600">{{ lock.status === 'online' ? 'Онлайн' : 'Офлайн' }}</span>
            </div>
          </div>

          <div class="mb-4 flex justify-center">
            <div class="text-center">
              <div class="h-24 w-24 rounded-full bg-gray-100 flex items-center justify-center mb-2">
                <i :class="['text-4xl', lock.locked ? 'fas fa-lock text-red-500' : 'fas fa-lock-open text-green-500']"></i>
              </div>
              <p class="font-medium" :class="lock.locked ? 'text-red-500' : 'text-green-500'">
                {{ lock.locked ? 'Заблокировано' : 'Разблокировано' }}
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-2">
            <button 
              class="flex-1 px-3 py-2 rounded-lg border"
              :class="lock.locked ? 'border-green-500 text-green-600 bg-green-50' : 'border-red-500 text-red-600 bg-red-50'"
              @click="toggleLock(lock)"
            >
              <i class="fas" :class="lock.locked ? 'fa-lock-open' : 'fa-lock'"></i>
              {{ lock.locked ? 'Разблокировать' : 'Заблокировать' }}
            </button>
            <button 
              class="flex-none px-3 py-2 rounded-lg border border-gray-300 text-gray-700"
              @click="showHistory(lock)"
            >
              <i class="fas fa-history"></i>
            </button>
          </div>
        </div>

        <div class="px-4 py-3 bg-gray-50 border-t">
          <div class="flex justify-between text-sm">
            <div>
              <span class="text-gray-600">Батарея:</span>
              <span :class="lock.battery < 20 ? 'text-red-500' : ''">{{ lock.battery }}%</span>
            </div>
            <div>
              <span class="text-gray-600">Последнее действие:</span>
              <span>{{ formatDate(lock.lastActivity) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- История активности -->
    <div class="mt-8">
      <h2 class="text-xl font-semibold mb-4">История активности</h2>
      <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-4 py-3 text-left">Время</th>
                <th class="px-4 py-3 text-left">Замок</th>
                <th class="px-4 py-3 text-left">Событие</th>
                <th class="px-4 py-3 text-left">Пользователь</th>
                <th class="px-4 py-3 text-left">Метод</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="activity in lockHistory" :key="activity.id" class="border-t">
                <td class="px-4 py-3">{{ formatDate(activity.timestamp) }}</td>
                <td class="px-4 py-3">{{ activity.lockName }}</td>
                <td class="px-4 py-3" :class="getActivityClass(activity.action)">{{ activity.action }}</td>
                <td class="px-4 py-3">{{ activity.user }}</td>
                <td class="px-4 py-3">{{ activity.method }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref } from 'vue'

export default defineComponent({
  name: 'LocksView',
  
  setup() {
    // Список замков
    const locks = ref([
      {
        id: 1,
        name: 'Замок входной двери',
        location: 'Входная дверь',
        status: 'online',
        locked: true,
        battery: 85,
        lastActivity: new Date(Date.now() - 3600000)
      },
      {
        id: 2,
        name: 'Замок гаража',
        location: 'Гараж',
        status: 'online',
        locked: false,
        battery: 72,
        lastActivity: new Date(Date.now() - 7200000)
      },
      {
        id: 3,
        name: 'Замок черного входа',
        location: 'Черный вход',
        status: 'offline',
        locked: true,
        battery: 15,
        lastActivity: new Date(Date.now() - 86400000)
      }
    ])
    
    // История активности замков
    const lockHistory = ref([
      {
        id: 1,
        timestamp: new Date(Date.now() - 3600000),
        lockName: 'Замок входной двери',
        action: 'Заблокировано',
        user: 'Иван Петров',
        method: 'Приложение'
      },
      {
        id: 2,
        timestamp: new Date(Date.now() - 7200000),
        lockName: 'Замок гаража',
        action: 'Разблокировано',
        user: 'Система',
        method: 'Автоматически'
      },
      {
        id: 3,
        timestamp: new Date(Date.now() - 10800000),
        lockName: 'Замок входной двери',
        action: 'Разблокировано',
        user: 'Иван Петров',
        method: 'Ключ-карта'
      },
      {
        id: 4,
        timestamp: new Date(Date.now() - 86400000),
        lockName: 'Замок черного входа',
        action: 'Заблокировано',
        user: 'Анна Сидорова',
        method: 'Приложение'
      },
      {
        id: 5,
        timestamp: new Date(Date.now() - 172800000),
        lockName: 'Замок гаража',
        action: 'Ошибка доступа',
        user: 'Неизвестно',
        method: 'Код доступа'
      }
    ])
    
    // Форматирование даты
    const formatDate = (date) => {
      return new Date(date).toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    // Переключение состояния замка
    const toggleLock = (lock) => {
      lock.locked = !lock.locked
      lock.lastActivity = new Date()
      
      // Добавляем новую запись в историю
      lockHistory.value.unshift({
        id: lockHistory.value.length + 1,
        timestamp: new Date(),
        lockName: lock.name,
        action: lock.locked ? 'Заблокировано' : 'Разблокировано',
        user: 'Текущий пользователь',
        method: 'Приложение'
      })
    }
    
    // Показать историю для конкретного замка
    const showHistory = (lock) => {
      // В реальном приложении здесь бы фильтровались данные
      alert(`История для замка: ${lock.name}`)
    }
    
    // Получение класса для действия с замком
    const getActivityClass = (action) => {
      switch (action) {
        case 'Заблокировано':
          return 'text-red-600'
        case 'Разблокировано':
          return 'text-green-600'
        case 'Ошибка доступа':
          return 'text-yellow-600'
        default:
          return ''
      }
    }
    
    return {
      locks,
      lockHistory,
      formatDate,
      toggleLock,
      showHistory,
      getActivityClass
    }
  }
})
</script> 