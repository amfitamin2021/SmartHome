<template>
  <div class="p-6">
    <div class="mb-6">
      <h1 class="text-2xl font-bold">Профиль</h1>
      <p class="text-gray-500">Управление личными данными и настройками</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <!-- Боковая панель с фото и основной информацией -->
      <div class="md:col-span-1">
        <div class="bg-white rounded-xl shadow-sm p-6">
          <div class="flex flex-col items-center mb-4">
            <div class="w-32 h-32 overflow-hidden rounded-full mb-4 bg-gray-100 flex items-center justify-center">
              <img 
                v-if="profileImage" 
                :src="profileImage" 
                alt="Фото профиля" 
                class="w-full h-full object-cover"
              />
              <i v-else class="fas fa-user text-5xl text-gray-400"></i>
            </div>
            <h2 class="text-xl font-semibold">{{ user.name }}</h2>
            <p class="text-gray-500">{{ user.email }}</p>
            
            <button class="mt-4 px-4 py-2 bg-blue-50 text-blue-600 rounded-lg text-sm">
              Изменить фото
            </button>
          </div>
          
          <div class="border-t pt-4">
            <div class="flex items-center mb-3">
              <i class="fas fa-home text-gray-500 w-6"></i>
              <span>{{ user.homeCount }} {{ getHomesText }}</span>
            </div>
            <div class="flex items-center mb-3">
              <i class="fas fa-mobile-alt text-gray-500 w-6"></i>
              <span>{{ user.devicesCount }} {{ getDevicesText }}</span>
            </div>
            <div class="flex items-center">
              <i class="fas fa-calendar-alt text-gray-500 w-6"></i>
              <span>Дата регистрации: {{ formatDate(user.registrationDate) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Основная информация профиля -->
      <div class="md:col-span-3">
        <div class="bg-white rounded-xl shadow-sm p-6 mb-6">
          <h2 class="text-xl font-semibold mb-6">Личные данные</h2>
          
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm text-gray-600 mb-1">Имя</label>
              <input 
                type="text" 
                v-model="form.name" 
                class="w-full px-3 py-2 border rounded-lg"
              />
            </div>
            
            <div>
              <label class="block text-sm text-gray-600 mb-1">Фамилия</label>
              <input 
                type="text" 
                v-model="form.lastName" 
                class="w-full px-3 py-2 border rounded-lg"
              />
            </div>
            
            <div>
              <label class="block text-sm text-gray-600 mb-1">Email</label>
              <input 
                type="email" 
                v-model="form.email" 
                class="w-full px-3 py-2 border rounded-lg"
              />
            </div>
            
            <div>
              <label class="block text-sm text-gray-600 mb-1">Номер телефона</label>
              <input 
                type="tel" 
                v-model="form.phone" 
                class="w-full px-3 py-2 border rounded-lg"
              />
            </div>
            
            <div class="md:col-span-2">
              <label class="block text-sm text-gray-600 mb-1">Адрес</label>
              <input 
                type="text" 
                v-model="form.address" 
                class="w-full px-3 py-2 border rounded-lg"
              />
            </div>
          </div>
          
          <div class="mt-6">
            <button 
              class="px-4 py-2 bg-blue-600 text-white rounded-lg"
              @click="saveProfile"
            >
              Сохранить изменения
            </button>
          </div>
        </div>
        
        <!-- Доступ к дому -->
        <div class="bg-white rounded-xl shadow-sm p-6 mb-6">
          <h2 class="text-xl font-semibold mb-6">Доступ к дому</h2>
          
          <div class="mb-4">
            <label class="block text-sm text-gray-600 mb-1">Основной дом</label>
            <select v-model="form.primaryHome" class="w-full px-3 py-2 border rounded-lg bg-white">
              <option v-for="home in homes" :key="home.id" :value="home.id">
                {{ home.name }}
              </option>
            </select>
          </div>
          
          <div class="border-t pt-4">
            <h3 class="font-medium mb-4">Пользователи с доступом</h3>
            
            <div class="space-y-4">
              <div v-for="member in homeMembers" :key="member.id" class="flex justify-between items-center">
                <div class="flex items-center">
                  <div class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center mr-3">
                    <i v-if="!member.image" class="fas fa-user text-gray-400"></i>
                    <img v-else :src="member.image" class="w-full h-full object-cover rounded-full" />
                  </div>
                  <div>
                    <p class="font-medium">{{ member.name }}</p>
                    <p class="text-sm text-gray-500">{{ member.role }}</p>
                  </div>
                </div>
                
                <div>
                  <button 
                    v-if="member.id !== user.id"
                    class="text-red-500 hover:text-red-700"
                  >
                    <i class="fas fa-times"></i>
                  </button>
                </div>
              </div>
            </div>
            
            <button class="mt-4 flex items-center text-blue-600">
              <i class="fas fa-plus mr-2"></i> Добавить пользователя
            </button>
          </div>
        </div>
        
        <!-- Активные сессии -->
        <div class="bg-white rounded-xl shadow-sm p-6">
          <h2 class="text-xl font-semibold mb-6">Активные сессии</h2>
          
          <div class="space-y-4">
            <div v-for="(session, index) in sessions" :key="index" class="flex justify-between items-center pb-3 border-b">
              <div>
                <div class="flex items-center">
                  <i :class="getDeviceIcon(session.device)" class="text-gray-600 mr-3"></i>
                  <div>
                    <p class="font-medium">{{ session.device }}</p>
                    <p class="text-sm text-gray-500">{{ session.location }} • {{ formatDate(session.lastActivity) }}</p>
                  </div>
                </div>
              </div>
              
              <div>
                <span v-if="session.current" class="text-green-500 mr-4">Текущая</span>
                <button class="text-red-500 hover:text-red-700">
                  Завершить
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'

export default defineComponent({
  name: 'ProfileView',
  
  setup() {
    // Данные пользователя (в реальном приложении загружались бы из API)
    const user = ref({
      id: 1,
      name: 'Иван Иванов',
      email: 'ivan@example.com',
      homeCount: 1,
      devicesCount: 5,
      registrationDate: new Date(2023, 0, 15)
    })
    
    // URL изображения профиля
    const profileImage = ref(null)
    
    // Форма редактирования профиля
    const form = ref({
      name: 'Иван',
      lastName: 'Иванов',
      email: 'ivan@example.com',
      phone: '+7 (999) 123-45-67',
      address: 'г. Москва, ул. Примерная, д. 1, кв. 123',
      primaryHome: 1
    })
    
    // Список домов пользователя
    const homes = ref([
      { id: 1, name: 'Квартира' },
      { id: 2, name: 'Дача' }
    ])
    
    // Список пользователей с доступом к основному дому
    const homeMembers = ref([
      { id: 1, name: 'Иван Иванов', role: 'Владелец', image: null },
      { id: 2, name: 'Елена Иванова', role: 'Член семьи', image: null }
    ])
    
    // Активные сессии
    const sessions = ref([
      { 
        device: 'Chrome на Windows',
        location: 'Москва, Россия',
        lastActivity: new Date(),
        current: true
      },
      { 
        device: 'Safari на iPhone',
        location: 'Москва, Россия',
        lastActivity: new Date(Date.now() - 86400000 * 2)
      },
      { 
        device: 'Firefox на Mac',
        location: 'Санкт-Петербург, Россия',
        lastActivity: new Date(Date.now() - 86400000 * 5)
      }
    ])
    
    // Множественное число для домов и устройств
    const getHomesText = computed(() => {
      const count = user.value.homeCount
      if (count === 1) return 'дом'
      if (count >= 2 && count <= 4) return 'дома'
      return 'домов'
    })
    
    const getDevicesText = computed(() => {
      const count = user.value.devicesCount
      if (count === 1) return 'устройство'
      if (count >= 2 && count <= 4) return 'устройства'
      return 'устройств'
    })
    
    // Форматирование даты
    const formatDate = (date) => {
      return new Date(date).toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    }
    
    // Получение иконки для устройства
    const getDeviceIcon = (deviceName) => {
      if (deviceName.includes('Chrome')) return 'fab fa-chrome'
      if (deviceName.includes('Safari')) return 'fab fa-safari'
      if (deviceName.includes('Firefox')) return 'fab fa-firefox'
      if (deviceName.includes('iPhone') || deviceName.includes('iPad')) return 'fab fa-apple'
      if (deviceName.includes('Android')) return 'fab fa-android'
      if (deviceName.includes('Windows')) return 'fab fa-windows'
      if (deviceName.includes('Mac')) return 'fab fa-apple'
      return 'fas fa-desktop'
    }
    
    // Сохранение профиля
    const saveProfile = () => {
      // В реальном приложении здесь был бы API-запрос
      alert('Профиль сохранен')
    }
    
    onMounted(() => {
      // В реальном приложении здесь загружались бы данные пользователя
    })
    
    return {
      user,
      profileImage,
      form,
      homes,
      homeMembers,
      sessions,
      getHomesText,
      getDevicesText,
      formatDate,
      getDeviceIcon,
      saveProfile
    }
  }
})
</script> 