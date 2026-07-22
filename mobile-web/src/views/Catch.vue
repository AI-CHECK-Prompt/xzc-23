<template>
  <div class="page">
    <van-nav-bar title="扫码登记渔获" left-arrow @click-left="$router.back()" />

    <div class="card" style="text-align:center;">
      <van-icon name="scan" size="64" color="#1989fa" />
      <p>码头扫码提交渔获预申报</p>
      <van-button type="primary" block @click="mockScan">模拟扫码（码头二维码）</van-button>
    </div>

    <div class="card" v-if="form.voyageId">
      <h4>航次：{{ form.voyageNo }}</h4>
      <p>船号：{{ form.vesselNo }}</p>

      <van-field v-model="items[0].species" label="渔获品种" placeholder="例如：带鱼" />
      <van-field v-model="items[0].estimatedWeight" label="预估重量(kg)" type="number" />
      <van-field v-model="items[0].juvenileRatio" label="幼鱼比例(%)" type="number" />
      <van-field name="protected" label="保护品种">
        <template #input>
          <van-switch v-model="items[0].isProtected" />
        </template>
      </van-field>

      <van-button size="small" @click="addItem" style="margin-top:8px;">+ 添加品种</van-button>

      <div style="margin-top:12px;">
        <van-button type="success" block @click="submit">提交预申报</van-button>
      </div>
    </div>

    <div class="card" v-if="submitted">
      <van-icon name="checked" size="48" color="#07c160" style="display:block;text-align:center;" />
      <p style="text-align:center;">已提交预申报，等待渔港管理员过磅确认</p>
      <p style="text-align:center;">单号：{{ submitted }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { showToast, showDialog } from 'vant'
import request from '@/utils/request'

const form = ref({ voyageId: '', voyageNo: '', vesselId: '', vesselNo: '' })
const items = ref([{ species: '', estimatedWeight: 0, juvenileRatio: 0, isProtected: false }])
const submitted = ref('')

const mockScan = async () => {
  // 模拟码头扫码获取最近一次航次
  const v = localStorage.getItem('vesselId')
  if (!v) {
    showToast('请先在首页选择船舶')
    return
  }
  const r = await request.get('/voyage/byVessel/' + v)
  if (r.code === 0 && r.data.length) {
    const latest = r.data.find(x => x.status === '已归港') || r.data[0]
    form.value.voyageId = latest.id
    form.value.voyageNo = latest.declarationNo
    form.value.vesselId = latest.vesselId
    form.value.vesselNo = latest.vesselNo
  } else {
    showToast('未找到可扫码的航次')
  }
}

const addItem = () => {
  items.value.push({ species: '', estimatedWeight: 0, juvenileRatio: 0, isProtected: false })
}

const submit = async () => {
  const total = items.value.reduce((s, i) => s + Number(i.estimatedWeight || 0), 0)
  const body = {
    voyageId: form.value.voyageId,
    vesselId: form.value.vesselId,
    itemsJson: JSON.stringify(items.value),
    estimatedTotal: total
  }
  const r = await request.post('/catch/submitPre', body)
  if (r.code === 0) {
    submitted.value = r.data.declarationNo
    showDialog({ title: '提交成功', message: '请等待过磅' })
  } else {
    showToast(r.message)
  }
}
</script>
