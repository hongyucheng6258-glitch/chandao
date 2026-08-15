<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">PMS 项目管理</div>
      <el-menu :default-active="$route.path" router class="menu">
        <template v-for="menu in menus" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path || String(menu.id)">
            <template #title>
              <el-icon><component :is="menu.icon || 'Folder'" /></el-icon>
              <span>{{ menu.permName }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
              {{ child.permName }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="menu.icon || 'Document'" /></el-icon>
            <span>{{ menu.permName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-input
            v-model="globalKeyword"
            placeholder="搜索需求 / Bug / 任务"
            style="width: 220px"
            clearable
            @keyup.enter="goSearch"
            @clear="goSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="$route.meta.title">{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="28" :src="userStore.avatar">{{ userStore.realName?.charAt(0) }}</el-avatar>
            <span class="name">{{ userStore.realName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ userStore.username }}</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => userStore.menus)
const globalKeyword = ref('')

function goSearch() {
  const kw = globalKeyword.value.trim()
  if (!kw) return
  router.push({ path: '/search', query: { keyword: kw } })
}

onMounted(async () => {
  if (!userStore.id) {
    await userStore.fetchInfo()
  }
})

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: #fff; border-right: 1px solid #e4e7ed; }
.logo {
  height: 60px; line-height: 60px; text-align: center;
  font-size: 17px; font-weight: 600; color: #409eff;
  border-bottom: 1px solid #e4e7ed;
}
.menu { border-right: none; }
.header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-bottom: 1px solid #e4e7ed;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-info .name { font-size: 14px; }
.main { background: #f5f7fa; padding: 16px; }
</style>
