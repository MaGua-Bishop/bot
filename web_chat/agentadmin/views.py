from django.utils import timezone
from datetime import datetime, timedelta

from django.contrib.auth.decorators import login_required
from django.shortcuts import render, redirect
from django.views.decorators.http import require_POST, require_GET
from django.http import HttpResponseNotFound, JsonResponse, HttpResponseNotAllowed
from django.contrib.auth import authenticate, login, update_session_auth_hash, logout
from django.views.decorators.csrf import csrf_exempt

from app.models import LotteryRecord, BetRecord, Admin


def page_not_found(request, exception):
    return render(request, '404.html', status=404)


def login_view(request):
    if request.method == 'GET':
        return render(request, 'agentadmin/login.html')
    elif request.method == 'POST':
        username = request.POST.get('account')
        password = request.POST.get('password')

        user = authenticate(request, username=username, password=password)
        if user is not None:
            if user.available_time and user.available_time < timezone.now():
                return JsonResponse({'error_code': 1, 'error_msg': '账号已过期'})
            login(request, user)
            return JsonResponse({'error_code': 0, 'url': '/agentadmin/kaijiangxinxi'})
        else:
            return JsonResponse({'error_code': 1, 'error_msg': '用户名或密码错误'})
    return HttpResponseNotAllowed(['GET', 'POST'])


def kaijiangxinxi(request):
    current_image_url = '../../media/uploads/1.png'
    history_image_url = '../../media/uploads/1.png'
    return render(request, 'agentadmin/kaijiangxinxi.html', {
        "current_image_url": current_image_url,
        "history_image_url": history_image_url,
    })


def zhanghaoguanli(request):
    if request.method == 'GET':
        return render(request, 'agentadmin/zhanghaoguanli.html')


def agentadmin_info(request):
    if request.method == 'GET':
        admin_username = request.user.username
        agentadmin_info = Admin.objects.filter(username=admin_username).values('id', 'username', 'available_time',
                                                                               'is_open',
                                                                               'total_score')
        agentadmin_info = list(agentadmin_info)
        agentadmin_info = [
            {
                'account': item['username'],
                'expired_time': item['available_time'].strftime('%Y-%m-%d %H:%M:%S') if item[
                    'available_time'] else None,
                'is_kp': item['is_open'],
                'group_credit': item['total_score'],
                'groupId': item['id']
            }
            for item in agentadmin_info
        ]

    return JsonResponse({"error_code": 0, 'agentadmin_info': agentadmin_info})


def kaiguanpan(request):
    if request.method == 'POST':
        groupId = request.POST.get('groupId')
        admin = Admin.objects.get(id=groupId)
        admin.is_open = not admin.is_open
        admin.save()
        return JsonResponse({'error_code': 0, 'kp': admin.is_open})


@login_required
def xiugaimima(request):
    if request.method == 'POST':
        old_password = request.POST.get('old')
        new_password = request.POST.get('new')
        confirm_password = request.POST.get('comfirm')

        user = request.user  # 获取当前登录用户

        # 验证旧密码
        if not user.check_password(old_password):
            return JsonResponse({'code': 400, 'msg': '旧密码不正确'})

        # 检查新密码和确认密码是否匹配
        if new_password != confirm_password:
            return JsonResponse({'code': 400, 'msg': '新密码和确认密码不匹配'})

        # 更新密码
        user.set_password(new_password)
        user.save()

        # 更新用户的会话信息
        update_session_auth_hash(request, user)  # 保持用户登录状态

        return JsonResponse({'code': 200, 'msg': '密码修改成功'})

    return render(request, 'agentadmin/xiugaimima.html')  # GET 请求时渲染修改密码页面


def tongjifenxi(request):
    today = datetime.now()
    date_list = [(today - timedelta(days=i)).strftime('%Y-%m-%d') for i in range(4)]
    return render(request, 'agentadmin/tongjifenxi.html', {
        'date_list': date_list,
    })


