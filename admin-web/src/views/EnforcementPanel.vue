<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span>执法办案 - 电子违规告知书</span>
        <el-button type="primary" @click="openDialog()">开具告知书</el-button>
      </div>
    </template>
    <el-table :data="list" border stripe>
      <el-table-column prop="noticeNo" label="告知书编号" width="240" />
      <el-table-column prop="vesselNo" label="船号" width="160" />
      <el-table-column prop="voyageId" label="关联航次" width="240" />
      <el-table-column prop="violationType" label="违规类型" width="140" />
      <el-table-column prop="description" label="违规描述" />
      <el-table-column prop="quotaDeducted" label="扣减配额(kg)" width="140" />
      <el-table-column prop="officerName" label="执法人员" width="120" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="issueTime" label="开具时间" width="180" />
    </el-table>

    <el-dialog v-model="visible" title="开具电子违规告知书" width="700px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="船舶">
          <el-select v-model="form.vesselId" placeholder="选择船舶" filterable>
            <el-option v-for="v in vessels" :key="v.id" :label="v.vesselNo + ' / ' + v.vesselName" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联航次">
          <el-select v-model="form.voyageId" placeholder="选择航次（可空）" filterable clearable>
            <el-option v-for="v in voyages" :key="v.id" :label="v.declarationNo" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规类型">
          <el-select v-model="form.violationType">
            <el-option label="越界作业" value="越界作业" />
            <el-option label="禁渔期捕捞" value="禁渔期捕捞" />
            <el-option label="幼鱼比例超标" value="幼鱼比例超标" />
            <el-option label="未开启船位终端" value="未开启船位终端" />
            <el-option label="虚假申报" value="虚假申报" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="扣减配额(kg)"><el-input-number v-model="form.quotaDeducted" :min="0" /></el-form-item>
        <el-form-item label="执法人员"><el-input v-model="form.officerName" placeholder="海警/渔政办案人员" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">开具</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref([])
const vessels = ref([])
const voyages = ref([])
const visible = ref(false)
const form = ref({ violationType: '越界作业' })

const load = async () => {
  const r = await request.get('/enforcement/list')
  if (r.code === 0) list.value = r.data
}

const openDialog = async () => {
  form.value = { violationType: '越界作业' }
  visible.value = true
  const v = await request.get('/vessel/list')
  if (v.code === 0) vessels.value = v.data
  const vo = await request.get('/voyage/list')
  if (vo.code === 0) voyages.value = vo.data
}

const submit = async () => {
  const r = await request.post('/enforcement/issue', form.value)
  if (r.code === 0) {
    ElMessage.success('已开具')
    visible.value = false
    load()
    // 自动应用配额扣减
    if (form.value.quotaDeducted && form.value.quotaDeducted > 0) {
      await request.post('/enforcement/applyQuotaDeduct/' + r.data.id, { deducted: form.value.quotaDeducted })
      ElMessage.success('已联动扣减配额')
    }
  }
}

onMounted(load)
</script>
