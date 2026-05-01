import ThreadRenderer from './ThreadRenderer.vue';
import MemoryRenderer from './MemoryRenderer.vue';
import StatusRenderer from './StatusRenderer.vue';
import EnhancerRenderer from './EnhancerRenderer.vue';
import FallbackRenderer from './FallbackRenderer.vue';

const rendererMap = {
  thread: ThreadRenderer,
  memory: MemoryRenderer,
  status: StatusRenderer,
  enhancer: EnhancerRenderer,
};

export function getRenderer(type) {
  return rendererMap[type] || FallbackRenderer;
}

export { ThreadRenderer, MemoryRenderer, StatusRenderer, EnhancerRenderer, FallbackRenderer };
