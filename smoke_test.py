#!/usr/bin/env python
"""
PMS 系统冒烟测试脚本
- 测试 dev01 + admin 两个账号
- 覆盖全部核心模块：登录、auth、产品、需求、项目、迭代、任务、Bug、用例、统计、看板、搜索、附件
- 结果输出到 /tmp/smoke_report.txt
"""
import json, urllib.request, sys, os, traceback

BASE = "http://localhost:8081"
results = []
errors = []

PASS, FAIL, WARN = "✅", "❌", "⚠️"

def log(msg):
    print(msg)

def req(method, path, token=None, body=None, desc=""):
    url = f"{BASE}{path}"
    data = json.dumps(body).encode("utf-8") if body else None
    req_obj = urllib.request.Request(
        url, data=data,
        headers={"Content-Type": "application/json"}
    )
    if token:
        req_obj.add_header("Authorization", f"Bearer {token}")
    req_obj.method = method
    try:
        resp = urllib.request.urlopen(req_obj, timeout=10)
        code = resp.getcode()
        raw = resp.read().decode("utf-8")
        j = json.loads(raw) if raw else {}
        biz_code = j.get("code", -1)
        return (code, biz_code, j)
    except urllib.error.HTTPError as e:
        try:
            raw = e.read().decode("utf-8")
            j = json.loads(raw) if raw else {}
        except:
            j = {"error": raw if raw else str(e)}
        return (e.code, j.get("code", -1), j)
    except Exception as e:
        return (0, -1, {"error": str(e)})

def test(desc, method, path, token=None, body=None, expect_biz=200):
    http_code, biz_code, j = req(method, path, token, body, desc)
    ok = (http_code == 200 and biz_code == expect_biz)
    status = PASS if ok else (FAIL if http_code == 0 else WARN)
    brief = j.get("message", str(j)[:100])
    line = f"  {status} {desc} → HTTP {http_code} 业务码 {biz_code}  msg={brief}"
    results.append(line)
    log(line)
    if not ok:
        errors.append(f"  {desc}: HTTP {http_code} biz={biz_code} {brief}")
    return ok, j

def login(username, password):
    http, biz, j = req("POST", "/api/auth/login", body={"username": username, "password": password})
    if biz == 200:
        return j["data"]["token"]
    log(f"  ❌ 登录失败: {j}")
    return None

def run_module(module_name, tests_fn):
    log(f"\n{'='*60}")
    log(f"  【{module_name}】")
    log(f"{'='*60}")
    tests_fn()

def get_id(j, ok):
    """从响应中提取 ID，兼容 data 是数字 or 对象的情况"""
    if not ok:
        return None
    data = j.get("data")
    if isinstance(data, dict):
        return data.get("id")
    if isinstance(data, (int, float)):
        return int(data)
    return None

