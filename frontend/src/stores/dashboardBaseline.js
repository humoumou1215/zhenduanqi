import { defineStore } from 'pinia';
import { ref } from 'vue';

const STORAGE_KEY = 'zhenduanqi_dashboard_baselines';

export const useDashboardBaselineStore = defineStore('dashboardBaseline', () => {
  const baselines = ref({});

  function loadFromStorage() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        baselines.value = JSON.parse(saved);
      }
    } catch (e) {
      console.warn('Failed to load dashboard baselines from storage:', e);
      baselines.value = {};
    }
  }

  function saveToStorage() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(baselines.value));
    } catch (e) {
      console.warn('Failed to save dashboard baselines to storage:', e);
    }
  }

  function getBaseline(serverId) {
    if (!serverId) return null;
    return baselines.value[serverId] || null;
  }

  function setBaseline(serverId, gcData) {
    if (!serverId) return;
    baselines.value[serverId] = {
      timestamp: new Date().toISOString(),
      gc: gcData,
    };
    saveToStorage();
  }

  function clearBaseline(serverId) {
    if (!serverId) return;
    delete baselines.value[serverId];
    saveToStorage();
  }

  function clearAllBaselines() {
    baselines.value = {};
    saveToStorage();
  }

  function calculateDelta(currentGc, baselineGc) {
    if (!baselineGc || !currentGc) return null;

    const result = [];
    const baselineMap = {};

    baselineGc.forEach((item) => {
      baselineMap[item.name] = item;
    });

    currentGc.forEach((item) => {
      const baseline = baselineMap[item.name];
      if (baseline) {
        result.push({
          ...item,
          countDelta: item.count - baseline.count,
          timeDelta: item.time - baseline.time,
        });
      } else {
        result.push({
          ...item,
          countDelta: null,
          timeDelta: null,
        });
      }
    });

    return result;
  }

  loadFromStorage();

  return {
    baselines,
    getBaseline,
    setBaseline,
    clearBaseline,
    clearAllBaselines,
    calculateDelta,
    loadFromStorage,
    saveToStorage,
  };
});
