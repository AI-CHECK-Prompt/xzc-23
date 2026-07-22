<template>
  <div class="page">
    <van-nav-bar title="历史申报与告警" left-arrow @click-left="$router.back()" />

    <van-tabs v-model:active="active">
      <van-tab title="申报记录">
        <van-cell-group inset>
          <van-cell
            v-for="d in declarations"
            :key="d.id"
            :title="d.declarationNo"
            :label="`${d.planSeaArea} / ${d.planDays}天 / ${d.status}`"
          />
        </van-cell-group>
      </van-tab>
      <van-tab title="告警事件">
        <van-cell-group inset>
          <van-cell
            v-for="a in alerts"
            :key="a.id"
            :title="a.alertType"
            :label="a.description"
            :value="a.status" />
        </van-cell-group>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const active = ref(0)
const declarations = ref([])
const alerts = ref([])

const load = async () => {
  const id = localStorage.getItem('vesselId')
  if (id) {
    const v = await request.get('/voyage/byVessel/' + id)
    if (v.code === 0) declarations.value = v.data
    const a = await request.get('/alert/byVessel/' + id)
    if (a.code === 0) alerts.value = a.data
  }
}

onMounted(load)
</script>
