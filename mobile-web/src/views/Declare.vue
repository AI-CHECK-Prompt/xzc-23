<template>
  <div class="page">
    <van-nav-bar title="出海申报" left-arrow @click-left="$router.back()" />
    <div class="card">
      <van-field
        v-model="form.vesselNo"
        label="船号"
        placeholder="选择船舶后自动填入"
        readonly
        is-link
        @click="showVesselPicker = true" />
      <van-field v-model="form.planSeaArea" label="计划海域" placeholder="例如：闽南近海渔区-外海" />
      <van-field name="planDays" label="计划天数">
        <template #input>
          <van-stepper v-model="form.planDays" min="1" max="60" />
        </template>
      </van-field>
      <van-field name="planMethod" label="作业方式">
        <template #input>
          <van-radio-group v-model="form.planMethod" direction="horizontal">
            <van-radio name="刺网">刺网</van-radio>
            <van-radio name="围网">围网</van-radio>
            <van-radio name="拖网">拖网</van-radio>
            <van-radio name="钓具">钓具</van-radio>
          </van-radio-group>
        </template>
      </van-field>
      <van-field v-model="form.netSpec" label="网具规格" placeholder="例如：刺网 网目 50mm 长 800m" />
      <van-field v-model="form.crewText" label="作业人员" type="textarea" rows="3"
                 placeholder="一行一人，例如：张三 350582199001011234" />
      <van-field name="departTime" label="计划出港">
        <template #input>
          <van-date-picker v-model="departTimeShow" />
        </template>
      </van-field>
    </div>

    <div style="padding: 0 12px;">
      <van-button type="primary" block @click="submit">提交申报</van-button>
    </div>

    <van-popup v-model:show="showVesselPicker" position="bottom" round>
      <van-picker :columns="vesselColumns" @confirm="onPick" @cancel="showVesselPicker = false" show-toolbar />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast, showDialog } from 'vant'

const router = useRouter()
const vessels = ref([])
const vesselColumns = ref([])
const showVesselPicker = ref(false)
const departTimeShow = ref(['2026', '01', '01])

const form = ref({
  vesselId: localStorage.getItem('vesselId') || '',
  vesselNo: '',
  planSeaArea: '',
  planDays: 5,
  planMethod: '刺网',
  netSpec: '',
  crewText: ''
})

const onPick = ({ selectedOptions }) => {
  const v = selectedOptions[0]
  const found = vessels.value.find(x => x.vesselNo === v.text)
  if (found) {
    form.value.vesselId = found.id
    form.value.vesselNo = found.vesselNo
  }
  showVesselPicker.value = false
}

const loadVessels = async () => {
  const r = await request.get('/vessel/list')
  if (r.code === 0) {
    vessels.value = r.data
    vesselColumns.value = r.data.map(v => ({ text: v.vesselNo, value: v.id }))
    if (!form.value.vesselNo && r.data.length) {
      form.value.vesselId = r.data[0].id
      form.value.vesselNo = r.data[0].vesselNo
    }
  }
}

const submit = async () => {
  if (!form.value.vesselId) {
    showToast('请选择船舶')
    return
  }
  const crewList = form.value.crewText.split('\n').filter(Boolean).map((line, i) => ({
    name: line.split(' ')[0],
    idNo: line.split(' ')[1] || ''
  }))
  const body = {
    vesselId: form.value.vesselId,
    planSeaArea: form.value.planSeaArea,
    planDays: form.value.planDays,
    planMethod: form.value.planMethod,
    netSpec: form.value.netSpec,
    crewListJson: JSON.stringify(crewList),
    planDepartureTime: departTimeShow.value.join('-') + ' 08:00:00'
  }
  const r = await request.post('/voyage/submit', body)
  if (r.code === 0) {
    showDialog({ title: '申报成功', message: '申报单号：' + r.data.declarationNo })
      .then(() => router.push('/home'))
  } else {
    showDialog({ title: '申报失败', message: r.message })
  }
}

onMounted(loadVessels)
</script>
