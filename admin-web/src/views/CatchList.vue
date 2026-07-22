<template>
  <el-card>
    <template #header><span>渔获与过磅</span></template>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="航次编号">
        <el-input v-model="voyageNo" placeholder="输入航次申报单号" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadByVoyage">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border stripe>
      <el-table-column prop="declarationNo" label="预申报单号" width="240" />
      <el-table-column prop="vesselNo" label="船号" width="160" />
      <el-table-column prop="portName" label="渔港" width="160" />
      <el-table-column prop="estimatedTotal" label="预估总重(kg)" width="140" />
      <el-table-column prop="actualTotal" label="实际过磅(kg)" width="140" />
      <el-table-column prop="deviationRatio" label="偏差(%)" width="120" />
      <el-table-column prop="status" label="状态" width="160">
        <template #default="{ row }">
          <el-tag :type="row.status === '偏差复核中' ? 'danger' : 'success'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="openWeigh(row)" v-if="row.status === '预申报已提交'">过磅确认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="过磅确认" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="实际过磅(kg)"><el-input-number v-model="form.actualTotal" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="复核人"><el-input v-model="form.operator" /></el-form-item>
        <el-form-item label="偏差原因"><el-input v-model="form.reason" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="confirm">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref([])
const voyageNo = ref('')
const visible = ref(false)
const form = ref({})

const loadByVoyage = async () => {
  if (!voyageNo.value) { ElMessage.warning('请输入航次编号'); return }
  // 简化查询：直接获取所有然后过滤
  const r = await request.get('/catch/byVoyage/' + voyageNo.value)
  if (r.code === 0) list.value = r.data
}

const openWeigh = (row) => {
  form.value = { id: row.id, actualTotal: row.estimatedTotal || 0, operator: '渔港管理员' }
  visible.value = true
}

const confirm = async () => {
  const r = await request.post('/catch/confirmWeigh/' + form.value.id, form.value)
  if (r.code === 0) {
    ElMessage.success('已确认')
    visible.value = false
    loadByVoyage()
  } else {
    ElMessage.error(r.message)
  }
}
</script>
