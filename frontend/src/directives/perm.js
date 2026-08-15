import { watch } from 'vue'
import { useUserStore } from '@/store/user'

/**
 * v-perm="'task:assign'" 无权限则隐藏元素
 * 注意：用户权限是登录后异步加载的，组件首次挂载时 perms 可能仍为空，
 * 因此这里用 display 切换并在 perms 变化时重新评估，避免一次性 removeChild 后无法恢复。
 */
export const permDirective = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const apply = () => {
      const need = binding.value
      if (!need) return
      const ok = userStore.hasPerm(need)
      el.style.display = ok ? '' : 'none'
    }
    apply()
    el._permStop = watch(
      () => userStore.perms,
      () => apply(),
      { deep: true }
    )
  },
  unmounted(el) {
    if (el._permStop) el._permStop()
  }
}
