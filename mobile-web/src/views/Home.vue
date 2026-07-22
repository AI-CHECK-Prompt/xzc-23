<template>
  <div class="page">
    <van-nav-bar title="渔船移动端" />
    <div class="card">
      <h3>欢迎，船东船长</h3>
      <p>当前角色：船东船长 / {{ vesselName || '请选择船舶' }}</p>
      <van-button type="primary" block @click="loadVessels">刷新船舶列表</van-button>
    </div>

    <div class="card">
      <h4>今日作业概览</h4>
      <van-cell-group inset>
        <van-cell title="在港船舶" :value="counts.atPort + ' 艘'" />
        <van-cell title="出海作业" :value="counts.atSea + ' 艘'" />
        <van-cell title="今日申报" :value="counts.declarations + ' 单'" />
        <van-cell title="待处理告警" :value="counts.alerts + ' 条'" is-link @click="goAlerts" />
      </van-cell-group>
    </div>

    <div class="card">
      <h4>我的船舶</h4>
      <van-cell-group inset>
        <van-cell
          v-for="v in vessels"
          :key="v.id"
          :title="v.vesselName"
          :label="v.vesselNo + ' / ' + v.portName"
          is-link
          @click="selectVessel(v)" />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const vessels = ref([])
const vesselName = ref(localStorage.getItem('vesselName') || '')
const counts = ref({ atPort: 0, atSea: 0, declarations: 0, alerts: 0 })

const loadVessels = async () => {
  const r = await request.get('/vessel/list')
  if (r.code === 0) {
    vessels.value = r.data
    counts.value.atPort = r.data.filter(v => v.status === '在港').length
    counts.value.atSea = r.data.filter(v => v.status === '已出港').length
  }
}

const selectVessel = (v) => {
  localStorage.setItem('vesselId', v.id)
  localStorage.setItem('vesselName', v.vesselName)
  vesselName.value = v.vesselName
  router.push('/declare')
}

const goAlerts = () => {
  router.push('/history')
}

onMounted(async () => {
  await loadVessels()
  const a = await request.get('/alert/pending')
  if (a.code === 0) counts.value.alerts = a.data.length
  const d = await request.get('/voyage/list')
  if (d.code === 0) counts.value.declarations = d.data.length
})
</script>
