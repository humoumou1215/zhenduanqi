<template>
  <Transition name="fade">
    <el-button
      v-if="visible"
      type="primary"
      size="small"
      class="focus-button"
      @click="$emit('click')"
    >
      聚焦最新行
    </el-button>
  </Transition>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['click']);
</script>

<style scoped>
.focus-button {
  position: fixed;
  bottom: 20px;
  right: 30px;
  z-index: 100;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.15);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
