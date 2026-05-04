import { defineStore } from 'pinia';
import { ref } from 'vue';

const STORAGE_KEY = 'zhenduanqi_command_cache';
const MAX_HISTORY_SIZE = 100;

export const useCommandCacheStore = defineStore('commandCache', () => {
  const commandHistory = ref([]);
  const paramHistory = ref({});

  function loadFromStorage() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const data = JSON.parse(saved);
        commandHistory.value = data.commandHistory || [];
        paramHistory.value = data.paramHistory || {};
      }
    } catch (e) {
      console.warn('Failed to load command cache from storage:', e);
    }
  }

  function saveToStorage() {
    try {
      const data = {
        commandHistory: commandHistory.value,
        paramHistory: paramHistory.value,
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch (e) {
      console.warn('Failed to save command cache to storage:', e);
    }
  }

  function addCommandToHistory(command) {
    if (!command || typeof command !== 'string') return;

    const trimmedCommand = command.trim();
    if (!trimmedCommand) return;

    const parts = trimmedCommand.split(/\s+/);
    const cmdName = parts[0];

    const existingIndex = commandHistory.value.indexOf(cmdName);
    if (existingIndex !== -1) {
      commandHistory.value.splice(existingIndex, 1);
    }

    commandHistory.value.unshift(cmdName);

    if (commandHistory.value.length > MAX_HISTORY_SIZE) {
      commandHistory.value = commandHistory.value.slice(0, MAX_HISTORY_SIZE);
    }

    saveToStorage();
  }

  function addParamToHistory(command, param) {
    if (!command || !param) return;

    const trimmedParam = param.trim();
    if (!trimmedParam) return;

    if (!paramHistory.value[command]) {
      paramHistory.value[command] = [];
    }

    const existingIndex = paramHistory.value[command].indexOf(trimmedParam);
    if (existingIndex !== -1) {
      paramHistory.value[command].splice(existingIndex, 1);
    }

    paramHistory.value[command].unshift(trimmedParam);

    if (paramHistory.value[command].length > MAX_HISTORY_SIZE) {
      paramHistory.value[command] = paramHistory.value[command].slice(0, MAX_HISTORY_SIZE);
    }

    saveToStorage();
  }

  function getCommandHistory() {
    return [...commandHistory.value];
  }

  function getParamHistory(command) {
    return paramHistory.value[command] ? [...paramHistory.value[command]] : [];
  }

  function getLatestParam(command) {
    const history = paramHistory.value[command];
    return history && history.length > 0 ? history[0] : '';
  }

  function clearHistory() {
    commandHistory.value = [];
    paramHistory.value = {};
    saveToStorage();
  }

  loadFromStorage();

  return {
    commandHistory,
    paramHistory,
    addCommandToHistory,
    addParamToHistory,
    getCommandHistory,
    getParamHistory,
    getLatestParam,
    clearHistory,
    loadFromStorage,
    saveToStorage,
  };
});
