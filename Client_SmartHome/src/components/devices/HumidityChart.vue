<template>
  <div class="humidity-chart">
    <Line
      :data="chartData"
      :options="chartOptions"
      :height="80"
    />
  </div>
</template>

<script>
import { ref, defineComponent, computed } from 'vue'
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler } from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

export default defineComponent({
  name: 'HumidityChart',
  components: { Line },
  props: {
    humidityData: {
      type: Array,
      default: () => []
    },
    color: {
      type: String,
      default: '#3B82F6'
    }
  },
  setup(props) {
    // Генерируем тестовые данные, если не переданы реальные
    const mockHumidityData = ref([
      { time: '00:00', value: 45 },
      { time: '06:00', value: 42 },
      { time: '12:00', value: 48 },
      { time: '18:00', value: 52 },
      { time: 'Сейчас', value: 50 }
    ])
    
    const chartData = computed(() => {
      const data = props.humidityData.length > 0 ? props.humidityData : mockHumidityData.value
      
      return {
        labels: data.map(item => item.time),
        datasets: [
          {
            label: 'Влажность (%)',
            data: data.map(item => item.value),
            borderColor: props.color,
            backgroundColor: `${props.color}20`, // 20 - прозрачность фона 0.2
            tension: 0.4, // сглаживание линии
            pointRadius: 3,
            pointBackgroundColor: props.color,
            pointBorderColor: '#fff',
            pointBorderWidth: 1,
            fill: true,
            borderWidth: 2
          }
        ]
      }
    })
    
    const chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          min: 20,
          max: 80,
          grid: {
            display: true,
            color: '#f1f5f9',
            drawBorder: false
          },
          ticks: {
            stepSize: 20,
            font: {
              size: 10
            }
          }
        },
        x: {
          grid: {
            display: false
          },
          ticks: {
            font: {
              size: 10
            }
          }
        }
      },
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          titleColor: '#334155',
          bodyColor: '#334155',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          cornerRadius: 8,
          padding: 10,
          usePointStyle: true,
          boxPadding: 6,
          boxWidth: 8,
          boxHeight: 8,
          displayColors: true
        }
      },
      interaction: {
        intersect: false,
        mode: 'index'
      }
    }
    
    return {
      chartData,
      chartOptions
    }
  }
})
</script>

<style scoped>
.humidity-chart {
  width: 100%;
  height: 80px;
  margin-top: 8px;
  margin-bottom: 4px;
}
</style> 