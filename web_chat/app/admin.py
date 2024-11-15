from django.contrib.admin import AdminSite
from django.contrib.auth import authenticate
from django.utils import timezone
from django.http import HttpResponseForbidden
from django.conf import settings
from django.contrib import admin
from django.utils.html import format_html
from django.utils.safestring import mark_safe

from app import models

# 自定义 AdminSite
class CustomAdminSite(AdminSite):
    def login(self, request, extra_context=None):
        # 执行原始登录逻辑
        response = super().login(request, extra_context)

        # 如果用户已认证
        if request.user.is_authenticated:
            try:
                admin_user = models.Admin.objects.get(username=request.user.username)
                # 检查可用时间
                if admin_user.available_time and admin_user.available_time < timezone.now():
                    # 过期则强制登出
                    from django.contrib.auth import logout
                    logout(request)
                    return HttpResponseForbidden('账号已过期')
            except models.Admin.DoesNotExist:
                pass
        return response

# 创建自定义管理站点实例
custom_admin_site = CustomAdminSite(name='custom_admin')

# 定义 ModelAdmin 类
class UserAdmin(admin.ModelAdmin):
    list_display = ['user', 'money', 'link', "operate"]
    list_display_links = ("user",)
    list_filter = ("user",)
    list_editable = ("money",)
    exclude = ("uid",)

    @admin.display(description='链接', ordering='operate')
    def link(self, obj):
        return format_html(
            f'<span>{settings.HOST}/game/?user={obj.uid}</span>',
        )

    @admin.display(description='操作', ordering='operate')
    def operate(self, obj):
        url = settings.HOST + "/reset_link/" + str(obj.uid)
        btn_approve = f"""<button onclick="window.location.href='{url}'"
                      class="el-button el-button--success el-button--small">重置链接</button>"""
        return mark_safe(f"<div>{btn_approve}</div>")


class ChangeMoneyAdmin(admin.ModelAdmin):
    list_display = ['user', "last_money", 'money', 'now_money', "change_type", 'create_time']
    list_filter = ("user",)
    date_hierarchy = "create_time"
    list_display_links = None  # 禁用编辑链接

    def has_add_permission(self, request):
        return False

    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        actions = super().get_actions(request)
        if request.user.username[0].upper() != 'J':
            if 'delete_selected' in actions:
                del actions['delete_selected']
        return actions


class AdminModelAdmin(admin.ModelAdmin):
    list_display = ['username', 'is_superuser', 'available_time']
    list_display_links = ('username',)

    def get_fieldsets(self, request, obj=None):
        # 新建用户时显示密码字段，修改时不显示
        if not obj:
            return (
                ('基本信息', {
                    'fields': ('username', 'password')
                }),
                ('权限信息', {
                    'fields': ('available_time',)
                }),
            )
        else:
            return (
                ('基本信息', {
                    'fields': ('username',)
                }),
                ('权限信息', {
                    'fields': ('available_time',)
                }),
            )

    def save_model(self, request, obj, form, change):
        # 只在创建新用户时对密码进行加密
        if not change:
            obj.set_password(obj.password)
            obj.is_staff = True
        super().save_model(request, obj, form, change)

# 注册模型
custom_admin_site.register(models.User, UserAdmin)
custom_admin_site.register(models.ChangeMoney, ChangeMoneyAdmin)
custom_admin_site.register(models.Admin, AdminModelAdmin)

# 设置站点标题
custom_admin_site.site_header = '番摊机器人后台'
custom_admin_site.site_title = '番摊机器人后台'


