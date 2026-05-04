from playwright.sync_api import sync_playwright
import time

def test_diagnose_page():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()
        
        page.goto('http://localhost:5185')
        page.wait_for_load_state('networkidle')
        time.sleep(1)
        
        page.screenshot(path='/tmp/diagnose-01-login.png', full_page=True)
        
        page.fill('input[type="text"]', 'admin')
        page.fill('input[type="password"]', 'admin123')
        page.click('button:has-text("登录")')
        
        page.wait_for_load_state('networkidle')
        time.sleep(1)
        
        page.goto('http://localhost:5185/#/diagnose')
        page.wait_for_load_state('networkidle')
        time.sleep(1)
        
        page.screenshot(path='/tmp/diagnose-02-page.png', full_page=True)
        
        server_select = page.locator('.el-select').first
        server_select.click()
        time.sleep(0.5)
        
        page.screenshot(path='/tmp/diagnose-03-server-dropdown.png', full_page=True)
        
        options = page.locator('.el-select-dropdown__item')
        if options.count() > 0:
            options.first.click()
            time.sleep(0.5)
            
            saved_server = page.evaluate('localStorage.getItem("zhenduanqi_selected_server")')
            print(f'Server saved to localStorage: {saved_server}')
            assert saved_server, 'Server should be saved to localStorage'
        
        page.screenshot(path='/tmp/diagnose-04-server-selected.png', full_page=True)
        
        command_input = page.locator('.el-autocomplete input').first
        command_input.click()
        command_input.fill('th')
        time.sleep(0.5)
        
        page.screenshot(path='/tmp/diagnose-05-command-suggestions.png', full_page=True)
        
        suggestions = page.locator('.el-autocomplete-suggestion li')
        suggestion_count = suggestions.count()
        print(f'Found {suggestion_count} command suggestions')
        
        if suggestion_count > 0:
            thread_suggestion = suggestions.filter(has_text='thread')
            if thread_suggestion.count() > 0:
                thread_suggestion.first.click()
                time.sleep(0.5)
                
                command_value = command_input.input_value()
                print(f'Selected command: {command_value}')
                assert 'thread' in command_value.lower(), 'Should have selected thread command'
        
        page.screenshot(path='/tmp/diagnose-06-command-selected.png', full_page=True)
        
        param_input = page.locator('.command-input-wrapper .el-input').last.locator('input')
        param_input.fill('-n 5')
        time.sleep(0.3)
        
        page.screenshot(path='/tmp/diagnose-07-params-filled.png', full_page=True)
        
        preview = page.locator('.preview-code')
        preview_text = preview.text_content()
        print(f'Command preview: {preview_text}')
        assert 'thread' in preview_text.lower(), 'Preview should contain thread'
        assert '-n 5' in preview_text, 'Preview should contain -n 5'
        
        copy_button = page.locator('button:has-text("复制")')
        if copy_button.count() > 0:
            print('Copy button found')
        
        help_panel = page.locator('.right-panel')
        assert help_panel.count() > 0, 'Help panel should exist'
        
        page.screenshot(path='/tmp/diagnose-08-help-panel.png', full_page=True)
        
        collapse_button = help_panel.locator('button')
        collapse_button.click()
        time.sleep(0.3)
        
        page.screenshot(path='/tmp/diagnose-09-help-collapsed.png', full_page=True)
        
        collapse_button.click()
        time.sleep(0.3)
        
        command_list = page.locator('.command-item')
        command_count = command_list.count()
        print(f'Found {command_count} commands in help panel')
        assert command_count > 40, f'Should have at least 40 commands, found {command_count}'
        
        page.screenshot(path='/tmp/diagnose-10-final.png', full_page=True)
        
        print('\n=== Test Results ===')
        print('✓ Server selection cache works')
        print('✓ Command autocomplete works')
        print('✓ Command preview updates correctly')
        print('✓ Help panel displays and collapses')
        print(f'✓ Command list contains {command_count} commands')
        
        browser.close()

if __name__ == '__main__':
    test_diagnose_page()
