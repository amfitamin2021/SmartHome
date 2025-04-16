<script>
import Sidebar from './components/layout/Sidebar.vue'
import Header from './components/layout/Header.vue'

export default {
  name: 'App',
  components: {
    Sidebar,
    Header
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex" id="smart-home-app">
    <!-- Боковое меню -->
    <Sidebar />
    
    <!-- Основной контент -->
    <main id="main-content" class="lg:ml-56 min-h-screen flex-1">
      <Header />
      <router-view />
    </main>
  </div>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

body {
  font-family: 'Inter', sans-serif;
  margin: 0;
  padding: 0;
}

#app {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

#main-content {
  min-height: 100vh;
  background-color: #f7f7f7;
}

/* Мобильная адаптация */
@media (max-width: 1024px) {
  #main-content {
    margin-left: 0;
  }
}
</style>
