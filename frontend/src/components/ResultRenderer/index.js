import ThreadRenderer from './ThreadRenderer.vue';
import MemoryRenderer from './MemoryRenderer.vue';
import StatusRenderer from './StatusRenderer.vue';
import EnhancerRenderer from './EnhancerRenderer.vue';
import FallbackRenderer from './FallbackRenderer.vue';
import DashboardRenderer from './DashboardRenderer.vue';
import VmoptionRenderer from './VmoptionRenderer.vue';
import SysenvRenderer from './SysenvRenderer.vue';
import HeapRenderer from './HeapRenderer.vue';

const rendererMap = {
  thread: ThreadRenderer,
  memory: MemoryRenderer,
  status: StatusRenderer,
  enhancer: EnhancerRenderer,
  dashboard: DashboardRenderer,
  vmoption: VmoptionRenderer,
  sysenv: SysenvRenderer,
  heap: HeapRenderer,
};

export function getRenderer(type) {
  return rendererMap[type] || FallbackRenderer;
}

export {
  ThreadRenderer,
  MemoryRenderer,
  StatusRenderer,
  EnhancerRenderer,
  FallbackRenderer,
  DashboardRenderer,
  VmoptionRenderer,
  SysenvRenderer,
};
