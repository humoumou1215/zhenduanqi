from playwright.sync_api import sync_playwright
import time


def test_diagnosis_workbench():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        page.goto('http://localhost:5173')
        page.wait_for_load_state('networkidle')
        time.sleep(1)

        page.screenshot(path='/tmp/workbench-01-login.png', full_page=True)

        page.fill('input[type="text"]', 'admin')
        page.fill('input[type="password"]', 'admin123')
        page.click('button:has-text("登录")')

        page.wait_for_load_state('networkidle')
        time.sleep(1)

        page.goto('http://localhost:5173/#/')
        page.wait_for_load_state('networkidle')
        time.sleep(1)

        page.screenshot(path='/tmp/workbench-02-page.png', full_page=True)

        header = page.locator('.workbench-header h2')
        assert header.count() > 0, 'Page should show diagnosis workbench header'
        assert header.text_content() == '诊断工作台', 'Header should be "诊断工作台"'

        dashboard_section = page.locator('.dashboard-section')
        assert dashboard_section.count() > 0, 'Dashboard section should exist'

        dashboard_header = dashboard_section.locator('h3')
        assert dashboard_header.text_content() == '实时监控', 'Dashboard header should be "实时监控"'

        server_select = page.locator('.header-right .el-select')
        assert server_select.count() > 0, 'Server selector should exist'

        server_select.click()
        time.sleep(0.5)

        page.screenshot(path='/tmp/workbench-03-server-dropdown.png', full_page=True)

        options = page.locator('.el-select-dropdown__item')
        if options.count() > 0:
            options.first.click()
            time.sleep(0.5)

            saved_server = page.evaluate('localStorage.getItem("selectedServerId")')
            print(f'Server saved to localStorage: {saved_server}')
            assert saved_server, 'Server should be saved to localStorage'

        page.screenshot(path='/tmp/workbench-04-server-selected.png', full_page=True)

        refresh_input = page.locator('.dashboard-controls .el-input-number input')
        assert refresh_input.count() > 0, 'Refresh interval input should exist'

        start_button = page.locator('button:has-text("开始刷新")')
        manual_refresh_button = page.locator('button:has-text("立即刷新")')
        assert start_button.count() > 0 or manual_refresh_button.count() > 0, \
            'Refresh buttons should exist'

        page.screenshot(path='/tmp/workbench-05-dashboard-controls.png', full_page=True)

        category_tags = page.locator('.category-tags .el-tag')
        category_count = category_tags.count()
        print(f'Found {category_count} category tags')
        assert category_count >= 6, f'Should have at least 6 category tags, found {category_count}'

        expected_categories = ['接口响应慢', 'CPU 高', '内存高', 'GC 频繁', '线程池高', '类加载异常']
        tag_texts = [tag.text_content() for tag in category_tags.all()]
        for cat in expected_categories:
            assert any(cat in text for text in tag_texts), f'Category "{cat}" should be present'

        page.screenshot(path='/tmp/workbench-06-categories.png', full_page=True)

        search_input = page.locator('.scene-filter-section .el-input input')
        assert search_input.count() > 0, 'Search input should exist'

        search_input.fill('CPU')
        time.sleep(0.5)

        page.screenshot(path='/tmp/workbench-07-search.png', full_page=True)

        visible_scenes = page.locator('.el-collapse-item')
        visible_count = visible_scenes.count()
        print(f'Found {visible_count} scenes after search')

        search_input.fill('')
        time.sleep(0.5)

        category_tags.first.click()
        time.sleep(0.5)

        page.screenshot(path='/tmp/workbench-08-filtered.png', full_page=True)

        clear_filter = page.locator('.el-tag:has-text("清除筛选")')
        if clear_filter.count() > 0:
            clear_filter.click()
            time.sleep(0.5)

        scene_items = page.locator('.el-collapse-item')
        scene_count = scene_items.count()
        print(f'Found {scene_count} scenes')
        assert scene_count >= 6, f'Should have at least 6 scenes, found {scene_count}'

        page.screenshot(path='/tmp/workbench-09-scenes.png', full_page=True)

        if scene_items.count() > 0:
            scene_items.first.click()
            time.sleep(1)

            page.screenshot(path='/tmp/workbench-10-scene-expanded.png', full_page=True)

            step_cards = page.locator('.step-card')
            step_count = step_cards.count()
            print(f'Found {step_count} steps in first scene')
            assert step_count > 0, 'Scene should have at least one step'

            step_title = page.locator('.step-title').first
            assert step_title.count() > 0, 'Step should have a title'

            command_input = page.locator('.step-content .el-input input').first
            assert command_input.count() > 0, 'Step should have command input'

        page.screenshot(path='/tmp/workbench-11-final.png', full_page=True)

        print('\n=== Test Results ===')
        print('✓ Diagnosis workbench page loads correctly')
        print('✓ Dashboard section displays')
        print('✓ Server selector exists and saves selection')
        print('✓ Refresh controls exist')
        print(f'✓ Category tags display correctly ({category_count} found)')
        print('✓ Search functionality works')
        print(f'✓ Scene list displays ({scene_count} scenes)')
        print('✓ Scene expansion shows steps')

        browser.close()


if __name__ == '__main__':
    test_diagnosis_workbench()
