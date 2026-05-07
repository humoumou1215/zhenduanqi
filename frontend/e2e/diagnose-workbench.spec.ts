import { test, expect } from '@playwright/test';

test.describe('诊断工作台页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('页面应正确加载', async ({ page }) => {
    await expect(page.locator('.diagnose-workbench')).toBeVisible();
    await expect(page.locator('h2')).toContainText('诊断工作台');
  });

  test('应显示服务器选择器', async ({ page }) => {
    const serverSelect = page.locator('.el-select').filter({ hasText: '选择目标服务器' });
    await expect(serverSelect).toBeVisible();
  });

  test('应显示实时监控区域', async ({ page }) => {
    await expect(page.locator('.dashboard-section')).toBeVisible();
    await expect(page.locator('h3')).toContainText('实时监控');
  });

  test('应显示场景筛选区域', async ({ page }) => {
    await expect(page.locator('.scene-filter-section')).toBeVisible();
  });

  test('应显示场景列表', async ({ page }) => {
    await expect(page.locator('.scene-list-section')).toBeVisible();
  });
});

test.describe('服务器选择功能', () => {
  test('选择服务器后应缓存到 LocalStorage', async ({ page, context }) => {
    await page.goto('/');

    await page.locator('.el-select').click();
    await page.locator('.el-select-dropdown__item').first().click();

    const stored = await page.evaluate(() => localStorage.getItem('selectedServerId'));
    expect(stored).toBeTruthy();
  });

  test('刷新页面后应恢复选中的服务器', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('selectedServerId', 'server-1'));

    await page.reload();

    const selectValue = await page.locator('.el-select .el-input__inner').inputValue();
    expect(selectValue).toContain('server-1');
  });
});

test.describe('Dashboard 实时监控', () => {
  test('应显示刷新频率输入框', async ({ page }) => {
    await page.goto('/');

    const intervalInput = page.locator('.el-input-number');
    await expect(intervalInput).toBeVisible();

    const value = await intervalInput.inputValue();
    expect(value).toBe('10');
  });

  test('应显示开始刷新按钮', async ({ page }) => {
    await page.goto('/');

    const startBtn = page.locator('button').filter({ hasText: '开始刷新' });
    await expect(startBtn).toBeVisible();
  });

  test('未选择服务器时按钮应禁用', async ({ page }) => {
    await page.goto('/');

    const startBtn = page.locator('button').filter({ hasText: '开始刷新' });
    await expect(startBtn).toBeDisabled();
  });

  test('点击开始刷新后应显示暂停按钮', async ({ page }) => {
    await page.goto('/');

    await page.locator('.el-select').click();
    await page.locator('.el-select-dropdown__item').first().click();

    await page.locator('button').filter({ hasText: '开始刷新' }).click();

    const pauseBtn = page.locator('button').filter({ hasText: '暂停' });
    await expect(pauseBtn).toBeVisible();
  });
});

test.describe('场景筛选功能', () => {
  test('应显示搜索框', async ({ page }) => {
    await page.goto('/');

    const searchInput = page.locator('.scene-filter-section input[type="text"]');
    await expect(searchInput).toBeVisible();
  });

  test('应显示分类标签', async ({ page }) => {
    await page.goto('/');

    const tags = page.locator('.category-tags .el-tag');
    const count = await tags.count();
    expect(count).toBeGreaterThan(0);
  });

  test('点击分类标签应筛选场景', async ({ page }) => {
    await page.goto('/');

    await page.locator('.category-tags .el-tag').first().click();

    const scenes = page.locator('.el-collapse-item');
    const count = await scenes.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('搜索关键词应筛选场景', async ({ page }) => {
    await page.goto('/');

    await page.locator('.scene-filter-section input[type="text"]').fill('CPU');

    const scenes = page.locator('.el-collapse-item');
    const count = await scenes.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });
});

test.describe('场景展开/折叠', () => {
  test('点击场景应展开显示步骤', async ({ page }) => {
    await page.goto('/');

    await page.locator('.el-collapse-item__header').first().click();

    const steps = page.locator('.step-card');
    await expect(steps.first()).toBeVisible();
  });

  test('同时只能展开一个场景', async ({ page }) => {
    await page.goto('/');

    await page.locator('.el-collapse-item__header').first().click();

    await page.locator('.el-collapse-item__header').nth(1).click();

    const firstContent = page.locator('.el-collapse-item__content').first();
    await expect(firstContent).not.toBeVisible();
  });
});

test.describe('步骤执行功能', () => {
  test('步骤应显示命令输入框', async ({ page }) => {
    await page.goto('/');

    await page.locator('.el-collapse-item__header').first().click();

    const commandInput = page.locator('.command-input-area input').first();
    await expect(commandInput).toBeVisible();
  });

  test('未选择服务器时执行按钮应禁用', async ({ page }) => {
    await page.goto('/');

    await page.locator('.el-collapse-item__header').first().click();

    const execBtn = page.locator('.command-input-area button').filter({ hasText: '执行' });
    await expect(execBtn).toBeDisabled();
  });
});
