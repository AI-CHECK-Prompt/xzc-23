<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card><div class="metric"><div class="num">{{ overview.vesselCount || 0 }}</div><div class="label">在册船舶</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="metric"><div class="num">{{ overview.voyageCount || 0 }}</div><div class="label">累计航次申报</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="metric"><div class="num" style="color:#e6a23c;">{{ overview.pendingAlertCount || 0 }}</div><div class="label">待处理告警</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="metric"><div class="num" style="color:#f56c6c;">{{ overview.violationCount || 0 }}</div><div class="label">违规告知书</div></div></el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px;">
      <template #header>
        <span>平台说明</span>
      </template>
      <p>本平台覆盖第一阶段：本省沿海岸段主要渔港与作业海区，提供出海申报、船位追踪、渔获登记、配额管控、执法办案全链路能力。</p>
      <ul>
        <li>角色：船东船长、渔港管理员、海洋与渔业局配额管理人员、海警渔政执法办案人员、水产采购商</li>
        <li>数据可追溯到具体航次与具体申报单</li>
        <li>支持 JT/T 808 等主流船位终端协议接入</li>
        <li>移动端在弱网环境下支持扫码申报与离线缓存</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const overview = ref({})

const load = async () => {
  const r = await request.get('/selfcheck/overview')
  if (r.code === 0) overview.value = r.data
}

onMounted(load)
</script>

<style scoped>
.metric { text-align: center; padding: 10px; }
.num { font-size: 36px; font-weight: 700; color: #1890ff; }
.label { color: #888; margin-top: 6px; }
</style>
