<template>
  <el-row :gutter="20">
    <el-col :span="16">
      <el-card>
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span>渔获价格指数（按海区/品种/规格/季节/周期）</span>
            <div>
              <el-button type="primary" :loading="calculating" @click="triggerLatest">触发最新一期计算</el-button>
              <el-button @click="load">刷新</el-button>
            </div>
          </div>
        </template>
        <el-form :inline="true" style="margin-bottom: 12px">
          <el-form-item label="海区">
            <el-input v-model="filter.seaArea" placeholder="可选" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="品种">
            <el-input v-model="filter.species" placeholder="可选" clearable style="width: 120px" />
          </el-form-item>
          <el-form-item label="规格">
            <el-select v-model="filter.specification" placeholder="全部" clearable style="width: 100px">
              <el-option label="小" value="小" />
              <el-option label="中" value="中" />
              <el-option label="大" value="大" />
            </el-select>
          </el-form-item>
          <el-form-item label="周期">
            <el-select v-model="filter.periodType" style="width: 100px" @change="load">
              <el-option label="日" value="DAY" />
              <el-option label="周" value="WEEK" />
              <el-option label="月" value="MONTH" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="load">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="filteredList" border stripe height="460">
          <el-table-column prop="periodKey" label="周期" width="110" />
          <el-table-column prop="seaArea" label="海区" width="150" />
          <el-table-column prop="species" label="品种" width="100" />
          <el-table-column prop="specification" label="规格" width="80" />
          <el-table-column prop="season" label="季节" width="80" />
          <el-table-column prop="median" label="中位数(P50)" width="120" />
          <el-table-column prop="p25" label="P25" width="90" />
          <el-table-column prop="p75" label="P75" width="90" />
          <el-table-column prop="p5" label="P5" width="80" />
          <el-table-column prop="p95" label="P95" width="80" />
          <el-table-column prop="sampleSize" label="样本数" width="80" />
          <el-table-column prop="anomalyFiltered" label="剔除数" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.anomalyFiltered > 0" type="warning">{{ row.anomalyFiltered }}</el-tag>
              <span v-else>0</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card>
        <template #header><span>趋势图（点击表格行查看）</span></template>
        <div ref="chartDom" style="width:100%;height:480px"></div>
        <div v-if="!trendData.length" style="color:#999;text-align:center;padding:20px">
          请从左侧表格选择一条记录
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const list = ref([])
const filter = ref({ seaArea: '', species: '', specification: '', periodType: 'DAY' })
const calculating = ref(false)
const chartDom = ref(null)
const trendData = ref([])
let chart = null

const filteredList = computed(() => list.value)

const load = async () => {
  const r = await request.get('/price-index/query', { params: filter.value })
  if (r.code === 0) list.value = r.data
  else ElMessage.error(r.message)
}

const triggerLatest = async () => {
  calculating.value = true
  try {
    const r = await request.post('/price-index/calculate')
    if (r.code === 0) {
      const summary = r.data || {}
      const total = Object.values(summary).reduce((a, b) => a + b, 0)
      ElMessage.success(`已计算 ${Object.keys(summary).length} 个周期，共 ${total} 条指数记录`)
      load()
    } else {
      ElMessage.error(r.message)
    }
  } finally {
    calculating.value = false
  }
}

const showTrend = async (row) => {
  if (!row) return
  const r = await request.get('/price-index/trend', {
    params: {
      seaArea: row.seaArea,
      species: row.species,
      specification: row.specification,
      season: row.season,
      periodType: row.periodType
    }
  })
  if (r.code !== 0) { ElMessage.error(r.message); return }
  trendData.value = r.data || []
  await nextTick()
  renderChart()
}

const renderChart = () => {
  if (!chartDom.value) return
  if (!chart) chart = echarts.init(chartDom.value)
  const xs = trendData.value.map(x => x.periodKey)
  const medians = trendData.value.map(x => x.median)
  const p25 = trendData.value.map(x => x.p25)
  const p75 = trendData.value.map(x => x.p75)
  chart.setOption({
    title: { text: '价格指数趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['P50 中位数', 'P25', 'P75'], top: 30 },
    xAxis: { type: 'category', data: xs },
    yAxis: { type: 'value', name: '元/kg' },
    series: [
      { name: 'P25', type: 'line', data: p25, smooth: true, lineStyle: { type: 'dashed' } },
      { name: 'P50 中位数', type: 'line', data: medians, smooth: true, symbolSize: 8 },
      { name: 'P75', type: 'line', data: p75, smooth: true, lineStyle: { type: 'dashed' } }
    ]
  })
}

// 行点击事件
const onRowClick = (row) => showTrend(row)

onMounted(() => {
  load()
})
</script>

<style scoped>
.el-table :deep(.el-table__row) { cursor: pointer; }
</style>
