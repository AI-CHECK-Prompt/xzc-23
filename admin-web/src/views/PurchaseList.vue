<template>
  <el-card>
    <template #header><span>采购回传记录</span></template>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="船舶">
        <el-select v-model="vesselId" placeholder="选择船舶" filterable clearable>
          <el-option v-for="v in vessels" :key="v.id" :label="v.vesselNo" :value="v.id" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="vesselNo" label="船号" width="160" />
      <el-table-column prop="buyerName" label="采购商" width="160" />
      <el-table-column prop="species" label="采购品种" width="140" />
      <el-table-column prop="weight" label="采购重量(kg)" width="140" />
      <el-table-column prop="price" label="单价(元/kg)" width="120" />
      <el-table-column prop="amount" label="金额(元)" width="120" />
      <el-table-column prop="destination" label="目的地加工企业" width="240" />
      <el-table-column prop="purchaseTime" label="采购时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref([])
const vessels = ref([])
const vesselId = ref('')

const loadVessels = async () => {
  const r = await request.get('/vessel/list')
  if (r.code === 0) vessels.value = r.data
}

const load = async () => {
  if (!vesselId.value) return
  const r = await request.get('/purchase/byVessel/' + vesselId.value)
  if (r.code === 0) list.value = r.data
}

onMounted(loadVessels)
</script>
