import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import './index.css'

// Импорт для Three.js
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'
// Глобальное добавление OrbitControls, чтобы избежать проблем с импортом
window.OrbitControls = OrbitControls

// Создание хранилища Pinia
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// Создание и монтирование приложения
const app = createApp(App)
app.use(router)
app.use(pinia)
app.mount('#app')