def tongjifenxi_info(request):
    '''统计分析中的查询数据需要写对应的逻辑'''
    selected_date = request.GET.get('date')
    if selected_date:
        response_data = {
            "error_code": 0,
            "error_msg": "",
            "data": [
                "数据汇总：总昨余：0，总流水：0，总有效流水：0，总盈亏：0，总反水：0，总上：0，总下：0，总上下：0",
                "机器人：ca008，总昨余：0，总流水：0.00，总有效流水：0，总盈亏：0，总反水：0，总上：0，总下：0，总上下：0;"
            ]
        }
        return JsonResponse(response_data)


def weijiesuan(request):
    print("当前管理员的用户名：", request.user.username)
    admin_username = request.user.username
    unprocessed_orders = BetRecord.objects.filter(admin_username=admin_username, status=0).values(
        'admin_username', 'user_name', 'message', 'issue', 'created_at'
    )

    # 将数据转换为列表
    unprocessed_orders_list = list(unprocessed_orders)

    # 将数据传递给模板
    context = {
        "unprocessed_orders": unprocessed_orders_list,
    }
    return render(request, 'agentadmin/weijiesuan.html', context)


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
    return render(request, 'agentadmin/lishi.html', context)


def anquantuichu(request):
    logout(request)
    return redirect('agentadmin:login')


def sxfen(request):
    '''账号管理中的上下分记录需要写对应的逻辑'''
    if request.method == 'GET':
        id = request.GET.get('groupId')
        data = [
            {"credit": 4326.83, "score_var": 200000, "create_time": "2024-11-29 03:42:50"},
            {"credit": 71583.15, "score_var": 200000, "create_time": "2024-11-21 10:53:46"}
        ]
        return render(request, 'agentadmin/sxfen.html', {'data': data})


@csrf_exempt
def shangfen(request):
    '''账号管理中的确认上分需要写对应的逻辑'''
    if request.method == 'POST':
        score = request.POST.get('score')
        group_id = request.POST.get('groupId')
        return JsonResponse({'status': 'success', 'message': '上分成功！'})


@csrf_exempt
def xiafen(request):
    '''账号管理中的确认下分需要写对应的逻辑'''
    if request.method == 'POST':
        score = request.POST.get('score')
        group_id = request.POST.get('groupId')
        return JsonResponse({'status': 'success', 'message': '下分成功！'})


def xiugaixiane(request):
    if request.method == 'GET':
        group_id = request.GET.get('groupId')
        agentadmin_info = Admin.objects.get(id=group_id)
        return render(request, 'agentadmin/xiugaixiane.html', {'agentadmin_info': agentadmin_info})

    elif request.method == 'POST':
        group_id = request.POST.get('groupId')
        agentadmin_info = Admin.objects.get(id=group_id)

        agentadmin_info.single_bet_limit = request.POST.get('te_wj', 0)
        agentadmin_info.normal_single_player_limit = request.POST.get('max', 0)
        agentadmin_info.single_order_max_limit = request.POST.get('allmax', 0)
        agentadmin_info.positive_order_limit = request.POST.get('zheng', 0)
        agentadmin_info.corner_order_limit = request.POST.get('jiao', 0)
        agentadmin_info.single_order_limit = request.POST.get('nian', 0)
        agentadmin_info.common_order_limit = request.POST.get('tong', 0)
        agentadmin_info.vehicle_order_limit = request.POST.get('che', 0)
        agentadmin_info.special_order_limit = request.POST.get('te', 0)
        agentadmin_info.single_order_limit = request.POST.get('dan', 0)
        agentadmin_info.double_order_limit = request.POST.get('shuang', 0)
        agentadmin_info.large_order_limit = request.POST.get('da', 0)
        agentadmin_info.small_order_limit = request.POST.get('xiao', 0)
        agentadmin_info.fan_order_limit = request.POST.get('fan', 0)
        agentadmin_info.add_order_limit = request.POST.get('jia', 0)

        agentadmin_info.save()

        return JsonResponse({'status': 'success', 'message': '修改成功！'})


def chauser(request):
    '''账号管理中的查看会员余额需要写对应的逻辑'''
    if request.method == 'GET':
        group_id = request.GET.get('groupId')
        context = {
            'not_tuo': 111.00,  # 非托会员
            'tuo': 222.00,  # 托会员
        }
        return render(request, 'agentadmin/chauser.html', context)
