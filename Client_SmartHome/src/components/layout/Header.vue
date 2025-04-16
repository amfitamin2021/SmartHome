<template>
  <header class="bg-white shadow-sm p-3 flex justify-between items-center">
    <h1 class="text-xl font-medium">Мой Дом</h1>
    
    <div class="flex items-center space-x-3">
      <div class="flex items-center space-x-2">
        <button class="notification-btn relative">
          <i class="fas fa-bell text-gray-600"></i>
        </button>
        <button class="dark-mode-toggle">
          <i class="fas fa-moon text-gray-600"></i>
        </button>
        <div class="w-8 h-8 rounded-full overflow-hidden bg-gray-200">
          <img src="https://ui-avatars.com/api/?name=User&background=random" alt="User" class="w-full h-full object-cover" />
        </div>
      </div>
    </div>
  </header>
  
  <div class="bg-white shadow-sm px-3 py-1 flex items-center space-x-3">
    <button class="px-3 py-1 rounded-md flex items-center text-red-600 hover:bg-red-50">
      <i class="fas fa-power-off mr-2"></i>
      <span>Выключить всё</span>
    </button>
    
    <button class="px-3 py-1 rounded-md flex items-center text-green-600 hover:bg-green-50">
      <i class="fas fa-leaf mr-2"></i>
      <span>Эко-режим</span>
    </button>
    
    <button class="px-3 py-1 rounded-md flex items-center text-blue-600 hover:bg-blue-50">
      <i class="fas fa-shield-alt mr-2"></i>
      <span>Охрана</span>
    </button>
  </div>
</template>

<script>
export default {
  name: 'Header'
}
</script> 