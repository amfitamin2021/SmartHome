<template>
  <div>
    <div class="mb-6 flex justify-between items-center">
      <h2 class="text-xl font-semibold">Камеры видеонаблюдения</h2>
      <div class="flex gap-3">
        <button class="px-4 py-2 bg-blue-600 text-white rounded-lg flex items-center">
          <i class="fas fa-plus mr-2"></i>Добавить камеру
        </button>
      </div>
    </div>

    <!-- Сетка камер -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 mb-8">
      <div v-for="camera in cameras" :key="camera.id" class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="relative">
          <img :src="camera.imageUrl" alt="Camera feed" class="w-full h-48 object-cover">
          
          <!-- Статус записи -->
          <div v-if="camera.recording" class="absolute top-4 left-4 bg-red-500 text-white rounded-full px-2 py-1 text-xs flex items-center">
            <span class="animate-pulse h-2 w-2 rounded-full bg-white mr-1"></span>
            REC
          </div>
          
          <!-- Кнопки управления камерой -->
          <div class="absolute bottom-4 right-4 flex gap-2">
            <button class="h-8 w-8 rounded-full bg-gray-800 bg-opacity-70 text-white flex items-center justify-center hover:bg-opacity-100">
              <i class="fas fa-arrows-alt"></i>
            </button>
            <button class="h-8 w-8 rounded-full bg-gray-800 bg-opacity-70 text-white flex items-center justify-center hover:bg-opacity-100">
              <i class="fas fa-video"></i>
            </button>
            <button class="h-8 w-8 rounded-full bg-gray-800 bg-opacity-70 text-white flex items-center justify-center hover:bg-opacity-100">
              <i class="fas fa-expand"></i>
            </button>
          </div>
        </div>
        
        <div class="p-4">
          <div class="flex justify-between items-center mb-2">
            <h3 class="font-medium">{{ camera.name }}</h3>
            <span :class="camera.online ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'" class="px-2 py-1 rounded-full text-xs">
              {{ camera.online ? 'Онлайн' : 'Офлайн' }}
            </span>
          </div>
          
          <p class="text-sm text-gray-600 mb-3">{{ camera.location }}</p>
          
          <div class="flex justify-between items-center text-sm">
            <div class="flex items-center text-gray-700">
              <i class="fas fa-calendar-alt mr-1"></i>
              {{ formatDate(camera.lastActivity) }}
            </div>
            
            <button @click="toggleCamera(camera)" :class="camera.active ? 'text-blue-600' : 'text-gray-400'" class="flex items-center">
              <i :class="camera.active ? 'fas fa-toggle-on text-lg' : 'fas fa-toggle-off text-lg'"></i>
              <span class="ml-1">{{ camera.active ? 'Активна' : 'Отключена' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Архив видео -->
    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
      <div class="px-6 py-4 border-b">
        <h3 class="text-lg font-medium">Архив видео</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-sm font-medium text-gray-500">Дата</th>
              <th class="px-6 py-3 text-left text-sm font-medium text-gray-500">Камера</th>
              <th class="px-6 py-3 text-left text-sm font-medium text-gray-500">Триггер</th>
              <th class="px-6 py-3 text-left text-sm font-medium text-gray-500">Длительность</th>
              <th class="px-6 py-3 text-left text-sm font-medium text-gray-500">Действия</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="recording in recordings" :key="recording.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 text-sm text-gray-700">{{ formatDate(recording.date) }}</td>
              <td class="px-6 py-4 text-sm text-gray-700">{{ recording.camera }}</td>
              <td class="px-6 py-4 text-sm text-gray-700">{{ recording.trigger }}</td>
              <td class="px-6 py-4 text-sm text-gray-700">{{ recording.duration }}</td>
              <td class="px-6 py-4 text-sm flex space-x-2">
                <button class="text-blue-600 hover:text-blue-800">
                  <i class="fas fa-play-circle"></i>
                </button>
                <button class="text-blue-600 hover:text-blue-800">
                  <i class="fas fa-download"></i>
                </button>
                <button class="text-red-600 hover:text-red-800">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="px-6 py-4 bg-gray-50 flex justify-between items-center">
        <button class="text-sm text-blue-600 hover:text-blue-800">
          Показать все записи
        </button>
        <div class="flex items-center space-x-2">
          <button class="px-3 py-1 border rounded-md text-sm">Назад</button>
          <span class="text-sm text-gray-700">Страница 1 из 3</span>
          <button class="px-3 py-1 border rounded-md text-sm">Далее</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref } from 'vue'

export default defineComponent({
  name: 'CamerasView',
  
  setup() {
    // Список камер
    const cameras = ref([
      {
        id: 1,
        name: 'Входная дверь',
        location: 'Прихожая',
        imageUrl: 'https://images.unsplash.com/photo-1600573472550-8090b5e0745e?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1170&q=80',
        online: true,
        active: true,
        recording: true,
        lastActivity: new Date(Date.now() - 300000)
      },
      {
        id: 2,
        name: 'Гостиная',
        location: 'Основной зал',
        imageUrl: 'https://images.unsplash.com/photo-1589834390005-5d4fb9bf3d32?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1074&q=80',
        online: true,
        active: true,
        recording: false,
        lastActivity: new Date(Date.now() - 1800000)
      },
      {
        id: 3,
        name: 'Задний двор',
        location: 'Улица',
        imageUrl: 'https://images.unsplash.com/photo-1594312180721-3b5217cfc65f?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1170&q=80',
        online: true,
        active: true,
        recording: false,
        lastActivity: new Date(Date.now() - 3600000)
      },
      {
        id: 4,
        name: 'Гараж',
        location: 'Технические помещения',
        imageUrl: 'https://images.unsplash.com/photo-1611288803139-5422c7c57a42?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1074&q=80',
        online: false,
        active: false,
        recording: false,
        lastActivity: new Date(Date.now() - 86400000)
      }
    ])
    
    // Архив записей
    const recordings = ref([
      {
        id: 1,
        date: new Date(Date.now() - 3600000),
        camera: 'Входная дверь',
        trigger: 'Движение',
        duration: '00:02:15'
      },
      {
        id: 2,
        date: new Date(Date.now() - 7200000),
        camera: 'Гостиная',
        trigger: 'Ручная запись',
        duration: '00:05:32'
      },
      {
        id: 3,
        date: new Date(Date.now() - 86400000),
        camera: 'Задний двор',
        trigger: 'Расписание',
        duration: '00:10:00'
      },
      {
        id: 4,
        date: new Date(Date.now() - 172800000),
        camera: 'Входная дверь',
        trigger: 'Движение',
        duration: '00:01:45'
      }
    ])
    
    // Форматирование даты
    const formatDate = (date) => {
      return new Date(date).toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    // Переключение активного состояния камеры
    const toggleCamera = (camera) => {
      camera.active = !camera.active
      // Здесь будет логика обновления камеры в API
    }
    
    return {
      cameras,
      recordings,
      formatDate,
      toggleCamera
    }
  }
})
</script> 