from django.shortcuts import render
from django.http import JsonResponse
from django.core.files.storage import default_storage
from django.conf import settings

from app import bot, models
from app.models import Message, User, generate_random_string, Admin, ChangeMoney,BetRecord
from django.views.decorators.csrf import csrf_exempt
import json
from django.views.decorators.http import require_POST, require_GET
from django.contrib.admin.views.decorators import staff_member_required
from django.shortcuts import get_object_or_404
import random
from decimal import Decimal, ROUND_HALF_UP
import decimal
from channels.layers import get_channel_layer
from asgiref.sync import async_to_sync
from app.consumers import ChatConsumer
from app.bot import ChatBot
from django.contrib.auth.decorators import login_required
from asgiref.sync import sync_to_async
from django.db.models import F
from channels.db import database_sync_to_async
from functools import partial
from django.utils import timezone
from datetime import timedelta
from app.models import LotteryRecord, BetRecord
from django.db.models import Sum, Case, When, DecimalField
from decimal import Decimal
from .backstage_view import kaijiangxinxi, tongjifenxi, weijiesuan, lishi

# 创建一个异步的JsonResponse
async_json_response = sync_to_async(JsonResponse)

def page_not_found(request, exception):
    return render(request, '404.html')


def room(request):
    user = request.GET.get('user')
    if user is None:
        return render(request, '404.html')
    user = User.objects.filter(uid=user).first()
    if user is None:
        return render(request, '404.html')
    context = {
        'admin_id': user.admin,
    }
    return render(request, 'chat/room.html', context)


def reset_link(request, uid):
    """重置链接"""
    user = User.objects.filter(uid=uid).first()
    if user:
        user.uid = generate_random_string()
        user.save()
    return JsonResponse({"code": 0})


@csrf_exempt
def upload(request):
    if request.method == 'POST':
        file = request.FILES['file']
        file_path = default_storage.save(f'uploads/{file.name}', file)
        file_url = default_storage.url(file_path)
        return JsonResponse({'file_url': file_url})
    else:
        return JsonResponse({'error': 'Only POST requests are allowed'}, status=400)


def get_messages(request):
    admin_username = request.GET.get('admin')
    print(f"获取历史消息 - admin_username: {admin_username}")

    messages = Message.objects.filter(admin_username=admin_username).order_by('timestamp')
    print(f"找到 {messages.count()} 条消息")

    data = []
    for message in messages:
        message_data = {
            'admin_username': message.admin_username,
            'message': message.message,
            'file_url': message.file_url,
            'file_type': message.file_type,
            'timestamp': message.timestamp.strftime('%H:%M'),
            'admin_id': message.admin_username
        }

        # 根据是否是机器人消息添加不同的用户信息
        if message.is_bot:
            message_data.update({
                'user': 'Anonymous',
                'user_name': 'Anonymous',
            })
        else:
            message_data.update({
                'user': message.user.uid if message.user else None,
                'user_name': message.user.user if message.user else None,
            })

        print(f"消息数据: {message_data}")
        data.append(message_data)

    return JsonResponse(data, safe=False)


@csrf_exempt
def save_message(request):
    if request.method == 'POST':
        try:
            data = json.loads(request.body)
            print("接收到的消息数据:", data)

            user = User.objects.get(uid=data['user'])

            message = Message.objects.create(
                user=user,
                admin_username=data['admin_username'],
                message=data.get('message'),
                file_url=data.get('file_url'),
                file_type=data.get('file_type')
            )

            # 返回完整的消息数，包含用户名
            return JsonResponse({
                'status': 'success',
                'message': {
                    'user': user.uid,
                    'user_name': user.user,  # 返回真实用户名
                    'message': message.message,
                    'file_url': message.file_url,
                    'file_type': message.file_type,
                    'admin_username': message.admin_username,
                    'timestamp': message.timestamp.strftime('%H:%M'),
                    'avatar': data.get('avatar')
                }
            })
        except Exception as e:
            print(f"保存消息时出错: {str(e)}")
            return JsonResponse({'status': 'error', 'message': str(e)}, status=500)
    return JsonResponse({'status': 'error'}, status=400)