def run():
    # 1. 登录 dev01
    log("\n" + "="*60)
    log("  第一阶段：dev01 账号（开发角色，无权限）")
    log("="*60)
    tok1 = login("dev01", "123456")
    if not tok1:
        return

    def dev01_tests():
        # auth/info
        test("auth/info", "GET", "/api/auth/info", tok1)

        # 产品
        test("GET /products/page", "GET", "/api/products/page?page=1&size=10", tok1)
        test("GET /products/options", "GET", "/api/products/options", tok1)

        # 需求
        test("GET /stories/page", "GET", "/api/stories/page?page=1&size=10", tok1)
        test("GET /stories/options", "GET", "/api/stories/options", tok1)

        # 项目
        test("GET /projects/page", "GET", "/api/projects/page?page=1&size=10", tok1)
        test("GET /projects/options", "GET", "/api/projects/options", tok1)

        # 迭代
        test("GET /sprints/options", "GET", "/api/sprints/options", tok1)

        # 任务
        test("GET /tasks/page", "GET", "/api/tasks/page?page=1&size=10", tok1)
        test("GET /tasks/board", "GET", "/api/tasks/board?sprintId=1", tok1)

        # Bug
        test("GET /bugs/page", "GET", "/api/bugs/page?page=1&size=10", tok1)

        # 仪表盘
        test("GET /dashboard/summary", "GET", "/api/dashboard/summary", tok1)
        test("GET /dashboard/my-tasks", "GET", "/api/dashboard/my-tasks", tok1)
        test("GET /dashboard/my-bugs", "GET", "/api/dashboard/my-bugs", tok1)

        # 统计
        test("GET /stats/burndown", "GET", "/api/stats/burndown?sprintId=1", tok1)
        test("GET /stats/bug-distribution", "GET", "/api/stats/bug-distribution", tok1)
        test("GET /stats/task-distribution", "GET", "/api/stats/task-distribution?sprintId=1", tok1)

        # 搜索
        test("GET /search?keyword", "GET", "/api/search?keyword=test", tok1)

        # 用户
        test("GET /users/page", "GET", "/api/users/page?page=1&size=10", tok1)
        test("GET /users/options", "GET", "/api/users/options", tok1)

        # 角色
        test("GET /roles/page", "GET", "/api/roles/page?page=1&size=10", tok1)
        test("GET /roles/options", "GET", "/api/roles/options", tok1)

        # 权限树
        test("GET /perms/tree", "GET", "/api/perms/tree", tok1)

        # 部门树
        test("GET /depts/tree", "GET", "/api/depts/tree", tok1)

        # 日志
        test("GET /logs/page", "GET", "/api/logs/page?page=1&size=10", tok1)

        # 用例
        test("GET /testcases/page", "GET", "/api/testcases/page?page=1&size=10", tok1)
        test("GET /test-suites/page", "GET", "/api/test-suites/page?page=1&size=10", tok1)

        # 发布
        test("GET /releases/page", "GET", "/api/releases/page?page=1&size=10", tok1)

    run_module("dev01 全部只读接口", dev01_tests)

    # 2. 登录 admin
    log("\n" + "="*60)
    log("  第二阶段：admin 账号（系统管理员，全权限）")
    log("="*60)
    tok2 = login("admin", "123456")
    if not tok2:
        return

    def admin_tests():
        # auth/info - 查看权限
        ok, j = test("auth/info", "GET", "/api/auth/info", tok2)
        if ok:
            perms = j.get("data", {}).get("perms", [])
            log(f"  ℹ️   admin 权限数: {len(perms)}")
            menus = j.get("data", {}).get("menus", [])
            log(f"  ℹ️   admin 菜单数: {len(menus)}")

        # 读接口
        test("GET /products/page", "GET", "/api/products/page?page=1&size=10", tok2)
        test("GET /stories/page", "GET", "/api/stories/page?page=1&size=10", tok2)
        test("GET /projects/page", "GET", "/api/projects/page?page=1&size=10", tok2)
        test("GET /tasks/page", "GET", "/api/tasks/page?page=1&size=10", tok2)
        test("GET /bugs/page", "GET", "/api/bugs/page?page=1&size=10", tok2)

        # 写接口测试 - 创建产品
        ok, j = test("POST /products 创建", "POST", "/api/products", tok2,
                     body={"name": "测试产品_冒烟", "code": "SMOKE01"})
        product_id = get_id(j, ok)
        if product_id:
            log(f"  ℹ️   创建产品 ID={product_id}")

        # 创建需求
        ok2, j2 = test("POST /stories 创建", "POST", "/api/stories", tok2,
                       body={"title": "测试需求_冒烟", "productId": product_id or 1, "pri": 2})
        story_id = get_id(j2, ok2)
        if story_id:
            log(f"  ℹ️   创建需求 ID={story_id}")

        # 创建项目
        ok3, j3 = test("POST /projects 创建", "POST", "/api/projects", tok2,
                       body={"name": "测试项目_冒烟", "code": "PRJ_SMOKE"})
        project_id = get_id(j3, ok3)
        if project_id:
            log(f"  ℹ️   创建项目 ID={project_id}")

        # 创建迭代
        ok4, j4 = test("POST /sprints 创建", "POST", "/api/sprints", tok2,
                       body={"name": "Sprint_冒烟", "projectId": project_id or 1})
        sprint_id = get_id(j4, ok4)
        if sprint_id:
            log(f"  ℹ️   创建迭代 ID={sprint_id}")

        # 创建任务
        ok5, j5 = test("POST /tasks 创建", "POST", "/api/tasks", tok2,
                       body={"name": "测试任务_冒烟", "projectId": project_id or 1,
                             "sprintId": sprint_id or 1, "assignedTo": "dev01", "taskType": "development"})
        task_id = get_id(j5, ok5)
        if task_id:
            log(f"  ℹ️   创建任务 ID={task_id}")

        # 创建 Bug
        ok6, j6 = test("POST /bugs 创建", "POST", "/api/bugs", tok2,
                       body={"title": "测试Bug_冒烟", "productId": product_id or 1,
                             "projectId": project_id or 1, "severity": 3})
        bug_id = get_id(j6, ok6)
        if bug_id:
            log(f"  ℹ️   创建Bug ID={bug_id}")

        # 需求拆分任务
        if story_id:
            test("POST /stories/{id}/tasks 拆分任务", "POST", f"/api/stories/{story_id}/tasks", tok2,
                 body={"name": "拆分任务_冒烟", "sprintId": sprint_id or 1, "assignedTo": "dev01"})

        # 状态流转
        if task_id:
            test("PUT /tasks/{id}/status 任务状态", "PUT", f"/api/tasks/{task_id}/status", tok2,
                 body={"status": "doing"})
        if bug_id and story_id:
            test("PUT /bugs/{id}/status Bug状态", "PUT", f"/api/bugs/{bug_id}/status", tok2,
                 body={"status": "resolved", "resolvedStoryId": story_id})

        # 指派
        if task_id:
            test("PUT /tasks/{id}/assign", "PUT", f"/api/tasks/{task_id}/assign", tok2,
                 body={"assignedTo": "dev01"})
        if bug_id:
            test("PUT /bugs/{id}/assign", "PUT", f"/api/bugs/{bug_id}/assign", tok2,
                 body={"assignedTo": "dev01"})

        # 看板拖拽
        if task_id:
            test("PUT /tasks/{id}/move", "PUT", f"/api/tasks/{task_id}/move", tok2,
                 body={"status": "done"})

        # 工时
        if task_id:
            test("PUT /tasks/{id}/hours", "PUT", f"/api/tasks/{task_id}/hours", tok2,
                 body={"consumedHours": 2.5, "remainingHours": 1.0})

        # 工时汇总
        test("GET /tasks/workhour-summary", "GET", "/api/tasks/workhour-summary", tok2)

        # 项目成员
        if project_id:
            test("GET /projects/{id}/members", "GET", f"/api/projects/{project_id}/members", tok2)
            test("POST /projects/{id}/members", "POST", f"/api/projects/{project_id}/members", tok2,
                 body={"userId": 4, "role": "developer"})

        # 产品计划
        if product_id:
            test("GET /products/{id}/plans", "GET", f"/api/products/{product_id}/plans", tok2)
            test("POST /products/{id}/plans", "POST", f"/api/products/{product_id}/plans", tok2,
                 body={"title": "测试计划_冒烟"})

        # 迭代关联需求
        if sprint_id and story_id:
            test("POST /sprints/{id}/stories", "POST", f"/api/sprints/{sprint_id}/stories", tok2,
                 body={"storyIds": [story_id]})

        # 批量操作
        test("POST /stories/batch-delete", "POST", "/api/stories/batch-delete", tok2,
             body={"ids": [story_id] if story_id else [99999]})
        test("POST /bugs/batch-delete", "POST", "/api/bugs/batch-delete", tok2,
             body={"ids": [bug_id] if bug_id else [99999]})
        test("POST /stories/batch-assign", "POST", "/api/stories/batch-assign", tok2,
             body={"ids": [story_id] if story_id else [99999], "assignedTo": "dev01"})
        test("POST /bugs/batch-assign", "POST", "/api/bugs/batch-assign", tok2,
             body={"ids": [bug_id] if bug_id else [99999], "assignedTo": "dev01"})

        # 迭代详情
        if sprint_id:
            test("GET /sprints/{id}/stories", "GET", f"/api/sprints/{sprint_id}/stories", tok2)

    run_module("admin 全部接口（含写操作）", admin_tests)

    # 3. 汇总
    log("\n" + "="*60)
    log("  测试汇总")
    log("="*60)
    total = len(results)
    passed = sum(1 for r in results if r.startswith(PASS))
    warned = sum(1 for r in results if r.startswith(WARN))
    failed = sum(1 for r in results if r.startswith(FAIL))
    log(f"  总计: {total}  |  通过: {passed}  |  警告: {warned}  |  失败: {failed}")
    if errors:
        log(f"\n  ⚠️  异常详情 ({len(errors)} 条):")
        for e in errors[:30]:
            log(e)
        if len(errors) > 30:
            log(f"  ... 还有 {len(errors)-30} 条未显示")

    # 写报告
    report = []
    report.append("PMS 系统冒烟测试报告")
    report.append(f"时间: {__import__('datetime').datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    report.append(f"总计: {total}  |  通过: {passed}  |  警告: {warned}  |  失败: {failed}")
    report.append("")
    report.extend(results)
    if errors:
        report.append("")
        report.append("--- 异常详情 ---")
        report.extend(errors[:30])
    with open("/tmp/smoke_report.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(report))
    log("\n  报告已写入 /tmp/smoke_report.txt")

if __name__ == "__main__":
    run()