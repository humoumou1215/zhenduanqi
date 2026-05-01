import { defineStore } from 'pinia';
import { ref } from 'vue';
import { JSONPath } from 'jsonpath-plus';

export const useDiagnoseStore = defineStore('diagnose', () => {
  const currentSceneId = ref(null);
  const currentSceneName = ref('');
  const selectedServerId = ref(null);
  const steps = ref([]);
  const variables = ref(new Map());
  const stepResults = ref(new Map());

  function initScene(scene, serverId) {
    currentSceneId.value = scene.id;
    currentSceneName.value = scene.name;
    selectedServerId.value = serverId;
    steps.value = scene.steps || [];
    variables.value = new Map();
    stepResults.value = new Map();
  }

  function reset() {
    currentSceneId.value = null;
    currentSceneName.value = '';
    selectedServerId.value = null;
    steps.value = [];
    variables.value = new Map();
    stepResults.value = new Map();
  }

  function extractVariables(stepId, results) {
    const step = steps.value.find(s => s.id === stepId);
    if (!step || !step.extract_rules) {
      return;
    }

    let rules;
    try {
      rules = typeof step.extract_rules === 'string'
        ? JSON.parse(step.extract_rules)
        : step.extract_rules;
    } catch {
      return;
    }

    const data = { results: results || [] };

    for (const rule of rules) {
      if (!rule.variable || !rule.jsonPath) {
        continue;
      }

      try {
        const values = JSONPath({
          path: rule.jsonPath,
          json: data,
          resultType: 'value'
        });

        if (values && values.length > 0) {
          variables.value.set(rule.variable, String(values[0]));
        }
      } catch {
        console.warn(`Failed to extract variable ${rule.variable} with jsonPath: ${rule.jsonPath}`);
      }
    }
  }

  function fillCommand(stepId) {
    const step = steps.value.find(s => s.id === stepId);
    if (!step) {
      return '';
    }

    let command = step.command || '';

    for (const [key, value] of variables.value.entries()) {
      const placeholder = `{${key}}`;
      if (command.includes(placeholder)) {
        command = command.replaceAll(placeholder, value);
      }
    }

    return command;
  }

  function getPreFilledCommand(stepId) {
    return fillCommand(stepId);
  }

  function saveStepResult(stepId, result) {
    stepResults.value.set(stepId, result);
    extractVariables(stepId, result.results);
  }

  function getStepResult(stepId) {
    return stepResults.value.get(stepId);
  }

  function getVariables() {
    return Object.fromEntries(variables.value);
  }

  function setVariable(key, value) {
    variables.value.set(key, String(value));
  }

  function getVariable(key) {
    return variables.value.get(key);
  }

  return {
    currentSceneId,
    currentSceneName,
    selectedServerId,
    steps,
    variables,
    stepResults,
    initScene,
    reset,
    extractVariables,
    fillCommand,
    getPreFilledCommand,
    saveStepResult,
    getStepResult,
    getVariables,
    setVariable,
    getVariable
  };
});
