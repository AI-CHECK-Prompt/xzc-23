<template>
  <el-row :gutter="20">
    <el-col :span="14">
      <el-card>
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span>配额规则（按海区/品种/年度）</span>
            <el-button type="primary" @click="openRule()">新增规则</el-button>
          </div>
        </template>
        <el-table :data="rules" border stripe>
          <el-table-column prop="year" label="年度" width="80" />
          <el-table-column prop="seaAreaName" label="海区" width="160" />
          <el-table-column prop="species" label="品种" width="120" />
          <el-table-column prop="totalQuota" label="年度总量(kg)" width="140" />
          <el-table-column prop="minSize" label="最小规格" width="160" />
          <el-table-column prop="banned" label="禁渔" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.banned" type="danger">禁渔</el-tag>
              <el-tag v-else type="success">开放</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="10">
      <el-card>
        <template #header><span>船东配额查询</span></template>
        <el-form label-width="80px">
          <el-form-item label="船东">
            <el-input v-model="query.owner" placeholder="例如：船东1-1" />
          </el-form-item>
          <el-form-item label="渔港">
            <el-input v-model="query.portName" placeholder="可选" />
          </el-form-item>
          <el-form-item label="年度">
            <el-input-number v-model="query.year" :min="2020" :max="2099" />
          </el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form>
        <div v-if="result.owner">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="船东">{{ result.owner }}</el-descriptions-item>
            <el-descriptions-item label="渔港">{{ result.portName }}</el-descriptions-item>
            <el-descriptions-item label="年度">{{ result.year }}</el-descriptions-item>
            <el-descriptions-item label="累计渔获(kg)">{{ result.totalUsed }}</el-descriptions-item>
            <el-descriptions-item label="违规扣减(kg)">{{ result.totalDeduct }}</el-descriptions-item>
            <el-descriptions-item label="配额总量(kg)">{{ result.totalQuota }}</el-descriptions-item>
            <el-descriptions-item label="剩余配额(kg)">{{ result.totalRemaining }}</el-descriptions-item>
          </el-descriptions>
          <h4>分品种配额</h4>
          <el-table :data="result.rules" border size="small">
            <el-table-column prop="species" label="品种" width="100" />
            <el-table-column prop="seaArea" label="海区" width="140" />
            <el-table-column prop="totalQuota" label="总量" width="100" />
            <el-table-column prop="used" label="已用" width="100" />
            <el-table-column prop="remaining" label="剩余" width="100" />
            <el-table-column prop="minSize" label="最小规格" />
          </el-table>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="ruleDialog" title="新增配额规则" width="500px">
    <el-form :model="ruleForm" label-width="100px">
      <el-form-item label="年度"><el-input-number v-model="ruleForm.year" /></el-form-item>
      <el-form-item label="海区"><el-input v-model="ruleForm.seaAreaName" placeholder="例如：闽南近海渔区" /></el-form-item>
      <el-form-item label="品种"><el-input v-model="ruleForm.species" placeholder="例如：带鱼" /></el-form-item>
      <el-form-item label="年度总量(kg)"><el-input-number v-model="ruleForm.totalQuota" :min="0" /></el-form-item>
      <el-form-item label="最小规格"><el-input v-model="ruleForm.minSize" /></el-form-item>
      <el-form-item label="是否禁渔"><el-switch v-model="ruleForm.banned" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="ruleDialog = false">取消</el-button>
      <el-button type="primary" @click="saveRule">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const rules = ref([])
const query = ref({ owner: '', portName: '', year: new Date().getFullYear() })
const result = ref({})
const ruleDialog = ref(false)
const ruleForm = ref({})

const loadRules = async () => {
  const r = await request.get('/quota/rule/list', { params: { year: new Date().getFullYear() } })
  if (r.code === 0) rules.value = r.data
}

const search = async () => {
  const r = await request.get('/quota/summary/owner', { params: query.value })
  if (r.code === 0) result.value = r.data
  else ElMessage.error(r.message)
}

const openRule = () => {
  ruleForm.value = { year: new Date().getFullYear(), banned: false }
  ruleDialog.value = true
}

const saveRule = async () => {
  const r = await request.post('/quota/rule/save', ruleForm.value)
  if (r.code === 0) { ElMessage.success('已保存'); ruleDialog.value = false; loadRules() }
}

onMounted(loadRules)
</script>