@require_POST
def create_user(request):
    try:
        nickname = request.POST.get('nickname')
        type = request.POST.get('type')
        if not nickname:
            return JsonResponse({
                'status': 'error',
                'message': '昵称不能为空'
            }, status=400)

        # 获当前管理员
        admin = request.user
        admin_user = Admin.objects.get(username=admin.username)

        # 检查该管理员下是否已存在相同昵称的用户
        if User.objects.filter(admin=admin_user, user=nickname).exists():
            return JsonResponse({
                'status': 'error',
                'message': '该昵称已存在'
            }, status=400)

        # 创建新用户
        if type == '1':
            new_user = User.objects.create(
                user=nickname,
                admin=admin_user,
            )
        else:
            new_user = User.objects.create(
                user=nickname,
                admin=admin_user,
                is_tou=True
            )

        return JsonResponse({
            'status': 'success',
            'data': {
                'id': new_user.id,
                'nickname': new_user.user,
                'uid': new_user.uid
            }
        })
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


def delete_user(request):
    if request.method == 'POST':
        try:
            user_id = request.POST.get('user_id')
            # 获取要删除的用户
            user = get_object_or_404(User, id=user_id)
            user.delete()
            return JsonResponse({'status': 'success'})
        except Exception as e:
            return JsonResponse({
                'status': 'error',
                'message': str(e)
            }, status=500)


@require_GET
def query_users(request):
    try:
        is_tou = request.GET.get('is_tou') == 'true'
        users = User.objects.filter(is_tou=is_tou, admin=request.user).order_by('-money')

        users_data = [{
            'id': user.id,
            'user': user.user,
            'money': str(user.money),
            'is_black': user.is_black,
            'is_auto_tou': user.is_auto_tou  # 确保模型中有这个字段
        } for user in users]

        return JsonResponse({
            'status': 'success',
            'users': users_data
        })
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@require_POST
def change_nickname(request):
    try:
        user_id = request.POST.get('user_id')
        new_nickname = request.POST.get('new_nickname')

        if not user_id or not new_nickname:
            return JsonResponse({
                'status': 'error',
                'message': '参数不完整'
            }, status=400)

        # 查找并更新用户
        user = User.objects.get(id=user_id)
        user.user = new_nickname
        user.save()

        return JsonResponse({
            'status': 'success'
        })
    except User.DoesNotExist:
        return JsonResponse({
            'status': 'error',
            'message': '用户不存在'
        }, status=404)
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@require_POST
def block_user(request):
    try:
        user_id = request.POST.get('user_id')

        if not user_id:
            return JsonResponse({
                'status': 'error',
                'message': '参数不完整'
            }, status=400)

        # 查找并更新用户
        user = User.objects.get(id=user_id)
        user.is_black = True
        user.save()

        return JsonResponse({
            'status': 'success'
        })
    except User.DoesNotExist:
        return JsonResponse({
            'status': 'error',
            'message': '用户不存在'
        }, status=404)
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


# 添加取消拉黑视图
@require_POST
def unblock_user(request):
    try:
        user_id = request.POST.get('user_id')

        if not user_id:
            return JsonResponse({
                'status': 'error',
                'message': '参数不完整'
            }, status=400)

        # 查找并更新用户
        user = User.objects.get(id=user_id)
        user.is_black = False
        user.save()

        return JsonResponse({
            'status': 'success'
        })
    except User.DoesNotExist:
        return JsonResponse({
            'status': 'error',
            'message': '用户不存在'
        }, status=404)
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@require_POST
def update_score(request):
    try:
        user_id = request.POST.get('user_id')
        score_change = request.POST.get('score_change')
        is_add = request.POST.get('is_add') == 'true'

        if not all([user_id, score_change]):
            return JsonResponse({
                'status': 'error',
                'message': '参数不完整'
            }, status=400)

        # 转换为Decimal类型进行计算
        try:
            score_change = Decimal(str(score_change))
            if score_change <= Decimal('0'):
                raise ValueError('积分必须大于0')
        except (decimal.InvalidOperation, ValueError) as e:
            return JsonResponse({
                'status': 'error',
                'message': str(e)
            }, status=400)

        # 查找用户
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return JsonResponse({
                'status': 'error',
                'message': '用户不存在'
            }, status=404)

        current_money = user.money

        # 使用Decimal进行计算
        if is_add:
            new_money = current_money + score_change
            change_type = '管理员上分'
        else:
            if current_money < score_change:
                return JsonResponse({
                    'status': 'error',
                    'message': '积分不足'
                }, status=400)
            new_money = current_money - score_change
            change_type = '管理员下分'

        new_money = new_money.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

        # 创建积分变动记录
        ChangeMoney.objects.create(
            user=user,
            money=score_change,
            last_money=current_money,
            now_money=new_money,
            change_type=change_type
        )

        # 更新用户余额（跳过信号处理）
        User.objects.filter(id=user_id).update(money=new_money)

        return JsonResponse({
            'status': 'success',
            'current_money': str(new_money)
        })
    except Exception as e:
        print(f"Error in update_score: {str(e)}")
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@require_POST
def admin_reset_link(request):
    """重置链接"""
    user_id = request.POST.get('user_id')
    user = User.objects.filter(id=user_id).first()
    if user:
        user.uid = generate_random_string()
        user.save()
        return JsonResponse({
            "code": 0,
            "uid": settings.HOST + "/game/?user=" + str(user.uid)
        })
    return JsonResponse({
        "code": 1,
        "message": "用户不存在"
    })


