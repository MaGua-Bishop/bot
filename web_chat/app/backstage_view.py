from django.contrib.admin.views.decorators import staff_member_required
from django.shortcuts import render
from django.http import JsonResponse
from datetime import datetime, timedelta

from app.models import LotteryRecord,BetRecord


@staff_member_required
def kaijiangxinxi(request):
    current_results = [
        {"date": "2024-11-30", "result": "10, 20, 30, 40"},
    ]
    history_results = [
        {"date": "2024-11-29", "result": "15, 25, 35, 45"},
        {"date": "2024-11-28", "result": "5, 10, 15, 20"},
    ]
    return render(request, 'admin/kaijiangxinxi.html', {
        "current_results": current_results,
        "history_results": history_results,
    })

@staff_member_required
def kaijiangxinxi(request):
    current_results = [
        {"date": "2024-11-30", "result": "10, 20, 30, 40"},
    ]
    history_results = [
        {"date": "2024-11-29", "result": "15, 25, 35, 45"},
        {"date": "2024-11-28", "result": "5, 10, 15, 20"},
    ]
    return render(request, 'admin/kaijiangxinxi.html', {
        "current_results": current_results,
        "history_results": history_results,
    })

@staff_member_required
def tongjifenxi(request):
    # 生成最近五天的日期
    today = datetime.now()
    date_list = [(today - timedelta(days=i)).strftime('%Y-%m-%d') for i in range(5)]

    # 模拟数据，这里可以从数据库获取统计分析数据
    current_summary = [
        {"date": "2024-11-30", "total": "96.69", "revenue": "40933", "profit": "-9401.68"},
    ]

    # 将数据传递给模板
    context = {
        "current_summary": current_summary,
        "date_list": date_list,  # 将日期列表传递给模板
    }
    return render(request, 'admin/tongjifenxi.html', context)

@staff_member_required
def weijiesuan(request):
    # 获取当前管理员的用户名
    admin_username = request.user.username
    unprocessed_orders = BetRecord.objects.filter(admin_username=admin_username, status=0).values(
        'admin_username', 'user_name', 'bet_type', 'issue', 'created_at'
    )

    # 将数据转换为列表
    unprocessed_orders_list = list(unprocessed_orders)

    # 将数据传递给模板
    context = {
        "unprocessed_orders": unprocessed_orders_list,
    }
    return render(request, 'admin/weijiesuan.html', context)

@staff_member_required
def lishi(request):
    # 获取最新的100条开奖记录
    history_results = LotteryRecord.objects.filter(status=2).order_by('-created_at')[:100]

    # 处理查询请求
    query_result = None
    if request.method == 'GET' and 'qihao' in request.GET:
        qihao = request.GET['qihao']
        try:
            query_result = LotteryRecord.objects.get(issue=qihao)
        except LotteryRecord.DoesNotExist:
            query_result = None

        # 返回 JSON 响应
        return JsonResponse({
            'query_result': {
                'issue': query_result.issue if query_result else None,
                'time': query_result.time if query_result else None,
                'code': query_result.code if query_result else None,
            }
        })

    # 将数据传递给模板
    context = {
        "history_results": history_results,
    }
    return render(request, 'admin/lishi.html', context)
