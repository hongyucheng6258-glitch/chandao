<template>
  <el-container class="layout">
    <el-aside :width="sidebarWidth" class="aside">
      <div class="sidebar-header">
        <div class="sidebar-logo" aria-label="橙子平台">C</div>
        <div class="sidebar-brand-info">
          <div class="sidebar-brand">橙子</div>
          <div class="sidebar-brand-sub">项目管理平台</div>
        </div>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="sidebar-menu"
        :collapse="false"
      >
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

      <div class="sidebar-footer">
        <div class="user-card" @click="handleUserClick">
          <el-avatar :size="38" :src="userStore.avatar || ''">{{ userStore.realName?.charAt(0) || 'U' }}</el-avatar>
          <div class="user-info">
            <div class="user-name">{{ userStore.realName || userStore.username }}</div>
            <div class="user-role">{{ userRoleText }}</div>
          </div>
          <el-icon class="logout-icon"><SwitchButton /></el-icon>
        </div>
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <h2 class="header-title">{{ $route.meta.title || '工作台' }}</h2>
        </div>
        <div class="header-center">
          <el-input
            v-model="globalKeyword"
            placeholder="搜索需求、任务、Bug..."
            class="header-search"
            clearable
            @keyup.enter="goSearch"
            @clear="goSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="header-actions">
          <el-badge :value="unreadCount" :max="99" class="header-badge" :hidden="unreadCount === 0">
            <el-button text class="header-icon-btn" @click="showNotifications = true">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-button text class="header-icon-btn" @click="showHelp">
            <el-icon :size="20"><QuestionFilled /></el-icon>
          </el-button>
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="header-user">
              <el-avatar :size="32" :src="userStore.avatar || ''">{{ userStore.realName?.charAt(0) || 'U' }}</el-avatar>
              <span class="header-username">{{ userStore.realName || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span style="color:var(--text-muted);font-size:12px;">{{ userStore.username }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 个人中心弹窗 -->
    <el-dialog v-model="showProfile" title="个人中心" width="480px">
      <div class="profile-content">
        <div class="profile-header">
          <div class="profile-avatar-wrapper" @click="triggerAvatarUpload" :class="{ uploading: avatarUploading }">
            <el-avatar :size="72" class="profile-avatar" :src="userStore.avatar || ''">
              {{ userStore.realName?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon v-if="!avatarUploading"><Camera /></el-icon>
              <span v-if="avatarUploading">上传中...</span>
              <span v-else>更换头像</span>
            </div>
            <input ref="avatarInput" type="file" accept="image/png,image/jpeg,image/jpg,image/gif,image/webp,image/bmp" style="display:none" @change="handleAvatarChange" />
          </div>
          <div class="profile-info">
            <div class="profile-name">{{ userStore.realName || userStore.username }}</div>
            <div class="profile-role">{{ userRoleText }}</div>
            <div class="profile-avatar-tip">点击头像可上传新头像</div>
          </div>
        </div>
        <el-divider />
        <div class="profile-details">
          <div class="profile-item">
            <span class="profile-label">用户名</span>
            <span class="profile-value">{{ userStore.username }}</span>
          </div>
          <div class="profile-item">
            <span class="profile-label">用户ID</span>
            <span class="profile-value">{{ userStore.id }}</span>
          </div>
          <div class="profile-item">
            <span class="profile-label">角色</span>
            <span class="profile-value">{{ (userStore.roles || []).join(', ') }}</span>
          </div>
          <div class="profile-item">
            <span class="profile-label">权限数量</span>
            <span class="profile-value">{{ (userStore.perms || []).length }} 项</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showProfile = false">关闭</el-button>
        <el-button type="primary" @click="handleLogout">退出登录</el-button>
      </template>
    </el-dialog>

    <!-- 通知列表弹窗 -->
    <el-dialog v-model="showNotifications" title="消息通知" width="420px">
      <div class="notification-list">
        <div v-if="notifications.length === 0" class="empty-notification">
          <div class="empty-icon">🔔</div>
          <div class="empty-text">暂无新通知</div>
        </div>
        <div v-else class="notification-item" v-for="(item, index) in notifications" :key="index" @click="item.read = true">
          <div class="notification-dot" v-if="!item.read"></div>
          <div class="notification-content">
            <div class="notification-title">{{ item.title }}</div>
            <div class="notification-desc">{{ item.desc }}</div>
            <div class="notification-time">{{ item.time }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showNotifications = false">关闭</el-button>
        <el-button type="primary" @click="markAllRead">全部已读</el-button>
      </template>
    </el-dialog>

    <!-- 帮助文档弹窗 -->
    <el-dialog v-model="showHelpDialog" title="帮助文档" width="720px" class="help-dialog">
      <el-tabs v-model="helpActiveTab" class="help-tabs">
        <!-- 系统介绍 -->
        <el-tab-pane label="系统介绍" name="intro">
          <div class="help-content">
            <h3 class="help-h3">🍊 橙子项目管理平台</h3>
            <p class="help-p">橙子项目管理平台是一款参考禅道核心业务的自研精简版敏捷项目管理系统，覆盖"产品→需求→项目→迭代→任务→Bug→发布"全流程管理。</p>
            <h4 class="help-h4">核心功能模块</h4>
            <ul class="help-list">
              <li><strong>工作台</strong>：个人待办、统计概览、项目图表</li>
              <li><strong>产品管理</strong>：产品列表、需求管理、发布管理</li>
              <li><strong>项目管理</strong>：项目列表、项目详情、迭代管理、迭代看板、任务管理</li>
              <li><strong>质量中心</strong>：Bug管理、测试用例、测试单</li>
              <li><strong>统计报表</strong>：燃尽图、Bug分布、项目进度</li>
              <li><strong>系统管理</strong>：用户管理、角色管理、权限管理、部门管理、操作日志、系统配置</li>
            </ul>
            <h4 class="help-h4">技术栈</h4>
            <ul class="help-list">
              <li>后端：Spring Boot 3 + MyBatis-Plus + Spring Security + JWT + Redis + MySQL 8</li>
              <li>前端：Vue 3 + Vite + Element Plus + Pinia + Axios + ECharts</li>
            </ul>
          </div>
        </el-tab-pane>

        <!-- 快速入门 -->
        <el-tab-pane label="快速入门" name="quickstart">
          <div class="help-content">
            <h3 class="help-h3">🚀 快速入门</h3>
            <h4 class="help-h4">演示账号</h4>
            <div class="help-account-grid">
              <div class="help-account-item">
                <div class="help-account-icon">👑</div>
                <div class="help-account-name">系统管理员</div>
                <div class="help-account-user">admin / 123456</div>
                <div class="help-account-desc">拥有所有权限，可管理用户、角色、配置</div>
              </div>
              <div class="help-account-item">
                <div class="help-account-icon">📝</div>
                <div class="help-account-name">产品经理</div>
                <div class="help-account-user">po01 / 123456</div>
                <div class="help-account-desc">负责产品和需求管理</div>
              </div>
              <div class="help-account-item">
                <div class="help-account-icon">📊</div>
                <div class="help-account-name">项目经理</div>
                <div class="help-account-user">pm01 / 123456</div>
                <div class="help-account-desc">负责项目、迭代、任务管理</div>
              </div>
              <div class="help-account-item">
                <div class="help-account-icon">💻</div>
                <div class="help-account-name">开发工程师</div>
                <div class="help-account-user">dev01 / 123456</div>
                <div class="help-account-desc">负责任务执行和Bug修复</div>
              </div>
              <div class="help-account-item">
                <div class="help-account-icon">🔍</div>
                <div class="help-account-name">测试工程师</div>
                <div class="help-account-user">qa01 / 123456</div>
                <div class="help-account-desc">负责测试用例、测试单、Bug提交</div>
              </div>
            </div>
            <h4 class="help-h4">基本操作流程</h4>
            <ol class="help-ol">
              <li><strong>创建产品</strong>：产品经理在产品列表中创建产品</li>
              <li><strong>录入需求</strong>：在需求管理中录入产品需求</li>
              <li><strong>创建项目</strong>：项目经理创建项目并关联产品</li>
              <li><strong>创建迭代</strong>：在项目详情中创建迭代</li>
              <li><strong>拉入需求</strong>：将需求拉入迭代</li>
              <li><strong>拆分任务</strong>：将需求拆分为任务并指派给开发</li>
              <li><strong>执行任务</strong>：开发在迭代看板中拖拽任务、更新状态</li>
              <li><strong>测试Bug</strong>：测试提交Bug，开发修复后关闭</li>
              <li><strong>发布版本</strong>：迭代完成后创建发布</li>
            </ol>
          </div>
        </el-tab-pane>

        <!-- 功能说明 -->
        <el-tab-pane label="功能说明" name="features">
          <div class="help-content">
            <h3 class="help-h3">📖 功能说明</h3>
            <el-collapse v-model="helpCollapse" class="help-collapse">
              <el-collapse-item title="工作台" name="dashboard">
                <p><strong>欢迎横幅</strong>：显示问候语、日期、待办统计</p>
                <p><strong>统计卡片</strong>：待办任务、待处理Bug、我的需求、进行中项目</p>
                <p><strong>项目概览</strong>：迭代燃尽图、Bug状态分布饼图</p>
                <p><strong>待办列表</strong>：我的待办任务、待我处理的Bug，点击可跳转</p>
              </el-collapse-item>
              <el-collapse-item title="需求管理" name="story">
                <p><strong>多级筛选</strong>：按产品、项目、迭代、状态、优先级、关键词筛选</p>
                <p><strong>需求状态</strong>：草稿(draft) → 激活(active) → 已变更(changed) → 已关闭(closed)</p>
                <p><strong>需求操作</strong>：提需求、编辑、激活、关闭、拆分任务、批量指派、批量删除</p>
                <p><strong>所属迭代</strong>：显示需求当前所属迭代，未规划显示"未规划"</p>
              </el-collapse-item>
              <el-collapse-item title="迭代看板" name="board">
                <p><strong>四列看板</strong>：待开始(wait)、进行中(doing)、已暂停(pause)、已完成(done)</p>
                <p><strong>拖拽操作</strong>：拖拽任务卡片可改变任务状态</p>
                <p><strong>任务操作</strong>：开始、完成、登记工时、指派、关闭</p>
                <p><strong>任务状态机</strong>：wait→doing/cancel；doing→done/pause/cancel；pause→doing/cancel；done→closed</p>
              </el-collapse-item>
              <el-collapse-item title="Bug管理" name="bug">
                <p><strong>Bug状态</strong>：激活(active) → 已解决(resolved) → 已关闭(closed)，可打回激活</p>
                <p><strong>严重程度</strong>：致命(blocker)、严重(critical)、一般(major)、轻微(minor)</p>
                <p><strong>Bug操作</strong>：提交Bug、编辑、解决、关闭、打回、指派</p>
              </el-collapse-item>
              <el-collapse-item title="系统配置" name="config">
                <p><strong>分组配置</strong>：基础设置、安全设置、文件上传、通知设置等</p>
                <p><strong>实时生效</strong>：修改配置后自动清除Redis缓存，无需重启服务</p>
                <p><strong>上传白名单</strong>：允许的文件扩展名可动态配置</p>
                <p><strong>手动刷新</strong>：可手动刷新配置缓存</p>
              </el-collapse-item>
              <el-collapse-item title="个人中心" name="profile">
                <p><strong>头像上传</strong>：点击头像可上传图片，支持png/jpg/gif/webp/bmp，最大5MB</p>
                <p><strong>用户信息</strong>：显示用户名、用户ID、角色、权限数量</p>
                <p><strong>退出登录</strong>：可在个人中心或用户下拉菜单中退出</p>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-tab-pane>

        <!-- 常见问题 -->
        <el-tab-pane label="常见问题" name="faq">
          <div class="help-content">
            <h3 class="help-h3">❓ 常见问题</h3>
            <el-collapse v-model="faqCollapse" class="help-collapse">
              <el-collapse-item title="登录后页面显示401错误怎么办？" name="faq1">
                <p>401错误表示登录态已失效。请退出登录后重新登录。如果频繁出现401，可能是JWT过期时间设置过短，可在系统配置中调整jwt.expire-hours。</p>
              </el-collapse-item>
              <el-collapse-item title="上传头像成功但不显示怎么办？" name="faq2">
                <p>请确认附件下载接口是否可正常访问。头像存储为附件记录，通过/api/attachments/{id}/download访问。如果返回403，说明Spring Security配置有误，需将/attachments/*/download添加到公开访问列表。</p>
              </el-collapse-item>
              <el-collapse-item title="迭代看板拖拽不生效怎么办？" name="faq3">
                <p>请确认浏览器是否支持HTML5拖拽API。当前看板使用原生拖拽实现，拖拽任务卡片到目标列即可改变状态。如果拖拽后状态未变化，请检查网络请求是否成功。</p>
              </el-collapse-item>
              <el-collapse-item title="如何修改系统名称？" name="faq4">
                <p>使用管理员账号登录，进入"系统管理 → 系统配置"，找到"基础设置"中的"系统名称"，修改后保存即可。配置修改后实时生效，刷新页面可见。</p>
              </el-collapse-item>
              <el-collapse-item title="如何添加新用户？" name="faq5">
                <p>使用管理员账号登录，进入"系统管理 → 用户管理"，点击"新增用户"，填写用户名、姓名、部门、角色等信息，保存即可。新用户默认密码为123456。</p>
              </el-collapse-item>
              <el-collapse-item title="需求如何拉入迭代？" name="faq6">
                <p>进入"项目管理 → 项目列表"，点击项目名称进入项目详情。在迭代列表中点击"拉入需求"按钮，在弹窗中选择目标迭代和需要拉入的需求，确认即可。</p>
              </el-collapse-item>
              <el-collapse-item title="任务如何拆分？" name="faq7">
                <p>在需求管理页面，点击需求操作列的"拆分"按钮，在弹窗中填写任务名称、选择项目和迭代、指派处理人、填写预估工时，保存即可生成任务。</p>
              </el-collapse-item>
              <el-collapse-item title="文件上传支持哪些格式？" name="faq8">
                <p>默认支持：doc/docx/pdf/xls/xlsx/ppt/pptx/txt/md/csv/png/jpg/jpeg/gif/webp/bmp/zip/rar/7z。管理员可在"系统配置 → 文件上传"中动态修改允许的扩展名白名单。</p>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-tab-pane>

        <!-- 关于 -->
        <el-tab-pane label="关于" name="about">
          <div class="help-content help-about">
            <div class="help-about-icon">🍊</div>
            <h3 class="help-about-title">橙子项目管理平台</h3>
            <p class="help-about-version">版本 v1.0.0</p>
            <p class="help-about-desc">一款参考禅道核心业务的自研精简版敏捷项目管理系统，用于毕业设计演示。</p>
            <div class="help-about-tech">
              <span class="help-tech-tag">Spring Boot 3</span>
              <span class="help-tech-tag">Vue 3</span>
              <span class="help-tech-tag">MyBatis-Plus</span>
              <span class="help-tech-tag">Element Plus</span>
              <span class="help-tech-tag">MySQL 8</span>
              <span class="help-tech-tag">Redis</span>
              <span class="help-tech-tag">JWT</span>
              <span class="help-tech-tag">ECharts</span>
            </div>
            <p class="help-about-footer">© 2026 橙子项目管理平台 · 毕业设计项目</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Bell, QuestionFilled, ArrowDown, User, SwitchButton, Camera } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { uploadAvatar } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const sidebarWidth = '260px'

const menus = computed(() => userStore.menus)
const globalKeyword = ref('')

// 个人中心和通知弹窗
const showProfile = ref(false)
const showNotifications = ref(false)
const showHelpDialog = ref(false)
const helpActiveTab = ref('intro')
const helpCollapse = ref(['dashboard'])
const faqCollapse = ref(['faq1'])
const notifications = ref([
  { title: '系统更新通知', desc: '系统已更新至v1.0版本，新增系统配置功能', time: '2026-08-24 10:30', read: false },
  { title: '任务提醒', desc: '您有3个待办任务即将到期', time: '2026-08-24 09:15', read: false },
  { title: 'Bug分配', desc: '有1个新Bug分配给您处理', time: '2026-08-23 16:45', read: true }
])
const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const userRoleText = computed(() => {
  const roles = userStore.roles || []
  if (roles.includes('ADMIN')) return '系统管理员'
  if (roles.includes('PO')) return '产品经理'
  if (roles.includes('PM')) return '项目经理'
  if (roles.includes('DEV')) return '开发工程师'
  if (roles.includes('QA')) return '测试工程师'
  return '用户'
})

function goSearch() {
  const kw = globalKeyword.value.trim()
  if (!kw) return
  router.push({ path: '/search', query: { keyword: kw } })
}

function handleUserClick() {
  handleCommand('profile')
}

onMounted(async () => {
  if (!userStore.id) {
    try {
      await userStore.fetchInfo()
    } catch (e) {
      // fetchInfo失败（如401），响应拦截器会处理跳转
      console.warn('获取用户信息失败', e)
    }
  }
})

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    await handleLogout()
  } else if (cmd === 'profile') {
    showProfile.value = true
  }
}

async function handleLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消' })
  await userStore.logout()
  showProfile.value = false
  router.push('/login')
}

function showHelp() {
  helpActiveTab.value = 'intro'
  showHelpDialog.value = true
}

function markAllRead() {
  notifications.value.forEach(n => n.read = true)
  ElMessage.success('已全部标记为已读')
}

// 头像上传
const avatarUploading = ref(false)
const avatarInput = ref(null)

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  // 校验文件类型
  const imageTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif', 'image/webp', 'image/bmp']
  if (!imageTypes.includes(file.type)) {
    ElMessage.error('只支持图片格式: png/jpg/jpeg/gif/webp/bmp')
    return
  }
  // 校验文件大小(5MB)
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过5MB')
    return
  }
  avatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    if (res.code === 200) {
      ElMessage.success('头像上传成功')
      // 更新用户store中的avatar
      userStore.avatar = res.data.avatar
      // 重新获取用户信息
      await userStore.fetchInfo()
    }
  } catch (e) {
    console.error('头像上传失败', e)
  } finally {
    avatarUploading.value = false
    // 清空input，允许重复上传同一文件
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: var(--surface-dark);
  border-right: 1px solid rgba(255, 255, 255, .06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 18px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.sidebar-logo {
  width: 40px;
  height: 40px;
  background: var(--accent-primary);
  color: var(--surface-dark);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -.04em;
  flex-shrink: 0;
}

.sidebar-brand-info {
  flex: 1;
  min-width: 0;
}

.sidebar-brand {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 17px;
  font-weight: 750;
  color: var(--text-on-dark);
  line-height: 1.2;
}

.sidebar-brand-sub {
  font-size: 11px;
  color: var(--text-on-dark-muted);
  font-weight: 500;
  margin-top: 2px;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: 12px 10px;
  border-right: none !important;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  border-radius: var(--radius-sm);
  margin-bottom: 2px;
  font-size: 14px;
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item.is-active),
.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  background-color: var(--surface-dark-active) !important;
  color: var(--accent-primary) !important;
  font-weight: 650;
  box-shadow: inset 3px 0 0 var(--accent-primary);
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 48px !important;
  font-size: 13px;
}

.sidebar-footer {
  padding: 14px;
  border-top: 1px solid var(--border-light);
  flex-shrink: 0;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
}

.user-card:hover {
  background: var(--surface-dark-raised);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 650;
  color: var(--text-on-dark);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 12px;
  color: var(--text-on-dark-muted);
  margin-top: 2px;
}

.logout-icon {
  font-size: 16px;
  color: var(--text-on-dark-muted);
}

.main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: var(--header-height);
  background: rgba(255, 255, 255, .94);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  padding: 0 28px;
  gap: 24px;
  flex-shrink: 0;
}

.header-left {
  flex-shrink: 0;
}

.header-title {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 19px;
  font-weight: 750;
  color: var(--text-primary);
  margin: 0;
}

.header-center {
  flex: 1;
  max-width: 380px;
}

.header-search {
  width: 100%;
}

.header-search :deep(.el-input__wrapper) {
  background: var(--surface-soft);
  box-shadow: 0 0 0 1px var(--border-light) inset !important;
  border-radius: var(--radius-sm);
}

.header-search :deep(.el-input__wrapper.is-focus) {
  background: var(--bg-secondary);
  box-shadow: 0 0 0 1.5px var(--accent-success) inset, var(--focus-ring) !important;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.header-icon-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
}

.header-icon-btn:hover {
  background: var(--accent-primary-bg);
  color: var(--surface-dark);
}

.header-badge {
  display: flex;
  align-items: center;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}

.header-user:hover {
  background: var(--accent-primary-bg);
}

.header-username {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.main {
  background: var(--bg-primary);
  padding: 0;
  overflow-y: auto;
  flex: 1;
  min-width: 0;
}

/* 个人中心弹窗 */
.profile-content {
  padding: 8px 0;
}
.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 0;
}
.profile-avatar-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
}
.profile-avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
.profile-avatar-wrapper.uploading .avatar-overlay {
  opacity: 1;
  background: rgba(0, 0, 0, 0.7);
}
.profile-avatar {
  width: 72px !important;
  height: 72px !important;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-primary-light)) !important;
  font-size: 28px !important;
  font-weight: 700;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: white;
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 50%;
}
.avatar-overlay .el-icon {
  font-size: 18px;
}
.profile-info {
  flex: 1;
}
.profile-name {
  font-family: 'Outfit', sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.profile-role {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.profile-avatar-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}
.profile-details {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 0;
}
.profile-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.profile-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}
.profile-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
}

/* 通知列表弹窗 */
.notification-list {
  max-height: 400px;
  overflow-y: auto;
}
.empty-notification {
  padding: 40px 20px;
  text-align: center;
}
.empty-icon {
  font-size: 40px;
  margin-bottom: 10px;
}
.empty-text {
  font-size: 14px;
  color: var(--text-muted);
}
.notification-item {
  display: flex;
  gap: 12px;
  padding: 14px 12px;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}
.notification-item:hover {
  background: var(--bg-tertiary);
}
.notification-item:last-child {
  border-bottom: none;
}
.notification-dot {
  width: 8px;
  height: 8px;
  background: var(--accent-danger);
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}
.notification-content {
  flex: 1;
  min-width: 0;
}
.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.notification-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
  line-height: 1.4;
}
.notification-time {
  font-size: 12px;
  color: var(--text-muted);
}

