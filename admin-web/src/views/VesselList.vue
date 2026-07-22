<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span>船舶档案</span>
        <el-button type="primary" @click="openDialog()">新增船舶</el-button>
      </div>
    </template>
    <el-table :data="list" border stripe>
      <el-table-column prop="vesselNo" label="船号" width="160" />
      <el-table-column prop="vesselName" label="船名" width="180" />
      <el-table-column prop="ownerName" label="船东" width="120" />
      <el-table-column prop="captainName" label="船长" width="100" />
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column prop="portName" label="所属渔港" width="180" />
      <el-table-column prop="seaAreaName" label="作业海区" width="160" />
      <el-table-column prop="certValidTo" label="证件有效期至" width="140" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑船舶' : '新增船舶'" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="船号"><el-input v-model="form.vesselNo" /></el-form-item>
        <el-form-item label="船名"><el-input v-model="form.vesselName" /></el-form-item>
        <el-form-item label="船东"><el-input v-model="form.ownerName" /></el-form-item>
        <el-form-item label="船长"><el-input v-model="form.captainName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="所属渔港"><el-input v-model="form.portName" placeholder="例如：泉州祥芝渔港" /></el-form-item>
        <el-form-item label="作业海区"><el-input v-model="form.seaAreaName" placeholder="例如：闽南近海渔区" /></el-form-item>
        <el-form-item label="证件有效期"><el-date-picker v-model="form.certValidTo" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="是否停业整改">
          <el-switch v-model="form.suspended" />
        </el-form-item>
        <el-form-item label="停业截止"><el-date-picker v-model="form.suspendUntil" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref([])
const visible = ref(false)
const form = ref({})

const load = async () => {
  const r = await request.get('/vessel/list')
  if (r.code === 0) list.value = r.data
}

const openDialog = (row) => {
  form.value = row ? { ...row } : { status: '在港' }
  visible.value = true
}

const save = async () => {
  const r = await request.post('/vessel/save', form.value)
  if (r.code === 0) {
    ElMessage.success('保存成功')
    visible.value = false
    load()
  } else {
    ElMessage.error(r.message)
  }
}

onMounted(load)
</script>
