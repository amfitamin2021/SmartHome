<template>
  <div class="bg-white p-6 rounded-xl shadow-sm">
    <div class="flex justify-between items-center mb-6">
      <h3 class="text-lg font-semibold">Безопасность</h3>
      <i :class="['text-green-600', securityStatus.icon]"></i>
    </div>
    <div class="space-y-4">
      <div 
        class="flex items-center gap-3 p-3 rounded-lg"
        :class="securityStatus.class"
      >
        <i :class="securityStatus.indicatorIcon"></i>
        <span>{{ securityStatus.text }}</span>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <button 
          class="p-3 rounded-lg bg-gray-50 text-center hover:bg-gray-100"
          @click="showCameras"
        >
          <i class="mb-2 fas fa-camera"></i>
          <span class="block text-sm">Камеры</span>
        </button>
        <button 
          class="p-3 rounded-lg bg-gray-50 text-center hover:bg-gray-100"
          @click="manageLocks"
        >
          <i class="mb-2 fas fa-door-closed"></i>
          <span class="block text-sm">Замки</span>
        </button>
      </div>
      
      <div class="flex items-center justify-between pt-2 mt-2 border-t border-gray-100">
        <span>Режим охраны</span>
        <label class="relative inline-flex items-center cursor-pointer">
          <input 
            type="checkbox" 
            class="sr-only peer" 
            v-model="securityEnabled"
            @change="toggleSecurity"
          >
          <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
        </label>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SecurityWidget',
  data() {
    return {
      securityEnabled: true,
      securityMode: 'active' // 'active', 'alarm', 'disabled'
    }
  },
  computed: {
    securityStatus() {
      if (this.securityMode === 'active') {
        return {
          text: 'Система активна',
          class: 'bg-green-50 text-green-700',
          icon: 'fas fa-shield-halved',
          indicatorIcon: 'fas fa-circle-check'
        }
      } else if (this.securityMode === 'alarm') {
        return {
          text: 'Тревога!',
          class: 'bg-red-50 text-red-700',
          icon: 'fas fa-shield-halved',
          indicatorIcon: 'fas fa-triangle-exclamation'
        }
      } else {
        return {
          text: 'Система отключена',
          class: 'bg-gray-50 text-gray-700',
          icon: 'fas fa-shield',
          indicatorIcon: 'fas fa-circle-info'
        }
      }
    }
  },
  methods: {
    toggleSecurity() {
      // В реальном приложении здесь будет вызов API
      console.log(`Режим охраны: ${this.securityEnabled ? 'включен' : 'выключен'}`)
      this.securityMode = this.securityEnabled ? 'active' : 'disabled'
    },
    showCameras() {
      // Переход на страницу камер
      this.$router.push('/security/cameras')
    },
    manageLocks() {
      // Переход на страницу управления замками
      this.$router.push('/security/locks')
    },
    fetchSecurityData() {
      // Здесь будет вызов API для получения состояния системы безопасности
      console.log('Загрузка данных о безопасности')
    }
  },
  mounted() {
    this.fetchSecurityData()
  }
}
</script> 