/* 帮助文档弹窗 */
.help-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.help-tabs {
  padding: 0 20px 20px;
}
.help-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.help-content {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 8px;
}
.help-h3 {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}
.help-h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 20px 0 10px 0;
}
.help-p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0 0 10px 0;
}
.help-list {
  margin: 0 0 10px 0;
  padding-left: 20px;
}
.help-list li {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
}
.help-ol {
  margin: 0 0 10px 0;
  padding-left: 20px;
}
.help-ol li {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: 6px;
}
.help-account-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 12px 0;
}
.help-account-item {
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: var(--surface-soft);
  text-align: center;
}
.help-account-icon {
  font-size: 28px;
  margin-bottom: 6px;
}
.help-account-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.help-account-user {
  font-size: 13px;
  color: var(--accent-primary);
  font-weight: 600;
  margin-bottom: 4px;
}
.help-account-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}
.help-collapse {
  border: none;
}
.help-collapse :deep(.el-collapse-item__header) {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-light);
  padding-left: 0;
}
.help-collapse :deep(.el-collapse-item__content) {
  padding: 12px 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
}
.help-collapse :deep(.el-collapse-item__content p) {
  margin: 0 0 8px 0;
}
.help-about {
  text-align: center;
  padding: 20px 0;
}
.help-about-icon {
  font-size: 64px;
  margin-bottom: 12px;
}
.help-about-title {
  font-family: 'Outfit', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}
