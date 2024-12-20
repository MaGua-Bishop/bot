import asyncio

import socks
from django.contrib import messages
from django.shortcuts import render, redirect
import os
from telegram_web import settings
from django.http import JsonResponse
from telethon.errors import SessionPasswordNeededError
from .models import CopyTelegramUser
from telethon.sync import TelegramClient
from app import models


# Create your views here.


def batch_add(request):
    if request.method == 'POST':
        text = request.POST.dict()['batch_data']
        user_len = 0
        for user in text.split('\n'):
            user = user.replace(" ", "")
            if len(user) == 0:
                continue
            models.TelegramUserName.objects.create(username=user)
            user_len += 1
        messages.add_message(request, messages.SUCCESS, f'添加 {user_len}个用户数据成功')
        return redirect('/admin/app/telegramusername/')
    else:
        return render(request, 'batch_add.html')


from telethon.errors.rpcerrorlist import PhoneNumberInvalidError

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
            proxy = (socks.SOCKS5, '127.0.0.1', 7890, True)
            client = TelegramClient(session_file_path, settings.API_ID, settings.API_HASH, proxy=proxy)
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
            proxy = (socks.SOCKS5, '127.0.0.1', 7890, True)
            client = TelegramClient(session_file_path, settings.API_ID, settings.API_HASH, proxy=proxy)
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
