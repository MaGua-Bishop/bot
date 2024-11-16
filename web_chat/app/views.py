from django.shortcuts import render
from django.http import JsonResponse
from django.core.files.storage import default_storage
from django.conf import settings
from app.models import Message, User, generate_random_string, Admin
from django.views.decorators.csrf import csrf_exempt
import json


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
            'user': message.user.uid,
            'user_name': message.user.user,
            'admin_username': message.admin_username,
            'message': message.message,
            'file_url': message.file_url,
            'file_type': message.file_type,
            'timestamp': message.timestamp.strftime('%H:%M'),
            'admin_id': message.admin_username
        }
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
            
            # 返回完整的消息数据，包含用户名
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