@require_POST
def toggle_tou_type(request):
    try:
        user_id = request.POST.get('user_id')

        if not user_id:
            return JsonResponse({
                'status': 'error',
                'message': '参数不完整'
            }, status=400)

        user = User.objects.get(id=user_id)
        # 切换自动托状态
        user.is_auto_tou = not user.is_auto_tou
        user.save()

        return JsonResponse({
            'status': 'success'
        })
    except User.DoesNotExist:
        return JsonResponse({
            'status': 'error',
            'message': '用户不存在'
        }, status=404)
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@require_POST
def update_admin_settings(request):
    try:
        setting_type = request.POST.get('type')
        value = request.POST.get('value')

        if not all([setting_type, value]):
            return JsonResponse({
                'status': 'error',
                'message': '参数不完整'
            }, status=400)

        admin = Admin.objects.get(username=request.user.username)

        if setting_type == 'is_open':
            admin.is_open = value.lower() == 'true'
        elif setting_type == 'odds':
            admin.odds = Decimal(value)
        elif setting_type == 'close_seconds':
            admin.close_seconds = int(value)
        elif setting_type == 'rebate':
            rebate = Decimal(value)
            if rebate < 0 or rebate > 100:
                return JsonResponse({
                    'status': 'error',
                    'message': '返水比例必须在0-100之间'
                }, status=400)
            admin.rebate = rebate
        elif setting_type == 'max_bet_amount':
            admin.max_bet_amount = Decimal(value)
        else:
            return JsonResponse({
                'status': 'error',
                'message': '未知的设置类型'
            }, status=400)

        admin.save()

        return JsonResponse({
            'status': 'success',
            'data': {
                'is_open': admin.is_open,
                'odds': str(admin.odds),
                'close_seconds': admin.close_seconds,
                'rebate': str(admin.rebate),
                'total_score': str(admin.total_score)
                , 'max_bet_amount': str(admin.max_bet_amount)
            }
        })
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@require_GET
def get_admin_settings(request):
    try:
        admin = Admin.objects.get(username=request.user.username)
        return JsonResponse({
            'status': 'success',
            'data': {
                'is_open': admin.is_open,
                'odds': format(admin.odds, '.2f'),  # 格式化为两位小数
                'total_score': format(admin.total_score, '.2f'),
                'close_seconds': admin.close_seconds,
                'rebate': format(admin.rebate, '.2f')  # 格式化为两位小数
                , 'max_bet_amount': format(admin.max_bet_amount, '.2f')
            }
        })
    except Admin.DoesNotExist:
        return JsonResponse({
            'status': 'error',
            'message': '管理员不存在'
        }, status=404)
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@require_POST
def send_broadcast(request):
    """发送广播"""
    try:
        data = json.loads(request.body)
        message = data.get('message')
        room_id = data.get('room_id')  # 获取可选的room_id参数

        if not message:
            return JsonResponse({
                'status': 'error',
                'message': '消息不能为空'
            }, status=400)

        bot = ChatBot()

        if room_id:
            # 如果指定了room_id，则只发送到该聊天室
            async_to_sync(bot.broadcast_message)(message, room_id)
        else:
            # 否则发送到所有聊天室
            async_to_sync(bot.broadcast_message)(message)

        return JsonResponse({
            'status': 'success',
            'message': '广播消息已发送'
        })
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)


