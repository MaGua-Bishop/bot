from django.contrib import messages
from django.shortcuts import render, redirect
import os
import json
from django.conf import settings
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


import socks


def manual_add(request):
    if request.method == 'POST':
        phone = request.POST.get('phone')
        second_password = request.POST.get('password')  # 二级密码
        verification_code = request.POST.get('verification_code')

        # 如果是第一次请求（手机号和密码提交）
        if 'phone' not in request.session:
            request.session['phone'] = phone
            request.session['second_password'] = second_password or None
            request.session.modified = True

        phone = request.session.get('phone')
        session_file_name = f'session_{phone}.session'
        session_file_path = os.path.join(settings.MEDIA_ROOT, 'session', session_file_name)

        try:
            # 初始化 Telethon 客户端
            proxy = (socks.SOCKS5, '127.0.0.1', 7890, True)
            client = TelegramClient(session_file_path, '21507271', '6f6d9d0b737034f07108ae1997e3305c', proxy=proxy)
            client.connect()

            # 如果用户未授权，先发送验证码
            if not client.is_user_authorized():
                if not verification_code:
                    sent_code = client.send_code_request(phone)
                    request.session['phone_code_hash'] = sent_code.phone_code_hash
                    request.session.modified = True
                    return JsonResponse({
                        'status': 'info',
                        'message': '验证码已发送，请输入验证码。'
                    })

                # 使用验证码登录
                phone_code_hash = request.session.get('phone_code_hash')
                client.sign_in(phone, verification_code, phone_code_hash=phone_code_hash)

            # 如果需要二级密码
            try:
                if not client.is_user_authorized():
                    if second_password:
                        client.sign_in(password=second_password)

            except SessionPasswordNeededError:
                return JsonResponse({
                    'status': 'info',
                    'message': '检测到二步验证，请输入您的二级密码。',
                    'needs_second_password': True
                })

            # 登录成功，保存数据
            CopyTelegramUser.objects.create(
                username=phone,
                phone=phone,
                session=session_file_name,
                user_id=client.get_me().id
            )

            # 清理 session
            for key in ['phone', 'second_password', 'phone_code_hash']:
                request.session.pop(key, None)
            request.session.modified = True

            return JsonResponse({'status': 'success', 'message': '登录成功，session 文件已生成！'})

        except Exception as e:
            return JsonResponse({'status': 'error', 'message': f'发生未知错误: {str(e)}'})

    return render(request, 'manual_add.html')
