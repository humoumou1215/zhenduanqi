import ThreadRenderer from './ThreadRenderer.vue';
import MemoryRenderer from './MemoryRenderer.vue';
import StatusRenderer from './StatusRenderer.vue';
import EnhancerRenderer from './EnhancerRenderer.vue';
import FallbackRenderer from './FallbackRenderer.vue';
import DashboardRenderer from './DashboardRenderer.vue';
import VmoptionRenderer from './VmoptionRenderer.vue';
import SysenvRenderer from './SysenvRenderer.vue';
import ClassInfoRenderer from './ClassInfoRenderer.vue';
import ClassloaderRenderer from './ClassloaderRenderer.vue';
import JadRenderer from './JadRenderer.vue';
import MonitorRenderer from './MonitorRenderer.vue';
import StackRenderer from './StackRenderer.vue';

const rendererMap = {
  thread: ThreadRenderer,
  memory: MemoryRenderer,
  status: StatusRenderer,
  enhancer: EnhancerRenderer,
  dashboard: DashboardRenderer,
  vmoption: VmoptionRenderer,
  sysenv: SysenvRenderer,
  classinfo: ClassInfoRenderer,
  sc: ClassInfoRenderer,
  classloader: ClassloaderRenderer,
  classloaderTree: ClassloaderRenderer,
  jad: JadRenderer,
  monitor: MonitorRenderer,
  stack: StackRenderer,
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
  ClassInfoRenderer,
  ClassloaderRenderer,
  JadRenderer,
  MonitorRenderer,
  StackRenderer,
};
