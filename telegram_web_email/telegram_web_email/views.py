import json
import smtplib
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from django.conf import settings

@csrf_exempt  # 使 POST 请求免于 CSRF 验证
def send_mail_to_admin(request):
    if request.method == 'POST':
        try:
            # 从 POST 请求的 body 中获取参数
            data = json.loads(request.body.decode('utf-8'))
            title = data.get('title', '')
            content = data.get('content', '')

            # 打印接收到的请求数据
            print(f"接收到的请求数据: title = {title}, content = {content}")

            if not title or not content:
                return JsonResponse({"error": "Title and content are required."}, status=400)

            sender_email = settings.EMAIL_USER
            sender_password = settings.EMAIL_PASSWORD
            admin_email = settings.ADMIN_EMAIL

            # 打印邮件发送信息
            print(f"发送者邮箱: {sender_email}")
            print(f"接收者邮箱: {admin_email}")
            print(f"邮件主题: {title}")

            # 创建邮件内容
            msg = MIMEMultipart()
            msg['From'] = sender_email
            msg['To'] = admin_email
            msg['Subject'] = title

            # 添加邮件正文
            msg.attach(MIMEText(content, 'html'))

            # 连接到 Gmail SMTP 服务器并发送邮件
            print("正在连接到 Gmail SMTP 服务器...")
            with smtplib.SMTP('smtp.gmail.com', 587) as server:
                server.starttls()  # 启用 TLS
                server.login(sender_email, sender_password)  # 登录
                server.send_message(msg)  # 发送邮件
                print("邮件发送成功")

            return JsonResponse({"message": "邮件发送成功"}, status=200)
        except Exception as e:
            # 打印异常信息
            print(f"邮件发送失败: {str(e)}")
            return JsonResponse({"error": f"邮件发送失败: {str(e)}"}, status=500)
    else:
        return JsonResponse({"error": "Invalid request method"}, status=400)
