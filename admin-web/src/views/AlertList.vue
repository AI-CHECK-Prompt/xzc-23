<template>
  <el-card>
    <template #header><span>异常告警事件</span></template>
    <el-table :data="list" border stripe>
      <el-table-column prop="vesselNo" label="船号" width="160" />
      <el-table-column prop="alertType" label="告警类型" width="160" />
      <el-table-column prop="level" label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="row.level === 'danger' ? 'danger' : 'warning'">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="handle(row)" v-if="row.status === '待处理'">处置</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])

const load = async () => {
  const r = await request.get('/alert/pending')
  if (r.code === 0) list.value = r.data
}

const handle = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入处置结果', '处置告警', { confirmButtonText: '提交' })
    const r = await request.post('/alert/handle/' + row.id, { handler: '渔港管理员', result: value })
    if (r.code === 0) { ElMessage.success('已处置'); load() }
  } catch (_) {}
}

onMounted(load)
</script>
