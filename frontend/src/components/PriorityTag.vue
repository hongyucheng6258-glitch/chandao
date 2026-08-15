<template>
  <el-tag :type="tagType" size="small" effect="plain">{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: { type: Number, default: 3 },
  kind: { type: String, default: 'priority' } // priority / severity
})

const PRIORITY = { 1: ['P1', 'danger'], 2: ['P2', 'warning'], 3: ['P3', 'primary'], 4: ['P4', 'info'] }
const SEVERITY = { 1: ['致命', 'danger'], 2: ['严重', 'warning'], 3: ['一般', 'primary'], 4: ['轻微', 'info'] }

const map = computed(() => (props.kind === 'severity' ? SEVERITY : PRIORITY))
const label = computed(() => map.value[props.level]?.[0] ?? props.level)
const tagType = computed(() => map.value[props.level]?.[1] ?? 'info')
</script>
