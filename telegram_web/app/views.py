import asyncio
import os

import django
from django.contrib import messages
from django.http import JsonResponse
from django.shortcuts import render, redirect
from telethon.sync import TelegramClient

from app import models
from telegram_web import settings
from utils import get_telethon_proxy
from .models import CopyTelegramUser


# Create your views here.
def batch_add(request):
    if request.method == 'POST':
        text = request.POST.dict()['batch_data']
        user_len = 0
        for user in text.split('\n'):
            user = user.replace(" ", "").replace("\r", "")
            if len(user) == 0:
                continue

            # 尝试创建新的用户
            try:
                models.TelegramUserName.objects.create(username=user)
                user_len += 1
            except django.db.IntegrityError:
                # 如果遇到重复的用户名，跳过并继续
                continue
        # 删除没有 name, about 的用户
        models.TelegramUserName.objects.filter(
            name__isnull=True, about__isnull=True
        ).delete()

        messages.add_message(request, messages.SUCCESS, f'添加 {user_len} 个用户数据成功')
        return redirect('/admin/app/telegramusername/')
    else:
        return render(request, 'batch_add.html')


from telethon.errors.rpcerrorlist import PhoneNumberInvalidError, SessionPasswordNeededError


def manual_add(request):
    if request.method == 'POST':
        phone = request.POST.get('phone')
        second_password = request.POST.get('password')  # 二级密码
        verification_code = request.POST.get('verification_code')
        session_file_name = f'session_{phone}.session'
        session_file_path = os.path.join(settings.MEDIA_ROOT, 'session', session_file_name)

        try:
            loop = asyncio.get_running_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)

        if verification_code is None:
            # 初始化 Telethon 客户端
            client = TelegramClient(session_file_path, settings.API_ID, settings.API_HASH, proxy=get_telethon_proxy())
            client.connect()

            if not client.is_user_authorized():
                try:
                    info = client.send_code_request(phone)  # 发送验证码
                    settings.clients[phone] = info.phone_code_hash  # 临时保存phone_code_hash到内存中
                except PhoneNumberInvalidError:
                    # 捕获电话号码无效的错误
                    return JsonResponse({
                        'status': 'error',
                        'message': '电话号码无效，请检查输入的号码。'
                    })

            return JsonResponse({
                'status': 'info',
                'message': '验证码已发送，请输入验证码。',
                'needs_second_password': False
            })

        else:
            # 验证码输入成功
            client = TelegramClient(session_file_path, settings.API_ID, settings.API_HASH, proxy=get_telethon_proxy())
            client.connect()

            try:
                # 尝试验证码登录
                client.sign_in(phone, verification_code, phone_code_hash=settings.clients[phone])
            except SessionPasswordNeededError:
                # 如果需要二级密码
                if second_password:
                    # 如果二级密码正确，继续
                    client.sign_in(password=second_password, phone_code_hash=settings.clients[phone])
                else:
                    # 如果没有输入二级密码
                    return JsonResponse({
                        'status': 'error',
                        'message': '需要输入二级密码。',
                        'needs_second_password': True
                    })

            if client.is_user_authorized():
                client.disconnect()  # 关闭客户端
                # 登录成功，保存数据
                CopyTelegramUser.objects.create(
                    phone=phone,
                    session='session/' + session_file_name,
                    user_id=request.user.id,
                    username=phone
                )
                return JsonResponse(
                    {'status': 'success', 'message': '登录成功', 'redirect_url': '/admin/app/copytelegramuser/'})
            else:
                # 登录失败，返回错误
                return JsonResponse({
                    'status': 'error',
                    'message': '验证码或二级密码错误，请重试。',
                    'needs_second_password': True
                })

    return render(request, 'manual_add.html')
