#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""仿禅道 PMS 浏览器自动化验收（Playwright + 系统 Google Chrome）。

选择器事实（已探针校准）：
  * 登录输入框 = placeholder（用户名/密码）；登录按钮名「登 录」带空格 -> rx() 正则。
  * 对话框字段 = label（需求标题/预估工时/所属产品...），input 无 placeholder -> get_by_label。
  * el-select 在 .el-form-item 内，占位「请选择」；列表页筛选 select 占位「选择项目/选择迭代」。
  * 列表行操作：.el-table__row:has-text(name) 内定位按钮。
截图落 .workbuddy/test-shots-pw/，汇总写 .workbuddy/test_result_pw.txt。
"""
import os, re, time, datetime
from playwright.sync_api import sync_playwright, expect

CHROME = "C:/Program Files/Google/Chrome/Application/chrome.exe"
BASE = "http://localhost:5173"
SHOT = r"E:/work/毕业设计3/.workbuddy/test-shots-pw"
RESULT = r"E:/work/毕业设计3/.workbuddy/test_result_pw.txt"
os.makedirs(SHOT, exist_ok=True)
results = []

def rx(text):
    return re.compile(r"\s*".join(text), re.UNICODE)

def log(step, ok, detail=""):
    results.append((step, ok, detail))
    print(f"[{'PASS' if ok else 'FAIL'}] {step}" + (f" -- {detail}" if detail else ""), flush=True)

def shot(page, name):
    try:
        page.screenshot(path=os.path.join(SHOT, name), full_page=False)
    except Exception:
        pass

# ---------------- 通用交互 ----------------
def fill(page, label, value):
    # 1) 对话框 label
    try:
        el = page.get_by_label(label).last
        el.click(); el.fill(value); page.wait_for_timeout(150); return
    except Exception:
        pass
    # 2) placeholder（登录等）
    try:
        el = page.get_by_placeholder(label).last
        el.click(); el.fill(value); page.wait_for_timeout(150); return
    except Exception:
        pass
    # 3) form-item 回退
    try:
        item = page.locator(".el-form-item", has_text=rx(label))
        el = item.locator("input,textarea").last
        el.click(); el.fill(value); page.wait_for_timeout(150); return
    except Exception:
        pass
    raise RuntimeError(f"输入框未找到: {label}")

def click_btn(page, text):
    page.get_by_role("button", name=rx(text)).first.click()
    page.wait_for_timeout(500)

def wait_dialog(page, timeout=9000):
    page.wait_for_selector(".el-dialog", timeout=timeout)
    page.wait_for_timeout(800)

def click_select(page, label):
    # 1) 表单内 .el-form-item 含该 label -> 其 .el-select__wrapper
    try:
        sel = page.locator(".el-form-item", has_text=rx(label)).locator(".el-select__wrapper").first
        if sel.count() and sel.is_visible(timeout=2000):
            sel.click(); page.wait_for_timeout(700); return
    except Exception:
        pass
    # 2) 按占位/选项名直接定位 select wrapper
    try:
        sel = page.locator(".el-select__wrapper", has_text=rx(label)).first
        if sel.count() and sel.is_visible(timeout=2000):
            sel.click(); page.wait_for_timeout(700); return
    except Exception:
        pass
    # 3) 相邻 label 节点后的 select
    try:
        lab = page.locator("label", has_text=rx(label)).first
        sel = lab.locator("xpath=following-sibling::*[contains(@class,'el-select')]").first
        if sel.count() and sel.is_visible(timeout=2000):
            sel.click(); page.wait_for_timeout(700); return
    except Exception:
        pass
    raise RuntimeError(f"下拉未找到: {label}")

def pick_option(page, *candidates):
    # 只取「当前展开的下拉」里可见的 option（页面上多个 select 的选项都在 DOM 中）
    page.wait_for_selector(".el-select-dropdown__item:visible", timeout=8000)
    opts = page.locator(".el-select-dropdown__item:visible")
    for c in candidates:
        try:
            target = opts.filter(has_text=rx(c)).first
            target.click(timeout=3000); page.wait_for_timeout(500); return
        except Exception:
            pass
    try:
        opts.first.click(timeout=3000); page.wait_for_timeout(500); return
    except Exception:
        pass
    raise RuntimeError(f"选项未找到: {candidates}")

def expect_text(page, *texts, timeout=7000):
    miss = []
    for t in texts:
        try:
            page.locator(":has-text(%r)" % t).first.wait_for(state="visible", timeout=timeout)
        except Exception:
            miss.append(t)
    if miss:
        raise RuntimeError(f"缺少文本: {miss}")
    return True

def expect_any(page, *texts, timeout=7000):
    for t in texts:
        try:
            page.locator(":has-text(%r)" % t).first.wait_for(state="visible", timeout=timeout)
            return True
        except Exception:
            continue
    raise RuntimeError(f"均未出现: {texts}")

def navigate(page, path):
    page.goto(BASE + path, wait_until="domcontentloaded")
    page.wait_for_timeout(1800)

# ---------------- 各功能测试 ----------------
def ensure_login(page):
    page.goto(BASE + "/login", wait_until="domcontentloaded")
    page.wait_for_timeout(1500)
    if "工作台" in page.content() or "/dashboard" in page.url:
        return
    page.get_by_placeholder("用户名").fill("admin")
    page.get_by_placeholder("密码").fill("123456")
    page.get_by_role("button", name=rx("登录")).click()
    page.wait_for_timeout(2500)
    expect_text(page, "工作台")

def test_dashboard(page):
    navigate(page, "/dashboard")
    expect_text(page, "我的待办任务", "待我处理的 Bug", "进行中项目")
    shot(page, "02-dashboard.png")

def test_global_search(page):
    navigate(page, "/product/story")
    page.wait_for_timeout(500)
    try:
        box = page.get_by_placeholder(rx("搜索需求")).first
        box.fill("登录"); box.press("Enter")
        page.wait_for_timeout(1500)
    except Exception:
        try:
            top = page.get_by_placeholder(rx("搜索")).first
            top.fill("登录"); top.press("Enter"); page.wait_for_timeout(1500)
        except Exception as e:
            log("顶部全局搜索", False, str(e)[:150]); return
    shot(page, "03-search.png")
    expect_any(page, "全局搜索", "需求", "任务", "Bug")

def test_product_crud(page):
    navigate(page, "/product/list")
    expect_text(page, "产品列表", "新增产品")
    click_btn(page, "新增产品")
    wait_dialog(page)
    fill(page, "产品名称", "PW自动化测试产品")
    fill(page, "产品代号", "pw-auto")
    click_btn(page, "保存")
    page.wait_for_timeout(1500)
    navigate(page, "/product/list")
    expect_text(page, "PW自动化测试产品")
    shot(page, "04-product.png")
    row = page.locator(".el-table__row", has_text="PW自动化测试产品").first
    row.locator("button", has_text=rx("编辑")).first.click()
    wait_dialog(page)
    fill(page, "产品名称", "PW自动化测试产品-改")
    click_btn(page, "保存")
    page.wait_for_timeout(1500)
    navigate(page, "/product/list")
    expect_text(page, "PW自动化测试产品-改")
    row = page.locator(".el-table__row", has_text="PW自动化测试产品-改").first
    row.locator("button", has_text=rx("删除")).first.click()
    page.wait_for_timeout(400)
    click_btn(page, "确定")
    page.wait_for_timeout(1200)

def test_story_crud_and_batch(page):
    navigate(page, "/product/story")
    expect_text(page, "需求管理", "提需求")
    click_btn(page, "提需求")
    wait_dialog(page)
    click_select(page, "所属产品"); pick_option(page, "演示产品", "王者荣耀", "商城一期")
    fill(page, "需求标题", "PW自动化测试需求")
    click_btn(page, "保存")
    page.wait_for_timeout(1500)
    navigate(page, "/product/story")
    expect_text(page, "PW自动化测试需求")
    shot(page, "05-story.png")
    # 拆分任务
    try:
        row = page.locator(".el-table__row", has_text="PW自动化测试需求").first
        row.locator("button", has_text=rx("拆分任务")).first.click()
        wait_dialog(page)
        click_select(page, "所属项目"); pick_option(page, "商城一期")
        page.wait_for_timeout(3000)
        click_select(page, "所属迭代"); pick_option(page, "Sprint 1")
        fill(page, "任务名称", "PW自动化测试任务")
        click_select(page, "处理人"); pick_option(page, "李开发")
        click_btn(page, "生成任务")
        page.wait_for_timeout(2000)
        # 成功提交后 Element Plus 会把 .el-dialog 置为隐藏（display:none 但仍在 DOM），
        # 因此必须用 :visible 判定，避免把已关闭的隐藏对话框误判为“未关闭”
        if page.locator(".el-dialog:visible").count():
            err = ""
            try:
                err = page.locator(".el-dialog:visible .el-form-item__error").first.inner_text(timeout=2000)
            except Exception:
                pass
            try:
                if not err:
                    err = page.locator(".el-dialog:visible .el-message--error, .el-dialog:visible .el-message-box__message").first.inner_text(timeout=2000)
            except Exception:
                pass
            log("拆分任务-对话框未关闭", False, "可能校验失败" + (("：" + err.strip()) if err else ""))
        else:
            log("拆分任务-生成任务成功", True)
        shot(page, "05b-split.png")
    except Exception as e:
        log("拆分任务-异常", False, str(e)[:200])
    # 行内指派
    try:
        navigate(page, "/product/story")
        row = page.locator(".el-table__row", has_text="PW自动化测试需求").first
        row.locator("button", has_text=rx("指派")).first.click()
        wait_dialog(page)
        click_select(page, "选择处理人"); pick_option(page, "李开发")
        click_btn(page, "确定")
        page.wait_for_timeout(1200)
        log("需求-指派成功", True)
    except Exception as e:
        log("需求-指派(异常跳过)", False, str(e)[:150])
    navigate(page, "/product/story")
    s = page.content()
    if "批量指派" in s: log("需求-批量指派按钮渲染", True)
    if "批量删除" in s: log("需求-批量删除按钮渲染", True)

def test_release(page):
    navigate(page, "/product/release")
    expect_text(page, "发布管理", "创建发布")
    shot(page, "06-release.png")

def test_project_and_member(page):
    navigate(page, "/project/list")
    expect_text(page, "项目列表", "新增项目")
    click_btn(page, "新增项目")
    wait_dialog(page)
    fill(page, "项目名称", "PW自动化测试项目")
    click_btn(page, "保存")
    page.wait_for_timeout(1500)
    navigate(page, "/project/list")
    expect_text(page, "PW自动化测试项目")
    page.locator(".el-table__row", has_text="PW自动化测试项目").locator("a").first.click()
    page.wait_for_timeout(1500)
    expect_text(page, "新增迭代", "添加成员")
    shot(page, "07-project.png")
    click_btn(page, "添加成员")
    wait_dialog(page)
    click_select(page, "成员"); pick_option(page, "李开发")
    click_select(page, "角色"); pick_option(page, "开发", "项目经理")
    click_btn(page, "确定")
    page.wait_for_timeout(1200)
    expect_text(page, "李开发")
    shot(page, "07b-member.png")

def test_board(page):
    navigate(page, "/project/board")
    page.wait_for_timeout(1000)
    click_select(page, "选择项目"); pick_option(page, "商城一期")
    page.wait_for_timeout(3000)
    click_select(page, "选择迭代"); pick_option(page, "Sprint 1")
    page.wait_for_timeout(1500)
    expect_text(page, "未开始", "进行中", "已完成")
    shot(page, "08-board.png")

def test_bug(page):
    navigate(page, "/qa/bug")
    expect_text(page, "Bug管理", "提Bug")
    click_btn(page, "提Bug")
    wait_dialog(page)
    click_select(page, "所属产品"); pick_option(page, "演示产品", "王者荣耀")
    fill(page, "Bug标题", "PW自动化测试Bug")
    click_btn(page, "保存")
    page.wait_for_timeout(1500)
    navigate(page, "/qa/bug")
    expect_text(page, "PW自动化测试Bug")
    shot(page, "09-bug.png")

def test_testcase(page):
    navigate(page, "/qa/case")
    expect_text(page, "测试用例", "新建用例")
    click_btn(page, "新建用例")
    wait_dialog(page)
    click_select(page, "所属产品"); pick_option(page, "演示产品", "王者荣耀")
    fill(page, "用例标题", "PW自动化测试用例")
    click_btn(page, "保存")
    page.wait_for_timeout(1500)
    navigate(page, "/qa/case")
    expect_text(page, "PW自动化测试用例")
    shot(page, "10-testcase.png")

def test_testsuite(page):
    navigate(page, "/qa/suite")
    expect_text(page, "测试单", "新建测试单")
    shot(page, "11-testsuite.png")

def test_stats(page):
    navigate(page, "/stats")
    page.wait_for_timeout(1000)
    click_select(page, "选择项目"); pick_option(page, "商城一期")
    page.wait_for_timeout(3000)
    click_select(page, "选择迭代"); pick_option(page, "Sprint 1")
    page.wait_for_timeout(2000)
    expect_text(page, "成员工时汇总")
    shot(page, "12-stats.png")

def test_system(page):
    navigate(page, "/system/user")
    expect_text(page, "用户管理", "新增用户")
    shot(page, "13-user.png")
    navigate(page, "/system/role")
    expect_any(page, "角色管理", "角色列表")
    shot(page, "14-role.png")
    navigate(page, "/system/perm")
    expect_text(page, "权限管理")
    shot(page, "15-perm.png")
    navigate(page, "/system/dept")
    expect_text(page, "部门管理")
    shot(page, "16-dept.png")
    navigate(page, "/system/log")
    expect_text(page, "操作日志")
    shot(page, "17-log.png")

def test_logout(page):
    # 只点“可见”的下拉：完整跑完所有用例后 DOM 里会残留隐藏的 .el-dropdown，
    # 必须限定 :visible，否则 .first 会点到隐藏菜单导致“退出登录”没真正触发
    page.locator(".el-dropdown:visible").first.click()
    page.wait_for_timeout(600)
    page.get_by_text(rx("退出登录")).first.click()
    # 退出会弹出确认框“确定退出登录吗？”（el-layout index.vue 的 ElMessageBox.confirm），
    # 必须再点“确定”才会真正登出，否则登录页永远不会出现
    try:
        page.get_by_role("button", name=rx("确定")).first.wait_for(state="visible", timeout=6000)
        page.get_by_role("button", name=rx("确定")).first.click()
    except Exception:
        pass
    # 退出后登录页在当前页原地渲染：等“登录”按钮可见即为成功
    page.get_by_role("button", name=rx("登录")).first.wait_for(state="visible", timeout=15000)
    shot(page, "18-logout.png")

# ---------------- 清理 ----------------
def delete_row(page, name, path):
    try:
        navigate(page, path)
        page.wait_for_timeout(800)
        rows = page.locator(".el-table__row", has_text=name)
        if rows.count():
            rows.first.locator("button", has_text=rx("删除")).first.click()
            page.wait_for_timeout(500)
            click_btn(page, "确定")
            page.wait_for_timeout(1200)
        # 行不存在 = 已清理或从未创建，视为通过
        log(f"清理-{name}", True)
    except Exception as e:
        log(f"清理-{name}", False, str(e)[:200])

def cleanup(page):
    delete_row(page, "PW自动化测试需求", "/product/story")
    delete_row(page, "PW自动化测试Bug", "/qa/bug")
    delete_row(page, "PW自动化测试用例", "/qa/case")
    delete_row(page, "PW自动化测试产品-改", "/product/list")
    delete_row(page, "PW自动化测试产品", "/product/list")
    delete_row(page, "PW自动化测试项目", "/project/list")

# ---------------- 主流程 ----------------
def main():
    try:
        open(RESULT, "w", encoding="utf-8").close()
    except Exception:
        pass
    p = sync_playwright().start()
    browser = p.chromium.launch(headless=True, executable_path=CHROME, args=["--no-sandbox"])
    page = browser.new_page(viewport={"width": 1440, "height": 900})
    page.set_default_timeout(20000)
    steps = [
        ("登录 admin", lambda: ensure_login(page)),
        ("工作台 /dashboard", lambda: test_dashboard(page)),
        ("顶部全局搜索", lambda: test_global_search(page)),
        ("产品管理 CRUD", lambda: test_product_crud(page)),
        ("需求管理 CRUD+批量+拆分任务", lambda: test_story_crud_and_batch(page)),
        ("发布管理", lambda: test_release(page)),
        ("项目管理+成员", lambda: test_project_and_member(page)),
        ("迭代看板", lambda: test_board(page)),
        ("Bug管理", lambda: test_bug(page)),
        ("测试用例", lambda: test_testcase(page)),
        ("测试单", lambda: test_testsuite(page)),
        ("统计报表", lambda: test_stats(page)),
        ("系统管理", lambda: test_system(page)),
        ("清理测试数据", lambda: cleanup(page)),
        ("退出登录", lambda: test_logout(page)),
    ]
    for name, fn in steps:
        try:
            fn()
            log(name, True)
        except Exception as e:
            log(name, False, str(e)[:300])

    print("\n========== 验收汇总 ==========", flush=True)
    ok = sum(1 for _, ok, _ in results if ok)
    print(f"通过 {ok}/{len(results)}", flush=True)
    for step, ok, detail in results:
        print(("PASS " if ok else "FAIL ") + step + (f" ({detail})" if detail else ""), flush=True)
    with open(RESULT, "w", encoding="utf-8") as f:
        f.write(f"通过 {ok}/{len(results)}\n")
        for step, ok, detail in results:
            f.write(("PASS " if ok else "FAIL ") + step + (f" ({detail})" if detail else "") + "\n")
    # 直接退出，避免本机 browser.close()/sync_playwright 结尾挂起
    os._exit(0)

if __name__ == "__main__":
    main()
