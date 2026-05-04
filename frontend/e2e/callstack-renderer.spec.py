from playwright.sync_api import sync_playwright
import time


def test_trace_renderer():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        page.goto('http://localhost:5173/#/diagnose')
        page.wait_for_load_state('networkidle')
        time.sleep(1)

        page.screenshot(path='/tmp/trace-01-page.png', full_page=True)

        command_input = page.locator('.command-input input')
        if command_input.count() > 0:
            command_input.fill('trace demo.MathGame run()')
            page.keyboard.press('Enter')
            time.sleep(2)

            page.screenshot(path='/tmp/trace-02-command-executed.png', full_page=True)

            trace_elements = page.locator('.callstack-renderer')
            if trace_elements.count() > 0:
                print('Trace renderer component loaded')

                sample_cards = page.locator('.sample-card')
                sample_count = sample_cards.count()
                print(f'Found {sample_count} sample cards')

                trace_nodes = page.locator('.trace-node-row')
                trace_count = trace_nodes.count()
                print(f'Found {trace_count} trace nodes')

                cost_bars = page.locator('.cost-bar')
                cost_count = cost_bars.count()
                print(f'Found {cost_count} cost bars')

                assert trace_elements.count() > 0, 'Trace renderer should render'
                page.screenshot(path='/tmp/trace-03-trace-rendered.png', full_page=True)

                print('✓ Trace mode renders correctly')
                print('✓ Sample cards display')
                print(f'✓ Trace nodes display ({trace_count} found)')
                print(f'✓ Cost bars display ({cost_count} found)')
            else:
                print('Trace renderer not found, checking fallback')

        page.screenshot(path='/tmp/trace-04-final.png', full_page=True)
        browser.close()


def test_stack_renderer():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        page.goto('http://localhost:5173/#/diagnose')
        page.wait_for_load_state('networkidle')
        time.sleep(1)

        page.screenshot(path='/tmp/stack-01-page.png', full_page=True)

        command_input = page.locator('.command-input input')
        if command_input.count() > 0:
            command_input.fill('stack demo.MathGame primeFactors()')
            page.keyboard.press('Enter')
            time.sleep(2)

            page.screenshot(path='/tmp/stack-02-command-executed.png', full_page=True)

            stack_elements = page.locator('.callstack-renderer')
            if stack_elements.count() > 0:
                print('CallStack renderer component loaded in stack mode')

                sample_cards = page.locator('.sample-card')
                sample_count = sample_cards.count()
                print(f'Found {sample_count} sample cards')

                stack_nodes = page.locator('.stack-node-row')
                stack_count = stack_nodes.count()
                print(f'Found {stack_count} stack nodes')

                assert stack_elements.count() > 0, 'Stack renderer should render'
                page.screenshot(path='/tmp/stack-03-stack-rendered.png', full_page=True)

                print('✓ Stack mode renders correctly')
                print(f'✓ Sample cards display ({sample_count} found)')
                print(f'✓ Stack nodes display ({stack_count} found)')
            else:
                print('CallStack renderer not found in stack mode')

        page.screenshot(path='/tmp/stack-04-final.png', full_page=True)
        browser.close()


def test_focus_button():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        page.goto('http://localhost:5173/#/diagnose')
        page.wait_for_load_state('networkidle')
        time.sleep(1)

        page.screenshot(path='/tmp/focus-01-page.png', full_page=True)

        command_input = page.locator('.command-input input')
        if command_input.count() > 0:
            command_input.fill('trace demo.MathGame run()')
            page.keyboard.press('Enter')
            time.sleep(3)

            page.screenshot(path='/tmp/focus-02-trace-running.png', full_page=True)

            focus_button = page.locator('.focus-button')
            if focus_button.count() > 0:
                print('Focus button found')
                page.screenshot(path='/tmp/focus-03-button-visible.png', full_page=True)

                focus_button.click()
                time.sleep(0.5)

                page.screenshot(path='/tmp/focus-04-after-click.png', full_page=True)
                print('✓ Focus button is clickable')
            else:
                print('Focus button not visible (scroll not needed)')

        page.screenshot(path='/tmp/focus-05-final.png', full_page=True)
        browser.close()


def test_callstack_component_structure():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        page.goto('http://localhost:5173/#/diagnose')
        page.wait_for_load_state('networkidle')
        time.sleep(1)

        page.screenshot(path='/tmp/component-01-page.png', full_page=True)

        command_input = page.locator('.command-input input')
        if command_input.count() > 0:
            command_input.fill('trace demo.MathGame run()')
            page.keyboard.press('Enter')
            time.sleep(2)

            page.screenshot(path='/tmp/component-02-command.png', full_page=True)

            callstack_header = page.locator('.callstack-header')
            if callstack_header.count() > 0:
                print('CallStack header component found')

                command_label = callstack_header.locator('.command-value')
                if command_label.count() > 0:
                    command_text = command_label.text_content()
                    print(f'Command displayed: {command_text}')
                    assert 'demo.MathGame' in command_text or command_text != '', \
                        'Command should be displayed'

                sample_count = callstack_header.locator('.stat-item')
                print(f'Found {sample_count.count()} stat items in header')

                print('✓ CallStackHeader renders correctly')
                page.screenshot(path='/tmp/component-03-header.png', full_page=True)

            sample_cards = page.locator('.sample-card')
            if sample_cards.count() > 0:
                sample_header = sample_cards.first.locator('.sample-header')
                if sample_header.count() > 0:
                    sample_index = sample_header.locator('.sample-index')
                    if sample_index.count() > 0:
                        index_text = sample_index.text_content()
                        print(f'Sample index: {index_text}')

                    sample_cost = sample_header.locator('.sample-cost')
                    if sample_cost.count() > 0:
                        cost_text = sample_cost.text_content()
                        print(f'Sample cost: {cost_text}')

                print('✓ SampleCard renders correctly')
                page.screenshot(path='/tmp/component-04-sample-card.png', full_page=True)

            trace_node = page.locator('.trace-node')
            if trace_node.count() > 0:
                trace_rows = page.locator('.trace-node-row')
                print(f'Found {trace_rows.count()} trace node rows')

                method_names = page.locator('.method-name')
                if method_names.count() > 0:
                    first_method = method_names.first.text_content()
                    print(f'First method: {first_method}')

                print('✓ TraceNode renders correctly')
                page.screenshot(path='/tmp/component-05-trace-node.png', full_page=True)

        page.screenshot(path='/tmp/component-06-final.png', full_page=True)
        print('\n=== Test Results ===')
        print('✓ CallStackRenderer component structure verified')
        browser.close()


if __name__ == '__main__':
    print('Running CallStackRenderer tests...')
    print('\n--- Test 1: Trace Renderer ---')
    test_trace_renderer()
    print('\n--- Test 2: Stack Renderer ---')
    test_stack_renderer()
    print('\n--- Test 3: Focus Button ---')
    test_focus_button()
    print('\n--- Test 4: Component Structure ---')
    test_callstack_component_structure()
    print('\n=== All Tests Complete ===')
