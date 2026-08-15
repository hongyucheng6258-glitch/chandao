<template>
  <el-tag :type="tagType" size="small">{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: '' },
  type: { type: String, default: 'task' } // task / bug / story / project / sprint
})

const MAPS = {
  task: {
    wait: ['未开始', 'info'], doing: ['进行中', 'primary'], done: ['已完成', 'success'],
    pause: ['已暂停', 'warning'], cancel: ['已取消', 'info'], closed: ['已关闭', 'info']
  },
  bug: {
    active: ['激活', 'danger'], resolved: ['已解决', 'warning'], closed: ['已关闭', 'success']
  },
  story: {
    draft: ['草稿', 'info'], active: ['已激活', 'primary'],
    changed: ['已变更', 'warning'], closed: ['已关闭', 'success']
  },
  project: {
    wait: ['未开始', 'info'], doing: ['进行中', 'primary'],
    suspended: ['已暂停', 'warning'], closed: ['已关闭', 'info']
  },
  sprint: {
    wait: ['未开始', 'info'], doing: ['进行中', 'primary'], closed: ['已关闭', 'success']
  }
}

const label = computed(() => MAPS[props.type]?.[props.status]?.[0] ?? props.status)
const tagType = computed(() => MAPS[props.type]?.[props.status]?.[1] ?? 'info')
</script>
