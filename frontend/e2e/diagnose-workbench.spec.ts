import { test, expect } from '@playwright/test';

test.describe('诊断工作台页面组件存在性验证', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/');
    await page.waitForLoadState('domcontentloaded');
  });

  test.skip('页面结构组件应存在', async ({ page }) => {
    await expect(page.locator('.diagnose-workbench')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.workbench-header')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.dashboard-section')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.scene-filter-section')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.scene-list-section')).toBeVisible({ timeout: 10000 });
  });

  test.skip('页面标题应正确', async ({ page }) => {
    await expect(page.locator('.workbench-header h2')).toContainText('诊断工作台', {
      timeout: 10000,
    });
  });

  test.skip('Dashboard 区域应包含正确标题', async ({ page }) => {
    await expect(page.locator('.dashboard-section h3')).toContainText('实时监控', {
      timeout: 10000,
    });
  });

  test.skip('服务器选择器应存在', async ({ page }) => {
    await expect(page.locator('.header-right .el-select')).toBeVisible({ timeout: 10000 });
  });

  test.skip('刷新控制按钮应存在', async ({ page }) => {
    await expect(page.locator('.dashboard-controls')).toBeVisible({ timeout: 10000 });
  });

  test.skip('场景筛选搜索框应存在', async ({ page }) => {
    await expect(page.locator('.scene-filter-section input')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('服务器选择交互', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/');
    await page.waitForLoadState('domcontentloaded');
  });

  test.skip('点击选择器应显示下拉选项', async ({ page }) => {
    await page.locator('.header-right .el-select').click();
    await page.waitForTimeout(500);
    await expect(page.locator('.el-select-dropdown')).toBeVisible({ timeout: 5000 });
  });

  test.skip('选择服务器应保存到 LocalStorage', async ({ page }) => {
    await page.locator('.header-right .el-select').click();
    await page.waitForSelector('.el-select-dropdown__item', { timeout: 5000 });
    await page.locator('.el-select-dropdown__item').first().click();
    await page.waitForTimeout(500);

    const stored = await page.evaluate(() => localStorage.getItem('selectedServerId'));
    expect(stored).toBeTruthy();
  });
});

test.describe('Dashboard 控件', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/');
    await page.waitForLoadState('domcontentloaded');
  });

  test.skip('刷新间隔输入框应存在', async ({ page }) => {
    await expect(page.locator('.dashboard-controls .el-input-number')).toBeVisible({
      timeout: 10000,
    });
  });

  test.skip('开始刷新按钮应存在', async ({ page }) => {
    await expect(page.locator('button:has-text("开始刷新")')).toBeVisible({ timeout: 10000 });
  });

  test.skip('立即刷新按钮应存在', async ({ page }) => {
    await expect(page.locator('button:has-text("立即刷新")')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('场景分类标签', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/');
    await page.waitForLoadState('domcontentloaded');
  });

  test.skip('分类标签容器应存在', async ({ page }) => {
    await expect(page.locator('.category-tags')).toBeVisible({ timeout: 15000 });
  });

  test.skip('分类标签应显示', async ({ page }) => {
    await page.waitForSelector('.category-tags .el-tag', { timeout: 15000 });
    const tags = page.locator('.category-tags .el-tag');
    const count = await tags.count();
    expect(count).toBeGreaterThan(0);
  });
});

test.describe('场景列表', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/');
    await page.waitForLoadState('domcontentloaded');
  });

  test.skip('场景折叠列表应存在', async ({ page }) => {
    await expect(page.locator('.scene-collapse')).toBeVisible({ timeout: 15000 });
  });

  test.skip('清除筛选按钮应在选择分类后显示', async ({ page }) => {
    await page.waitForSelector('.category-tags .el-tag', { timeout: 15000 });
    await page.locator('.category-tags .el-tag').first().click();
    await page.waitForTimeout(500);

    const clearBtn = page.locator('.el-tag:has-text("清除筛选")');
    await expect(clearBtn).toBeVisible({ timeout: 5000 });
  });
});
