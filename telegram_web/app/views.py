import json

from django.shortcuts import render, redirect
from django.contrib import admin, messages
from app import models
from django.http import JsonResponse


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

def imitation_status_view(request, success_count, failure_messages):
    """显示模仿状态的自定义页面"""
    return render(request, 'imitation_status.html', {
        'success_count': success_count,
        'failure_messages': failure_messages,
    })