.help-about-version {
  font-size: 14px;
  color: var(--accent-primary);
  font-weight: 600;
  margin: 0 0 16px 0;
}
.help-about-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0 auto 20px auto;
  max-width: 400px;
}
.help-about-tech {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}
.help-tech-tag {
  padding: 4px 12px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-light);
  border-radius: 20px;
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}
.help-about-footer {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
}

@media (max-width: 900px) {
  .header { padding: 0 16px; gap: 12px; }
  .header-center { max-width: 240px; }
  .header-username { display: none; }
  .header-title { font-size: 17px; }
  .help-account-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 680px) {
  .aside { width: 68px !important; }
  .sidebar-header { padding: 14px; justify-content: center; }
  .sidebar-brand-info, .sidebar-menu :deep(.el-menu-item span), .sidebar-menu :deep(.el-sub-menu__title span), .sidebar-footer .user-info, .sidebar-footer .logout-icon { display: none; }
  .sidebar-menu { padding: 12px 8px; }
  .sidebar-menu :deep(.el-menu-item), .sidebar-menu :deep(.el-sub-menu__title) { padding-left: 0 !important; justify-content: center; }
  .sidebar-menu :deep(.el-sub-menu .el-menu-item) { padding-left: 0 !important; }
  .sidebar-footer { padding: 10px 8px; }
  .user-card { justify-content: center; padding: 8px 0; }
  .header-center { flex: 0 1 180px; }
  .header-actions { gap: 0; }
}
</style>
