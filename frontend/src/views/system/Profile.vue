<template>
  <div class="page-container">
    <el-card style="max-width: 560px">
      <template #header><span>个人信息</span></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ userStore.userInfo?.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag v-for="r in userStore.roles" :key="r" size="small" style="margin-right: 4px">{{ r }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import useUserStore from '@/store/user'

const userStore = useUserStore()
</script>