@login_required
@require_GET
def get_current_bets(request):
    """获取当前期下注记录"""
    try:
        # 获取当前管理员用户名
        admin_username = request.user.username

        # 获取当前时间
        now = timezone.now()
        five_minutes_ago = now - timedelta(minutes=5)

        # 查找最新的可下注期号
        record = (LotteryRecord.objects
                  .filter(status=0)
                  .filter(created_at__gte=five_minutes_ago)
                  .order_by('-issue')
                  .first())

        if not record:
            return JsonResponse({'status': 'error', 'message': '无法获取当前期号'})

        # 获取 is_tou 参数
        is_tou = request.GET.get('is_tou', 'false').lower() == 'true'

        # 根据 is_tou 参数获取下注记录
        bets = list(BetRecord.objects.filter(
            issue=record.issue,
            admin_username=admin_username,
            status=0
        ).filter(user_id__in=User.objects.filter(is_tou=is_tou).values_list('id', flat=True))
        .values('user_name', 'bet_type', 'amount').order_by('-id'))

        # 统计总下注金额
        total_bet_normal = BetRecord.objects.filter(
            issue=record.issue,
            admin_username=admin_username,
            status=0,
            user_id__in=User.objects.filter(is_tou=False).values_list('id', flat=True)
        ).aggregate(total_amount=Sum('amount'))['total_amount'] or 0

        total_bet_tou = BetRecord.objects.filter(
            issue=record.issue,
            admin_username=admin_username,
            status=0,
            user_id__in=User.objects.filter(is_tou=True).values_list('id', flat=True)
        ).aggregate(total_amount=Sum('amount'))['total_amount'] or 0

        # 格式化金额
        formatted_bets = [{
            'user_name': bet['user_name'],
            'bet_type': bet['bet_type'],
            'amount': float(bet['amount'])
        } for bet in bets]

        return JsonResponse({
            'status': 'success',
            'issue': record.issue,
            'bets': formatted_bets,
            'total_bet_normal': float(total_bet_normal),
            'total_bet_tou': float(total_bet_tou)
        })

    except Exception as e:
        import traceback
        print(traceback.format_exc())
        return JsonResponse({'status': 'error', 'message': str(e)})


@login_required
def change_money_records(request):
    """查看积分变更记录"""
    try:
        admin_username = request.user.username

        # 获取日期参数，默认为今天
        date_str = request.GET.get('date', timezone.now().strftime('%Y-%m-%d'))
        selected_date = timezone.datetime.strptime(date_str, '%Y-%m-%d').date()

        # 获取该管理员下的所有用户ID
        user_ids = User.objects.filter(admin__username=admin_username).values_list('id', flat=True)

        # 获取这些用户的所有金额变更记录，过滤日期
        records = ChangeMoney.objects.filter(
            user_id__in=user_ids,
            create_time__date=selected_date
        ).select_related('user').order_by('-create_time')

        # 计算统计数据
        stats = records.aggregate(
            total_win = Sum(Case(
                When(change_type='中奖增加', then='money'),
                default=0,
                output_field=DecimalField()
            )),
            total_bet = Sum(Case(
                When(change_type='下注扣除', then=Decimal('-1.0') * F('money')),
                default=0,
                output_field=DecimalField()
            ))
        )

        # 计算总盈亏（下注总额 - 中奖总额）
        total_profit = (stats['total_bet'] or Decimal('0.00')) - (stats['total_win'] or Decimal('0.00'))

        context = {
            'records': records,
            'admin_username': admin_username,
            'stats': {
                'total_win': stats['total_win'] or Decimal('0.00'),
                'total_bet': stats['total_bet'] or Decimal('0.00'),
                'total_profit': total_profit
            },
            'selected_date': selected_date
        }

        return render(request, 'admin/change_money_records.html', context)

    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        })




