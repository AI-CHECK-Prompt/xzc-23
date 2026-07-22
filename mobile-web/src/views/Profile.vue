<template>
  <div class="page">
    <van-nav-bar title="个人中心" left-arrow @click-left="$router.back()" />
    <div class="card">
      <van-cell-group inset>
        <van-cell title="船号" :value="form.vesselNo || '未选择'" />
        <van-cell title="船名" :value="form.vesselName" />
        <van-cell title="所属渔港" :value="form.portName" />
        <van-cell title="联系电话" :value="form.phone" />
        <van-cell title="证件有效期" :value="form.certValidTo" />
      </van-cell-group>
    </div>
    <div class="card">
      <van-button block @click="$router.push('/history')">历史申报与告警</van-button>
      <van-button block style="margin-top:8px;" @click="logout">切换船舶</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const form = ref({})

const load = async () => {
  const id = localStorage.getItem('vesselId')
  if (id) {
    const r = await request.get('/vessel/detail/' + id)
    if (r.code === 0) form.value = r.data
  }
}

const logout = () => {
  localStorage.removeItem('vesselId')
  localStorage.removeItem('vesselName')
  router.push('/home')
}

onMounted(load)
</script>
