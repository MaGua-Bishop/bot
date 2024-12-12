from django.contrib import admin, messages
from django.http import HttpResponseRedirect
from django.utils.html import format_html
from django.shortcuts import redirect
from simplepro.action import CellAction
from simplepro.decorators import button
from simplepro.dialog import ModalDialog
from simplepro.monitor.views import JsonResponse

from app import models
from django.utils.safestring import mark_safe
from django.conf import settings
from django.urls import path
from django.shortcuts import render
from utils import copy_user_info


@admin.register(models.TelegramUserName)
class TelegramUserName(admin.ModelAdmin):
    list_display = ['id', "img", 'username_link', 'name', 'about', "status", 'create_time']
    search_fields = ['username', 'name']
    list_editable = ("status",)

    exclude = ('create_time', "status")

    # 在list页面显示头像
    @admin.display(description='头像', ordering='img')
    def img(self, obj):
        div = f"<img src='{settings.MEDIA_URL}{obj.image}' width='32px'>"
        return mark_safe(div)

    @admin.display(description='用户名', ordering='username_link')
    def username_link(self, obj):
        div = f"<a href='https://t.me/{obj.username}' target='_blank'>{obj.username}</a>"
        return mark_safe(div)
        # 添加一个额外的 URL 路由

    actions = ['custom_button']

    @button(type='danger', short_description='批量导入用户', enable=True, icon="fas fa-audio-description")
    def custom_button(self, request, queryset):
        return redirect('/batch_add/')


@admin.register(models.CopyTelegramUser)
class CopyTelegramUser(admin.ModelAdmin):
    list_display = ['id', "username", "phone", "copyObj", "create_time", "custom_action"]
    search_fields = ['username', ]
    list_editable = ("copyObj",)
    raw_id_fields = ("copyObj",)
    exclude = ('create_time',)
    actions = ('layer_input', "test_action")

    def test_action(self, request, queryset):
        # 通过单元格执行的action，可以通过request.POST.get('ids')获取到选中的id
        # queryset 的数据只有一个，如果通过自定义按钮勾行执行，则有多个
        for obj in queryset:
            pass
        return JsonResponse(data={
            'status': 'success',
            'msg': '修改状态成功！'
        })

    test_action.short_description = '模仿'

    def custom_action(self, obj):
        return CellAction(text='模仿', action=self.test_action)

    custom_action.short_description = '模仿目标账号'

    def layer_input(self, request, queryset):
        # 这里的queryset 会有数据过滤，只包含选中的数据
        # post = request.POST
        # 这里获取到数据后，可以做些业务处理
        # post中的_action 是方法名
        # post中 _selected 是选中的数据，逗号分割
        print("111")
        # return JsonResponse(data={
        #     'status': 'success',
        #     'msg': '处理成功！'
        # })

    layer_input.short_description = '批量添加协议号'
    layer_input.type = 'success'
    layer_input.icon = 'el-icon-s-promotion'
    layer_input.enable = True

    # 指定一个输入参数，应该是一个数组

    # 指定为弹出层，这个参数最关键
    layer_input.layer = {
        # 弹出层中的输入框配置
        # 这里指定对话框的标题
        'title': '批量添加协议号',
        # 提示信息
        'tips': '请上传ZIP文件，大致结构为： ZIP -> 文件夹 -> session+json',
        # 确认按钮显示文本
        'confirm_button': '确认提交',
        # 取消按钮显示文本
        'cancel_button': '取消',

        # 弹出层对话框的宽度，默认50%
        'width': '60%',

        # 表单中 label的宽度，对应element-ui的 label-width，默认80px
        'labelWidth': "80px",
        'params': [{
            # 这里的type 对应el-input的原生input属性，默认为input
            'type': 'file',
            # key 对应post参数中的key
            'key': 'zip_file',
            # 显示的文本
            'label': 'ZIP文件',
            # 为空校验，默认为False
            'require': True,
            # 附加参数
            'extras': {
                'prefix-icon': 'el-icon-delete',
                'suffix-icon': 'el-icon-setting',
                'clearable': True
            }
        }]
    }
