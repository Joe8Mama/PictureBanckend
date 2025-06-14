

<template>
  <a-config-provider :locale="appLocale">
    <div id="app">
      <BasicLayout/>
    </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { healthUsingGet } from '@/api/mainController'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import  'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')
const appLocale = zhCN;

// healthUsingGet().then((res) => {
//   console.log(res)
// })
//
// const loginUserStore = useLoginUserStore()
// loginUserStore.fetchLoginUser()

</script>

<style scoped>

</style>
