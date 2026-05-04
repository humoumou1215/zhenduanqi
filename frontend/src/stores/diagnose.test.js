import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useDiagnoseStore } from '../stores/diagnose';

describe('useDiagnoseStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe('extractVariables', () => {
    it('should extract variable from thread result using jsonpath', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Thread Deadlock Detection',
        steps: [
          {
            id: 101,
            command: 'thread -b',
            extract_rules: JSON.stringify([
              {
                variable: 'threadId',
                jsonPath: '$[?(@.type=="thread")][0].data.threadId',
                description: 'Extract first thread id',
              },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      const results = [
        { type: 'thread', data: { threadId: 42, name: 'Thread-1', state: 'BLOCKED' } },
      ];

      store.extractVariables(101, results);

      expect(store.getVariable('threadId')).toBe('42');
    });

    it('should extract multiple variables from results', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [
          {
            id: 101,
            command: 'thread -n 5',
            extract_rules: JSON.stringify([
              {
                variable: 'threadId',
                jsonPath: '$[0].data.threadId',
                description: 'First thread id',
              },
              {
                variable: 'threadName',
                jsonPath: '$[0].data.name',
                description: 'First thread name',
              },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      const results = [{ type: 'thread', data: { threadId: 123, name: 'Worker-Thread-1' } }];

      store.extractVariables(101, results);

      expect(store.getVariable('threadId')).toBe('123');
      expect(store.getVariable('threadName')).toBe('Worker-Thread-1');
    });

    it('should not extract variables when step has no extract_rules', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [{ id: 101, command: 'thread -b' }],
      };

      store.initScene(scene, 'server-1');

      const results = [{ type: 'thread', data: { threadId: 42 } }];

      store.extractVariables(101, results);

      expect(store.getVariable('threadId')).toBeUndefined();
    });

    it('should handle invalid jsonpath gracefully', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [
          {
            id: 101,
            command: 'test',
            extract_rules: JSON.stringify([
              {
                variable: 'testVar',
                jsonPath: '$.invalid[?(@.nonexistent',
                description: 'Invalid jsonpath',
              },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      const results = [];

      expect(() => store.extractVariables(101, results)).not.toThrow();
      expect(store.getVariable('testVar')).toBeUndefined();
    });

    it('should handle invalid extract_rules JSON gracefully', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [{ id: 101, command: 'test', extract_rules: 'not valid json' }],
      };

      store.initScene(scene, 'server-1');

      const results = [];

      expect(() => store.extractVariables(101, results)).not.toThrow();
    });

    it('should support simple array index jsonpath for data.sql format', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [
          {
            id: 101,
            command: 'thread -n 5',
            extract_rules: JSON.stringify([
              {
                variable: 'threadId',
                jsonPath: '$[0].id',
                description: 'First thread id from simple array',
              },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      const results = [{ id: 123, name: 'Worker-Thread-1' }];

      store.extractVariables(101, results);

      expect(store.getVariable('threadId')).toBe('123');
    });

    it('should try fallback jsonpaths when first path fails', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [
          {
            id: 101,
            command: 'thread -n 5',
            extract_rules: JSON.stringify([
              {
                variable: 'threadId',
                jsonPath: '$[?(@.type=="thread")][0].data.id',
                description: 'First try using id field',
              },
              {
                variable: 'threadId',
                jsonPath: '$[?(@.type=="thread")][0].data.threadId',
                description: 'Fallback using threadId field',
              },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      const results = [
        { type: 'thread', data: { threadId: 987, name: 'Thread-1' } },
      ];

      store.extractVariables(101, results);

      expect(store.getVariable('threadId')).toBe('987');
    });

    it('should skip subsequent rules for same variable once extracted', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [
          {
            id: 101,
            command: 'thread -n 5',
            extract_rules: JSON.stringify([
              {
                variable: 'threadId',
                jsonPath: '$[?(@.type=="thread")][0].data.id',
                description: 'First path that succeeds',
              },
              {
                variable: 'threadId',
                jsonPath: '$[?(@.type=="thread")][0].data.threadId',
                description: 'This should be skipped',
              },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      const results = [
        { type: 'thread', data: { id: 456, threadId: 789, name: 'Thread-1' } },
      ];

      store.extractVariables(101, results);

      // Should use the value from the first successful rule, not the second one
      expect(store.getVariable('threadId')).toBe('456');
    });
  });

  describe('fillCommand', () => {
    it('should replace single placeholder with extracted variable', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [{ id: 101, command: 'thread {threadId}' }],
      };

      store.initScene(scene, 'server-1');
      store.setVariable('threadId', '42');

      const filled = store.fillCommand(101);

      expect(filled).toBe('thread 42');
    });

    it('should replace multiple placeholders with extracted variables', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [{ id: 101, command: 'trace {className} {methodName} -n 5' }],
      };

      store.initScene(scene, 'server-1');
      store.setVariable('className', 'com.example.UserService');
      store.setVariable('methodName', 'getUser');

      const filled = store.fillCommand(101);

      expect(filled).toBe('trace com.example.UserService getUser -n 5');
    });

    it('should not replace placeholders that have no variable', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [{ id: 101, command: 'watch {className} {methodName}' }],
      };

      store.initScene(scene, 'server-1');
      store.setVariable('className', 'com.example.Service');

      const filled = store.fillCommand(101);

      expect(filled).toBe('watch com.example.Service {methodName}');
    });

    it('should return empty string for non-existent step', () => {
      const store = useDiagnoseStore();
      store.initScene({ id: 1, steps: [] }, 'server-1');

      const filled = store.fillCommand(999);

      expect(filled).toBe('');
    });

    it('should handle command with no placeholders', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [{ id: 101, command: 'dashboard -n 1' }],
      };

      store.initScene(scene, 'server-1');

      const filled = store.fillCommand(101);

      expect(filled).toBe('dashboard -n 1');
    });
  });

  describe('getPreFilledCommand', () => {
    it('should return pre-filled command using stored variables', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [
          {
            id: 101,
            command: 'thread {threadId}',
            extract_rules: JSON.stringify([
              { variable: 'threadId', jsonPath: '$[0].data.threadId' },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      store.extractVariables(101, [{ type: 'thread', data: { threadId: 99 } }]);

      const preFilled = store.getPreFilledCommand(101);

      expect(preFilled).toBe('thread 99');
    });
  });

  describe('saveStepResult', () => {
    it('should save result and extract variables', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [
          {
            id: 101,
            command: 'thread -b',
            extract_rules: JSON.stringify([
              { variable: 'threadId', jsonPath: '$[0].data.threadId' },
            ]),
          },
        ],
      };

      store.initScene(scene, 'server-1');

      const result = {
        state: 'SUCCESS',
        results: [{ type: 'thread', data: { threadId: 55 } }],
      };

      store.saveStepResult(101, result);

      expect(store.getStepResult(101)).toEqual(result);
      expect(store.getVariable('threadId')).toBe('55');
    });
  });

  describe('reset', () => {
    it('should clear all state including variables', () => {
      const store = useDiagnoseStore();

      const scene = {
        id: 1,
        name: 'Test Scene',
        steps: [{ id: 101, command: 'test' }],
      };

      store.initScene(scene, 'server-1');
      store.setVariable('testVar', 'testValue');
      store.saveStepResult(101, { state: 'SUCCESS' });

      store.reset();

      expect(store.currentSceneId).toBeNull();
      expect(store.currentSceneName).toBe('');
      expect(store.selectedServerId).toBeNull();
      expect(store.steps).toEqual([]);
      expect(store.variables.size).toBe(0);
      expect(store.stepResults.size).toBe(0);
    });
  });

  describe('getVariables', () => {
    it('should return plain object from Map', () => {
      const store = useDiagnoseStore();
      store.setVariable('var1', 'value1');
      store.setVariable('var2', '123');

      const vars = store.getVariables();

      expect(vars).toEqual({ var1: 'value1', var2: '123' });
    });

    it('should return empty object when no variables', () => {
      const store = useDiagnoseStore();

      const vars = store.getVariables();

      expect(vars).toEqual({});
    });
  });
});
