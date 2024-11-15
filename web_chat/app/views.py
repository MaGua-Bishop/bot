from django.shortcuts import render
from django.http import JsonResponse
from django.core.files.storage import default_storage
from django.conf import settings
from app.models import Message,User,generate_random_string
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
    return render(request, 'chat/room.html')

def reset_link(request,uid):
    """重置链接"""
    user = User.objects.filter(uid=uid).first()
    if user:
        user.uid = generate_random_string()
        user.save()
    return JsonResponse({"code":0})

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
    messages = Message.objects.all().order_by('timestamp')
    data = [
        {
            "user": msg.user.uid,
            "username": msg.user.user,
            "message": msg.message,
            "file_url": settings.HOST+msg.file_url if msg.file_url != "" else "",
            "file_type": msg.file_type,
            # "timestamp": msg.timestamp.strftime("%H:%M"),
            "avatar":"https://lh3.googleusercontent.com/a/ALm5wu2Vm-KiLrLQW9QfemC-QvUWqJpuOha8VDg7m70D=k-s48"
        }
        for msg in messages
    ]
    return JsonResponse(data, safe=False)

@csrf_exempt
def save_message(request):
    if request.method == "POST":
        data = json.loads(request.body)
        user = User.objects.filter(uid=data['user']).first()
        if user:
            Message.objects.create(
                user=user,
                message=data.get('message', ''),
                file_url=data.get('file_url', ''),
                file_type=data.get('file_type', '')
            )
        return JsonResponse({"status": "success"})
