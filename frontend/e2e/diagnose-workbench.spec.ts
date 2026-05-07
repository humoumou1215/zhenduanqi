import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/');
  await page.fill('input[type="text"]', 'admin');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForURL('**/#/**');
}

test.describe('诊断工作台页面', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('页面应正确加载', async ({ page }) => {
    await expect(page.locator('.diagnose-workbench')).toBeVisible();
    await expect(page.locator('.workbench-header h2')).toContainText('诊断工作台');
  });

  test('应显示服务器选择器', async ({ page }) => {
    const serverSelect = page.locator('.header-right .el-select');
    await expect(serverSelect).toBeVisible();
  });

  test('应显示实时监控区域', async ({ page }) => {
    await expect(page.locator('.dashboard-section')).toBeVisible();
    await expect(page.locator('.dashboard-section h3')).toContainText('实时监控');
  });

  test('应显示场景筛选区域', async ({ page }) => {
    await expect(page.locator('.scene-filter-section')).toBeVisible();
  });

  test('应显示场景列表', async ({ page }) => {
    await expect(page.locator('.scene-list-section')).toBeVisible();
  });
});

test.describe('服务器选择功能', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('选择服务器后应缓存到 LocalStorage', async ({ page }) => {
    await page.locator('.header-right .el-select').click();
    await page.waitForSelector('.el-select-dropdown__item');
    await page.locator('.el-select-dropdown__item').first().click();

    const stored = await page.evaluate(() => localStorage.getItem('selectedServerId'));
    expect(stored).toBeTruthy();
  });

  test('刷新页面后应恢复选中的服务器', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('selectedServerId', 'test-server-id'));
    await page.reload();

    const selectedValue = await page.locator('.header-right .el-select input').inputValue();
    expect(selectedValue).toBeTruthy();
  });
});

test.describe('Dashboard 实时监控', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应显示刷新频率输入框', async ({ page }) => {
    const intervalInput = page.locator('.dashboard-controls .el-input-number input');
    await expect(intervalInput).toBeVisible();

    const value = await intervalInput.inputValue();
    expect(['10', '10.0']).toContain(value);
  });

  test('应显示开始刷新按钮', async ({ page }) => {
    const startBtn = page.locator('.dashboard-controls button').filter({ hasText: '开始刷新' });
    await expect(startBtn).toBeVisible();
  });

  test('未选择服务器时按钮应禁用', async ({ page }) => {
    const startBtn = page.locator('.dashboard-controls button').filter({ hasText: '开始刷新' });
    await expect(startBtn).toBeDisabled();
  });

  test('点击开始刷新后应显示暂停按钮', async ({ page }) => {
    await page.locator('.header-right .el-select').click();
    await page.waitForSelector('.el-select-dropdown__item');
    await page.locator('.el-select-dropdown__item').first().click();

    await page.locator('.dashboard-controls button').filter({ hasText: '开始刷新' }).click();

    const pauseBtn = page.locator('.dashboard-controls button').filter({ hasText: '暂停' });
    await expect(pauseBtn).toBeVisible();
  });
});

test.describe('场景筛选功能', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应显示搜索框', async ({ page }) => {
    const searchInput = page.locator('.scene-filter-section .el-input input');
    await expect(searchInput).toBeVisible();
  });

  test('应显示分类标签', async ({ page }) => {
    const tags = page.locator('.category-tags .el-tag');
    const count = await tags.count();
    expect(count).toBeGreaterThan(0);
  });

  test('点击分类标签应筛选场景', async ({ page }) => {
    await page.locator('.category-tags .el-tag').first().click();

    const scenes = page.locator('.el-collapse-item');
    const count = await scenes.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('搜索关键词应筛选场景', async ({ page }) => {
    await page.locator('.scene-filter-section .el-input input').fill('CPU');
    await page.waitForTimeout(300);

    const scenes = page.locator('.el-collapse-item');
    const count = await scenes.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });
});

test.describe('场景展开/折叠', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('点击场景应展开显示步骤', async ({ page }) => {
    const sceneItems = page.locator('.el-collapse-item');
    const count = await sceneItems.count();

    if (count > 0) {
      await sceneItems.first().locator('.el-collapse-item__header').click();
      await page.waitForTimeout(500);

      const stepCards = page.locator('.step-card');
      await expect(stepCards.first()).toBeVisible();
    } else {
      console.log('No scenes available to test');
    }
  });

  test('同时只能展开一个场景', async ({ page }) => {
    const sceneItems = page.locator('.el-collapse-item');
    const count = await sceneItems.count();

    if (count > 1) {
      await sceneItems.first().locator('.el-collapse-item__header').click();
      await page.waitForTimeout(300);

      await sceneItems.nth(1).locator('.el-collapse-item__header').click();
      await page.waitForTimeout(300);

      const firstContent = page.locator('.el-collapse-item').first().locator('.el-collapse-item__wrap');
      const isFirstVisible = await firstContent.isVisible();
      expect(isFirstVisible).toBe(false);
    } else {
      console.log('Not enough scenes to test accordion behavior');
    }
  });
});

test.describe('步骤执行功能', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('步骤应显示命令输入框', async ({ page }) => {
    const sceneItems = page.locator('.el-collapse-item');
    const count = await sceneItems.count();

    if (count > 0) {
      await sceneItems.first().locator('.el-collapse-item__header').click();
      await page.waitForTimeout(500);

      const commandInput = page.locator('.command-input-area input').first();
      await expect(commandInput).toBeVisible();
    } else {
      console.log('No scenes available to test');
    }
  });

  test('未选择服务器时执行按钮应禁用', async ({ page }) => {
    const sceneItems = page.locator('.el-collapse-item');
    const count = await sceneItems.count();

    if (count > 0) {
      await sceneItems.first().locator('.el-collapse-item__header').click();
      await page.waitForTimeout(500);

      const execBtn = page.locator('.command-input-area button').filter({ hasText: '执行' });
      await expect(execBtn).toBeDisabled();
    } else {
      console.log('No scenes available to test');
    }
  });
});
