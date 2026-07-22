<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span>出海申报</span>
        <el-button type="primary" @click="openDialog()">新增申报</el-button>
      </div>
    </template>
    <el-table :data="list" border stripe>
      <el-table-column prop="declarationNo" label="申报单号" width="260" />
      <el-table-column prop="vesselNo" label="船号" width="160" />
      <el-table-column prop="vesselName" label="船名" width="160" />
      <el-table-column prop="ownerName" label="船东" width="120" />
      <el-table-column prop="portName" label="渔港" width="160" />
      <el-table-column prop="planSeaArea" label="计划海域" width="160" />
      <el-table-column prop="planDays" label="计划天数" width="100" />
      <el-table-column prop="planMethod" label="作业方式" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" v-if="row.status === '已申报'" @click="depart(row)">出港</el-button>
          <el-button size="small" v-if="row.status === '已出港'" @click="ret(row)">归港</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="新增出海申报" width="700px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="选择船舶">
          <el-select v-model="form.vesselId" placeholder="选择船舶" filterable>
            <el-option v-for="v in vessels" :key="v.id" :label="v.vesselNo + ' / ' + v.vesselName" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业人员"><el-input v-model="form.crewListJson" type="textarea" placeholder='例如：[{"name":"张三","idNo":"..."}]' /></el-form-item>
        <el-form-item label="计划海域"><el-input v-model="form.planSeaArea" placeholder="例如：闽南近海渔区-外海" /></el-form-item>
        <el-form-item label="计划天数"><el-input-number v-model="form.planDays" :min="1" :max="60" /></el-form-item>
        <el-form-item label="作业方式">
          <el-select v-model="form.planMethod" placeholder="请选择">
            <el-option label="刺网" value="刺网" />
            <el-option label="围网" value="围网" />
            <el-option label="拖网" value="拖网" />
            <el-option label="钓具" value="钓具" />
          </el-select>
        </el-form-item>
        <el-form-item label="网具规格"><el-input v-model="form.netSpec" placeholder="例如：刺网 网目 50mm 长 800m" /></el-form-item>
        <el-form-item label="计划出港时间"><el-date-picker v-model="form.planDepartureTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">提交申报</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const vessels = ref([])
const visible = ref(false)
const form = ref({ planDays: 5, planMethod: '刺网' })

const statusType = (s) => {
  if (s === '已申报') return 'info'
  if (s === '已出港') return 'warning'
  if (s === '已归港') return 'success'
  return ''
}

const load = async () => {
  const r = await request.get('/voyage/list')
  if (r.code === 0) list.value = r.data
}

const loadVessels = async () => {
  const r = await request.get('/vessel/list')
  if (r.code === 0) vessels.value = r.data
}

const openDialog = () => {
  form.value = { planDays: 5, planMethod: '刺网' }
  visible.value = true
  loadVessels()
}

const submit = async () => {
  const r = await request.post('/voyage/submit', form.value)
  if (r.code === 0) {
    ElMessage.success('申报成功')
    visible.value = false
    load()
  } else {
    ElMessageBox.alert(r.message, '申报失败')
  }
}

const depart = async (row) => {
  const r = await request.post('/voyage/depart/' + row.id)
  if (r.code === 0) { ElMessage.success('已出港'); load() }
}

const ret = async (row) => {
  const r = await request.post('/voyage/return/' + row.id)
  if (r.code === 0) { ElMessage.success('已归港'); load() }
}

onMounted(load)
</script>
