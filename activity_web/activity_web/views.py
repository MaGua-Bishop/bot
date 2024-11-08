import json

from django.shortcuts import render
from django.http import JsonResponse
from .forms import ResultForm

from io import BytesIO
from random import randint
from django.http import HttpResponse
from PIL import Image, ImageDraw, ImageFont
from admin_web.models import Audit


def handle_result(request, types, form):
    if types == '0':
        return submit_type_0(request, form)
    elif types == '1':
        return submit_type_1(request, form)
    elif types == '2':
        return submit_type_2(request, form)


#         查询审核结果

def submit_type_0(request, form):
    account = form.cleaned_data['account']
    phone = form.cleaned_data['phone']
    audit_exists = Audit.objects.filter(account=account, type=0, status=0).exists()
    if audit_exists:
        return render(request, 'index.html', {'error_message': f'会员账户:{account}已经申请过办理存款彩金！请等待审核'})
    audit = Audit(account=account, phone=phone, type=0)
    audit.save()
    return render(request, 'index.html', {'success_message': f'会员账户:{account}成功申请办理存款彩金！请等待审核'})


def submit_type_1(request, form):
    account = form.cleaned_data['account']
    phone = form.cleaned_data['phone']
    audit_exists = Audit.objects.filter(account=account, type=1, status=0).exists()
    if audit_exists:
        return render(request, 'index.html', {'error_message': f'会员账户:{account}已经申请过办理提现彩金！请等待审核'})
    audit = Audit(account=account, phone=phone, type=1)
    audit.save()
    return render(request, 'index.html', {'success_message': f'会员账户:{account}成功申请办理提现彩金！请等待审核'})


def submit_type_2(request, form):
    account = form.cleaned_data['account']

    # 获取与账号相关的所有审核记录
    audit_list = Audit.objects.filter(account=account).values_list('account', 'type', 'status', 'create_time')

    # 构建返回的字符串
    string = '办理类型\\t审核状态'

    # 如果存在审核记录
    if audit_list:
        # 遍历所有审核记录
        for item in audit_list:
            types = '办理存款彩金' if item[1] == 0 else '办理提现彩金'
            status = '待审核' if item[2] == 0 else ('已通过' if item[2] == 1 else '已拒绝')
            string += f'\\n{types}\\t{status}'  # 每条记录添加到字符串中

        # 将查询结果传递给模板
        return render(request, 'index.html', {
            'success_message': string,  # 将字符串传递给模板
            'form': form
        })

    # 如果没有找到相关记录，返回提示
    else:
        return render(request, 'index.html', {
            'success_message': f'会员账户:{account}暂无申请',
            'form': form
        })


def handle_result_from(request):
    if request.method == 'POST':
        form = ResultForm(request.POST)
        if form.is_valid():
            account = form.cleaned_data['account']
            phone = form.cleaned_data['phone']
            code = form.cleaned_data['code']
            types = form.cleaned_data['type']
            print(f'获取到了type:{types}')
            session_captcha = request.session.get('captcha')

            # 验证验证码
            if session_captcha and session_captcha.lower() == code.lower():
                return handle_result(request, types, form)
            else:
                # 验证码错误，保持其他字段内容
                form.add_error('code', '验证码错误')
                return render(request, 'index.html', {'form': form})  # 表单已经包含了用户输入的其他数据
        else:
            return render(request, 'index.html', {'form': form})  # 如果表单验证失败，返回表单

    return render(request, 'index.html', {'form': ResultForm()})  # 如果是 GET 请求，返回一个空表单


def get_index(request):
    form = ResultForm()
    return render(request, 'index.html', {'form': form})


def generate_captcha(request):
    '''
        生成验证码
    '''
    # 创建图像
    image = Image.new('RGB', (200, 80), color=(255, 255, 255))
    draw = ImageDraw.Draw(image)

    # 生成验证码文本（可以替换为固定的文本或随机生成）
    captcha_text = ''.join([str(randint(0, 9)) for _ in range(4)])  # 动态生成
    # 或者使用固定的验证码
    # captcha_text = "1234"  # 固定验证码文本

    try:
        font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 40)  # 可选的字体路径
    except IOError:
        font = ImageFont.load_default()

    # 在图像上绘制验证码文本
    draw.text((30, 20), captcha_text, font=font, fill=(0, 0, 0))

    # 通过响应返回图片
    response = HttpResponse(content_type='image/png')
    image.save(response, 'PNG')

    # 将生成的验证码存储在 session 中
    request.session['captcha'] = captcha_text  # 存储验证码内容，以便后续验证

    return response
