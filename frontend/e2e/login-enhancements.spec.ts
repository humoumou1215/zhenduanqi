import { test, expect } from '@playwright/test';

test.describe('登录页面增强功能测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('应该显示登录页面并包含版本信息', async ({ page }) => {
    await expect(page.locator('h2')).toContainText('Arthas 远程诊断工具');
    await expect(page.locator('.login-subtitle')).toContainText('v1.0.0');
  });

  test('记住我功能应该正常工作', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    
    // 勾选记住我
    await page.check('text=记住我');
    
    // 点击登录
    await page.click('button:has-text("登 录")');
    
    // 等待导航完成
    await page.waitForURL('**/scenes');
    
    // 验证localStorage
    const rememberedUsername = await page.evaluate(() => localStorage.getItem('rememberedUsername'));
    const loginRemembered = await page.evaluate(() => localStorage.getItem('loginRemembered'));
    const lastLoginTime = await page.evaluate(() => localStorage.getItem('lastLoginTime'));
    
    expect(rememberedUsername).toBe('admin');
    expect(loginRemembered).toBe('true');
    expect(lastLoginTime).not.toBeNull();
  });

  test('密码强度指示器应该显示', async ({ page }) => {
    // 输入弱密码
    await page.fill('input[placeholder="请输入密码"]', '123');
    await expect(page.locator('.password-strength')).toBeVisible();
    await expect(page.locator('.strength-text')).toContainText('弱');
    
    // 输入中等强度密码
    await page.fill('input[placeholder="请输入密码"]', 'password123');
    await expect(page.locator('.strength-text')).toContainText('中');
    
    // 输入强密码
    await page.fill('input[placeholder="请输入密码"]', 'MySecureP@ssw0rd123');
    await expect(page.locator('.strength-text')).toContainText('强');
  });
});
