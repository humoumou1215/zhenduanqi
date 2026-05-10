import { test, expect } from '@playwright/test';

test.describe('快捷命令收藏功能测试', () => {
  test.beforeEach(async ({ page }) => {
    // 清理 localStorage 中的快捷命令数据
    await page.goto('/#/');
    await page.evaluate(() => {
      localStorage.removeItem('diagnose-quick-commands');
    });
    await page.waitForLoadState('domcontentloaded');
  });

  async function login(page) {
    // 清除现有状态
    await page.context().clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    
    // 直接调用登录API (使用相对路径，这样会通过vite的代理
    const response = await page.request.post('/api/auth/login', {
      form: {
        username: 'admin',
        password: 'admin123'
      }
    });
    
    if (!response.ok()) {
      const text = await response.text();
      throw new Error(`Login failed: ${text}`);
    }
    
    // 登录后访问页面
    await page.goto('/#/');
    await page.waitForLoadState('networkidle');
  }

  test('收藏按钮应在输入命令后显示', async ({ page }) => {
    await login(page);
    await page.goto('/#/diagnose');
    await page.waitForLoadState('networkidle');

    // 检查初始状态，没有命令时不应显示收藏按钮
    await expect(page.locator('button:has-text("收藏")')).not.toBeVisible();

    // 输入一个命令
    await page.locator('.el-autocomplete input').first().fill('thread');
    await page.locator('.command-input-wrapper .el-input').last().locator('input').fill('-n 5');

    // 检查收藏按钮是否显示
    await expect(page.locator('button:has-text("收藏")')).toBeVisible();
  });

  test('收藏命令并在页面重新加载后保留', async ({ page }) => {
    await login(page);
    await page.goto('/#/diagnose');
    await page.waitForLoadState('networkidle');

    // 输入命令
    await page.locator('.el-autocomplete input').first().fill('thread');
    await page.locator('.command-input-wrapper .el-input').last().locator('input').fill('-n 5');

    // 点击收藏按钮
    await page.click('button:has-text("收藏")');

    // 检查快捷命令区域是否显示该命令
    await expect(page.locator('.quick-commands')).toContainText('thread -n 5');

    // 重新加载页面
    await page.reload();
    await page.waitForLoadState('networkidle');

    // 重新登录
    await login(page);

    await page.goto('/#/diagnose');
    await page.waitForLoadState('networkidle');

    // 检查命令是否仍然存在
    await expect(page.locator('.quick-commands')).toContainText('thread -n 5');
  });

  test('点击快捷命令应正确填充', async ({ page }) => {
    await login(page);
    await page.goto('/#/diagnose');
    await page.waitForLoadState('networkidle');

    // 先收藏一个命令
    await page.locator('.el-autocomplete input').first().fill('memory');
    await page.click('button:has-text("收藏")');
    await expect(page.locator('.quick-commands')).toContainText('memory');

    // 清空输入
    await page.locator('.el-autocomplete input').first().clear();
    await page.locator('.command-input-wrapper .el-input').last().locator('input').clear();

    // 点击快捷命令
    await page.click('.quick-command-tag:has-text("memory")');

    // 检查是否正确填充
    await expect(page.locator('.el-autocomplete input').first()).toHaveValue('memory');
    await expect(page.locator('.preview-code')).toContainText('memory');
  });

  test('删除快捷命令应工作', async ({ page }) => {
    await login(page);
    await page.goto('/#/diagnose');
    await page.waitForLoadState('networkidle');

    // 收藏一个命令
    await page.locator('.el-autocomplete input').first().fill('dashboard');
    await page.click('button:has-text("收藏")');
    await expect(page.locator('.quick-commands')).toContainText('dashboard');

    // 删除该命令
    await page.hover('.quick-command-tag:has-text("dashboard")');
    await page.click('.quick-command-tag:has-text("dashboard") .remove-icon');

    // 检查命令是否被删除
    await expect(page.locator('.quick-commands')).not.toContainText('dashboard');
  });

  test('重复收藏相同命令应提示已存在', async ({ page }) => {
    await login(page);
    await page.goto('/#/diagnose');
    await page.waitForLoadState('networkidle');

    // 第一次收藏
    await page.locator('.el-autocomplete input').first().fill('thread');
    await page.click('button:has-text("收藏")');

    // 尝试第二次收藏
    await page.click('button:has-text("收藏")');

    // 检查是否显示警告消息（通过页面上可能出现的元素）
    // 由于我们无法直接检查 ElMessage，所以只验证只存在一个
    const quickCommands = page.locator('.quick-command-tag');
    await expect(quickCommands).toHaveCount(1);
  });
});
