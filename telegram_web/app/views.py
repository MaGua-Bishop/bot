import asyncio

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




def manual_add(request):
    if request.method == 'POST':
        print(request.POST)
        phone = request.POST.get('phone')
        second_password = request.POST.get('password')  # 二级密码
        verification_code = request.POST.get('verification_code')
        session_file_name = f'session_{phone}.session'
        session_file_path = os.path.join(settings.MEDIA_ROOT, 'session', session_file_name)
        # 显式设置事件循环
        try:
            loop = asyncio.get_running_loop()
        except RuntimeError:  # 没有正在运行的事件循环
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)

        if verification_code is None:
            # 初始化 Telethon 客户端
            client = TelegramClient(session_file_path, settings.API_ID, settings.API_HASH)
            client.connect()
            if not client.is_user_authorized():
                info = client.send_code_request(phone) # 发送验证码
                settings.clients[phone] = info.phone_code_hash  # 临时保存phone_code_hash到内存中

            return JsonResponse({
                'status': 'info',
                'message': '验证码已发送，请输入验证码。'
            })


        else:
            # 发送验证码成功
            client = TelegramClient(session_file_path, settings.API_ID, settings.API_HASH)
            client.connect()


            try:
                client.sign_in(phone, verification_code,phone_code_hash=settings.clients[phone])
            except SessionPasswordNeededError:
                if second_password:
                    client.sign_in(password=second_password,phone_code_hash=settings.clients[phone])
            if client.is_user_authorized():
                client.disconnect() # 关闭客户端
                return JsonResponse({'status': 'success', 'message': '登录成功'})

        #
        #     # 登录成功，保存数据
        #     CopyTelegramUser.objects.create(
        #         username=phone,
        #         phone=phone,
        #         session=session_file_name,
        #         user_id=client.get_me().id
        #     )
        #
        #     # 清理 session
        #     for key in ['phone', 'second_password', 'phone_code_hash']:
        #         request.session.pop(key, None)
        #     request.session.modified = True
        #
        #     return JsonResponse({'status': 'success', 'message': '登录成功，session 文件已生成！'})
        #
        # except Exception as e:
        #     return JsonResponse({'status': 'error', 'message': f'发生未知错误: {str(e)}'})

    return render(request, 'manual_add.html')
