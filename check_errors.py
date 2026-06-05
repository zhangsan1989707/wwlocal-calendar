from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        # 捕获控制台消息
        console_messages = []
        page.on('console', lambda msg: console_messages.append({
            'type': msg.type,
            'text': msg.text
        }))
        
        # 访问页面
        page.goto('http://localhost:3007/')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(2000)  # 等待额外时间让异步请求完成
        
        # 打印控制台消息
        print("控制台消息：")
        for msg in console_messages:
            if msg['type'] in ['error', 'warning']:
                print(f"  [{msg['type'].upper()}] {msg['text']}")
        
        # 检查网络请求
        print("\n网络请求：")
        for response in page.evaluate("""() => {
            return window.performance.getEntriesByType('resource')
                .filter(r => r.initiatorType === 'xmlhttprequest' || r.initiatorType === 'fetch')
                .map(r => ({
                    name: r.name,
                    responseTime: r.responseStart - r.requestStart,
                    transferSize: r.transferSize
                }));
        """):
            print(f"  {response}")
        
        # 截图保存
        page.screenshot(path='error_screenshot.png', full_page=True)
        print("\n截图已保存到 error_screenshot.png")
        
        browser.close()

if __name__ == '__main__':
    run()
